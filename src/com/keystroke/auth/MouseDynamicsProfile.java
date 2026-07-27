package com.keystroke.auth;

/**
 * MouseDynamicsProfile.java — Data model for mouse behavioral biometrics.
 *
 * Stores aggregated mouse dynamics features captured during enrollment or
 * authentication sessions. Features include click timing, movement speed,
 * and scroll behaviour.
 *
 * Phase 2 Enhancement — Mouse Dynamics Biometrics
 */
public class MouseDynamicsProfile {

    private String username;

    // Click dynamics
    private double avgClickDuration;      // Average ms from press to release
    private double clickDurationStdDev;   // Standard deviation of click durations
    private int totalClicks;

    // Movement dynamics
    private double avgMoveSpeed;          // Average px/ms movement speed
    private double moveSpeedStdDev;       // Standard deviation of movement speed

    // Scroll dynamics
    private double avgScrollSpeed;        // Average notches per scroll event
    private int totalScrollEvents;

    /**
     * Constructs an empty mouse dynamics profile for a user.
     */
    public MouseDynamicsProfile(String username) {
        this.username = username;
    }

    // ==================== Getters and Setters ====================

    public String getUsername() { return username; }

    public double getAvgClickDuration() { return avgClickDuration; }
    public void setAvgClickDuration(double v) { this.avgClickDuration = v; }

    public double getClickDurationStdDev() { return clickDurationStdDev; }
    public void setClickDurationStdDev(double v) { this.clickDurationStdDev = v; }

    public int getTotalClicks() { return totalClicks; }
    public void setTotalClicks(int v) { this.totalClicks = v; }

    public double getAvgMoveSpeed() { return avgMoveSpeed; }
    public void setAvgMoveSpeed(double v) { this.avgMoveSpeed = v; }

    public double getMoveSpeedStdDev() { return moveSpeedStdDev; }
    public void setMoveSpeedStdDev(double v) { this.moveSpeedStdDev = v; }

    public double getAvgScrollSpeed() { return avgScrollSpeed; }
    public void setAvgScrollSpeed(double v) { this.avgScrollSpeed = v; }

    public int getTotalScrollEvents() { return totalScrollEvents; }
    public void setTotalScrollEvents(int v) { this.totalScrollEvents = v; }

    // ==================== Serialization ====================

    /**
     * Serializes this profile to a single CSV line.
     */
    public String toCSV() {
        return String.format("%.6f,%.6f,%d,%.6f,%.6f,%.6f,%d",
                avgClickDuration, clickDurationStdDev, totalClicks,
                avgMoveSpeed, moveSpeedStdDev,
                avgScrollSpeed, totalScrollEvents);
    }

    /**
     * Deserializes a mouse profile from a CSV line.
     */
    public static MouseDynamicsProfile fromCSV(String username, String csv) {
        MouseDynamicsProfile p = new MouseDynamicsProfile(username);
        if (csv == null || csv.trim().isEmpty()) return p;
        try {
            String[] parts = csv.trim().split(",");
            if (parts.length >= 7) {
                p.avgClickDuration = Double.parseDouble(parts[0]);
                p.clickDurationStdDev = Double.parseDouble(parts[1]);
                p.totalClicks = Integer.parseInt(parts[2]);
                p.avgMoveSpeed = Double.parseDouble(parts[3]);
                p.moveSpeedStdDev = Double.parseDouble(parts[4]);
                p.avgScrollSpeed = Double.parseDouble(parts[5]);
                p.totalScrollEvents = Integer.parseInt(parts[6]);
            }
        } catch (NumberFormatException e) {
            System.out.println("  [!] Error parsing mouse profile: " + e.getMessage());
        }
        return p;
    }

    // ==================== Similarity Scoring ====================

    /**
     * Calculates similarity between this profile and another using
     * Mean Absolute Percentage Deviation across all features.
     *
     * @param other the profile to compare against
     * @return similarity score (0-100)
     */
    public double calculateSimilarity(MouseDynamicsProfile other) {
        if (other == null) return 0.0;

        double totalMAD = 0.0;
        int features = 0;

        totalMAD += madPercent(this.avgClickDuration, other.avgClickDuration);
        features++;

        totalMAD += madPercent(this.avgMoveSpeed, other.avgMoveSpeed);
        features++;

        totalMAD += madPercent(this.avgScrollSpeed, other.avgScrollSpeed);
        features++;

        totalMAD += madPercent(this.clickDurationStdDev, other.clickDurationStdDev);
        features++;

        totalMAD += madPercent(this.moveSpeedStdDev, other.moveSpeedStdDev);
        features++;

        double avgMAD = totalMAD / features;
        return Math.max(0.0, Math.min(100.0, 100.0 - avgMAD));
    }

    /**
     * Computes Mean Absolute Percentage Deviation between two values.
     */
    private double madPercent(double reference, double sample) {
        double denom = Math.max(reference, 10.0); // Minimum denominator to avoid div-by-zero
        return Math.abs(reference - sample) / denom * 100.0;
    }

    @Override
    public String toString() {
        return String.format("MouseProfile{user='%s', avgClick=%.1fms, moveSpeed=%.2fpx/ms, scrolls=%d}",
                username, avgClickDuration, avgMoveSpeed, totalScrollEvents);
    }
}
