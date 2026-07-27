package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MouseEnrollmentPanel.java — Interactive mouse dynamics enrollment panel.
 * Users click animated target dots, move their mouse naturally, and scroll
 * to build a behavioral biometric profile of their mouse usage patterns.
 *
 * Phase 2 Enhancement — Mouse Dynamics Biometrics GUI
 */
public class MouseEnrollmentPanel extends JPanel {

    private final MainWindow mainWindow;
    private String enrollUsername = "";

    private MouseDynamicsCapture capture;
    private final List<Point> targets = new ArrayList<>();
    private int currentTargetIndex = 0;
    private static final int TOTAL_TARGETS = 12;
    private final Random random = new Random();

    // UI state
    private boolean enrollmentComplete = false;
    private float animPhase = 0f;
    private Timer animTimer;

    // Widgets
    private JLabel lblTitle;
    private JLabel lblStatus;
    private JProgressBar progressBar;
    private JLabel lblProgress;
    private JButton btnSave;
    private JButton btnSkip;
    private JPanel captureArea;

    // Stats labels
    private JLabel lblClicks;
    private JLabel lblMoves;
    private JLabel lblScrolls;

    public MouseEnrollmentPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 0));
        setOpaque(true);
        buildUI();

        animTimer = new Timer(30, e -> {
            animPhase += 0.03f;
            if (captureArea != null) captureArea.repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(StyleManager.bgGradient(getWidth(), getHeight()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private void buildUI() {
        // ── Top: instructions ────────────────────────────────────
        JPanel top = StyleManager.card();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        lblTitle = StyleManager.label("Mouse Dynamics Enrollment",
                StyleManager.FONT_SUBTITLE, StyleManager.ACCENT);
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lblTitle);
        top.add(Box.createVerticalStrut(6));

        JLabel instruct = StyleManager.label(
                "<html>Click each glowing target as it appears. Move your mouse naturally between clicks.<br>" +
                "Scroll up and down when prompted. Your mouse behavior becomes part of your biometric profile.</html>",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        instruct.setAlignmentX(LEFT_ALIGNMENT);
        top.add(instruct);
        top.add(Box.createVerticalStrut(10));

        // Progress
        JPanel progRow = new JPanel();
        progRow.setOpaque(false);
        progRow.setLayout(new BoxLayout(progRow, BoxLayout.X_AXIS));
        progRow.setAlignmentX(LEFT_ALIGNMENT);
        progressBar = StyleManager.progressBar();
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        lblProgress = StyleManager.label("0/" + TOTAL_TARGETS + " targets",
                StyleManager.FONT_TINY, StyleManager.TEXT_SECONDARY);
        progRow.add(progressBar);
        progRow.add(Box.createHorizontalStrut(10));
        progRow.add(lblProgress);
        top.add(progRow);
        top.add(Box.createVerticalStrut(8));

        // Stats row
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(LEFT_ALIGNMENT);
        lblClicks = StyleManager.label("Clicks: 0", StyleManager.FONT_TINY, StyleManager.ACCENT);
        lblMoves = StyleManager.label("Moves: 0", StyleManager.FONT_TINY, StyleManager.ACCENT);
        lblScrolls = StyleManager.label("Scrolls: 0", StyleManager.FONT_TINY, StyleManager.ACCENT);
        statsRow.add(lblClicks);
        statsRow.add(lblMoves);
        statsRow.add(lblScrolls);
        top.add(statsRow);
        top.add(Box.createVerticalStrut(6));

        lblStatus = StyleManager.label("Click the glowing target to begin.",
                StyleManager.FONT_SMALL, StyleManager.TEXT_DIM);
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lblStatus);
        top.add(Box.createVerticalStrut(6));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(LEFT_ALIGNMENT);
        btnSave = StyleManager.button("  Save Mouse Profile  ");
        btnSave.setEnabled(false);
        btnSkip = StyleManager.secondaryButton("  Skip  ");
        btnRow.add(btnSave);
        btnRow.add(btnSkip);
        top.add(btnRow);

        add(top, BorderLayout.NORTH);

        // ── Centre: capture area ────────────────────────────────
        captureArea = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawCaptureArea((Graphics2D) g.create());
            }
        };
        captureArea.setBackground(StyleManager.BG_CARD);
        captureArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 14, 8, 14),
                BorderFactory.createLineBorder(StyleManager.BORDER, 1, true)));
        captureArea.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        add(captureArea, BorderLayout.CENTER);

        // ── Events ───────────────────────────────────────────────
        captureArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (capture != null) capture.mousePressed(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (capture != null) {
                    capture.mouseReleased(e);
                    checkTargetHit(e.getPoint());
                    updateStats();
                }
            }
        });
        captureArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (capture != null) {
                    capture.mouseMoved(e);
                    updateStats();
                }
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (capture != null) capture.mouseDragged(e);
            }
        });
        captureArea.addMouseWheelListener(e -> {
            if (capture != null) {
                capture.mouseWheelMoved(e);
                updateStats();
            }
        });

        btnSave.addActionListener(e -> saveProfile());
        btnSkip.addActionListener(e -> {
            if (capture != null) capture.stopCapture();
            mainWindow.showPanel("login");
        });
    }

    private void drawCaptureArea(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = captureArea.getWidth(), h = captureArea.getHeight();

        // Draw subtle grid
        g2.setColor(new Color(255, 140, 0, 8));
        for (int x = 0; x < w; x += 40) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += 40) {
            g2.drawLine(0, y, w, y);
        }

        // Draw completed targets (dimmed)
        for (int i = 0; i < currentTargetIndex && i < targets.size(); i++) {
            Point p = targets.get(i);
            g2.setColor(new Color(255, 160, 40, 30));
            g2.fillOval(p.x - 12, p.y - 12, 24, 24);
            g2.setColor(new Color(255, 160, 40, 60));
            g2.drawOval(p.x - 12, p.y - 12, 24, 24);
        }

        // Draw current target (animated glow)
        if (currentTargetIndex < targets.size() && !enrollmentComplete) {
            Point target = targets.get(currentTargetIndex);
            float pulse = (float) (Math.sin(animPhase * 3) * 0.3 + 0.7);
            int glowSize = (int) (30 + pulse * 15);

            // Outer glow
            g2.setColor(new Color(255, 140, 0, (int) (40 * pulse)));
            g2.fillOval(target.x - glowSize, target.y - glowSize, glowSize * 2, glowSize * 2);

            // Inner circle
            g2.setColor(StyleManager.ACCENT);
            g2.fillOval(target.x - 14, target.y - 14, 28, 28);

            // Cross-hair
            g2.setColor(StyleManager.BG_PRIMARY);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(target.x - 6, target.y, target.x + 6, target.y);
            g2.drawLine(target.x, target.y - 6, target.x, target.y + 6);

            // Label
            g2.setFont(StyleManager.FONT_TINY);
            g2.setColor(StyleManager.TEXT_SECONDARY);
            String label = "Target " + (currentTargetIndex + 1) + "/" + TOTAL_TARGETS;
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, target.x - fm.stringWidth(label) / 2, target.y + 30);
        }

        // Scroll prompt
        if (currentTargetIndex >= TOTAL_TARGETS / 2 && currentTargetIndex < TOTAL_TARGETS) {
            g2.setFont(StyleManager.FONT_SMALL);
            g2.setColor(StyleManager.TEXT_DIM);
            g2.drawString("v^ Also scroll up/down while clicking targets", 20, h - 20);
        }

        if (enrollmentComplete) {
            g2.setFont(StyleManager.FONT_SUBTITLE);
            g2.setColor(StyleManager.SUCCESS);
            String msg = "[OK] Mouse Capture Complete!";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, w / 2 - fm.stringWidth(msg) / 2, h / 2);
        }

        g2.dispose();
    }

    private void checkTargetHit(Point click) {
        if (enrollmentComplete || currentTargetIndex >= targets.size()) return;

        Point target = targets.get(currentTargetIndex);
        double dist = click.distance(target);

        if (dist < 35) { // Hit radius
            currentTargetIndex++;
            progressBar.setValue(currentTargetIndex * 100 / TOTAL_TARGETS);
            lblProgress.setText(currentTargetIndex + "/" + TOTAL_TARGETS + " targets");

            if (currentTargetIndex >= TOTAL_TARGETS) {
                enrollmentComplete = true;
                btnSave.setEnabled(true);
                lblStatus.setForeground(StyleManager.SUCCESS);
                lblStatus.setText("[OK] All targets hit! Click 'Save Mouse Profile'.");
            } else {
                lblStatus.setForeground(StyleManager.ACCENT);
                lblStatus.setText(">> Target hit! Click the next glowing target.");
            }
        }
    }

    private void updateStats() {
        if (capture == null) return;
        lblClicks.setText("Clicks: " + capture.getClickCount());
        lblMoves.setText("Moves: " + capture.getMoveCount());
        lblScrolls.setText("Scrolls: " + capture.getScrollCount());
    }

    private void saveProfile() {
        if (capture == null) return;
        capture.stopCapture();

        MouseDynamicsProfile profile = capture.getProfile();
        if (profile == null) {
            GUIUtils.showWarning(this, "Not enough mouse data captured. Try again.");
            return;
        }

        mainWindow.getFileManager().saveMouseProfile(enrollUsername, profile);

        GUIUtils.showInfo(this,
                "Mouse profile saved for '" + enrollUsername + "'!\n\n" +
                "Avg Click Duration: " + String.format("%.1f ms", profile.getAvgClickDuration()) + "\n" +
                "Avg Move Speed: " + String.format("%.3f px/ms", profile.getAvgMoveSpeed()) + "\n" +
                "Total Clicks: " + profile.getTotalClicks() + "\n" +
                "Total Scrolls: " + profile.getTotalScrollEvents(),
                "[OK] Mouse Enrollment Complete");

        mainWindow.showPanel("login");
    }

    /**
     * Generates random target positions within the capture area.
     */
    private void generateTargets() {
        targets.clear();
        // Delay to ensure capture area has a size
        SwingUtilities.invokeLater(() -> {
            int w = Math.max(captureArea.getWidth() - 60, 200);
            int h = Math.max(captureArea.getHeight() - 60, 200);
            for (int i = 0; i < TOTAL_TARGETS; i++) {
                int x = 30 + random.nextInt(w);
                int y = 30 + random.nextInt(h);
                targets.add(new Point(x, y));
            }
            captureArea.repaint();
        });
    }

    /**
     * Starts the mouse enrollment process for a user.
     */
    public void startEnrollment(String username) {
        this.enrollUsername = username;
        this.currentTargetIndex = 0;
        this.enrollmentComplete = false;

        lblTitle.setText("Mouse Enrollment: " + username);
        lblStatus.setText("Click the glowing target to begin.");
        lblStatus.setForeground(StyleManager.TEXT_DIM);
        progressBar.setValue(0);
        lblProgress.setText("0/" + TOTAL_TARGETS + " targets");
        btnSave.setEnabled(false);
        lblClicks.setText("Clicks: 0");
        lblMoves.setText("Moves: 0");
        lblScrolls.setText("Scrolls: 0");

        capture = new MouseDynamicsCapture(username);
        // We manually forward events from the captureArea listeners
        // so we don't attach capture directly to the panel

        generateTargets();
    }
}
