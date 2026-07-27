package com.keystroke.auth;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * MouseDynamicsCapture.java — Captures mouse behavioral biometrics from a
 * Swing component. Tracks click durations, movement speeds, and scroll patterns.
 *
 * Usage:
 *   1. Attach to a component via startCapture(component)
 *   2. User interacts normally
 *   3. Call stopCapture() and getProfile() to retrieve the biometric data
 *
 * Phase 2 Enhancement — Mouse Dynamics Biometrics
 */
public class MouseDynamicsCapture implements MouseListener, MouseMotionListener, MouseWheelListener {

    // Raw captured data
    private final List<Double> clickDurations = new ArrayList<>();
    private final List<Double> moveSpeeds = new ArrayList<>();
    private final List<Double> scrollAmounts = new ArrayList<>();

    // Tracking state
    private long mousePressTime = 0;
    private Point lastMovePoint = null;
    private long lastMoveTime = 0;
    private JComponent attachedComponent = null;

    private String username;

    public MouseDynamicsCapture(String username) {
        this.username = username;
    }

    /**
     * Attaches mouse listeners to the given component to begin capture.
     */
    public void startCapture(JComponent component) {
        this.attachedComponent = component;
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addMouseWheelListener(this);
        reset();
        System.out.println("  [Mouse] Capture started for: " + username);
    }

    /**
     * Detaches all listeners and finalizes the capture session.
     */
    public void stopCapture() {
        if (attachedComponent != null) {
            attachedComponent.removeMouseListener(this);
            attachedComponent.removeMouseMotionListener(this);
            attachedComponent.removeMouseWheelListener(this);
            attachedComponent = null;
        }
        System.out.println("  [Mouse] Capture stopped. Clicks: " + clickDurations.size()
                + ", Moves: " + moveSpeeds.size() + ", Scrolls: " + scrollAmounts.size());
    }

    /**
     * Resets all captured data for a fresh session.
     */
    public void reset() {
        clickDurations.clear();
        moveSpeeds.clear();
        scrollAmounts.clear();
        mousePressTime = 0;
        lastMovePoint = null;
        lastMoveTime = 0;
    }

    /**
     * Returns the number of click data points captured so far.
     */
    public int getClickCount() { return clickDurations.size(); }

    /**
     * Returns the number of movement data points captured so far.
     */
    public int getMoveCount() { return moveSpeeds.size(); }

    /**
     * Returns the number of scroll data points captured so far.
     */
    public int getScrollCount() { return scrollAmounts.size(); }

    /**
     * Builds a MouseDynamicsProfile from the captured data.
     *
     * @return the constructed profile, or null if insufficient data
     */
    public MouseDynamicsProfile getProfile() {
        if (clickDurations.size() < 3) {
            System.out.println("  [Mouse] Insufficient data for profile (need 3+ clicks).");
            return null;
        }

        MouseDynamicsProfile profile = new MouseDynamicsProfile(username);

        // Click duration statistics
        double[] clicks = clickDurations.stream().mapToDouble(Double::doubleValue).toArray();
        profile.setAvgClickDuration(mean(clicks));
        profile.setClickDurationStdDev(stdDev(clicks));
        profile.setTotalClicks(clicks.length);

        // Movement speed statistics
        if (!moveSpeeds.isEmpty()) {
            double[] speeds = moveSpeeds.stream().mapToDouble(Double::doubleValue).toArray();
            profile.setAvgMoveSpeed(mean(speeds));
            profile.setMoveSpeedStdDev(stdDev(speeds));
        }

        // Scroll statistics
        if (!scrollAmounts.isEmpty()) {
            double[] scrolls = scrollAmounts.stream().mapToDouble(Double::doubleValue).toArray();
            profile.setAvgScrollSpeed(mean(scrolls));
            profile.setTotalScrollEvents(scrolls.length);
        }

        return profile;
    }

    // ==================== Mouse Event Handlers ====================

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressTime = System.nanoTime();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (mousePressTime > 0) {
            double duration = (System.nanoTime() - mousePressTime) / 1_000_000.0;
            clickDurations.add(duration);
            mousePressTime = 0;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        long now = System.nanoTime();
        Point current = e.getPoint();

        if (lastMovePoint != null && lastMoveTime > 0) {
            double dx = current.x - lastMovePoint.x;
            double dy = current.y - lastMovePoint.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            double timeMs = (now - lastMoveTime) / 1_000_000.0;

            if (timeMs > 1.0 && distance > 2.0) { // Filter noise
                double speed = distance / timeMs; // px/ms
                moveSpeeds.add(speed);
            }
        }

        lastMovePoint = current;
        lastMoveTime = now;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scrollAmounts.add((double) Math.abs(e.getWheelRotation()));
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }

    // ==================== Statistics Helpers ====================

    private double mean(double[] arr) {
        if (arr.length == 0) return 0;
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private double stdDev(double[] arr) {
        if (arr.length < 2) return 0;
        double m = mean(arr);
        double sum = 0;
        for (double v : arr) sum += (v - m) * (v - m);
        return Math.sqrt(sum / (arr.length - 1));
    }
}
