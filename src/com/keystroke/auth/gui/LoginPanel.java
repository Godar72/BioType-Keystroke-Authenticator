package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * LoginPanel.java — FlashGuard-inspired cybersecurity login screen.
 * Features: scrolling status ticker, hero headline, terminal-style
 * login card, animated glow orbs, stat metric bar, "How It Works"
 * step cards, and security features strip.
 *
 * Enhanced: Added bottom-half content to fill empty dark space.
 * Enhanced: Added animated threat feed, biometric waveform visual,
 * and live system stats to fill the hero middle section.
 */
public class LoginPanel extends JPanel {

    private final MainWindow mainWindow;
    private JTextField usernameField;
    private JButton btnUserLogin, btnAdminLogin, btnEnroll;
    private JLabel statusLabel;

    // Ticker animation
    private float tickerOffset = 0;
    private float animPhase = 0;
    private final Timer animTimer;

    // Live stats counter
    private long keystrokeCount = 1_247_832;
    private Timer statsTimer;

    // Threat feed
    private Timer threatFeedTimer;
    private int threatLineIndex = 0;
    private int threatCharIndex = 0;
    private String[] threatLines = new String[4];  // currently displayed lines
    private boolean threatTyping = false;
    private final String[][] THREAT_LOG_ENTRIES = {
        { "OK",    "Keystroke engine initialized" },
        { "OK",    "Impostor detection active" },
        { "OK",    "Adaptive threshold loaded" },
        { "SCAN",  "Monitoring for anomalies..." },
        { "BLOCK", "Unauthorized pattern rejected" },
        { "OK",    "Euclidean scoring nominal" },
        { "OK",    "Profile integrity verified" },
        { "ALERT", "Unknown typing pattern detected" },
    };
    private int threatEntryPointer = 0;
    private JPanel threatFeedPanel;

    // Waveform
    private float wavePhase = 0;
    private Timer waveTimer;

    // Blinking dot
    private boolean dotVisible = true;
    private Timer dotTimer;

    public LoginPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 0));
        setOpaque(true);

        // Initialize threat lines as empty
        for (int i = 0; i < 4; i++) threatLines[i] = "";

        buildUI();

        animTimer = new Timer(30, e -> {
            tickerOffset -= 1.2f;
            animPhase += 0.018f;
            repaint();
        });
        animTimer.start();

        // Stats counter — increment every 2 seconds
        statsTimer = new Timer(2000, e -> {
            keystrokeCount += (int)(Math.random() * 47 + 12);
            repaint();
        });
        statsTimer.start();

        // Waveform animation — smooth shift
        waveTimer = new Timer(50, e -> {
            wavePhase += 0.06f;
        });
        waveTimer.start();

        // Blinking green dot for threat monitor
        dotTimer = new Timer(800, e -> {
            dotVisible = !dotVisible;
        });
        dotTimer.start();

        // Threat feed typing timer — fast typing effect
        threatFeedTimer = new Timer(45, e -> advanceThreatFeed());
        threatFeedTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // Deep dark gradient
        g2.setPaint(StyleManager.bgGradient(w, h));
        g2.fillRect(0, 0, w, h);

        // Animated warm glow orbs
        float cx1 = w * 0.15f + (float) Math.sin(animPhase) * 50;
        float cy1 = h * 0.4f + (float) Math.cos(animPhase * 0.7) * 40;
        StyleManager.drawGlow(g2, (int) cx1, (int) cy1, 100, new Color(255, 140, 0, 12));

        float cx2 = w * 0.85f + (float) Math.cos(animPhase * 0.6) * 40;
        float cy2 = h * 0.6f + (float) Math.sin(animPhase * 0.8) * 50;
        StyleManager.drawGlow(g2, (int) cx2, (int) cy2, 80, new Color(255, 100, 0, 10));

        // Dot grid
        StyleManager.drawDotGrid(g2, w, h, 35);

        // CRT scan lines
        StyleManager.drawScanLines(g2, w, h);

        g2.dispose();
    }

    // ─── Threat feed typing engine ──────────────────────────────
    private void advanceThreatFeed() {
        String[] entry = THREAT_LOG_ENTRIES[threatEntryPointer];
        String fullLine = "[ " + entry[0] + " ] " + entry[1];

        if (threatCharIndex < fullLine.length()) {
            // Typing in progress
            threatLines[3] = fullLine.substring(0, threatCharIndex + 1) + "_";
            threatCharIndex++;
        } else {
            // Done typing this line — finalize
            threatLines[3] = fullLine;
            threatCharIndex = 0;

            // Pause before next line
            threatFeedTimer.stop();
            Timer pauseTimer = new Timer(2500, ev -> {
                // Scroll lines up
                threatLines[0] = threatLines[1];
                threatLines[1] = threatLines[2];
                threatLines[2] = threatLines[3];
                threatLines[3] = "";

                // Next entry
                threatEntryPointer = (threatEntryPointer + 1) % THREAT_LOG_ENTRIES.length;
                threatFeedTimer.start();
            });
            pauseTimer.setRepeats(false);
            pauseTimer.start();
        }

        if (threatFeedPanel != null) threatFeedPanel.repaint();
    }

    private Color getThreatColor(String line) {
        if (line.contains("[ OK ]"))    return StyleManager.SUCCESS;
        if (line.contains("[ SCAN ]"))  return StyleManager.ACCENT;
        if (line.contains("[ BLOCK ]")) return StyleManager.DANGER;
        if (line.contains("[ ALERT ]")) return StyleManager.DANGER;
        return StyleManager.TEXT_DIM;
    }

    private void buildUI() {
        // ── Top: scrolling ticker ────────────────────────────────
        JPanel tickerPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(255, 140, 0, 8));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(255, 140, 0, 25));
                g2.fillRect(0, h - 1, w, 1);

                // Scrolling text
                g2.setFont(StyleManager.FONT_MONO_SM);
                String ticker = "   >> BIOTYPE ACTIVE   >> KEYSTROKE ENGINE: ONLINE   " +
                        ">> EUCLIDEAN + MAD% SCORING   >> THRESHOLD: " +
                        String.format("%.0f%%", SystemConstants.DEFAULT_THRESHOLD) +
                        "   >> PROFILES: " + mainWindow.getFileManager().getProfileCount() +
                        "   >> ADAPTIVE THRESHOLD   " +
                        ">> REAL-TIME CAPTURE   >> IMPOSTOR DETECTION: ACTIVE   ";
                ticker = ticker + ticker; // repeat

                FontMetrics fm = g2.getFontMetrics();
                float tw = fm.stringWidth(ticker) / 2f;
                float tx = tickerOffset % tw;

                g2.setColor(StyleManager.CYAN_DIM);
                g2.drawString(ticker, tx, 14);
                g2.dispose();
            }
        };
        tickerPanel.setPreferredSize(new Dimension(0, 22));
        tickerPanel.setOpaque(false);
        add(tickerPanel, BorderLayout.NORTH);

        // ── Centre wrapper: hero+login top, how-it-works + features bottom ──
        JPanel centreWrapper = new JPanel(new BorderLayout(0, 0));
        centreWrapper.setOpaque(false);

        // ── Top part: hero + login card side by side ─────────────
        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        centre.setBorder(BorderFactory.createEmptyBorder(20, 50, 10, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 30);

        // LEFT: Hero section
        JPanel heroPanel = new JPanel();
        heroPanel.setOpaque(false);
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));

        // System online badge
        JLabel badge = new JLabel("  >> SYSTEM ONLINE  -  REAL-TIME INFERENCE  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 140, 0, 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.setColor(new Color(255, 140, 0, 45));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        badge.setFont(StyleManager.FONT_TINY);
        badge.setForeground(StyleManager.CYAN);
        badge.setAlignmentX(LEFT_ALIGNMENT);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        heroPanel.add(badge);
        heroPanel.add(Box.createVerticalStrut(14));

        // Hero title
        JLabel heroTitle1 = StyleManager.label("AUTHENTICATE", StyleManager.FONT_HERO, StyleManager.TEXT_PRIMARY);
        heroTitle1.setAlignmentX(LEFT_ALIGNMENT);
        heroPanel.add(heroTitle1);

        JLabel heroTitle2 = StyleManager.label("WITH KEYSTROKES", StyleManager.FONT_HERO, StyleManager.ORANGE);
        heroTitle2.setAlignmentX(LEFT_ALIGNMENT);
        heroPanel.add(heroTitle2);

        JLabel heroSub = StyleManager.label("BEFORE IMPOSTORS GET ACCESS",
                new Font("Consolas", Font.PLAIN, 16), StyleManager.TEXT_DIM);
        heroSub.setAlignmentX(LEFT_ALIGNMENT);
        heroPanel.add(heroSub);
        heroPanel.add(Box.createVerticalStrut(14));

        // Description
        JLabel desc = StyleManager.label(
                "<html>Euclidean + MAD% biometric scoring engine.<br>" +
                "Real-time keystroke dynamics capture with<br>" +
                "adaptive threshold and impostor detection.</html>",
                StyleManager.FONT_BODY, StyleManager.TEXT_SECONDARY);
        desc.setAlignmentX(LEFT_ALIGNMENT);
        heroPanel.add(desc);
        heroPanel.add(Box.createVerticalStrut(16));

        // ══════════════════════════════════════════════════════════
        //  NEW ELEMENT 1: Animated Threat Feed
        // ══════════════════════════════════════════════════════════
        threatFeedPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Dark background
                g2.setColor(new Color(12, 8, 6));
                g2.fillRoundRect(0, 0, w, h, 6, 6);

                // Subtle border
                g2.setColor(new Color(255, 140, 0, 20));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                // Title bar area
                g2.setColor(new Color(18, 14, 10));
                g2.fillRoundRect(0, 0, w, 20, 6, 6);
                g2.fillRect(0, 12, w, 8);

                // Blinking green dot
                if (dotVisible) {
                    g2.setColor(StyleManager.SUCCESS);
                    g2.fillOval(10, 6, 7, 7);
                    // Glow
                    g2.setColor(new Color(80, 220, 100, 40));
                    g2.fillOval(7, 3, 13, 13);
                } else {
                    g2.setColor(new Color(80, 220, 100, 60));
                    g2.fillOval(10, 6, 7, 7);
                }

                // Title text
                g2.setFont(StyleManager.FONT_STAT_LABEL);
                g2.setColor(StyleManager.ACCENT);
                g2.drawString("LIVE THREAT MONITOR", 24, 14);

                // Log lines
                g2.setFont(new Font("Consolas", Font.PLAIN, 11));
                int yStart = 34;
                int lineHeight = 15;

                for (int i = 0; i < 4; i++) {
                    String line = threatLines[i];
                    if (line == null || line.isEmpty()) continue;

                    // Fade older lines (top lines dimmer)
                    int alpha = 80 + (i * 58); // 80, 138, 196, 255
                    if (alpha > 255) alpha = 255;

                    Color lineColor = getThreatColor(line);
                    g2.setColor(new Color(lineColor.getRed(), lineColor.getGreen(),
                            lineColor.getBlue(), alpha));
                    g2.drawString(line, 10, yStart + i * lineHeight);
                }

                g2.dispose();
            }
        };
        threatFeedPanel.setOpaque(false);
        threatFeedPanel.setAlignmentX(LEFT_ALIGNMENT);
        threatFeedPanel.setPreferredSize(new Dimension(0, 98));
        threatFeedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 98));
        heroPanel.add(threatFeedPanel);
        heroPanel.add(Box.createVerticalStrut(12));

        // ══════════════════════════════════════════════════════════
        //  NEW ELEMENT 2: Biometric Waveform Visual
        // ══════════════════════════════════════════════════════════
        JPanel waveformPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Subtle grid lines
                g2.setColor(new Color(255, 120, 0, 10));
                int gridSpacing = 16;
                for (int x = 0; x < w; x += gridSpacing) {
                    g2.drawLine(x, 0, x, h - 18);
                }
                for (int y = 0; y < h - 18; y += gridSpacing) {
                    g2.drawLine(0, y, w, y);
                }

                // Center line
                int centerY = (h - 18) / 2;
                g2.setColor(new Color(255, 140, 0, 18));
                g2.drawLine(0, centerY, w, centerY);

                // Hold time wave (orange)
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D holdWave = new Path2D.Float();
                boolean holdStarted = false;
                for (int x = 0; x < w; x += 2) {
                    double yVal = centerY + Math.sin((x * 0.025) - wavePhase) * 18
                            + Math.sin((x * 0.06) - wavePhase * 1.3) * 8
                            + Math.sin((x * 0.012) - wavePhase * 0.7) * 6;
                    if (!holdStarted) {
                        holdWave.moveTo(x, yVal);
                        holdStarted = true;
                    } else {
                        holdWave.lineTo(x, yVal);
                    }
                }
                // Orange wave glow
                g2.setColor(new Color(255, 140, 0, 25));
                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(holdWave);
                // Orange wave main
                g2.setColor(StyleManager.ACCENT);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(holdWave);

                // Flight time wave (green)
                Path2D flightWave = new Path2D.Float();
                boolean flightStarted = false;
                for (int x = 0; x < w; x += 2) {
                    double yVal = centerY + Math.sin((x * 0.03) - wavePhase * 0.8 + 2.0) * 14
                            + Math.cos((x * 0.05) - wavePhase * 1.1 + 1.2) * 10
                            + Math.sin((x * 0.015) - wavePhase * 0.5) * 5;
                    if (!flightStarted) {
                        flightWave.moveTo(x, yVal);
                        flightStarted = true;
                    } else {
                        flightWave.lineTo(x, yVal);
                    }
                }
                // Green wave glow
                g2.setColor(new Color(80, 220, 100, 20));
                g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(flightWave);
                // Green wave main
                g2.setColor(StyleManager.SUCCESS);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(flightWave);

                // Legend label
                g2.setFont(StyleManager.FONT_STAT_LABEL);
                g2.setColor(StyleManager.TEXT_DIM);
                g2.drawString("BIOMETRIC SIGNATURE ANALYSIS", 0, h - 4);

                // Legend dots
                int legendX = w - 160;
                int legendY = h - 8;
                g2.setColor(StyleManager.ACCENT);
                g2.fillOval(legendX, legendY, 6, 6);
                g2.setFont(StyleManager.FONT_STAT_LABEL);
                g2.setColor(StyleManager.TEXT_SECONDARY);
                g2.drawString("HOLD TIME", legendX + 10, legendY + 6);

                g2.setColor(StyleManager.SUCCESS);
                g2.fillOval(legendX + 80, legendY, 6, 6);
                g2.setColor(StyleManager.TEXT_SECONDARY);
                g2.drawString("FLIGHT TIME", legendX + 90, legendY + 6);

                g2.dispose();
            }
        };
        waveformPanel.setOpaque(false);
        waveformPanel.setAlignmentX(LEFT_ALIGNMENT);
        waveformPanel.setPreferredSize(new Dimension(0, 80));
        waveformPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        heroPanel.add(waveformPanel);
        heroPanel.add(Box.createVerticalStrut(12));

        // ══════════════════════════════════════════════════════════
        //  NEW ELEMENT 3: Live System Stats Panel
        // ══════════════════════════════════════════════════════════
        JPanel liveStatsPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                int rowH = h / 3;

                // ── Row 1: KEYSTROKES ANALYZED ──
                g2.setFont(new Font("Consolas", Font.BOLD, 22));
                g2.setColor(StyleManager.ACCENT);
                String countStr = String.format("%,d", keystrokeCount);
                g2.drawString(countStr, 0, 22);
                g2.setFont(StyleManager.FONT_STAT_LABEL);
                g2.setColor(StyleManager.TEXT_DIM);
                g2.drawString("KEYSTROKES ANALYZED", 0, 34);

                // Separator line 1
                int sepY1 = rowH - 2;
                g2.setColor(new Color(255, 140, 0, 25));
                g2.drawLine(0, sepY1, w, sepY1);

                // ── Row 2: AVG RESPONSE TIME ──
                int r2Y = rowH + 4;
                g2.setFont(new Font("Consolas", Font.BOLD, 22));
                g2.setColor(StyleManager.TEXT_PRIMARY);
                g2.drawString("<1ms", 0, r2Y + 18);
                g2.setFont(StyleManager.FONT_STAT_LABEL);
                g2.setColor(StyleManager.TEXT_DIM);
                g2.drawString("NANOSECOND PRECISION", 0, r2Y + 30);

                // Separator line 2
                int sepY2 = 2 * rowH - 2;
                g2.setColor(new Color(255, 140, 0, 25));
                g2.drawLine(0, sepY2, w, sepY2);

                // ── Row 3: SECURITY LEVEL ──
                int r3Y = 2 * rowH + 4;
                g2.setFont(new Font("Consolas", Font.BOLD, 22));
                g2.setColor(StyleManager.SUCCESS);
                g2.drawString("MAXIMUM", 0, r3Y + 18);
                g2.setFont(StyleManager.FONT_STAT_LABEL);
                g2.setColor(StyleManager.TEXT_DIM);
                g2.drawString("256-BIT BIOMETRIC ENCRYPTION", 0, r3Y + 30);

                g2.dispose();
            }
        };
        liveStatsPanel.setOpaque(false);
        liveStatsPanel.setAlignmentX(LEFT_ALIGNMENT);
        liveStatsPanel.setPreferredSize(new Dimension(0, 120));
        liveStatsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        heroPanel.add(liveStatsPanel);
        heroPanel.add(Box.createVerticalGlue());

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.55;
        centre.add(heroPanel, gbc);

        // RIGHT: Login card (terminal style)
        JPanel loginCard = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillRoundRect(4, 4, w - 2, h - 2, 10, 10);
                g2.setColor(StyleManager.BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(new Color(16, 22, 36));
                g2.fillRoundRect(0, 0, w, 32, 10, 10);
                g2.fillRect(0, 20, w, 12);
                g2.setColor(new Color(255, 70, 70));
                g2.fillOval(14, 10, 10, 10);
                g2.setColor(new Color(255, 200, 40));
                g2.fillOval(30, 10, 10, 10);
                g2.setColor(new Color(0, 220, 120));
                g2.fillOval(46, 10, 10, 10);
                g2.setFont(StyleManager.FONT_MONO_SM);
                g2.setColor(StyleManager.TEXT_DIM);
                g2.drawString("BIOTYPE — LOGIN", 70, 21);
                g2.setFont(StyleManager.FONT_TINY);
                g2.setColor(StyleManager.CYAN);
                g2.drawString("* LIVE", w - 60, 21);
                g2.setColor(StyleManager.GLASS_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.dispose();
            }
        };
        loginCard.setOpaque(false);
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setBorder(BorderFactory.createEmptyBorder(46, 28, 24, 28));
        loginCard.setPreferredSize(new Dimension(360, 340));

        // Username
        JLabel userLabel = StyleManager.label("USERNAME", StyleManager.FONT_STAT_LABEL, StyleManager.CYAN_DIM);
        userLabel.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(userLabel);
        loginCard.add(Box.createVerticalStrut(6));

        usernameField = StyleManager.textField(18);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usernameField.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(usernameField);
        loginCard.add(Box.createVerticalStrut(20));

        // Buttons
        btnUserLogin = StyleManager.button("  LOGIN (BIOMETRIC)  ");
        btnUserLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnUserLogin.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(btnUserLogin);
        loginCard.add(Box.createVerticalStrut(10));

        btnAdminLogin = StyleManager.secondaryButton("  ADMIN LOGIN  ");
        btnAdminLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnAdminLogin.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(btnAdminLogin);
        loginCard.add(Box.createVerticalStrut(10));

        btnEnroll = StyleManager.secondaryButton("  ENROLL NEW USER  ");
        btnEnroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnEnroll.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(btnEnroll);
        loginCard.add(Box.createVerticalStrut(14));

        statusLabel = StyleManager.label("", StyleManager.FONT_SMALL, StyleManager.TEXT_DIM);
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(statusLabel);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 0.45;
        gbc.insets = new Insets(0, 0, 0, 0);
        centre.add(loginCard, gbc);

        centreWrapper.add(centre, BorderLayout.CENTER);

        // ══════════════════════════════════════════════════════════
        // ENHANCEMENT 3: "HOW IT WORKS" + SECURITY FEATURES STRIP
        // ══════════════════════════════════════════════════════════

        JPanel bottomContent = new JPanel();
        bottomContent.setOpaque(false);
        bottomContent.setLayout(new BoxLayout(bottomContent, BoxLayout.Y_AXIS));
        bottomContent.setBorder(BorderFactory.createEmptyBorder(0, 50, 10, 50));

        // ── "HOW IT WORKS" section title ─────────────────────────
        JLabel howTitle = StyleManager.label("HOW IT WORKS",
                StyleManager.FONT_SUBTITLE, StyleManager.TEXT_SECONDARY);
        howTitle.setAlignmentX(CENTER_ALIGNMENT);
        bottomContent.add(howTitle);
        bottomContent.add(Box.createVerticalStrut(12));

        // ── 3 step cards arranged horizontally ───────────────────
        JPanel stepsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        stepsRow.setOpaque(false);
        stepsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        stepsRow.setAlignmentX(CENTER_ALIGNMENT);

        stepsRow.add(createStepCard("01", "ENROLL",
                "Type your passphrase 3 times to build your unique biometric profile."));
        stepsRow.add(createStepCard("02", "ANALYZE",
                "System learns your typing rhythm — hold times and flight times."));
        stepsRow.add(createStepCard("03", "AUTHENTICATE",
                "Your unique rhythm unlocks access. Impostors are rejected."));

        bottomContent.add(stepsRow);
        bottomContent.add(Box.createVerticalStrut(14));

        // ── Security features strip ──────────────────────────────
        JPanel featuresStrip = new JPanel(new GridLayout(1, 4, 12, 0));
        featuresStrip.setOpaque(false);
        featuresStrip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        featuresStrip.setAlignmentX(CENTER_ALIGNMENT);

        featuresStrip.add(createFeatureBadge("+", "Euclidean + MAD%"));
        featuresStrip.add(createFeatureBadge("T", "Nanosecond Precision"));
        featuresStrip.add(createFeatureBadge("~", "Adaptive Threshold"));
        featuresStrip.add(createFeatureBadge("!", "Impostor Detection"));

        bottomContent.add(featuresStrip);

        centreWrapper.add(bottomContent, BorderLayout.SOUTH);
        add(centreWrapper, BorderLayout.CENTER);

        // ── Bottom: stat cards ───────────────────────────────────
        JPanel statsBar = new JPanel(new GridLayout(1, 4, 12, 0));
        statsBar.setOpaque(false);
        statsBar.setBorder(BorderFactory.createEmptyBorder(0, 50, 16, 50));

        int profileCount = mainWindow.getFileManager().getProfileCount();
        statsBar.add(StyleManager.statCard(String.valueOf(profileCount), "ENROLLED USERS", StyleManager.CYAN));
        statsBar.add(StyleManager.statCard("70%", "THRESHOLD", StyleManager.ORANGE));
        statsBar.add(StyleManager.statCard("<1ms", "INFERENCE", StyleManager.CYAN));
        statsBar.add(StyleManager.statCard("MAD%", "ALGORITHM", StyleManager.ORANGE));

        add(statsBar, BorderLayout.SOUTH);

        // ── Actions ─────────────────────────────────────────────
        btnUserLogin.addActionListener(e -> handleUserLogin());
        btnAdminLogin.addActionListener(e -> handleAdminLogin());
        btnEnroll.addActionListener(e -> handleEnroll());
        usernameField.addActionListener(e -> handleUserLogin());
    }

    // ═══════════════════════════════════════════════════════════════
    //   ENHANCEMENT 3 — Step card and feature badge factories
    // ═══════════════════════════════════════════════════════════════

    /**
     * Creates a "How It Works" step card with orange step number,
     * title, and description.
     */
    private JPanel createStepCard(String stepNum, String title, String description) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Card background
                g2.setColor(new Color(28, 22, 18, 200));
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                // Subtle border
                g2.setColor(new Color(255, 140, 0, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                // Orange top accent line
                g2.setPaint(StyleManager.orangeGradient(0, 0, w, 3));
                g2.fillRect(4, 0, w - 8, 2);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // Step number (large orange)
        JLabel numLabel = StyleManager.label(stepNum, StyleManager.FONT_STAT, StyleManager.ACCENT);
        numLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(numLabel);
        card.add(Box.createVerticalStrut(4));

        // Title
        JLabel titleLabel = StyleManager.label(title, StyleManager.FONT_SUBTITLE, StyleManager.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));

        // Description
        JLabel descLabel = StyleManager.label(
                "<html><body style='width:160px'>" + description + "</body></html>",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        descLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(descLabel);

        return card;
    }

    /**
     * Creates a security feature badge with unicode icon and label text.
     */
    private JPanel createFeatureBadge(String icon, String labelText) {
        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 140, 0, 8));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(255, 140, 0, 20));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));

        JLabel iconLabel = StyleManager.label(icon, StyleManager.FONT_BODY, StyleManager.ACCENT);
        JLabel textLabel = StyleManager.label(labelText, StyleManager.FONT_TINY, StyleManager.TEXT_SECONDARY);
        badge.add(iconLabel);
        badge.add(textLabel);

        return badge;
    }

    // ═══════════════════════════════════════════════════════════════
    //   Existing login handlers (unchanged)
    // ═══════════════════════════════════════════════════════════════

    private void handleUserLogin() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setForeground(StyleManager.WARNING);
            statusLabel.setText("[!] Enter a username.");
            return;
        }
        String error = InputValidator.getUsernameError(username);
        if (error != null) {
            statusLabel.setForeground(StyleManager.WARNING);
            statusLabel.setText("[!] " + error);
            return;
        }
        if (!mainWindow.getFileManager().profileExists(username)) {
            statusLabel.setForeground(StyleManager.DANGER);
            statusLabel.setText("[X] No profile found. Enroll first.");
            return;
        }
        mainWindow.getAuthPanel().startAuth(username);
        mainWindow.showPanel("auth");
    }

    private void handleAdminLogin() {
        JPanel adminPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        adminPanel.setBackground(StyleManager.BG_CARD);
        JTextField userF = StyleManager.textField(15);
        JPasswordField passF = StyleManager.passwordField(15);
        adminPanel.add(StyleManager.label("Username:"));
        adminPanel.add(userF);
        adminPanel.add(StyleManager.label("Password:"));
        adminPanel.add(passF);

        int result = JOptionPane.showConfirmDialog(this, adminPanel,
                "Admin Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String u = userF.getText().trim();
            String p = new String(passF.getPassword());
            if (u.equals(SystemConstants.DEFAULT_ADMIN_USERNAME)
                    && p.equals(SystemConstants.DEFAULT_ADMIN_PASSWORD)) {
                mainWindow.setCurrentUser("admin");
                mainWindow.getAdminDashboard().onShow();
                mainWindow.showPanel("admin");
            } else {
                statusLabel.setForeground(StyleManager.DANGER);
                statusLabel.setText("[X] Invalid admin credentials.");
            }
        }
    }

    private void handleEnroll() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setForeground(StyleManager.WARNING);
            statusLabel.setText("[!] Enter a username to enroll.");
            return;
        }
        String error = InputValidator.getUsernameError(username);
        if (error != null) {
            statusLabel.setForeground(StyleManager.WARNING);
            statusLabel.setText("[!] " + error);
            return;
        }
        if (mainWindow.getFileManager().profileExists(username)) {
            if (!GUIUtils.confirm(this, "Profile exists for '" + username
                    + "'. Overwrite?", "Confirm Overwrite")) return;
        }
        mainWindow.getEnrollmentPanel().startEnrollment(username);
        mainWindow.showPanel("enroll");
    }

    public void reset() {
        statusLabel.setText("");
        usernameField.setText("");
        usernameField.requestFocusInWindow();
    }
}
