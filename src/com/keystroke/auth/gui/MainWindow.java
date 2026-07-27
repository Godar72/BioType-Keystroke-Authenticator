package com.keystroke.auth.gui;

import com.keystroke.auth.*;
import com.keystroke.auth.gui.utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * MainWindow.java — Premium application frame with gradient header,
 * animated status bar, smooth panel transitions, and Phase 2 feature support.
 *
 * Phase 2 — Enhanced with MouseEnrollmentPanel, ConfigManager, feature status
 */
public class MainWindow extends JFrame {

    private final FileManager fileManager = new FileManager();
    private final ConfigManager configManager = new ConfigManager();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private LoginPanel loginPanel;
    private EnrollmentPanel enrollmentPanel;
    private AuthPanel authPanel;
    private AdminDashboard adminDashboard;
    private MonitoringPanel monitoringPanel;
    private SecurityPanel securityPanel;
    private MouseEnrollmentPanel mouseEnrollmentPanel;

    // Status bar
    private JLabel statusUser;
    private JLabel statusSystem;
    private JLabel statusUsers;
    private JLabel statusFeatures;

    private String currentUser = null;

    public MainWindow() {
        setTitle("BioType — Keystroke Dynamics Authentication System v2.0");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1120, 750);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(StyleManager.BG_PRIMARY);

        buildMenuBar();
        buildPanels();
        buildStatusBar();
        showPanel("login");
    }

    // ─── Menu bar ────────────────────────────────────────────────

    private void buildMenuBar() {
        JMenuBar mb = new JMenuBar() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(StyleManager.BG_SECONDARY);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Subtle bottom border
                g2.setColor(StyleManager.BORDER);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        mb.setBorderPainted(false);

        mb.add(menu("File",
                item("Login Screen", e -> showPanel("login")),
                null,
                item("Exit", e -> System.exit(0))));

        mb.add(menu("Users",
                item("Enroll New User", e -> {
                    String u = JOptionPane.showInputDialog(this, "Enter username:");
                    if (u != null && !u.trim().isEmpty()) {
                        enrollmentPanel.startEnrollment(u.trim());
                        showPanel("enroll");
                    }
                }),
                item("Authenticate", e -> showPanel("login"))));

        mb.add(menu("Admin",
                item("Admin Dashboard", e -> {
                    if (currentUser != null && currentUser.equals("admin")) {
                        adminDashboard.onShow();
                        showPanel("admin");
                    } else {
                        GUIUtils.showWarning(this, "Admin login required.");
                    }
                })));

        mb.add(menu("Tools",
                item("Monitoring", e -> showPanel("monitor")),
                item("Security", e -> { securityPanel.onShow(); showPanel("security"); })));

        mb.add(menu("Help",
                item("About", e -> GUIUtils.showInfo(this,
                        "BioType v2.0\n\n" +
                        "Keystroke Dynamics Biometric Authentication\n" +
                        "Phase 2 — Professional GUI Edition\n\n" +
                        "Features:\n" +
                        "  • Real-time KeyListener capture\n" +
                        "  • Euclidean Distance + MAD% scoring\n" +
                        "  • Adaptive threshold management\n" +
                        "  • Custom passphrase support\n" +
                        "  • Live timing visualization\n" +
                        "  • Two-Factor Authentication (OTP)\n" +
                        "  • Mouse Dynamics Biometrics\n" +
                        "  • ML-Based Adaptive Threshold\n\n" +
                        "© 2026 AIML 2nd Year Project",
                        "About BioType")),
                item("How It Works", e -> GUIUtils.showInfo(this,
                        "1. Enroll: set a passphrase, type it 3 times.\n" +
                        "2. Login: type your passphrase — timing is compared.\n" +
                        "3. Each keystroke's hold time & flight time are measured.\n" +
                        "4. Similarity score computed via MAD% algorithm.\n" +
                        "5. If score ≥ threshold (70%), access is granted.\n" +
                        "6. Optional: Mouse dynamics adds 30% to score.\n" +
                        "7. Optional: 2FA OTP for extra security.\n" +
                        "8. Optional: ML learns your personal threshold.",
                        "How It Works"))));

        setJMenuBar(mb);
    }

    private JMenu menu(String title, JMenuItem... items) {
        JMenu m = new JMenu(title);
        m.setFont(StyleManager.FONT_BUTTON);
        m.setForeground(StyleManager.TEXT_PRIMARY);
        for (JMenuItem mi : items) {
            if (mi == null) m.addSeparator();
            else m.add(mi);
        }
        return m;
    }

    private JMenuItem item(String text, ActionListener action) {
        JMenuItem mi = new JMenuItem(text);
        mi.setFont(StyleManager.FONT_SMALL);
        mi.setBackground(StyleManager.BG_SECONDARY);
        mi.setForeground(StyleManager.TEXT_PRIMARY);
        mi.addActionListener(action);
        return mi;
    }

    // ─── Panels ──────────────────────────────────────────────────

    private void buildPanels() {
        contentPanel.setBackground(StyleManager.BG_PRIMARY);

        loginPanel            = new LoginPanel(this);
        enrollmentPanel       = new EnrollmentPanel(this);
        authPanel             = new AuthPanel(this);
        adminDashboard        = new AdminDashboard(this);
        monitoringPanel       = new MonitoringPanel(this);
        securityPanel         = new SecurityPanel(this);
        mouseEnrollmentPanel  = new MouseEnrollmentPanel(this);

        contentPanel.add(loginPanel,           "login");
        contentPanel.add(enrollmentPanel,      "enroll");
        contentPanel.add(authPanel,            "auth");
        contentPanel.add(adminDashboard,       "admin");
        contentPanel.add(monitoringPanel,      "monitor");
        contentPanel.add(securityPanel,        "security");
        contentPanel.add(mouseEnrollmentPanel, "mouseEnroll");

        add(contentPanel, BorderLayout.CENTER);
    }

    // ─── Status bar ──────────────────────────────────────────────

    private void buildStatusBar() {
        JPanel bar = new JPanel(new GridLayout(1, 4)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(StyleManager.BG_SECONDARY);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Top accent line
                g2.setPaint(StyleManager.cyanGradient(0, 0, getWidth(), 2));
                g2.fillRect(0, 0, getWidth(), 2);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        bar.setPreferredSize(new Dimension(0, 28));

        statusUser     = StyleManager.label("  User: none", StyleManager.FONT_TINY, StyleManager.TEXT_SECONDARY);
        statusSystem   = StyleManager.label("System: Ready", StyleManager.FONT_TINY, StyleManager.SUCCESS);
        statusUsers    = StyleManager.label("Profiles: " + fileManager.getProfileCount(),
                StyleManager.FONT_TINY, StyleManager.TEXT_SECONDARY);
        statusFeatures = StyleManager.label(getFeatureStatusText(),
                StyleManager.FONT_TINY, StyleManager.TEXT_DIM);

        bar.add(statusUser);
        bar.add(statusSystem);
        bar.add(statusUsers);
        bar.add(statusFeatures);
        add(bar, BorderLayout.SOUTH);
    }

    private void updateStatusBar() {
        statusUser.setText("  User: " + (currentUser != null ? currentUser : "none"));
        statusUsers.setText("Profiles: " + fileManager.getProfileCount());
        statusFeatures.setText(getFeatureStatusText());
    }

    /**
     * Builds the feature status string for the status bar.
     */
    private String getFeatureStatusText() {
        StringBuilder sb = new StringBuilder();
        if (configManager.getBoolean("enable_2fa", false)) sb.append("2FA ");
        if (configManager.getBoolean("enable_mouse_dynamics", false)) sb.append("Mouse ");
        if (configManager.getBoolean("enable_ml_threshold", false)) sb.append("ML ");
        return sb.length() > 0 ? "Features: " + sb.toString().trim() : "Features: none";
    }

    /**
     * Called from AdminDashboard after saving settings to update the status bar.
     */
    public void updateFeatureStatus() {
        if (statusFeatures != null) {
            statusFeatures.setText(getFeatureStatusText());
        }
    }

    // ─── Public API ──────────────────────────────────────────────

    public void showPanel(String name) {
        cardLayout.show(contentPanel, name);
        if ("login".equals(name)) loginPanel.reset();
        updateStatusBar();
    }

    public FileManager            getFileManager()          { return fileManager; }
    public ConfigManager          getConfigManager()        { return configManager; }
    public EnrollmentPanel        getEnrollmentPanel()      { return enrollmentPanel; }
    public AuthPanel              getAuthPanel()            { return authPanel; }
    public AdminDashboard         getAdminDashboard()       { return adminDashboard; }
    public MonitoringPanel        getMonitoringPanel()      { return monitoringPanel; }
    public MouseEnrollmentPanel   getMouseEnrollmentPanel() { return mouseEnrollmentPanel; }
    public String                 getCurrentUser()          { return currentUser; }

    public void setCurrentUser(String user) {
        this.currentUser = user;
        updateStatusBar();
    }
}
