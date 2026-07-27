package com.keystroke.auth.gui.utils;

import java.awt.*;
import java.awt.geom.*;

/**
 * ChartBuilder.java — Lightweight chart drawing helpers using Graphics2D.
 */
public final class ChartBuilder {

    private ChartBuilder() {}

    /**
     * Draw a simple vertical bar chart.
     * @param g2     graphics context
     * @param values data values
     * @param colors per-bar colours (cycled if fewer than values)
     * @param bounds drawing rectangle
     * @param labels optional per-bar labels (may be null)
     */
    public static void drawBarChart(Graphics2D g2, double[] values, Color[] colors,
                                     Rectangle bounds, String[] labels) {
        if (values == null || values.length == 0) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        double maxVal = 0;
        for (double v : values) if (v > maxVal) maxVal = v;
        if (maxVal == 0) maxVal = 1;

        int n = values.length;
        int gap = 4;
        int barW = Math.max(4, (bounds.width - (n + 1) * gap) / n);
        int labelH = 18;
        int chartH = bounds.height - labelH - 4;

        for (int i = 0; i < n; i++) {
            int x = bounds.x + gap + i * (barW + gap);
            int barH = (int) (values[i] / maxVal * chartH);
            int y = bounds.y + chartH - barH;

            Color c = colors[i % colors.length];
            g2.setColor(c);
            g2.fillRoundRect(x, y, barW, barH, 4, 4);

            // Label below
            if (labels != null && i < labels.length) {
                g2.setColor(StyleManager.TEXT_DIM);
                g2.setFont(StyleManager.FONT_MONO_SM);
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(labels[i]);
                g2.drawString(labels[i], x + (barW - tw) / 2,
                        bounds.y + chartH + labelH - 2);
            }
        }
    }

    /**
     * Draw a pie chart from slices.
     * @param g2     graphics context
     * @param values slice sizes
     * @param colors slice colours
     * @param labels slice labels
     * @param bounds drawing rectangle
     */
    public static void drawPieChart(Graphics2D g2, double[] values, Color[] colors,
                                     String[] labels, Rectangle bounds) {
        if (values == null || values.length == 0) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        double total = 0;
        for (double v : values) total += v;
        if (total == 0) return;

        int diam = Math.min(bounds.width, bounds.height) - 20;
        int cx = bounds.x + (bounds.width - diam) / 2;
        int cy = bounds.y + 10;

        int startAngle = 0;
        for (int i = 0; i < values.length; i++) {
            int arc = (int) Math.round(values[i] / total * 360);
            if (i == values.length - 1) arc = 360 - startAngle; // close gap
            g2.setColor(colors[i % colors.length]);
            g2.fillArc(cx, cy, diam, diam, startAngle, arc);
            startAngle += arc;
        }

        // Legend
        int ly = cy + diam + 16;
        g2.setFont(StyleManager.FONT_SMALL);
        for (int i = 0; i < values.length && labels != null && i < labels.length; i++) {
            g2.setColor(colors[i % colors.length]);
            g2.fillRect(cx, ly, 12, 12);
            g2.setColor(StyleManager.TEXT_PRIMARY);
            g2.drawString(labels[i] + " (" + (int) values[i] + ")", cx + 18, ly + 11);
            ly += 18;
        }
    }
}
