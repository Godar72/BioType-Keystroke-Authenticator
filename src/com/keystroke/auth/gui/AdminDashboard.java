package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminDashboard.java — Tabbed admin dashboard with user management,
 * authentication logs, analytics charts, ML threshold panel, and system settings
 * with toggle controls for all Phase 2 features.
 *
 * Enhanced:
 *   - Users tab: loads profiles from FileManager, enrollment date, last login,
 *     color-coded rows (green=active, yellow=inactive 7d, red=never auth'd)
 *   - Logs tab: fixed parsing to match AuthLogger format, auto-refresh (5s),
 *     filter buttons (ALL/SUCCESS/FAILED/TODAY), summary stats bar,
 *     color-coded rows, CSV export
 */
public class AdminDashboard extends JPanel {

    private final MainWindow mainWindow;
    private JTabbedPane tabs;

    // Tab 1: User Management
    private DefaultTableModel userTableModel;
    private JTable userTable;
    private JLabel lblUserCount;

    // Tab 2: Logs
    private DefaultTableModel logTableModel;
    private JTable logTable;
    private JLabel lblLogTotal, lblLogRate, lblLogLast;
    private String logFilter = "ALL";               // current filter
    private Timer logAutoRefreshTimer;               // 5-second refresh
    private List<String[]> allLogEntries = new ArrayList<>(); // parsed cache

    // Tab 3: Analytics
    private JPanel analyticsChartPanel;
    private int totalSuccess = 0;
    private int totalFail = 0;

    // Tab 4: ML Threshold
    private MLThresholdPanel mlThresholdPanel;

    // Tab 5: Settings
    private JSlider thresholdSlider;
    private JLabel lblThresholdVal;
    private JCheckBox chk2FA;
    private JCheckBox chkMouseDynamics;
    private JCheckBox chkMLThreshold;

    public AdminDashboard(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout());
        setBackground(StyleManager.BG_PRIMARY);
        buildUI();
    }

    private void buildUI() {
        // Header
        JPanel header = StyleManager.panel();
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 14));
        JLabel title = StyleManager.label("[*] Admin Dashboard",
                StyleManager.FONT_SUBTITLE, StyleManager.ACCENT);
        JButton btnLogout = StyleManager.secondaryButton("Logout");
        btnLogout.addActionListener(e -> {
            stopAutoRefresh();
            mainWindow.setCurrentUser(null);
            mainWindow.showPanel("login");
        });
        header.add(title, BorderLayout.WEST);
        header.add(btnLogout, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Tabs
        tabs = new JTabbedPane();
        tabs.setFont(StyleManager.FONT_BUTTON);
        tabs.setBackground(StyleManager.BG_SECONDARY);
        tabs.setForeground(StyleManager.TEXT_PRIMARY);

        tabs.addTab("Users", buildUserTab());
        tabs.addTab("Auth Logs", buildLogsTab());
        tabs.addTab("Analytics", buildAnalyticsTab());
        tabs.addTab("ML Threshold", buildMLTab());
        tabs.addTab("Settings", buildSettingsTab());

        // Auto-refresh users when switching to Users tab
        tabs.addChangeListener(e -> {
            int idx = tabs.getSelectedIndex();
            if (idx == 0) refreshUsers();
            else if (idx == 1) { refreshLogs(); startAutoRefresh(); }
            else stopAutoRefresh();
        });

        add(tabs, BorderLayout.CENTER);
    }

    // ═══════════════════ Tab 1: Users ═══════════════════════════
    // ENHANCEMENT 1: Fixed user loading, added columns and color coding

    private JPanel buildUserTab() {
        JPanel p = StyleManager.panel();
        p.setLayout(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // Stats bar
        JPanel stats = StyleManager.card();
        stats.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 6));
        lblUserCount = StyleManager.label("Total users: 0",
                StyleManager.FONT_BODY, StyleManager.TEXT_PRIMARY);
        stats.add(lblUserCount);
        p.add(stats, BorderLayout.NORTH);

        // Table with extended columns
        userTableModel = new DefaultTableModel(
                new String[]{"Username", "Avg Hold (ms)", "Avg Flight (ms)",
                        "Hold σ", "Flight σ", "Enrolled", "Last Login"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userTableModel);
        GUIUtils.styleTable(userTable);

        // Color-coded row renderer for users
        userTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel && row < userTableModel.getRowCount()) {
                    String lastLogin = (String) userTableModel.getValueAt(row, 6);
                    if ("Never".equals(lastLogin)) {
                        // Red: never authenticated
                        c.setBackground(new Color(60, 20, 20));
                        c.setForeground(new Color(255, 130, 130));
                    } else if (isOlderThan7Days(lastLogin)) {
                        // Yellow: inactive > 7 days
                        c.setBackground(new Color(50, 45, 15));
                        c.setForeground(new Color(255, 220, 130));
                    } else {
                        // Green: recently active
                        c.setBackground(new Color(15, 45, 25));
                        c.setForeground(new Color(130, 255, 150));
                    }
                }
                ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        });

        p.add(GUIUtils.darkScrollPane(userTable), BorderLayout.CENTER);

        // Buttons
        JPanel btns = StyleManager.panel();
        btns.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnRefresh = StyleManager.button("Refresh");
        JButton btnDelete = StyleManager.dangerButton("Delete Selected");
        JButton btnView = StyleManager.secondaryButton("View Profile");
        btns.add(btnRefresh);
        btns.add(btnDelete);
        btns.add(btnView);
        p.add(btns, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshUsers());
        btnDelete.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row < 0) { GUIUtils.showWarning(this, "Select a user first."); return; }
            String user = (String) userTableModel.getValueAt(row, 0);
            if (GUIUtils.confirm(this, "Delete profile for '" + user + "'?", "Confirm Delete")) {
                mainWindow.getFileManager().deleteProfile(user);
                refreshUsers();
            }
        });
        btnView.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row < 0) { GUIUtils.showWarning(this, "Select a user first."); return; }
            String user = (String) userTableModel.getValueAt(row, 0);
            KeystrokeProfile prof = mainWindow.getFileManager().loadUserProfile(user);
            if (prof != null) {
                GUIUtils.showInfo(this, prof.toString(), "Profile: " + user);
            }
        });

        return p;
    }

    private void refreshUsers() {
        userTableModel.setRowCount(0);
        File usersDir = new File(SystemConstants.USERS_DIR);
        if (!usersDir.exists()) { lblUserCount.setText("Total users: 0"); return; }

        File[] files = usersDir.listFiles((d, n) -> n.endsWith(SystemConstants.PROFILE_EXTENSION));
        if (files == null || files.length == 0) {
            lblUserCount.setText("Total users: 0");
            return;
        }

        for (File f : files) {
            String name = f.getName().replace(SystemConstants.PROFILE_EXTENSION, "");
            KeystrokeProfile prof = mainWindow.getFileManager().loadUserProfile(name);
            if (prof != null) {
                // Enrollment date from file modification time
                String enrolled = getFileDate(f);
                // Last login from auth logs
                String lastLogin = getLastLoginDate(name);

                userTableModel.addRow(new Object[]{
                    name,
                    String.format("%.1f", prof.getAverageHoldTime()),
                    String.format("%.1f", prof.getAverageFlightTime()),
                    String.format("%.1f", prof.getHoldStdDev()),
                    String.format("%.1f", prof.getFlightStdDev()),
                    enrolled,
                    lastLogin
                });
            }
        }
        lblUserCount.setText("Total users: " + files.length);
    }

    /** Gets the file's last-modified date as a formatted string (proxy for creation). */
    private String getFileDate(File f) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
            long millis = attrs.creationTime().toMillis();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(new java.util.Date(millis));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /** Scans auth logs for the latest entry matching this username. */
    private String getLastLoginDate(String username) {
        File logsDir = new File(SystemConstants.LOGS_DIR);
        if (!logsDir.exists()) return "Never";

        File[] logFiles = logsDir.listFiles((d, n) -> n.startsWith("auth_") && n.endsWith(".txt"));
        if (logFiles == null) return "Never";

        String lastTimestamp = null;
        for (File lf : logFiles) {
            try (BufferedReader br = new BufferedReader(new FileReader(lf))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("] " + username + " - ") && line.contains("SUCCESS")) {
                        // Extract timestamp from [YYYY-MM-DD HH:MM:SS]
                        int open = line.indexOf('[');
                        int close = line.indexOf(']');
                        if (open >= 0 && close > open) {
                            String ts = line.substring(open + 1, close).trim();
                            if (lastTimestamp == null || ts.compareTo(lastTimestamp) > 0) {
                                lastTimestamp = ts;
                            }
                        }
                    }
                }
            } catch (IOException ignored) {}
        }
        if (lastTimestamp != null && lastTimestamp.length() >= 10) {
            return lastTimestamp.substring(0, 10); // YYYY-MM-DD
        }
        return "Never";
    }

    /** Checks if a YYYY-MM-DD date string is older than 7 days. */
    private boolean isOlderThan7Days(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.isBefore(LocalDate.now().minusDays(7));
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════ Tab 2: Logs ════════════════════════════
    // ENHANCEMENT 5: Auto-refresh, color-coded rows, filters, stats

    private JPanel buildLogsTab() {
        JPanel p = StyleManager.panel();
        p.setLayout(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // ── Summary stats bar ────────────────────────────────────
        JPanel statsPanel = StyleManager.card();
        statsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 6));
        lblLogTotal = StyleManager.label("Total today: 0",
                StyleManager.FONT_SMALL, StyleManager.TEXT_PRIMARY);
        lblLogRate = StyleManager.label("Success rate: —",
                StyleManager.FONT_SMALL, StyleManager.SUCCESS);
        lblLogLast = StyleManager.label("Last activity: —",
                StyleManager.FONT_SMALL, StyleManager.TEXT_SECONDARY);
        statsPanel.add(lblLogTotal);
        statsPanel.add(lblLogRate);
        statsPanel.add(lblLogLast);

        // ── Filter buttons ───────────────────────────────────────
        JPanel filterPanel = StyleManager.panel();
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));

        JButton btnAll = createFilterButton("ALL", true);
        JButton btnSuccess = createFilterButton("SUCCESS", false);
        JButton btnFailed = createFilterButton("FAILED", false);
        JButton btnToday = createFilterButton("TODAY", false);

        JButton[] filterBtns = {btnAll, btnSuccess, btnFailed, btnToday};
        for (JButton fb : filterBtns) {
            fb.addActionListener(e -> {
                logFilter = fb.getText();
                for (JButton b : filterBtns) b.setForeground(StyleManager.TEXT_SECONDARY);
                fb.setForeground(StyleManager.ACCENT);
                applyLogFilter();
            });
            filterPanel.add(fb);
        }

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setOpaque(false);
        topSection.add(statsPanel, BorderLayout.NORTH);
        topSection.add(filterPanel, BorderLayout.SOUTH);
        p.add(topSection, BorderLayout.NORTH);

        // ── Log table ────────────────────────────────────────────
        logTableModel = new DefaultTableModel(
                new String[]{"Timestamp", "Username", "Result", "Confidence", "Threshold", "Risk"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        logTable = new JTable(logTableModel);
        GUIUtils.styleTable(logTable);

        // Color-coded row renderer for logs
        logTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel && row < logTableModel.getRowCount()) {
                    String result = (String) logTableModel.getValueAt(row, 2);
                    if (result != null) {
                        if (result.contains("2FA")) {
                            // Yellow for 2FA events
                            c.setBackground(new Color(50, 45, 15));
                            c.setForeground(new Color(255, 220, 100));
                        } else if (result.contains("SUCCESS")) {
                            // Green for success
                            c.setBackground(new Color(15, 40, 20));
                            c.setForeground(new Color(130, 255, 150));
                        } else if (result.contains("FAILED")) {
                            // Red for failed
                            c.setBackground(new Color(55, 18, 18));
                            c.setForeground(new Color(255, 130, 130));
                        } else {
                            c.setBackground(StyleManager.BG_CARD);
                            c.setForeground(StyleManager.TEXT_PRIMARY);
                        }
                    }
                }
                ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        });

        p.add(GUIUtils.darkScrollPane(logTable), BorderLayout.CENTER);

        // ── Bottom buttons ───────────────────────────────────────
        JPanel btns = StyleManager.panel();
        btns.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnRefresh = StyleManager.button("Refresh Logs");
        JButton btnExport = StyleManager.secondaryButton("Export CSV");
        btns.add(btnRefresh);
        btns.add(btnExport);
        p.add(btns, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshLogs());
        btnExport.addActionListener(e -> exportLogs());

        return p;
    }

    private JButton createFilterButton(String text, boolean active) {
        JButton b = StyleManager.secondaryButton(text);
        b.setPreferredSize(new Dimension(90, 30));
        b.setForeground(active ? StyleManager.ACCENT : StyleManager.TEXT_SECONDARY);
        return b;
    }

    /** Start auto-refresh timer for logs (every 5 seconds). */
    private void startAutoRefresh() {
        if (logAutoRefreshTimer == null) {
            logAutoRefreshTimer = new Timer(5000, e -> refreshLogs());
        }
        if (!logAutoRefreshTimer.isRunning()) {
            logAutoRefreshTimer.start();
        }
    }

    /** Stop auto-refresh timer. */
    private void stopAutoRefresh() {
        if (logAutoRefreshTimer != null && logAutoRefreshTimer.isRunning()) {
            logAutoRefreshTimer.stop();
        }
    }

    /**
     * Parses ALL log files and populates allLogEntries cache.
     * Log format from AuthLogger:
     *   [YYYY-MM-DD HH:MM:SS] USERNAME - RESULT - Confidence: XX.XX% - Threshold: XX.XX%
     *   [YYYY-MM-DD HH:MM:SS] USERNAME - RESULT - Confidence: XX.XX% - Threshold: XX.XX% - ImpostorRisk: XX.XX%
     *   [YYYY-MM-DD HH:MM:SS] USERNAME - 2FA_SUCCESS/2FA_FAILED - Two-Factor OTP Verification
     */
    private void refreshLogs() {
        allLogEntries.clear();
        totalSuccess = 0;
        totalFail = 0;

        File logsDir = new File(SystemConstants.LOGS_DIR);
        if (!logsDir.exists()) {
            updateLogStats();
            applyLogFilter();
            return;
        }

        File[] logFiles = logsDir.listFiles((d, n) -> n.startsWith("auth_") && n.endsWith(".txt"));
        if (logFiles == null) {
            updateLogStats();
            applyLogFilter();
            return;
        }

        for (File lf : logFiles) {
            try (BufferedReader br = new BufferedReader(new FileReader(lf))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || !line.startsWith("[")) continue;

                    // Parse: [TIMESTAMP] USERNAME - RESULT - details...
                    int closeBracket = line.indexOf(']');
                    if (closeBracket < 0) continue;

                    String ts = line.substring(1, closeBracket).trim();
                    String rest = line.substring(closeBracket + 2).trim();
                    String[] parts = rest.split(" - ");
                    if (parts.length < 2) continue;

                    String user = parts[0].trim();
                    String result = parts[1].trim();
                    String confidence = "—";
                    String threshold = "—";
                    String risk = "—";

                    // Parse confidence & threshold
                    for (int i = 2; i < parts.length; i++) {
                        String part = parts[i].trim();
                        if (part.startsWith("Confidence:")) {
                            confidence = part.replace("Confidence:", "").trim();
                        } else if (part.startsWith("Threshold:")) {
                            threshold = part.replace("Threshold:", "").trim();
                        } else if (part.startsWith("ImpostorRisk:")) {
                            risk = part.replace("ImpostorRisk:", "").trim();
                        }
                    }

                    allLogEntries.add(new String[]{ts, user, result, confidence, threshold, risk});

                    if (result.contains("SUCCESS") && !result.contains("2FA")) totalSuccess++;
                    else if (result.contains("FAILED") && !result.contains("2FA")) totalFail++;
                }
            } catch (IOException ignored) {}
        }

        updateLogStats();
        applyLogFilter();

        // Refresh analytics chart
        if (analyticsChartPanel != null) analyticsChartPanel.repaint();
    }

    /** Updates the summary stats labels above the log table. */
    private void updateLogStats() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int todayCount = 0;
        String lastActivity = "—";

        for (String[] entry : allLogEntries) {
            if (entry[0].startsWith(today)) todayCount++;
            // Track latest timestamp
            if (lastActivity.equals("—") || entry[0].compareTo(lastActivity) > 0) {
                lastActivity = entry[0];
            }
        }

        lblLogTotal.setText("Total today: " + todayCount);
        int total = totalSuccess + totalFail;
        if (total > 0) {
            double rate = totalSuccess * 100.0 / total;
            lblLogRate.setText(String.format("Success rate: %.1f%%", rate));
        } else {
            lblLogRate.setText("Success rate: —");
        }
        lblLogLast.setText("Last activity: " + lastActivity);
    }

    /** Applies the current filter to populate the visible log table. */
    private void applyLogFilter() {
        logTableModel.setRowCount(0);
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        for (String[] entry : allLogEntries) {
            boolean show = false;
            switch (logFilter) {
                case "ALL":
                    show = true;
                    break;
                case "SUCCESS":
                    show = entry[2].contains("SUCCESS") && !entry[2].contains("2FA");
                    break;
                case "FAILED":
                    show = entry[2].contains("FAILED") && !entry[2].contains("2FA");
                    break;
                case "TODAY":
                    show = entry[0].startsWith(today);
                    break;
            }
            if (show) {
                logTableModel.addRow(entry);
            }
        }
    }

    private void exportLogs() {
        // Build default filename with timestamp
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("export_" + timestamp + ".csv"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                pw.println("Timestamp,Username,Result,Confidence,Threshold,Risk");
                for (int r = 0; r < logTableModel.getRowCount(); r++) {
                    for (int c = 0; c < logTableModel.getColumnCount(); c++) {
                        if (c > 0) pw.print(",");
                        pw.print(logTableModel.getValueAt(r, c));
                    }
                    pw.println();
                }
                GUIUtils.showInfo(this, "Logs exported to:\n" + fc.getSelectedFile().getAbsolutePath(),
                        "Export Complete");
            } catch (IOException ex) {
                GUIUtils.showError(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    // ═══════════════════ Tab 3: Analytics ═══════════════════════

    private JPanel buildAnalyticsTab() {
        JPanel p = StyleManager.panel();
        p.setLayout(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JButton btnRefresh = StyleManager.button("Refresh Analytics");
        btnRefresh.addActionListener(e -> { refreshLogs(); analyticsChartPanel.repaint(); });

        JPanel top = StyleManager.panel();
        top.setLayout(new FlowLayout(FlowLayout.LEFT));
        top.add(btnRefresh);
        p.add(top, BorderLayout.NORTH);

        analyticsChartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                // Pie chart: success vs fail
                if (totalSuccess + totalFail > 0) {
                    g2.setColor(StyleManager.TEXT_PRIMARY);
                    g2.setFont(StyleManager.FONT_SUBTITLE);
                    g2.drawString("Authentication Results", 20, 30);

                    ChartBuilder.drawPieChart(g2,
                            new double[]{totalSuccess, totalFail},
                            new Color[]{StyleManager.SUCCESS, StyleManager.DANGER},
                            new String[]{"Success", "Failed"},
                            new Rectangle(20, 40, w / 3, h - 60));

                    // Stats on the right
                    int sx = w / 3 + 40;
                    g2.setFont(StyleManager.FONT_BODY);
                    g2.setColor(StyleManager.TEXT_PRIMARY);
                    g2.drawString("Total Attempts: " + (totalSuccess + totalFail), sx, 80);
                    g2.setColor(StyleManager.SUCCESS);
                    g2.drawString("Successful: " + totalSuccess, sx, 110);
                    g2.setColor(StyleManager.DANGER);
                    g2.drawString("Failed: " + totalFail, sx, 140);
                    g2.setColor(StyleManager.ACCENT);
                    double rate = totalSuccess * 100.0 / (totalSuccess + totalFail);
                    g2.drawString(String.format("Success Rate: %.1f%%", rate), sx, 170);

                    g2.setFont(StyleManager.FONT_SMALL);
                    g2.setColor(StyleManager.TEXT_DIM);
                    g2.drawString("Enrolled Users: " + mainWindow.getFileManager().getProfileCount(), sx, 210);
                } else {
                    g2.setColor(StyleManager.TEXT_DIM);
                    g2.setFont(StyleManager.FONT_BODY);
                    g2.drawString("No data. Click 'Refresh Analytics' to load.", 40, h / 2);
                }
                g2.dispose();
            }
        };
        analyticsChartPanel.setBackground(StyleManager.BG_CARD);
        p.add(analyticsChartPanel, BorderLayout.CENTER);

        return p;
    }

    // ═══════════════════ Tab 4: ML Threshold ════════════════════

    private JPanel buildMLTab() {
        mlThresholdPanel = new MLThresholdPanel(mainWindow);
        return mlThresholdPanel;
    }

    // ═══════════════════ Tab 5: Settings ════════════════════════

    private JPanel buildSettingsTab() {
        JPanel p = StyleManager.panel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        ConfigManager config = mainWindow.getConfigManager();

        // ── Phase 2 Feature Toggles ─────────────────────────────
        JPanel featureCard = StyleManager.card();
        featureCard.setLayout(new BoxLayout(featureCard, BoxLayout.Y_AXIS));
        featureCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        featureCard.setAlignmentX(LEFT_ALIGNMENT);

        JLabel featureTitle = StyleManager.label("Phase 2 Features",
                StyleManager.FONT_BODY, StyleManager.ACCENT);
        featureTitle.setAlignmentX(LEFT_ALIGNMENT);
        featureCard.add(featureTitle);
        featureCard.add(Box.createVerticalStrut(8));

        chk2FA = createStyledCheckBox("Two-Factor Authentication (OTP after keystroke auth)",
                config.getBoolean("enable_2fa", false));
        chkMouseDynamics = createStyledCheckBox("Mouse Dynamics Biometrics (70/30 combined scoring)",
                config.getBoolean("enable_mouse_dynamics", false));
        chkMLThreshold = createStyledCheckBox("ML-Based Adaptive Threshold (per-user learning)",
                config.getBoolean("enable_ml_threshold", false));

        featureCard.add(chk2FA);
        featureCard.add(Box.createVerticalStrut(4));
        featureCard.add(chkMouseDynamics);
        featureCard.add(Box.createVerticalStrut(4));
        featureCard.add(chkMLThreshold);

        p.add(featureCard);
        p.add(Box.createVerticalStrut(12));

        // ── Threshold slider ────────────────────────────────────
        JPanel threshCard = StyleManager.card();
        threshCard.setLayout(new BoxLayout(threshCard, BoxLayout.Y_AXIS));
        threshCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        threshCard.setAlignmentX(LEFT_ALIGNMENT);

        JLabel threshTitle = StyleManager.label("Authentication Threshold",
                StyleManager.FONT_BODY, StyleManager.TEXT_PRIMARY);
        threshTitle.setAlignmentX(LEFT_ALIGNMENT);
        threshCard.add(threshTitle);
        threshCard.add(Box.createVerticalStrut(6));

        thresholdSlider = new JSlider(40, 80, (int) SystemConstants.DEFAULT_THRESHOLD);
        thresholdSlider.setMajorTickSpacing(10);
        thresholdSlider.setMinorTickSpacing(5);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        thresholdSlider.setBackground(StyleManager.BG_CARD);
        thresholdSlider.setForeground(StyleManager.TEXT_PRIMARY);
        thresholdSlider.setAlignmentX(LEFT_ALIGNMENT);
        threshCard.add(thresholdSlider);

        lblThresholdVal = StyleManager.label("Current: " + thresholdSlider.getValue() + "%",
                StyleManager.FONT_SMALL, StyleManager.ACCENT);
        lblThresholdVal.setAlignmentX(LEFT_ALIGNMENT);
        threshCard.add(lblThresholdVal);

        thresholdSlider.addChangeListener(e -> {
            lblThresholdVal.setText("Current: " + thresholdSlider.getValue() + "%");
        });

        p.add(threshCard);
        p.add(Box.createVerticalStrut(12));

        // Save settings
        JButton btnSave = StyleManager.button("Save All Settings");
        btnSave.setAlignmentX(LEFT_ALIGNMENT);
        btnSave.addActionListener(e -> saveAllSettings());
        p.add(btnSave);
        p.add(Box.createVerticalStrut(16));

        // Backup
        JPanel backupCard = StyleManager.card();
        backupCard.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        backupCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        backupCard.setAlignmentX(LEFT_ALIGNMENT);
        JButton btnBackup = StyleManager.secondaryButton("Create Backup");
        JButton btnRestore = StyleManager.secondaryButton("Restore Backup");
        backupCard.add(btnBackup);
        backupCard.add(btnRestore);
        p.add(backupCard);
        p.add(Box.createVerticalStrut(16));

        btnBackup.addActionListener(e -> {
            BackupManager bm = new BackupManager();
            String path = bm.createSystemBackup();
            if (path != null) {
                GUIUtils.showInfo(this, "Backup created:\n" + path, "Backup");
            } else {
                GUIUtils.showWarning(this, "Backup failed or no data to backup.");
            }
        });
        btnRestore.addActionListener(e -> {
            if (GUIUtils.confirm(this, "Restore will overwrite current data. Continue?", "Confirm Restore")) {
                BackupManager bm = new BackupManager();
                JFileChooser fc = new JFileChooser(SystemConstants.BACKUP_DIR);
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    boolean ok = bm.restoreFromBackup(fc.getSelectedFile().getAbsolutePath());
                    if (ok) GUIUtils.showInfo(this, "Restore complete.", "Restore");
                    else GUIUtils.showError(this, "Restore failed.");
                }
            }
        });

        // Reset
        JButton btnReset = StyleManager.dangerButton("Reset System");
        btnReset.setAlignmentX(LEFT_ALIGNMENT);
        btnReset.addActionListener(e -> {
            if (GUIUtils.confirm(this, "This will delete ALL profiles and logs.\nAre you sure?",
                    "⚠ Full System Reset")) {
                File usersDir = new File(SystemConstants.USERS_DIR);
                if (usersDir.exists()) {
                    File[] files = usersDir.listFiles();
                    if (files != null) for (File f : files) f.delete();
                }
                GUIUtils.showInfo(this, "System reset complete.", "Reset");
                refreshUsers();
            }
        });
        p.add(btnReset);
        p.add(Box.createVerticalGlue());

        return p;
    }

    /**
     * Saves all settings including feature toggles to ConfigManager.
     */
    private void saveAllSettings() {
        ConfigManager config = mainWindow.getConfigManager();

        // Save feature toggles
        config.set("enable_2fa", String.valueOf(chk2FA.isSelected()));
        config.set("enable_mouse_dynamics", String.valueOf(chkMouseDynamics.isSelected()));
        config.set("enable_ml_threshold", String.valueOf(chkMLThreshold.isSelected()));

        // Save threshold
        try {
            String path = SystemConstants.THRESHOLDS_DIR + File.separator + "system_threshold.txt";
            Files.write(Paths.get(path), String.valueOf(thresholdSlider.getValue()).getBytes());
        } catch (IOException ex) {
            GUIUtils.showError(this, "Failed to save threshold: " + ex.getMessage());
            return;
        }

        config.saveConfig();

        // Update status bar
        mainWindow.updateFeatureStatus();

        GUIUtils.showInfo(this,
                "All settings saved!\n\n" +
                "Threshold: " + thresholdSlider.getValue() + "%\n" +
                "2FA: " + (chk2FA.isSelected() ? "Enabled" : "Disabled") + "\n" +
                "Mouse Dynamics: " + (chkMouseDynamics.isSelected() ? "Enabled" : "Disabled") + "\n" +
                "ML Threshold: " + (chkMLThreshold.isSelected() ? "Enabled" : "Disabled"),
                "Settings Saved");
    }

    /**
     * Creates a styled checkbox matching the dark orange theme.
     */
    private JCheckBox createStyledCheckBox(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setFont(StyleManager.FONT_SMALL);
        cb.setForeground(StyleManager.TEXT_PRIMARY);
        cb.setBackground(StyleManager.BG_CARD);
        cb.setFocusPainted(false);
        cb.setAlignmentX(LEFT_ALIGNMENT);
        return cb;
    }

    /** Called when the admin tab is shown. */
    public void onShow() {
        refreshUsers();
        refreshLogs();
        if (mlThresholdPanel != null) mlThresholdPanel.refreshData();

        // Sync checkbox state with config
        ConfigManager config = mainWindow.getConfigManager();
        if (chk2FA != null) chk2FA.setSelected(config.getBoolean("enable_2fa", false));
        if (chkMouseDynamics != null) chkMouseDynamics.setSelected(config.getBoolean("enable_mouse_dynamics", false));
        if (chkMLThreshold != null) chkMLThreshold.setSelected(config.getBoolean("enable_ml_threshold", false));

        // Start auto-refresh if on Logs tab
        if (tabs.getSelectedIndex() == 1) startAutoRefresh();
    }
}
