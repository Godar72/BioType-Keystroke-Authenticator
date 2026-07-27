package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import java.awt.*;

/**
 * MonitoringPanel.java — Premium real-time system monitoring with animated
 * metric cards, memory gauge, and threat alert feed.
 */
public class MonitoringPanel extends JPanel {

    private final MainWindow mainWindow;
    private JLabel lblMemory, lblProfiles, lblThreshold, lblUptime;
    private JPanel alertsPanel;
    private final long startTime = System.currentTimeMillis();

    public MonitoringPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 12));
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        buildUI();
        Timer timer = new Timer(2000, e -> refresh());
        timer.start();
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
        // Header
        JLabel title = StyleManager.label("[M]  Real-Time Monitoring",
                StyleManager.FONT_SUBTITLE, StyleManager.ACCENT);
        add(title, BorderLayout.NORTH);

        // Metric cards grid
        JPanel grid = new JPanel(new GridLayout(1, 4, 12, 0));
        grid.setOpaque(false);

        grid.add(metricCard("M", "Memory", lblMemory = StyleManager.label("—",
                StyleManager.FONT_SUBTITLE, StyleManager.ACCENT)));
        grid.add(metricCard("U", "Profiles", lblProfiles = StyleManager.label("—",
                StyleManager.FONT_SUBTITLE, StyleManager.ACCENT2)));
        grid.add(metricCard("*", "Threshold", lblThreshold = StyleManager.label("—",
                StyleManager.FONT_SUBTITLE, StyleManager.WARNING)));
        grid.add(metricCard("T", "Uptime", lblUptime = StyleManager.label("—",
                StyleManager.FONT_SUBTITLE, StyleManager.SUCCESS)));

        add(grid, BorderLayout.CENTER);

        // Alerts card
        JPanel alertsCard = StyleManager.card();
        alertsCard.setLayout(new BorderLayout(0, 8));
        alertsCard.add(StyleManager.label("[!]  Threat Alerts",
                StyleManager.FONT_BODY, StyleManager.WARNING), BorderLayout.NORTH);
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setOpaque(false);
        JScrollPane sp = GUIUtils.darkScrollPane(alertsPanel);
        sp.setPreferredSize(new Dimension(0, 140));
        alertsCard.add(sp, BorderLayout.CENTER);
        add(alertsCard, BorderLayout.SOUTH);
    }

    private JPanel metricCard(String icon, String title, JLabel valueLabel) {
        JPanel card = StyleManager.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel ic = StyleManager.label(icon, new Font("Segoe UI Emoji", Font.PLAIN, 28),
                StyleManager.TEXT_DIM);
        ic.setAlignmentX(LEFT_ALIGNMENT);
        card.add(ic);
        card.add(Box.createVerticalStrut(8));

        JLabel t = StyleManager.label(title, StyleManager.FONT_TINY, StyleManager.TEXT_SECONDARY);
        t.setAlignmentX(LEFT_ALIGNMENT);
        card.add(t);
        card.add(Box.createVerticalStrut(4));

        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(valueLabel);
        return card;
    }

    public void refresh() {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long total = rt.totalMemory() / (1024 * 1024);
        lblMemory.setText(used + " / " + total + " MB");

        lblProfiles.setText(String.valueOf(mainWindow.getFileManager().getProfileCount()));

        try {
            java.nio.file.Path tp = java.nio.file.Paths.get(
                    SystemConstants.THRESHOLDS_DIR, "system_threshold.txt");
            if (java.nio.file.Files.exists(tp)) {
                String t = new String(java.nio.file.Files.readAllBytes(tp)).trim();
                lblThreshold.setText(t + "%");
            } else {
                lblThreshold.setText(SystemConstants.DEFAULT_THRESHOLD + "%");
            }
        } catch (Exception ex) {
            lblThreshold.setText("—");
        }

        long sec = (System.currentTimeMillis() - startTime) / 1000;
        lblUptime.setText(String.format("%02d:%02d:%02d", sec / 3600, (sec / 60) % 60, sec % 60));
    }

    public void addAlert(String message) {
        JLabel alert = StyleManager.label("[!] " + message,
                StyleManager.FONT_SMALL, StyleManager.DANGER);
        alert.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        alertsPanel.add(alert);
        alertsPanel.revalidate();
    }
}
