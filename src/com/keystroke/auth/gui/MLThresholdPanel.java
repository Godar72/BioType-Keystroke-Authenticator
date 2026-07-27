package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;

/**
 * MLThresholdPanel.java — Admin dashboard panel for viewing and managing
 * the ML-based adaptive threshold learning system. Shows per-user learning
 * progress, confidence intervals, pattern change detection, and threshold
 * recommendation reports.
 *
 * Phase 2 Enhancement — ML-Based Adaptive Threshold GUI
 */
public class MLThresholdPanel extends JPanel {

    private final MainWindow mainWindow;
    private final AdaptiveThresholdML mlEngine = new AdaptiveThresholdML();

    private DefaultTableModel tableModel;
    private JTextArea reportArea;
    private JPanel ciPanel;   // Confidence interval visualization

    // Selected user state
    private String selectedUser = null;
    private double[] selectedCI = null;
    private double selectedThreshold = 0;

    public MLThresholdPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 8));
        setBackground(StyleManager.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        buildUI();
    }

    private void buildUI() {
        // ── Top: User table ──────────────────────────────────────
        tableModel = new DefaultTableModel(
                new String[]{"Username", "Data Points", "Personalized Threshold", "Pattern Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        GUIUtils.styleTable(table);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    selectedUser = (String) tableModel.getValueAt(row, 0);
                    loadUserReport(selectedUser);
                }
            }
        });

        JScrollPane tableScroll = GUIUtils.darkScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(0, 180));

        JPanel topPanel = StyleManager.panel();
        topPanel.setLayout(new BorderLayout(0, 6));

        JPanel topHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topHeader.setOpaque(false);
        JLabel tableTitle = StyleManager.label("ML Learning Status — Per User",
                StyleManager.FONT_BODY, StyleManager.ACCENT);
        JButton btnRefresh = StyleManager.button("Refresh");
        btnRefresh.addActionListener(e -> refreshData());
        topHeader.add(tableTitle);
        topHeader.add(btnRefresh);
        topPanel.add(topHeader, BorderLayout.NORTH);
        topPanel.add(tableScroll, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // ── Centre: Report + Confidence Interval ─────────────────
        JPanel centre = new JPanel(new GridLayout(1, 2, 10, 0));
        centre.setOpaque(false);

        // Left: text report
        JPanel reportCard = StyleManager.card();
        reportCard.setLayout(new BorderLayout(0, 4));
        JLabel reportTitle = StyleManager.label("Threshold Report",
                StyleManager.FONT_BODY, StyleManager.TEXT_PRIMARY);
        reportCard.add(reportTitle, BorderLayout.NORTH);

        reportArea = new JTextArea("Select a user to view their ML threshold report.");
        reportArea.setEditable(false);
        reportArea.setFont(StyleManager.FONT_MONO_SM);
        reportArea.setBackground(StyleManager.BG_CARD);
        reportArea.setForeground(StyleManager.TEXT_PRIMARY);
        reportArea.setCaretColor(StyleManager.ACCENT);
        reportArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        reportCard.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        centre.add(reportCard);

        // Right: confidence interval visualization
        JPanel ciCard = StyleManager.card();
        ciCard.setLayout(new BorderLayout(0, 4));
        JLabel ciTitle = StyleManager.label("Confidence Interval",
                StyleManager.FONT_BODY, StyleManager.TEXT_PRIMARY);
        ciCard.add(ciTitle, BorderLayout.NORTH);

        ciPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawConfidenceInterval((Graphics2D) g.create());
            }
        };
        ciPanel.setBackground(StyleManager.BG_CARD);
        ciCard.add(ciPanel, BorderLayout.CENTER);
        centre.add(ciCard);

        add(centre, BorderLayout.CENTER);
    }

    private void drawConfidenceInterval(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = ciPanel.getWidth(), h = ciPanel.getHeight();

        if (selectedCI == null || selectedUser == null) {
            g2.setColor(StyleManager.TEXT_DIM);
            g2.setFont(StyleManager.FONT_BODY);
            g2.drawString("Select a user with sufficient data", 20, h / 2);
            g2.dispose();
            return;
        }

        double lower = selectedCI[0];
        double mean = selectedCI[1];
        double upper = selectedCI[2];
        double threshold = selectedThreshold;

        int marginX = 50, marginY = 40;
        int barW = w - marginX * 2;
        int barH = 30;
        int barY = h / 2 - barH / 2;

        // Scale: 0-100
        double scale = barW / 100.0;

        // Background bar
        g2.setColor(new Color(40, 30, 20));
        g2.fillRoundRect(marginX, barY, barW, barH, 8, 8);

        // Confidence interval range
        int ciX = marginX + (int) (lower * scale);
        int ciW = (int) ((upper - lower) * scale);
        g2.setColor(new Color(255, 160, 40, 50));
        g2.fillRoundRect(ciX, barY, ciW, barH, 6, 6);
        g2.setColor(new Color(255, 160, 40, 100));
        g2.drawRoundRect(ciX, barY, ciW, barH, 6, 6);

        // Mean marker
        int meanX = marginX + (int) (mean * scale);
        g2.setColor(StyleManager.ACCENT);
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(meanX, barY - 8, meanX, barY + barH + 8);
        g2.fillOval(meanX - 5, barY + barH / 2 - 5, 10, 10);

        // Threshold marker
        int threshX = marginX + (int) (threshold * scale);
        g2.setColor(StyleManager.WARNING);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{5f, 5f}, 0f));
        g2.drawLine(threshX, barY - 12, threshX, barY + barH + 12);

        // Labels
        g2.setFont(StyleManager.FONT_TINY);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(StyleManager.ACCENT);
        g2.drawString(String.format("Mean: %.1f%%", mean), meanX - 25, barY - 14);

        g2.setColor(StyleManager.WARNING);
        g2.drawString(String.format("Threshold: %.1f%%", threshold), threshX - 30, barY + barH + 26);

        g2.setColor(StyleManager.TEXT_DIM);
        g2.drawString(String.format("%.1f%%", lower), ciX - 10, barY + barH + 16);
        g2.drawString(String.format("%.1f%%", upper), ciX + ciW - 10, barY + barH + 16);

        // Scale markers
        g2.setColor(new Color(255, 255, 255, 30));
        for (int pct = 0; pct <= 100; pct += 10) {
            int x = marginX + (int) (pct * scale);
            g2.drawLine(x, barY + barH + 2, x, barY + barH + 6);
            if (pct % 20 == 0) {
                g2.setFont(StyleManager.FONT_TINY);
                g2.drawString(pct + "%", x - 8, barY + barH + 40);
            }
        }

        // Legend
        int ly = 20;
        g2.setFont(StyleManager.FONT_TINY);
        g2.setColor(StyleManager.ACCENT);
        g2.fillRect(w - 150, ly, 10, 10);
        g2.drawString("Mean Score", w - 135, ly + 9);
        g2.setColor(StyleManager.WARNING);
        g2.fillRect(w - 150, ly + 16, 10, 10);
        g2.drawString("Threshold", w - 135, ly + 25);
        g2.setColor(new Color(255, 160, 40, 80));
        g2.fillRect(w - 150, ly + 32, 10, 10);
        g2.setColor(StyleManager.TEXT_DIM);
        g2.drawString("±1σ Range", w - 135, ly + 41);

        g2.dispose();
    }

    /**
     * Refreshes the user table with current ML data.
     */
    public void refreshData() {
        tableModel.setRowCount(0);

        File usersDir = new File(SystemConstants.USERS_DIR);
        if (!usersDir.exists()) return;

        File[] files = usersDir.listFiles((d, n) -> n.endsWith(SystemConstants.PROFILE_EXTENSION));
        if (files == null) return;

        for (File f : files) {
            String name = f.getName().replace(SystemConstants.PROFILE_EXTENSION, "");
            int dataPoints = mlEngine.getDataPointCount(name);
            String threshold;
            String status;

            if (mlEngine.hasEnoughData(name)) {
                threshold = String.format("%.1f%%", mlEngine.getPersonalizedThreshold(name));
                status = mlEngine.detectPatternChange(name);
            } else {
                threshold = "Default (" + String.format("%.0f%%", SystemConstants.DEFAULT_THRESHOLD) + ")";
                status = "Learning... (" + dataPoints + "/5 needed)";
            }

            tableModel.addRow(new Object[]{name, dataPoints, threshold, status});
        }
    }

    /**
     * Loads the ML report for a specific user.
     */
    private void loadUserReport(String username) {
        String report = mlEngine.generateReport(username);
        reportArea.setText(report);
        reportArea.setCaretPosition(0);

        selectedCI = mlEngine.getConfidenceInterval(username);
        selectedThreshold = mlEngine.getPersonalizedThreshold(username);
        ciPanel.repaint();
    }
}
