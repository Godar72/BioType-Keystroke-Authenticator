package com.keystroke.auth.gui;

import com.keystroke.auth.gui.utils.StyleManager;
import javax.swing.*;
import java.awt.*;

/**
 * CircularGaugePanel.java — Custom-painted circular confidence gauge.
 *
 * Features:
 *   - Dark background arc with colored value overlay
 *   - Color gradient: red (0-50%) → yellow (50-70%) → green (70-100%)
 *   - Large bold percentage number in center
 *   - Outer ring in orange accent color
 *   - Smooth animation when value changes (Timer-based increment)
 *   - Configurable status label below gauge
 *
 * Usage: AuthPanel right side — biometric confidence display.
 */
public class CircularGaugePanel extends JPanel {

    private double currentValue = 0;
    private double targetValue = 0;
    private String statusText = "BIOMETRIC CONFIDENCE";
    private final Timer animTimer;

    public CircularGaugePanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(240, 260));

        animTimer = new Timer(16, e -> {
            if (Math.abs(currentValue - targetValue) > 0.3) {
                currentValue += (targetValue - currentValue) * 0.06;
                repaint();
            } else if (currentValue != targetValue) {
                currentValue = targetValue;
                repaint();
            }
        });
        animTimer.start();
    }

    /** Animate to the given value (0-100). */
    public void setValue(double value) {
        this.targetValue = Math.max(0, Math.min(100, value));
    }

    /** Jump immediately to the given value without animation. */
    public void setValueImmediate(double value) {
        this.targetValue = Math.max(0, Math.min(100, value));
        this.currentValue = this.targetValue;
        repaint();
    }

    /** Set the label displayed below the gauge. */
    public void setStatusText(String text) {
        this.statusText = text;
        repaint();
    }

    /** Reset gauge to zero with default label. */
    public void reset() {
        currentValue = 0;
        targetValue = 0;
        statusText = "BIOMETRIC CONFIDENCE";
        repaint();
    }

    public double getCurrentValue() { return currentValue; }

    // ─── Color gradient: red → yellow → green ────────────────────

    private Color getGaugeColor(double pct) {
        if (pct >= 70) {
            return StyleManager.SUCCESS;
        } else if (pct >= 50) {
            float t = (float) ((pct - 50) / 20.0);
            return new Color(
                    (int) (255 + (80 - 255) * t),
                    (int) (200 + (220 - 200) * t),
                    (int) (40 + (100 - 40) * t));
        } else {
            float t = (float) (pct / 50.0);
            return new Color(255, (int) (60 + (200 - 60) * t), (int) (50 + (40 - 50) * t));
        }
    }

    // ─── Paint ───────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2 - 15;
        int radius = Math.min(w, h - 50) / 2 - 20;
        if (radius < 30) radius = 30;

        // ── Outer decorative ring (orange accent, subtle) ────────
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(StyleManager.ACCENT.getRed(),
                StyleManager.ACCENT.getGreen(),
                StyleManager.ACCENT.getBlue(), 40));
        g2.drawOval(cx - radius - 10, cy - radius - 10,
                (radius + 10) * 2, (radius + 10) * 2);

        // ── Background arc track ─────────────────────────────────
        g2.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(40, 32, 24));
        g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, 225, -270);

        // ── Value arc ────────────────────────────────────────────
        if (currentValue > 0) {
            Color gaugeColor = getGaugeColor(currentValue);
            int arc = (int) (currentValue / 100.0 * 270);

            // Glow halo
            g2.setStroke(new BasicStroke(24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(gaugeColor.getRed(), gaugeColor.getGreen(),
                    gaugeColor.getBlue(), 22));
            g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, 225, -arc);

            // Main arc
            g2.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(gaugeColor);
            g2.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, 225, -arc);

            // Centre percentage
            g2.setFont(StyleManager.FONT_HERO);
            g2.setColor(gaugeColor);
            String pctText = String.format("%.1f%%", currentValue);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pctText, cx - fm.stringWidth(pctText) / 2,
                    cy + fm.getAscent() / 3);
        } else {
            g2.setFont(StyleManager.FONT_HERO);
            g2.setColor(StyleManager.TEXT_DIM);
            String pctText = "—";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pctText, cx - fm.stringWidth(pctText) / 2,
                    cy + fm.getAscent() / 3);
        }

        // ── Status label ─────────────────────────────────────────
        g2.setFont(StyleManager.FONT_STAT_LABEL);
        g2.setColor(StyleManager.TEXT_SECONDARY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(statusText, cx - fm.stringWidth(statusText) / 2,
                cy + radius + 32);

        g2.dispose();
    }
}
