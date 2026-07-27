package com.keystroke.auth.gui.utils;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * GUIUtils.java — Utility methods for premium dialog boxes,
 * dark-themed scroll panes, and styled table rendering.
 */
public final class GUIUtils {

    private GUIUtils() {}

    // ── Dialog shortcuts ─────────────────────────────────────────

    public static void showInfo(Component parent, String msg, String title) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarning(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void showError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg, String title) {
        return JOptionPane.showConfirmDialog(parent, msg, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ── Scroll pane ──────────────────────────────────────────────

    public static JScrollPane darkScrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBackground(StyleManager.BG_PRIMARY);
        sp.getViewport().setBackground(StyleManager.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(StyleManager.BORDER, 1));
        sp.setOpaque(false);
        // Dark scrollbars
        sp.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
        return sp;
    }

    // ── Table styling ────────────────────────────────────────────

    public static void styleTable(JTable table) {
        table.setBackground(StyleManager.BG_CARD);
        table.setForeground(StyleManager.TEXT_PRIMARY);
        table.setFont(StyleManager.FONT_SMALL);
        table.setGridColor(StyleManager.BORDER);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(255, 140, 0, 35));
        table.setSelectionForeground(StyleManager.TEXT_PRIMARY);
        table.setFocusable(false);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(StyleManager.FONT_BUTTON);
        header.setBackground(StyleManager.BG_SECONDARY);
        header.setForeground(StyleManager.TEXT_SECONDARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, StyleManager.ACCENT));
        header.setReorderingAllowed(false);

        // Alternating row colours
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? StyleManager.BG_CARD :
                            new Color(StyleManager.BG_CARD.getRed() + 5,
                                      StyleManager.BG_CARD.getGreen() + 5,
                                      StyleManager.BG_CARD.getBlue() + 8));
                }
                c.setForeground(StyleManager.TEXT_PRIMARY);
                ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return c;
            }
        });
    }

    // ── Custom dark scrollbar ────────────────────────────────────

    private static class DarkScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            this.thumbColor = new Color(80, 60, 40);
            this.trackColor = StyleManager.BG_SECONDARY;
        }
        @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(100, 75, 50, 160));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(StyleManager.BG_SECONDARY);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}
