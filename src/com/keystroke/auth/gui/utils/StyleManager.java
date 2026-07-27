package com.keystroke.auth.gui.utils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * StyleManager.java — Cybersecurity terminal aesthetic design system.
 * Inspired by fintech dashboards: deep dark, cyan + orange accents,
 * monospaced fonts, thin-border glassmorphism, and glow effects.
 */
public final class StyleManager {

    private StyleManager() {}

    // ── Colour palette — Warm Orange Premium Dark ─────────────
    public static final Color BG_PRIMARY     = new Color(14, 10, 8);
    public static final Color BG_SECONDARY   = new Color(22, 16, 12);
    public static final Color BG_CARD        = new Color(28, 22, 18);
    public static final Color BG_CARD_HOVER  = new Color(38, 30, 24);
    public static final Color BG_INPUT       = new Color(18, 14, 10);
    public static final Color BG_ELEVATED    = new Color(34, 26, 20);

    // Accents — Orange family
    public static final Color CYAN           = new Color(255, 160, 40);   // amber-orange (primary)
    public static final Color CYAN_DIM       = new Color(180, 110, 30);   // muted amber
    public static final Color CYAN_GLOW      = new Color(255, 140, 0, 30);
    public static final Color ORANGE         = new Color(255, 120, 0);    // deep orange
    public static final Color ORANGE_BRIGHT  = new Color(255, 180, 60);   // bright amber
    public static final Color ORANGE_GLOW    = new Color(255, 120, 0, 35);
    public static final Color ACCENT         = new Color(255, 150, 30);   // golden amber
    public static final Color ACCENT2        = new Color(255, 90, 0);     // burnt orange
    public static final Color ACCENT_GLOW    = new Color(255, 150, 30, 30);

    public static final Color SUCCESS        = new Color(80, 220, 100);
    public static final Color SUCCESS_GLOW   = new Color(80, 220, 100, 25);
    public static final Color WARNING        = new Color(255, 220, 60);
    public static final Color DANGER         = new Color(255, 60, 50);
    public static final Color DANGER_GLOW    = new Color(255, 60, 50, 25);

    public static final Color TEXT_PRIMARY   = new Color(245, 230, 210);  // warm cream
    public static final Color TEXT_SECONDARY = new Color(160, 135, 110);  // warm gray
    public static final Color TEXT_DIM       = new Color(80, 65, 50);     // warm muted
    public static final Color BORDER         = new Color(255, 140, 0, 35); // orange border
    public static final Color BORDER_ORANGE  = new Color(255, 120, 0, 55);
    public static final Color GLASS          = new Color(255, 200, 140, 5);
    public static final Color GLASS_BORDER   = new Color(255, 150, 40, 25);

    // ── Gradients ───────────────────────────────────────────────
    public static GradientPaint cyanGradient(int x, int y, int w, int h) {
        return new GradientPaint(x, y, ACCENT, x + w, y + h, ACCENT2);
    }
    public static GradientPaint orangeGradient(int x, int y, int w, int h) {
        return new GradientPaint(x, y, ORANGE, x + w, y + h, new Color(200, 70, 0));
    }
    public static GradientPaint bgGradient(int w, int h) {
        return new GradientPaint(0, 0, BG_PRIMARY, 0, h, new Color(10, 7, 5));
    }

    // ── Fonts — Monospaced tech look ────────────────────────────
    public static final Font FONT_HERO       = new Font("Consolas", Font.BOLD, 34);
    public static final Font FONT_TITLE      = new Font("Consolas", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE   = new Font("Consolas", Font.BOLD, 15);
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_TINY       = new Font("Consolas", Font.PLAIN, 10);
    public static final Font FONT_MONO       = new Font("Consolas", Font.PLAIN, 13);
    public static final Font FONT_MONO_SM    = new Font("Consolas", Font.PLAIN, 11);
    public static final Font FONT_BUTTON     = new Font("Consolas", Font.BOLD, 12);
    public static final Font FONT_STAT       = new Font("Consolas", Font.BOLD, 28);
    public static final Font FONT_STAT_LABEL = new Font("Consolas", Font.PLAIN, 9);

    public static final int PAD = 14;
    public static final int RADIUS = 8;

    // ── Label ───────────────────────────────────────────────────
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }
    public static JLabel label(String text) {
        return label(text, FONT_BODY, TEXT_PRIMARY);
    }

    // ── Text field — thin cyan border ───────────────────────────
    public static JTextField textField(int cols) {
        JTextField tf = new JTextField(cols) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setFont(FONT_MONO);
        tf.setOpaque(false);
        tf.setBackground(BG_INPUT);
        tf.setForeground(ORANGE_BRIGHT);
        tf.setCaretColor(ORANGE);
        tf.setSelectionColor(new Color(255, 140, 0, 50));
        tf.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 6),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return tf;
    }

    /** Password field */
    public static JPasswordField passwordField(int cols) {
        JPasswordField pf = new JPasswordField(cols);
        pf.setFont(FONT_MONO);
        pf.setBackground(BG_INPUT);
        pf.setForeground(ORANGE_BRIGHT);
        pf.setCaretColor(ORANGE);
        pf.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 6),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        return pf;
    }

    // ── Primary button — orange gradient, tech style ────────────
    public static JButton button(String text) {
        JButton b = new JButton(text) {
            private boolean hovered = false;
            {
                setContentAreaFilled(false);
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (isEnabled()) {
                    // Glow
                    if (hovered) {
                        g2.setColor(ORANGE_GLOW);
                        g2.fillRoundRect(-2, 2, w + 4, h + 2, 8, 8);
                    }
                    g2.setPaint(orangeGradient(0, 0, w, h));
                } else {
                    g2.setColor(new Color(40, 40, 50));
                }
                g2.fillRoundRect(0, 0, w, h, 6, 6);
                // Scanline effect
                if (hovered) {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fillRoundRect(0, 0, w, h / 2, 6, 6);
                }
                g2.setColor(isEnabled() ? Color.BLACK : TEXT_DIM);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                // > prefix
                String display = "> " + getText().trim();
                tx = (w - fm.stringWidth(display)) / 2;
                g2.drawString(display, tx, ty);
                g2.dispose();
            }
        };
        b.setFont(FONT_BUTTON);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width + 40, 38));
        return b;
    }

    // ── Secondary button — cyan outline ─────────────────────────
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text) {
            private boolean hovered = false;
            {
                setContentAreaFilled(false);
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(hovered ? new Color(255, 140, 0, 12) : BG_CARD);
                g2.fillRoundRect(0, 0, w, h, 6, 6);
                g2.setColor(hovered ? ACCENT : CYAN_DIM);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
                g2.setColor(hovered ? ACCENT : TEXT_PRIMARY);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText().trim(), (w - fm.stringWidth(getText().trim())) / 2,
                        (h + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(FONT_BUTTON);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width + 30, 38));
        return b;
    }

    /** Danger button */
    public static JButton dangerButton(String text) {
        JButton b = secondaryButton(text);
        // Override to red
        return b;
    }

    // ── Card — thin cyan-border glassmorphism ───────────────────
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                // Subtle top glass
                g2.setColor(GLASS);
                g2.fillRoundRect(0, 0, w, Math.min(h / 4, 40), RADIUS, RADIUS);
                // Thin cyan border
                g2.setColor(GLASS_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        return p;
    }

    /** Orange-bordered card variant */
    public static JPanel orangeCard() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, w, h, RADIUS, RADIUS);
                g2.setColor(BORDER_ORANGE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, RADIUS, RADIUS);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));
        return p;
    }

    /** Plain panel */
    public static JPanel panel() {
        JPanel p = new JPanel();
        p.setBackground(BG_PRIMARY);
        p.setOpaque(false);
        return p;
    }

    // ── Stat card — big number + small label ────────────────────
    public static JPanel statCard(String value, String label, Color valueColor) {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel valLabel = label(value, FONT_STAT, valueColor);
        valLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel descLabel = label(label.toUpperCase(), FONT_STAT_LABEL, TEXT_DIM);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(valLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(descLabel);
        return card;
    }

    // ── Progress bar — cyan gradient fill ───────────────────────
    public static JProgressBar progressBar() {
        JProgressBar pb = new JProgressBar(0, 100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(30, 22, 16));
                g2.fillRoundRect(0, 0, w, h, 4, 4);
                int fillW = (int) (w * (getValue() / 100.0));
                if (fillW > 0) {
                    g2.setPaint(cyanGradient(0, 0, w, h));
                    g2.fillRoundRect(0, 0, fillW, h, 4, 4);
                    g2.setColor(CYAN_GLOW);
                    g2.fillRoundRect(0, -1, fillW, h + 2, 4, 4);
                }
                if (isStringPainted()) {
                    g2.setColor(TEXT_PRIMARY);
                    g2.setFont(FONT_TINY);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(getString(), (w - fm.stringWidth(getString())) / 2,
                            (h + fm.getAscent() - fm.getDescent()) / 2);
                }
                g2.dispose();
            }
        };
        pb.setStringPainted(true);
        pb.setOpaque(false);
        pb.setBorderPainted(false);
        pb.setPreferredSize(new Dimension(300, 20));
        return pb;
    }

    public static TitledBorder titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                new RoundBorder(BORDER, RADIUS), title);
        tb.setTitleFont(FONT_MONO_SM);
        tb.setTitleColor(CYAN_DIM);
        return tb;
    }

    public static Color confidenceColor(double pct) {
        if (pct >= 80) return SUCCESS;
        if (pct >= 60) return ORANGE;
        return DANGER;
    }

    // ── Glow helpers ────────────────────────────────────────────
    public static void drawGlow(Graphics2D g2, int cx, int cy, int r, Color c) {
        for (int i = 4; i >= 0; i--) {
            int alpha = (int) (c.getAlpha() * (1.0 - i * 0.22));
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, alpha)));
            int rr = r + i * 14;
            g2.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }
    }

    public static void drawDotGrid(Graphics2D g2, int w, int h, int spacing) {
        g2.setColor(new Color(255, 140, 0, 6));
        for (int x = spacing; x < w; x += spacing)
            for (int y = spacing; y < h; y += spacing)
                g2.fillRect(x, y, 1, 1);
    }

    /** Draw horizontal scan lines for CRT effect */
    public static void drawScanLines(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 12));
        for (int y = 0; y < h; y += 3)
            g2.drawLine(0, y, w, y);
    }

    // ── Rounded border ──────────────────────────────────────────
    public static class RoundBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        public RoundBorder(Color c, int r) { this.color = c; this.radius = r; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(2, 2, 2, 2); }
    }
}
