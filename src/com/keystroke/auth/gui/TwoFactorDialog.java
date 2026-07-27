package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * TwoFactorDialog.java — Premium styled OTP verification dialog with
 * animated circular countdown timer, matching the warm orange dark theme.
 *
 * Displays the generated OTP, provides an input field for verification,
 * and counts down from 30 seconds with a visual arc animation.
 *
 * Phase 2 Enhancement — Two-Factor Authentication GUI
 */
public class TwoFactorDialog extends JDialog {

    private final String otp;
    private final TwoFactorAuth twoFactorAuth;
    private boolean verified = false;

    private JTextField otpInput;
    private JLabel lblTimer;
    private JLabel lblStatus;
    private JPanel countdownPanel;
    private Timer countdownTimer;
    private int remainingSeconds;

    /**
     * Constructs the 2FA dialog.
     *
     * @param parent        the parent frame
     * @param twoFactorAuth the TwoFactorAuth instance with an active OTP
     * @param otp           the generated OTP to display
     */
    public TwoFactorDialog(JFrame parent, TwoFactorAuth twoFactorAuth, String otp) {
        super(parent, "Two-Factor Authentication", true);
        this.otp = otp;
        this.twoFactorAuth = twoFactorAuth;
        this.remainingSeconds = SystemConstants.OTP_EXPIRY_SECONDS;

        setSize(420, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
        setUndecorated(true);
        getRootPane().setBorder(BorderFactory.createLineBorder(StyleManager.BORDER, 2));

        buildUI();
        startCountdown();
    }

    private void buildUI() {
        JPanel main = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(StyleManager.bgGradient(getWidth(), getHeight()));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Subtle glow
                StyleManager.drawGlow(g2, getWidth() / 2, 120, 80,
                        new Color(255, 140, 0, 15));
                g2.dispose();
            }
        };
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        // Title
        JLabel title = StyleManager.label("[2FA]  Two-Factor Verification",
                StyleManager.FONT_SUBTITLE, StyleManager.ACCENT);
        title.setAlignmentX(CENTER_ALIGNMENT);
        main.add(title);
        main.add(Box.createVerticalStrut(8));

        JLabel subtitle = StyleManager.label(
                "Enter the OTP code shown below to complete authentication",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        main.add(subtitle);
        main.add(Box.createVerticalStrut(20));

        // OTP Display
        JPanel otpDisplay = StyleManager.card();
        otpDisplay.setLayout(new BoxLayout(otpDisplay, BoxLayout.Y_AXIS));
        otpDisplay.setMaximumSize(new Dimension(360, 80));
        otpDisplay.setAlignmentX(CENTER_ALIGNMENT);

        JLabel otpLabel = StyleManager.label("Your OTP Code",
                StyleManager.FONT_SMALL, StyleManager.TEXT_DIM);
        otpLabel.setAlignmentX(CENTER_ALIGNMENT);
        otpDisplay.add(otpLabel);
        otpDisplay.add(Box.createVerticalStrut(4));

        // Large monospaced OTP with letter spacing
        JLabel otpValue = new JLabel(formatOTP(otp));
        otpValue.setFont(new Font("Consolas", Font.BOLD, 36));
        otpValue.setForeground(StyleManager.ACCENT);
        otpValue.setAlignmentX(CENTER_ALIGNMENT);
        otpDisplay.add(otpValue);

        main.add(otpDisplay);
        main.add(Box.createVerticalStrut(16));

        // Countdown circle
        countdownPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawCountdown((Graphics2D) g.create());
            }
        };
        countdownPanel.setOpaque(false);
        countdownPanel.setPreferredSize(new Dimension(100, 100));
        countdownPanel.setMaximumSize(new Dimension(100, 100));
        countdownPanel.setAlignmentX(CENTER_ALIGNMENT);
        main.add(countdownPanel);
        main.add(Box.createVerticalStrut(16));

        // Input field
        JLabel inputLabel = StyleManager.label("Enter OTP",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        inputLabel.setAlignmentX(CENTER_ALIGNMENT);
        main.add(inputLabel);
        main.add(Box.createVerticalStrut(4));

        otpInput = StyleManager.textField(10);
        otpInput.setFont(new Font("Consolas", Font.BOLD, 24));
        otpInput.setHorizontalAlignment(JTextField.CENTER);
        otpInput.setMaximumSize(new Dimension(200, 50));
        otpInput.setAlignmentX(CENTER_ALIGNMENT);
        main.add(otpInput);
        main.add(Box.createVerticalStrut(8));

        // Status
        lblStatus = StyleManager.label(" ", StyleManager.FONT_SMALL, StyleManager.TEXT_DIM);
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);
        main.add(lblStatus);
        main.add(Box.createVerticalStrut(12));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(CENTER_ALIGNMENT);

        JButton btnVerify = StyleManager.button("  Verify  ");
        JButton btnCancel = StyleManager.secondaryButton("  Cancel  ");

        btnVerify.addActionListener(e -> verifyOTP());
        btnCancel.addActionListener(e -> {
            verified = false;
            stopCountdown();
            dispose();
        });

        // Enter key to verify
        otpInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) verifyOTP();
            }
        });

        btnRow.add(btnVerify);
        btnRow.add(btnCancel);
        main.add(btnRow);

        setContentPane(main);
        otpInput.requestFocusInWindow();
    }

    private void drawCountdown(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = 80;
        int x = (countdownPanel.getWidth() - size) / 2;
        int y = (countdownPanel.getHeight() - size) / 2;

        // Background arc
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(60, 45, 30));
        g2.drawArc(x, y, size, size, 0, 360);

        // Progress arc
        double fraction = (double) remainingSeconds / SystemConstants.OTP_EXPIRY_SECONDS;
        int arc = (int) (fraction * 360);
        Color arcColor = remainingSeconds > 10 ? StyleManager.ACCENT :
                          remainingSeconds > 5 ? StyleManager.WARNING : StyleManager.DANGER;
        g2.setColor(arcColor);
        g2.drawArc(x, y, size, size, 90, arc);

        // Glow
        g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(arcColor.getRed(), arcColor.getGreen(), arcColor.getBlue(), 25));
        g2.drawArc(x, y, size, size, 90, arc);

        // Center text
        g2.setFont(new Font("Consolas", Font.BOLD, 22));
        g2.setColor(arcColor);
        String text = String.valueOf(remainingSeconds);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + size / 2 - fm.stringWidth(text) / 2,
                y + size / 2 + fm.getAscent() / 2 - 2);

        g2.setFont(StyleManager.FONT_TINY);
        g2.setColor(StyleManager.TEXT_DIM);
        String label = "seconds";
        fm = g2.getFontMetrics();
        g2.drawString(label, x + size / 2 - fm.stringWidth(label) / 2, y + size / 2 + 18);

        g2.dispose();
    }

    private void startCountdown() {
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds = twoFactorAuth.getRemainingSeconds();
            countdownPanel.repaint();
            if (remainingSeconds <= 0) {
                stopCountdown();
                lblStatus.setForeground(StyleManager.DANGER);
                lblStatus.setText("[!] OTP expired! Authentication failed.");
                otpInput.setEnabled(false);
                // Auto-close after 2 seconds
                Timer closeTimer = new Timer(2000, ev -> {
                    verified = false;
                    dispose();
                });
                closeTimer.setRepeats(false);
                closeTimer.start();
            }
        });
        countdownTimer.start();
    }

    private void stopCountdown() {
        if (countdownTimer != null) countdownTimer.stop();
    }

    private void verifyOTP() {
        String input = otpInput.getText().trim();
        if (input.isEmpty()) {
            lblStatus.setForeground(StyleManager.WARNING);
            lblStatus.setText("[!] Enter the OTP code.");
            return;
        }
        if (twoFactorAuth.validateOTP(input)) {
            verified = true;
            stopCountdown();
            lblStatus.setForeground(StyleManager.SUCCESS);
            lblStatus.setText("[OK] OTP verified successfully!");
            Timer closeTimer = new Timer(800, e -> dispose());
            closeTimer.setRepeats(false);
            closeTimer.start();
        } else {
            if (twoFactorAuth.isExpired()) {
                lblStatus.setForeground(StyleManager.DANGER);
                lblStatus.setText("[X] OTP has expired.");
                otpInput.setEnabled(false);
            } else {
                lblStatus.setForeground(StyleManager.DANGER);
                lblStatus.setText("[X] Incorrect OTP. Try again.");
                otpInput.setText("");
                otpInput.requestFocusInWindow();
            }
        }
    }

    private String formatOTP(String otp) {
        // Add spaces between digits for readability
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otp.length(); i++) {
            if (i > 0) sb.append("  ");
            sb.append(otp.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Returns whether the OTP was successfully verified.
     */
    public boolean isVerified() {
        return verified;
    }
}
