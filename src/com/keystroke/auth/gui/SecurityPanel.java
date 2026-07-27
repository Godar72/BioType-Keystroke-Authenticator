package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;

/**
 * SecurityPanel.java — Premium security dashboard with failed attempts table,
 * threat analysis, and auto-generated recommendations.
 */
public class SecurityPanel extends JPanel {

    private final MainWindow mainWindow;
    private DefaultTableModel failedTableModel;
    private JPanel recommendationsPanel;
    private JLabel lblTotalAttempts;
    private JLabel lblRiskLevel;

    public SecurityPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        setLayout(new BorderLayout(0, 12));
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
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
        // Header row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel title = StyleManager.label("[S]  Security Dashboard",
                StyleManager.FONT_SUBTITLE, StyleManager.ACCENT);
        headerRow.add(title, BorderLayout.WEST);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        badges.setOpaque(false);
        lblTotalAttempts = StyleManager.label("Failures: 0",
                StyleManager.FONT_SMALL, StyleManager.DANGER);
        lblRiskLevel = StyleManager.label("Risk: LOW",
                StyleManager.FONT_SMALL, StyleManager.SUCCESS);
        badges.add(lblTotalAttempts);
        badges.add(lblRiskLevel);
        headerRow.add(badges, BorderLayout.EAST);
        add(headerRow, BorderLayout.NORTH);

        // Main split
        JPanel centre = new JPanel(new GridLayout(1, 2, 12, 0));
        centre.setOpaque(false);

        // Left: failed attempts table
        JPanel failedCard = StyleManager.card();
        failedCard.setLayout(new BorderLayout(0, 8));
        failedCard.add(StyleManager.label("Failed Authentication Attempts",
                StyleManager.FONT_BODY, StyleManager.DANGER), BorderLayout.NORTH);

        failedTableModel = new DefaultTableModel(
                new String[]{"Time", "User", "Score", "Risk"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable failedTable = new JTable(failedTableModel);
        GUIUtils.styleTable(failedTable);
        failedCard.add(GUIUtils.darkScrollPane(failedTable), BorderLayout.CENTER);

        JButton btnRefresh = StyleManager.secondaryButton("  Refresh  ");
        btnRefresh.addActionListener(e -> refreshFailedAttempts());
        failedCard.add(btnRefresh, BorderLayout.SOUTH);
        centre.add(failedCard);

        // Right: recommendations
        JPanel recCard = StyleManager.card();
        recCard.setLayout(new BorderLayout(0, 8));
        recCard.add(StyleManager.label("Security Recommendations",
                StyleManager.FONT_BODY, StyleManager.WARNING), BorderLayout.NORTH);
        recommendationsPanel = new JPanel();
        recommendationsPanel.setLayout(new BoxLayout(recommendationsPanel, BoxLayout.Y_AXIS));
        recommendationsPanel.setOpaque(false);
        recCard.add(GUIUtils.darkScrollPane(recommendationsPanel), BorderLayout.CENTER);
        centre.add(recCard);

        add(centre, BorderLayout.CENTER);
    }

    public void refreshFailedAttempts() {
        failedTableModel.setRowCount(0);
        recommendationsPanel.removeAll();

        File logsDir = new File(SystemConstants.LOGS_DIR);
        if (!logsDir.exists()) {
            addRecommendation("[OK] No logs found. System is clean.", StyleManager.SUCCESS);
            lblTotalAttempts.setText("Failures: 0");
            lblRiskLevel.setText("Risk: LOW");
            lblRiskLevel.setForeground(StyleManager.SUCCESS);
            update();
            return;
        }

        File[] logFiles = logsDir.listFiles((d, n) -> n.startsWith("auth_") && n.endsWith(".txt"));
        if (logFiles == null) return;

        int totalFailed = 0;
        java.util.Map<String, Integer> failCounts = new java.util.TreeMap<>();

        for (File lf : logFiles) {
            try (BufferedReader br = new BufferedReader(new FileReader(lf))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("FAILED")) {
                        String[] parts = line.split("\\|");
                        String ts = parts.length > 0 ? parts[0].trim() : "?";
                        String user = parts.length > 1 ? parts[1].replace("User:", "").trim() : "?";
                        String conf = parts.length > 3 ? parts[3].replace("Similarity:", "").trim() : "—";
                        String risk = parts.length > 5 ? parts[5].replace("Risk:", "").trim() : "—";
                        failedTableModel.addRow(new Object[]{ts, user, conf, risk});
                        totalFailed++;
                        failCounts.merge(user, 1, Integer::sum);
                    }
                }
            } catch (IOException ignored) {}
        }

        lblTotalAttempts.setText("Failures: " + totalFailed);

        if (totalFailed == 0) {
            addRecommendation("[OK] No failed attempts detected.", StyleManager.SUCCESS);
            lblRiskLevel.setText("Risk: LOW");
            lblRiskLevel.setForeground(StyleManager.SUCCESS);
        } else if (totalFailed < 5) {
            lblRiskLevel.setText("Risk: LOW");
            lblRiskLevel.setForeground(StyleManager.WARNING);
            addRecommendation("Minor activity detected. Monitor ongoing.", StyleManager.WARNING);
        } else {
            lblRiskLevel.setText("Risk: HIGH");
            lblRiskLevel.setForeground(StyleManager.DANGER);
            addRecommendation("[!] High failure count! Review auth logs.", StyleManager.DANGER);
        }

        for (java.util.Map.Entry<String, Integer> entry : failCounts.entrySet()) {
            if (entry.getValue() >= 3) {
                addRecommendation("User '" + entry.getKey() + "' has " + entry.getValue() +
                        " failures — possible impostor attempt.", StyleManager.DANGER);
            }
        }

        if (totalFailed > 10) {
            addRecommendation("Consider raising the threshold.", StyleManager.WARNING);
        }
        addRecommendation("Review logs periodically for suspicious patterns.", StyleManager.TEXT_SECONDARY);

        update();
    }

    private void addRecommendation(String text, Color c) {
        JLabel l = StyleManager.label("  • " + text, StyleManager.FONT_SMALL, c);
        l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        recommendationsPanel.add(l);
    }

    private void update() {
        recommendationsPanel.revalidate();
        recommendationsPanel.repaint();
    }

    public void onShow() {
        refreshFailedAttempts();
    }
}
