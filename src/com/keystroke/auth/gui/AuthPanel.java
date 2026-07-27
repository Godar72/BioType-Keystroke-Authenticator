package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AuthPanel.java — Premium biometric authentication panel with animated
 * circular confidence gauge, real-time timing graph, glassmorphism results,
 * two-factor authentication, dual-score display, decision banner with
 * pulse animation, and timing statistics comparison.
 *
 * Enhanced: Right side now shows CircularGaugePanel, stat cards,
 * decision banner, and timing stats vs enrolled profile.
 */
public class AuthPanel extends JPanel {

    private final MainWindow mainWindow;

    private String authUsername = "";
    private final java.util.Map<Integer, Long> pressTimeMap = new java.util.HashMap<>();
    private final List<Double> currentHold = new ArrayList<>();
    private final List<Double> currentFlight = new ArrayList<>();
    private long lastReleaseNano = 0;

    // Mouse capture for passive mouse dynamics during auth
    private MouseDynamicsCapture mouseCapture;

    // Reference profile data (enrolled)
    private double refAvgHold = 0, refAvgFlight = 0;

    // Widgets — top
    private JLabel lblTitle;
    private JLabel lblPhrase;
    private JTextField inputField;
    private JButton btnAuthenticate;
    private JButton btnTryAgain;
    private JButton btnLogout;
    private JLabel lblStatus;
    private TimingGraphPanel graphPanel;

    // Right side — Enhancement 2
    private CircularGaugePanel gaugePanel;
    private JLabel lblKeystrokeScoreVal;
    private JLabel lblMouseScoreVal;
    private JPanel decisionBanner;
    private JLabel lblDecision;
    private JLabel lblTimingHold, lblTimingFlight, lblTimingDeviation;

    // Decision banner pulse
    private Timer pulseTimer;
    private float pulseAlpha = 0f;
    private boolean pulseUp = true;
    private Color bannerColor = StyleManager.TEXT_DIM;

    public AuthPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setOpaque(true);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        buildUI();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(StyleManager.bgGradient(getWidth(), getHeight()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        StyleManager.drawDotGrid(g2, getWidth(), getHeight(), 50);
        g2.dispose();
    }

    private void buildUI() {
        // ══════════════════════════════════════════════════════════
        //  TOP CARD: keystroke capture area (left-aligned)
        // ══════════════════════════════════════════════════════════
        JPanel top = StyleManager.card();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        lblTitle = StyleManager.label("Authenticate", StyleManager.FONT_SUBTITLE, StyleManager.ACCENT);
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lblTitle);
        top.add(Box.createVerticalStrut(6));

        JLabel instruct = StyleManager.label(
                "Type your passphrase naturally. Your typing rhythm is being captured.",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        instruct.setAlignmentX(LEFT_ALIGNMENT);
        top.add(instruct);
        top.add(Box.createVerticalStrut(8));

        lblPhrase = StyleManager.label("\"...\"", StyleManager.FONT_MONO, StyleManager.WARNING);
        lblPhrase.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lblPhrase);
        top.add(Box.createVerticalStrut(10));

        inputField = StyleManager.textField(40);
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        inputField.setAlignmentX(LEFT_ALIGNMENT);
        top.add(inputField);
        top.add(Box.createVerticalStrut(8));

        lblStatus = StyleManager.label("Start typing...", StyleManager.FONT_SMALL, StyleManager.TEXT_DIM);
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lblStatus);
        top.add(Box.createVerticalStrut(6));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(LEFT_ALIGNMENT);
        btnAuthenticate = StyleManager.button("  Authenticate  ");
        btnTryAgain = StyleManager.secondaryButton("  Try Again  ");
        btnLogout = StyleManager.secondaryButton("  Back to Login  ");
        btnRow.add(btnAuthenticate);
        btnRow.add(btnTryAgain);
        btnRow.add(btnLogout);
        top.add(btnRow);

        add(top, BorderLayout.NORTH);

        // ══════════════════════════════════════════════════════════
        //  CENTRE: graph (left) + results panel (right)
        // ══════════════════════════════════════════════════════════
        JPanel centre = new JPanel(new GridLayout(1, 2, 12, 0));
        centre.setOpaque(false);

        // LEFT: Timing graph
        graphPanel = new TimingGraphPanel();
        graphPanel.setChartTitle("Live Keystroke Timing");
        centre.add(graphPanel);

        // RIGHT: Results panel — Enhancement 2
        JPanel rightPanel = new JPanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        // ── Circular gauge ───────────────────────────────────────
        gaugePanel = new CircularGaugePanel();
        gaugePanel.setAlignmentX(CENTER_ALIGNMENT);
        gaugePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        rightPanel.add(gaugePanel);
        rightPanel.add(Box.createVerticalStrut(8));

        // ── Two score cards side by side ──────────────────────────
        JPanel scoresRow = new JPanel(new GridLayout(1, 2, 8, 0));
        scoresRow.setOpaque(false);
        scoresRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        scoresRow.setAlignmentX(CENTER_ALIGNMENT);

        // Keystroke score card
        JPanel ksCard = createScoreCard("KEYSTROKE SCORE", StyleManager.ACCENT);
        lblKeystrokeScoreVal = (JLabel) ((JPanel) ksCard).getComponent(0);
        scoresRow.add(ksCard);

        // Mouse score card
        JPanel msCard = createScoreCard("MOUSE SCORE", StyleManager.TEXT_DIM);
        lblMouseScoreVal = (JLabel) ((JPanel) msCard).getComponent(0);
        scoresRow.add(msCard);

        rightPanel.add(scoresRow);
        rightPanel.add(Box.createVerticalStrut(8));

        // ── Decision banner with pulse animation ─────────────────
        decisionBanner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int alpha = (int) (pulseAlpha * 255);
                g2.setColor(new Color(bannerColor.getRed(), bannerColor.getGreen(),
                        bannerColor.getBlue(), Math.max(15, alpha / 4)));
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.setColor(new Color(bannerColor.getRed(), bannerColor.getGreen(),
                        bannerColor.getBlue(), Math.max(20, alpha / 2)));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.dispose();
            }
        };
        decisionBanner.setOpaque(false);
        decisionBanner.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 8));
        decisionBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        decisionBanner.setAlignmentX(CENTER_ALIGNMENT);

        lblDecision = StyleManager.label("", StyleManager.FONT_SUBTITLE, StyleManager.TEXT_DIM);
        decisionBanner.add(lblDecision);
        rightPanel.add(decisionBanner);
        rightPanel.add(Box.createVerticalStrut(8));

        // ── Timing statistics panel ──────────────────────────────
        JPanel timingStats = StyleManager.card();
        timingStats.setLayout(new BoxLayout(timingStats, BoxLayout.Y_AXIS));
        timingStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        timingStats.setAlignmentX(CENTER_ALIGNMENT);

        JLabel statsTitle = StyleManager.label("TIMING STATISTICS",
                StyleManager.FONT_STAT_LABEL, StyleManager.CYAN_DIM);
        statsTitle.setAlignmentX(LEFT_ALIGNMENT);
        timingStats.add(statsTitle);
        timingStats.add(Box.createVerticalStrut(4));

        lblTimingHold = StyleManager.label("Mean Hold: — vs Enrolled: —",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        lblTimingHold.setAlignmentX(LEFT_ALIGNMENT);
        timingStats.add(lblTimingHold);

        lblTimingFlight = StyleManager.label("Mean Flight: — vs Enrolled: —",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        lblTimingFlight.setAlignmentX(LEFT_ALIGNMENT);
        timingStats.add(lblTimingFlight);

        lblTimingDeviation = StyleManager.label("Deviation: —",
                StyleManager.FONT_SMALL, StyleManager.TEXT_DIM);
        lblTimingDeviation.setAlignmentX(LEFT_ALIGNMENT);
        timingStats.add(lblTimingDeviation);

        rightPanel.add(timingStats);
        rightPanel.add(Box.createVerticalGlue());

        centre.add(rightPanel);
        add(centre, BorderLayout.CENTER);

        // ── Events ───────────────────────────────────────────────
        inputField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKeyPress(e); }
            @Override public void keyReleased(KeyEvent e) { onKeyRelease(e); }
        });

        btnAuthenticate.addActionListener(e -> doAuthenticate());
        inputField.addActionListener(e -> doAuthenticate()); // Enter key triggers authentication
        btnTryAgain.addActionListener(e -> resetCapture());
        btnLogout.addActionListener(e -> {
            mainWindow.setCurrentUser(null);
            mainWindow.showPanel("login");
        });

        // Pulse animation timer for decision banner
        pulseTimer = new Timer(40, e -> {
            if (pulseUp) {
                pulseAlpha += 0.04f;
                if (pulseAlpha >= 1f) { pulseAlpha = 1f; pulseUp = false; }
            } else {
                pulseAlpha -= 0.03f;
                if (pulseAlpha <= 0.3f) { pulseAlpha = 0.3f; pulseUp = true; }
            }
            decisionBanner.repaint();
        });
    }

    // ─── Score card factory ─────────────────────────────────────

    private JPanel createScoreCard(String label, Color accentColor) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(StyleManager.BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(),
                        accentColor.getBlue(), 35));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                // Top accent line
                g2.setColor(accentColor);
                g2.fillRect(4, 0, w - 8, 2);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));

        JLabel valLabel = StyleManager.label("—",
                new Font("Consolas", Font.BOLD, 20), accentColor);
        valLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(valLabel);

        JLabel descLabel = StyleManager.label(label, StyleManager.FONT_STAT_LABEL, StyleManager.TEXT_DIM);
        descLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(descLabel);

        return card;
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
            lblStatus.setForeground(StyleManager.ACCENT);
            lblStatus.setText(">> Capturing keystrokes... (" + (currentHold.size() + 1) + " keys)");
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

    // ─── Authentication ──────────────────────────────────────────

    private void doAuthenticate() {
        if (currentHold.size() < 5) {
            lblStatus.setForeground(StyleManager.WARNING);
            lblStatus.setText("[!] Type more before authenticating.");
            return;
        }

        double[] holdArr = currentHold.stream().mapToDouble(Double::doubleValue).toArray();
        double[] flightArr = currentFlight.stream().mapToDouble(Double::doubleValue).toArray();
        AuthenticationEngine engine = new AuthenticationEngine(mainWindow.getFileManager());

        // Set feature flags from config
        ConfigManager config = mainWindow.getConfigManager();
        engine.setMLThresholdEnabled(config.getBoolean("enable_ml_threshold", false));
        engine.setMouseDynamicsEnabled(config.getBoolean("enable_mouse_dynamics", false));

        // Get mouse profile if capturing
        MouseDynamicsProfile mouseProfile = null;
        if (mouseCapture != null && config.getBoolean("enable_mouse_dynamics", false)) {
            mouseCapture.stopCapture();
            mouseProfile = mouseCapture.getProfile();
        }

        try {
            AuthResult result = engine.authenticate(authUsername, holdArr, flightArr, mouseProfile);
            showResult(result, config, holdArr, flightArr);
        } catch (AuthenticationException ex) {
            GUIUtils.showError(this, ex.getMessage());
            lblStatus.setForeground(StyleManager.DANGER);
            lblStatus.setText("[X] " + ex.getMessage());
        }
    }

    private void showResult(AuthResult result, ConfigManager config,
                            double[] holdArr, double[] flightArr) {
        double conf = result.getConfidenceScore();

        // ── Update circular gauge ────────────────────────────────
        gaugePanel.setValue(conf);
        gaugePanel.setStatusText("BIOMETRIC CONFIDENCE");

        // ── Update keystroke score card ───────────────────────────
        lblKeystrokeScoreVal.setForeground(StyleManager.confidenceColor(conf));
        lblKeystrokeScoreVal.setText(String.format("%.1f%%", conf));

        // ── Update mouse score card ──────────────────────────────
        if (result.getMouseScore() >= 0) {
            lblMouseScoreVal.setForeground(StyleManager.ACCENT);
            lblMouseScoreVal.setText(String.format("%.1f%%", result.getMouseScore()));
        } else {
            lblMouseScoreVal.setText("N/A");
            lblMouseScoreVal.setForeground(StyleManager.TEXT_DIM);
        }

        // ── Update timing statistics ─────────────────────────────
        double curAvgHold = mean(holdArr);
        double curAvgFlight = mean(flightArr);
        lblTimingHold.setText(String.format("Mean Hold: %.1f ms  vs  Enrolled: %.1f ms",
                curAvgHold, refAvgHold));
        lblTimingFlight.setText(String.format("Mean Flight: %.1f ms  vs  Enrolled: %.1f ms",
                curAvgFlight, refAvgFlight));

        double holdDev = refAvgHold > 0 ? Math.abs(curAvgHold - refAvgHold) / refAvgHold * 100 : 0;
        double flightDev = refAvgFlight > 0 ? Math.abs(curAvgFlight - refAvgFlight) / refAvgFlight * 100 : 0;
        double avgDev = (holdDev + flightDev) / 2;
        Color devColor = avgDev < 20 ? StyleManager.SUCCESS :
                          avgDev < 40 ? StyleManager.WARNING : StyleManager.DANGER;
        lblTimingDeviation.setForeground(devColor);
        lblTimingDeviation.setText(String.format("Deviation from profile: %.1f%%", avgDev));

        // ── Decision banner + pulse ──────────────────────────────
        if (result.isAuthenticated()) {
            if (config.getBoolean("enable_2fa", false)) {
                TwoFactorAuth tfa = new TwoFactorAuth();
                String otp = tfa.generateOTP();
                AuthLogger logger = new AuthLogger();

                TwoFactorDialog dialog = new TwoFactorDialog(
                        (JFrame) SwingUtilities.getWindowAncestor(this), tfa, otp);
                dialog.setVisible(true);

                if (dialog.isVerified()) {
                    logger.log2FAAttempt(authUsername, true);
                    showGranted("[OK] 2FA verified. Authentication successful.");
                    mainWindow.setCurrentUser(authUsername);
                } else {
                    logger.log2FAAttempt(authUsername, false);
                    showDenied("[X] 2FA verification failed. Access denied.");
                }
            } else {
                showGranted("[OK] Authentication successful.");
                mainWindow.setCurrentUser(authUsername);
            }
        } else {
            showDenied("[X] Authentication failed.");
        }

        String detail = String.format("Threshold: %.1f%%  |  Risk: %.1f%%",
                result.getThresholdUsed(), result.getImpostorRisk());
        lblStatus.setText(detail);
        lblStatus.setForeground(StyleManager.TEXT_SECONDARY);
    }

    private void showGranted(String statusMsg) {
        bannerColor = StyleManager.SUCCESS;
        lblDecision.setText("[OK]  ACCESS GRANTED");
        lblDecision.setForeground(StyleManager.SUCCESS);
        lblStatus.setForeground(StyleManager.SUCCESS);
        lblStatus.setText(statusMsg);
        pulseAlpha = 0.3f; pulseUp = true;
        pulseTimer.start();
    }

    private void showDenied(String statusMsg) {
        bannerColor = StyleManager.DANGER;
        lblDecision.setText("[X]  ACCESS DENIED");
        lblDecision.setForeground(StyleManager.DANGER);
        lblStatus.setForeground(StyleManager.DANGER);
        lblStatus.setText(statusMsg);
        pulseAlpha = 0.3f; pulseUp = true;
        pulseTimer.start();
    }

    private double mean(double[] arr) {
        if (arr == null || arr.length == 0) return 0;
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    // ─── Reset & Start ──────────────────────────────────────────

    private void resetCapture() {
        currentHold.clear();
        currentFlight.clear();
        pressTimeMap.clear();
        lastReleaseNano = 0;
        inputField.setText("");
        graphPanel.clear();
        gaugePanel.reset();
        pulseTimer.stop();
        pulseAlpha = 0;
        bannerColor = StyleManager.TEXT_DIM;

        lblDecision.setText("");
        lblStatus.setForeground(StyleManager.TEXT_DIM);
        lblStatus.setText("Start typing...");
        lblKeystrokeScoreVal.setText("—");
        lblKeystrokeScoreVal.setForeground(StyleManager.ACCENT);
        lblMouseScoreVal.setText("—");
        lblMouseScoreVal.setForeground(StyleManager.TEXT_DIM);
        lblTimingHold.setText("Mean Hold: — vs Enrolled: —");
        lblTimingFlight.setText("Mean Flight: — vs Enrolled: —");
        lblTimingDeviation.setText("Deviation: —");
        lblTimingDeviation.setForeground(StyleManager.TEXT_DIM);

        inputField.requestFocusInWindow();
        startMouseCapture();
        repaint();
    }

    private void startMouseCapture() {
        if (mouseCapture != null) mouseCapture.stopCapture();
        ConfigManager config = mainWindow.getConfigManager();
        if (config.getBoolean("enable_mouse_dynamics", false)) {
            mouseCapture = new MouseDynamicsCapture(authUsername);
            mouseCapture.startCapture(this);
        }
    }

    public void startAuth(String username) {
        this.authUsername = username;
        lblTitle.setText("Authenticate: " + username);
        resetCapture();

        KeystrokeProfile ref = mainWindow.getFileManager().loadUserProfile(username);
        if (ref != null) {
            graphPanel.setReferenceProfile(ref.getHoldTimings(), ref.getFlightTimings());
            lblPhrase.setText("\"" + ref.getPhrase() + "\"");
            refAvgHold = ref.getAverageHoldTime();
            refAvgFlight = ref.getAverageFlightTime();
        }
    }
}
