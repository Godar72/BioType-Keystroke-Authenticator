package com.keystroke.auth.gui;

import com.keystroke.auth.gui.utils.StyleManager;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * TimingGraphPanel.java — Premium real-time keystroke timing visualization
 * with gradient fills, glow effects, and smooth curves.
 */
public class TimingGraphPanel extends JPanel {

    private double[] holdData = {};
    private double[] flightData = {};
    private double[] refHold = null;
    private double[] refFlight = null;
    private String chartTitle = "Keystroke Timing";

    public TimingGraphPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(350, 220));
    }

    public void setTimingData(double[] hold, double[] flight) {
        this.holdData = hold != null ? hold : new double[0];
        this.flightData = flight != null ? flight : new double[0];
        repaint();
    }

    public void setReferenceProfile(double[] refHold, double[] refFlight) {
        this.refHold = refHold;
        this.refFlight = refFlight;
        repaint();
    }

    public void setChartTitle(String title) {
        this.chartTitle = title;
    }

    public void clear() {
        holdData = new double[0];
        flightData = new double[0];
        refHold = null;
        refFlight = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int pad = 50, padTop = 38, padBottom = 30;
        int graphW = w - pad * 2;
        int graphH = h - padTop - padBottom;

        // Card background
        g2.setColor(StyleManager.BG_CARD);
        g2.fillRoundRect(4, 4, w - 8, h - 8, 16, 16);
        g2.setColor(StyleManager.GLASS_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(4, 4, w - 9, h - 9, 16, 16);

        // Title
        g2.setFont(StyleManager.FONT_SMALL);
        g2.setColor(StyleManager.TEXT_SECONDARY);
        g2.drawString(chartTitle, pad, 24);

        if (holdData.length == 0) {
            g2.setFont(StyleManager.FONT_BODY);
            g2.setColor(StyleManager.TEXT_DIM);
            String msg = "Waiting for keystrokes...";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            g2.dispose();
            return;
        }

        // Find max value across all data
        double maxVal = 1;
        for (double v : holdData) maxVal = Math.max(maxVal, v);
        for (double v : flightData) maxVal = Math.max(maxVal, v);
        if (refHold != null) for (double v : refHold) maxVal = Math.max(maxVal, v);
        if (refFlight != null) for (double v : refFlight) maxVal = Math.max(maxVal, v);
        maxVal *= 1.15;

        // Grid lines
        g2.setStroke(new BasicStroke(0.5f));
        g2.setFont(StyleManager.FONT_TINY);
        for (int i = 0; i <= 4; i++) {
            int y = padTop + (int) (graphH * (1.0 - i / 4.0));
            g2.setColor(new Color(255, 255, 255, 10));
            g2.drawLine(pad, y, pad + graphW, y);
            g2.setColor(StyleManager.TEXT_DIM);
            g2.drawString(String.format("%.0f", maxVal * i / 4) + "ms", 4, y + 4);
        }

        int n = holdData.length;

        // Draw reference profiles (if loaded) as dashed lines
        if (refHold != null && refHold.length > 1) {
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{6, 4}, 0));
            drawLine(g2, refHold, pad, padTop, graphW, graphH, maxVal,
                    new Color(255, 150, 30, 80));
        }
        if (refFlight != null && refFlight.length > 1) {
            drawLine(g2, refFlight, pad, padTop, graphW, graphH, maxVal,
                    new Color(80, 220, 100, 60));
        }

        // Draw hold times — accent gradient fill
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (n > 1) {
            drawFilledCurve(g2, holdData, pad, padTop, graphW, graphH, maxVal,
                    StyleManager.ORANGE, new Color(255, 140, 0, 25));
        }

        // Draw flight times — green
        if (flightData.length > 1) {
            drawFilledCurve(g2, flightData, pad, padTop, graphW, graphH, maxVal,
                    StyleManager.SUCCESS, new Color(80, 220, 100, 18));
        }

        // Draw data points for hold
        for (int i = 0; i < n; i++) {
            int x = pad + (int) ((double) i / (n - 1) * graphW);
            int y = padTop + graphH - (int) (holdData[i] / maxVal * graphH);
            // Glow dot
            g2.setColor(new Color(255, 140, 0, 40));
            g2.fillOval(x - 6, y - 6, 12, 12);
            g2.setColor(StyleManager.ORANGE);
            g2.fillOval(x - 3, y - 3, 6, 6);
        }

        // Legend
        int ly = h - 14;
        g2.setFont(StyleManager.FONT_TINY);
        g2.setColor(StyleManager.ACCENT);
        g2.fillRoundRect(pad, ly, 10, 10, 3, 3);
        g2.setColor(StyleManager.TEXT_SECONDARY);
        g2.drawString("Hold Time", pad + 14, ly + 9);

        g2.setColor(StyleManager.SUCCESS);
        g2.fillRoundRect(pad + 85, ly, 10, 10, 3, 3);
        g2.setColor(StyleManager.TEXT_SECONDARY);
        g2.drawString("Flight Time", pad + 99, ly + 9);

        if (refHold != null) {
            g2.setColor(new Color(255, 150, 30, 120));
            g2.fillRoundRect(pad + 178, ly, 10, 10, 3, 3);
            g2.setColor(StyleManager.TEXT_SECONDARY);
            g2.drawString("Reference", pad + 192, ly + 9);
        }

        g2.dispose();
    }

    private void drawLine(Graphics2D g2, double[] data, int px, int py,
                           int gw, int gh, double max, Color c) {
        g2.setColor(c);
        int n = data.length;
        if (n < 2) return;
        for (int i = 0; i < n - 1; i++) {
            int x1 = px + (int) ((double) i / (n - 1) * gw);
            int y1 = py + gh - (int) (data[i] / max * gh);
            int x2 = px + (int) ((double) (i + 1) / (n - 1) * gw);
            int y2 = py + gh - (int) (data[i + 1] / max * gh);
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawFilledCurve(Graphics2D g2, double[] data, int px, int py,
                                  int gw, int gh, double max, Color lineColor, Color fillColor) {
        int n = data.length;
        if (n < 2) return;

        int[] xs = new int[n + 2];
        int[] ys = new int[n + 2];
        for (int i = 0; i < n; i++) {
            xs[i] = px + (int) ((double) i / (n - 1) * gw);
            ys[i] = py + gh - (int) (data[i] / max * gh);
        }
        // Close polygon at bottom
        xs[n] = xs[n - 1];
        ys[n] = py + gh;
        xs[n + 1] = xs[0];
        ys[n + 1] = py + gh;

        // Fill
        g2.setColor(fillColor);
        g2.fillPolygon(xs, ys, n + 2);

        // Line
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < n - 1; i++) {
            g2.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1]);
        }
    }
}
