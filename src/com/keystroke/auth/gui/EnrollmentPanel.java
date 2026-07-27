package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EnrollmentPanel.java — Premium enrollment panel with custom passphrase,
 * animated progress ring, real-time timing graph, and glassmorphism cards.
 */
public class EnrollmentPanel extends JPanel {

    private final MainWindow mainWindow;

    private String enrollUsername = "";
    private int requiredSamples = 3;
    private int completedSamples = 0;
    private final List<double[]> holdSamples  = new ArrayList<>();
    private final List<double[]> flightSamples = new ArrayList<>();

    // Keystroke capture state
    private final java.util.Map<Integer, Long> pressTimeMap = new java.util.HashMap<>();
    private final List<Double> currentHold = new ArrayList<>();
    private final List<Double> currentFlight = new ArrayList<>();
    private long lastReleaseNano = 0;

    // Widgets
    private JLabel lblTitle;
    private JLabel lblPhrase;
    private JTextField passphraseField;
    private JButton btnLockPhrase;
    private JTextField inputField;
    private JButton btnCapture;
    private JButton btnSave;
    private JButton btnCancel;
    private String lockedPhrase = null;
    private JLabel lblProgress;
    private JLabel lblStatus;
    private JProgressBar sampleProgress;
    private DefaultTableModel timingTableModel;
    private TimingGraphPanel graphPanel;
    private JLabel lblStats;

    // Animation
    private float animPhase = 0f;
    private Timer animTimer;

    public EnrollmentPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 0));
        setOpaque(true);
        buildUI();
        animTimer = new Timer(40, e -> { animPhase += 0.015f; repaint(); });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(StyleManager.bgGradient(getWidth(), getHeight()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        // Subtle animated glow
        float cx = getWidth() * 0.8f + (float) Math.sin(animPhase) * 40;
        float cy = getHeight() * 0.3f + (float) Math.cos(animPhase * 0.7) * 30;
        StyleManager.drawGlow(g2, (int) cx, (int) cy, 70, new Color(255, 120, 0, 10));
        g2.dispose();
    }

    private void buildUI() {
        // ── Top: instructions ────────────────────────────────────
        JPanel top = StyleManager.card();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        lblTitle = StyleManager.label("Enroll", StyleManager.FONT_SUBTITLE, StyleManager.ACCENT);
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lblTitle);
        top.add(Box.createVerticalStrut(6));

        JLabel instruct = StyleManager.label(
                "<html>Set your passphrase below, then type it <b>3 times</b> naturally.<br>" +
                "Use something memorable (8+ chars). Your typing rhythm becomes your password.</html>",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        instruct.setAlignmentX(LEFT_ALIGNMENT);
        top.add(instruct);
        top.add(Box.createVerticalStrut(10));

        // Passphrase row
        JLabel phraseLabel = StyleManager.label("Your Passphrase",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        phraseLabel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(phraseLabel);
        top.add(Box.createVerticalStrut(4));

        JPanel phraseRow = new JPanel();
        phraseRow.setOpaque(false);
        phraseRow.setLayout(new BoxLayout(phraseRow, BoxLayout.X_AXIS));
        phraseRow.setAlignmentX(LEFT_ALIGNMENT);
        passphraseField = StyleManager.textField(28);
        passphraseField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passphraseField.setToolTipText("Enter your custom passphrase (8+ characters)");
        btnLockPhrase = StyleManager.button("  Lock Phrase  ");
        phraseRow.add(passphraseField);
        phraseRow.add(Box.createHorizontalStrut(8));
        phraseRow.add(btnLockPhrase);
        top.add(phraseRow);
        top.add(Box.createVerticalStrut(6));

        lblPhrase = StyleManager.label("Set your passphrase above, then lock it.",
                StyleManager.FONT_MONO_SM, StyleManager.TEXT_DIM);
        lblPhrase.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lblPhrase);
        top.add(Box.createVerticalStrut(10));

        // Typing input
        JLabel typLabel = StyleManager.label("Type here",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        typLabel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(typLabel);
        top.add(Box.createVerticalStrut(4));

        inputField = StyleManager.textField(40);
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        inputField.setAlignmentX(LEFT_ALIGNMENT);
        top.add(inputField);
        top.add(Box.createVerticalStrut(8));

        // Progress bar
        JPanel progRow = new JPanel();
        progRow.setOpaque(false);
        progRow.setLayout(new BoxLayout(progRow, BoxLayout.X_AXIS));
        progRow.setAlignmentX(LEFT_ALIGNMENT);
        sampleProgress = StyleManager.progressBar();
        sampleProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        lblProgress = StyleManager.label("0/3 samples", StyleManager.FONT_TINY, StyleManager.TEXT_SECONDARY);
        progRow.add(sampleProgress);
        progRow.add(Box.createHorizontalStrut(10));
        progRow.add(lblProgress);
        top.add(progRow);
        top.add(Box.createVerticalStrut(8));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(LEFT_ALIGNMENT);
        btnCapture = StyleManager.button("  Capture Sample  ");
        btnSave = StyleManager.button("  Save Profile  ");
        btnSave.setEnabled(false);
        btnCancel = StyleManager.secondaryButton("  Cancel  ");
        btnRow.add(btnCapture);
        btnRow.add(btnSave);
        btnRow.add(btnCancel);
        top.add(btnRow);
        top.add(Box.createVerticalStrut(6));

        lblStatus = StyleManager.label("", StyleManager.FONT_SMALL, StyleManager.TEXT_DIM);
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lblStatus);

        add(top, BorderLayout.NORTH);

        // ── Centre: timing table + graph ─────────────────────────
        JPanel centre = new JPanel(new GridLayout(1, 2, 10, 0));
        centre.setOpaque(false);
        centre.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        timingTableModel = new DefaultTableModel(
                new String[]{"#", "Key", "Hold (ms)", "Flight (ms)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(timingTableModel);
        GUIUtils.styleTable(table);
        centre.add(GUIUtils.darkScrollPane(table));

        graphPanel = new TimingGraphPanel();
        graphPanel.setChartTitle("Live Keystroke Timing");
        centre.add(graphPanel);

        add(centre, BorderLayout.CENTER);

        // ── Bottom: stats ────────────────────────────────────────
        JPanel bottom = StyleManager.card();
        bottom.setLayout(new FlowLayout(FlowLayout.LEFT));
        lblStats = StyleManager.label("Statistics will appear after first sample.",
                StyleManager.FONT_SMALL, StyleManager.TEXT_DIM);
        bottom.add(lblStats);
        add(bottom, BorderLayout.SOUTH);

        // ── Events ───────────────────────────────────────────────
        inputField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKeyPress(e); }
            @Override public void keyReleased(KeyEvent e) { onKeyRelease(e); }
        });

        btnLockPhrase.addActionListener(e -> lockPhrase());
        btnCapture.addActionListener(e -> captureSample());
        btnSave.addActionListener(e -> saveProfile());
        btnCancel.addActionListener(e -> cancel());
    }

    private void lockPhrase() {
        String phrase = passphraseField.getText().trim();
        if (phrase.length() < 8) {
            lblStatus.setForeground(StyleManager.WARNING);
            lblStatus.setText("[!] Passphrase must be at least 8 characters.");
            return;
        }
        lockedPhrase = phrase;
        passphraseField.setEditable(false);
        passphraseField.setBackground(StyleManager.BG_SECONDARY);
        btnLockPhrase.setEnabled(false);
        lblPhrase.setText("[OK] Locked: \"" + lockedPhrase + "\"");
        lblPhrase.setForeground(StyleManager.SUCCESS);
        lblStatus.setForeground(StyleManager.ACCENT);
        lblStatus.setText(">> Now type your passphrase below and click 'Capture Sample'.");
        inputField.requestFocusInWindow();
    }

    // ─── Keystroke capture ───────────────────────────────────────

    private boolean isCharacterKey(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL ||
            code == KeyEvent.VK_ALT || code == KeyEvent.VK_META ||
            code == KeyEvent.VK_CAPS_LOCK || code == KeyEvent.VK_NUM_LOCK ||
            code == KeyEvent.VK_SCROLL_LOCK || code == KeyEvent.VK_ENTER ||
            code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE ||
            code == KeyEvent.VK_TAB || code == KeyEvent.VK_ESCAPE ||
            code == KeyEvent.VK_INSERT || code == KeyEvent.VK_HOME ||
            code == KeyEvent.VK_END || code == KeyEvent.VK_PAGE_UP ||
            code == KeyEvent.VK_PAGE_DOWN ||
            code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN ||
            code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT ||
            (code >= KeyEvent.VK_F1 && code <= KeyEvent.VK_F24) ||
            code == KeyEvent.VK_WINDOWS || code == KeyEvent.VK_CONTEXT_MENU ||
            code == KeyEvent.VK_PRINTSCREEN || code == KeyEvent.VK_PAUSE) {
            return false;
        }
        return true;
    }

    private void onKeyPress(KeyEvent e) {
        if (!isCharacterKey(e)) return;
        int code = e.getKeyCode();
        if (!pressTimeMap.containsKey(code)) {
            long now = System.nanoTime();
            pressTimeMap.put(code, now);
            if (lastReleaseNano > 0) {
                double flight = (now - lastReleaseNano) / 1_000_000.0;
                if (flight < 0) flight = 0;
                currentFlight.add(flight);
            }
        }
    }

    private void onKeyRelease(KeyEvent e) {
        if (!isCharacterKey(e)) return;
        int code = e.getKeyCode();
        Long pressTime = pressTimeMap.remove(code);
        if (pressTime != null) {
            long now = System.nanoTime();
            double hold = (now - pressTime) / 1_000_000.0;
            currentHold.add(hold);
            lastReleaseNano = now;
            double[] h = currentHold.stream().mapToDouble(Double::doubleValue).toArray();
            double[] f = currentFlight.stream().mapToDouble(Double::doubleValue).toArray();
            graphPanel.setTimingData(h, f);
        }
    }

    // ─── Sample capture ──────────────────────────────────────────

    private void captureSample() {
        if (lockedPhrase == null) {
            lblStatus.setForeground(StyleManager.WARNING);
            lblStatus.setText("[!] Lock your passphrase first.");
            return;
        }
        String typed = inputField.getText().trim();
        if (typed.isEmpty()) {
            lblStatus.setForeground(StyleManager.WARNING);
            lblStatus.setText("[!] Type your passphrase before capturing.");
            return;
        }
        if (currentHold.size() < 5) {
            lblStatus.setForeground(StyleManager.WARNING);
            lblStatus.setText("[!] Not enough keystrokes. Type the full passphrase.");
            return;
        }

        double[] holdArr = currentHold.stream().mapToDouble(Double::doubleValue).toArray();
        double[] flightArr = currentFlight.stream().mapToDouble(Double::doubleValue).toArray();
        holdSamples.add(holdArr);
        flightSamples.add(flightArr);
        completedSamples++;

        // Update table
        timingTableModel.setRowCount(0);
        for (int i = 0; i < holdArr.length; i++) {
            String key = (i < lockedPhrase.length()) ?
                    (lockedPhrase.charAt(i) == ' ' ? "SPC" : String.valueOf(lockedPhrase.charAt(i))) : "?";
            String fVal = (i < flightArr.length) ? String.format("%.1f", flightArr[i]) : "—";
            timingTableModel.addRow(new Object[]{i + 1, key, String.format("%.1f", holdArr[i]), fVal});
        }

        sampleProgress.setValue(completedSamples * 100 / requiredSamples);
        lblProgress.setText(completedSamples + "/" + requiredSamples + " samples");

        double holdMean = KeystrokeCapture.calculateMean(holdArr);
        double holdStd = KeystrokeCapture.calculateStdDev(holdArr);
        double flightMean = KeystrokeCapture.calculateMean(flightArr);
        double flightStd = KeystrokeCapture.calculateStdDev(flightArr);
        lblStats.setForeground(StyleManager.TEXT_PRIMARY);
        lblStats.setText(String.format(
                "Sample %d — Hold: μ=%.1fms σ=%.1fms  |  Flight: μ=%.1fms σ=%.1fms",
                completedSamples, holdMean, holdStd, flightMean, flightStd));

        if (completedSamples >= requiredSamples) {
            btnSave.setEnabled(true);
            lblStatus.setForeground(StyleManager.SUCCESS);
            lblStatus.setText("[OK] " + requiredSamples + " samples captured! Click 'Save Profile'.");
        } else {
            lblStatus.setForeground(StyleManager.ACCENT);
            lblStatus.setText(">> Sample " + completedSamples + " captured. Type again.");
        }
        resetInput();
    }

    private void resetInput() {
        inputField.setText("");
        currentHold.clear();
        currentFlight.clear();
        pressTimeMap.clear();
        lastReleaseNano = 0;
        inputField.requestFocusInWindow();
    }

    private void saveProfile() {
        if (holdSamples.isEmpty()) return;
        KeystrokeProfile profile = new KeystrokeProfile(enrollUsername);
        profile.setPhrase(lockedPhrase);
        profile.buildProfile(holdSamples, flightSamples);

        String filename = enrollUsername + SystemConstants.PROFILE_EXTENSION;
        mainWindow.getFileManager().saveUserProfile(profile, filename);

        // Check if mouse dynamics enrollment is enabled
        ConfigManager config = mainWindow.getConfigManager();
        if (config.getBoolean("enable_mouse_dynamics", false)) {
            GUIUtils.showInfo(this,
                    "Keystroke profile saved for '" + enrollUsername + "'!\n\n" +
                    "Next: Mouse Dynamics Enrollment\n" +
                    "Click targets and move your mouse to capture\n" +
                    "your mouse behavioral biometrics.",
                    "[OK] Keystroke Enrollment Complete");

            mainWindow.getMouseEnrollmentPanel().startEnrollment(enrollUsername);
            mainWindow.showPanel("mouseEnroll");
        } else {
            GUIUtils.showInfo(this,
                    "Profile saved for '" + enrollUsername + "'!\n\n" +
                    "Passphrase: \"" + lockedPhrase + "\"\n" +
                    "Samples: " + completedSamples + "\n" +
                    "Avg Hold: " + String.format("%.1f ms", profile.getAverageHoldTime()) + "\n" +
                    "Avg Flight: " + String.format("%.1f ms", profile.getAverageFlightTime()),
                    "[OK] Enrollment Complete");

            mainWindow.setCurrentUser(null);
            mainWindow.showPanel("login");
        }
    }

    private void cancel() {
        if (completedSamples > 0) {
            if (!GUIUtils.confirm(this, "Discard " + completedSamples + " captured samples?",
                    "Cancel Enrollment")) return;
        }
        mainWindow.showPanel("login");
    }

    public void startEnrollment(String username) {
        this.enrollUsername = username;
        this.lockedPhrase = null;
        completedSamples = 0;
        holdSamples.clear();
        flightSamples.clear();
        resetInput();
        timingTableModel.setRowCount(0);
        graphPanel.clear();
        btnSave.setEnabled(false);
        sampleProgress.setValue(0);
        lblProgress.setText("0/" + requiredSamples + " samples");
        lblTitle.setText("Enroll: " + username);
        lblStatus.setText("Set your passphrase above, then lock it.");
        lblStatus.setForeground(StyleManager.TEXT_DIM);
        lblStats.setText("Statistics will appear after first sample.");
        lblStats.setForeground(StyleManager.TEXT_DIM);
        passphraseField.setText("");
        passphraseField.setEditable(true);
        passphraseField.setBackground(StyleManager.BG_INPUT);
        btnLockPhrase.setEnabled(true);
        lblPhrase.setText("Set your passphrase above, then lock it.");
        lblPhrase.setForeground(StyleManager.TEXT_DIM);
        passphraseField.requestFocusInWindow();
    }
}
