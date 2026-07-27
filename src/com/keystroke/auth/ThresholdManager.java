package com.keystroke.auth;

import java.io.*;
import java.nio.file.*;

/**
 * ThresholdManager.java - Manages adaptive authentication thresholds.
 *
 * The threshold determines the minimum similarity score required for successful
 * authentication. This class implements adaptive threshold adjustment:
 *   - If a genuine user fails (high similarity but rejected), threshold is lowered
 *   - If an impostor passes (low similarity but accepted), threshold is raised
 *   - Threshold remains bounded between MIN_THRESHOLD and MAX_THRESHOLD
 *
 * Threshold settings persist to file across sessions.
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class ThresholdManager {

    /** Current system-wide authentication threshold (0-100 scale) */
    private double currentThreshold;

    /** Number of adaptive adjustments performed in this session */
    private int adjustmentCount;

    /**
     * Constructs a ThresholdManager, loading the threshold from file if available.
     * Falls back to the default threshold if no saved value is found.
     */
    public ThresholdManager() {
        this.adjustmentCount = 0;
        this.currentThreshold = loadThresholdFromFile();
    }

    /**
     * Returns the current authentication threshold.
     *
     * @return the threshold value (0-100 scale)
     */
    public double getCurrentThreshold() {
        return currentThreshold;
    }

    /**
     * Sets the threshold to a specific value, clamped within allowed bounds.
     *
     * @param threshold the desired threshold value
     */
    public void setThreshold(double threshold) {
        this.currentThreshold = clampThreshold(threshold);
        saveThresholdToFile();
        System.out.printf("  [✓] Threshold set to: %.2f%%\n", currentThreshold);
    }

    /**
     * Adaptively adjusts the threshold based on an authentication outcome.
     *
     * Logic:
     *   - Genuine user rejected (false reject): similarity was high but below threshold
     *     → Lower the threshold slightly to be more permissive
     *   - Impostor accepted (false accept): similarity was low but above threshold
     *     → Raise the threshold to be more strict
     *   - Correct outcome: no adjustment needed
     *
     * @param authSuccess     whether the authentication was successful
     * @param similarityScore the similarity score from the attempt (0-100)
     * @return the new threshold value after adjustment
     */
    public double adjustThreshold(boolean authSuccess, double similarityScore) {
        double oldThreshold = currentThreshold;
        double adjustStep = SystemConstants.THRESHOLD_ADJUST_STEP;

        if (!authSuccess && similarityScore >= (currentThreshold - 15.0)) {
            // False reject scenario: genuine user was close but rejected
            // Lower the threshold to be more lenient
            currentThreshold -= adjustStep;
            System.out.println("  [↓] Threshold lowered (potential false reject detected)");
        } else if (authSuccess && similarityScore < (currentThreshold + 10.0)) {
            // Marginal accept scenario: user barely passed
            // Raise the threshold slightly for security
            currentThreshold += (adjustStep * 0.5);
            System.out.println("  [↑] Threshold raised (marginal acceptance detected)");
        }
        // If decisively passed or decisively failed, no adjustment needed

        // Clamp to valid range
        currentThreshold = clampThreshold(currentThreshold);

        // Save if changed
        if (currentThreshold != oldThreshold) {
            adjustmentCount++;
            saveThresholdToFile();
            System.out.printf("  [~] Threshold adjusted: %.2f%% → %.2f%% (adjustment #%d)\n",
                    oldThreshold, currentThreshold, adjustmentCount);
        }

        return currentThreshold;
    }

    /**
     * Resets the threshold to the system default.
     */
    public void resetToDefault() {
        this.currentThreshold = SystemConstants.DEFAULT_THRESHOLD;
        this.adjustmentCount = 0;
        saveThresholdToFile();
        System.out.printf("  [✓] Threshold reset to default: %.2f%%\n", currentThreshold);
    }

    /**
     * Returns the total number of adaptive adjustments made in this session.
     *
     * @return the adjustment count
     */
    public int getAdjustmentCount() {
        return adjustmentCount;
    }

    /**
     * Displays the current threshold status.
     */
    public void displayStatus() {
        System.out.println("\n  ──── THRESHOLD STATUS ────");
        System.out.printf("  Current Threshold  : %.2f%%\n", currentThreshold);
        System.out.printf("  Allowed Range      : %.0f%% - %.0f%%\n",
                SystemConstants.MIN_THRESHOLD, SystemConstants.MAX_THRESHOLD);
        System.out.printf("  Default Value      : %.0f%%\n", SystemConstants.DEFAULT_THRESHOLD);
        System.out.printf("  Adjustments Made   : %d\n", adjustmentCount);
        System.out.println("  ─────────────────────────\n");
    }

    // ==================== File Persistence ====================

    /**
     * Saves the current threshold to a text file.
     */
    private void saveThresholdToFile() {
        try {
            Path dirPath = Paths.get(SystemConstants.THRESHOLDS_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String filePath = SystemConstants.THRESHOLDS_DIR + File.separator
                    + SystemConstants.THRESHOLD_FILE;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write(String.valueOf(currentThreshold));
                writer.newLine();
                writer.write(String.valueOf(adjustmentCount));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to save threshold: " + e.getMessage());
        }
    }

    /**
     * Loads the threshold from a text file. Returns the default if file doesn't exist.
     *
     * @return the loaded threshold value, or the default if unavailable
     */
    private double loadThresholdFromFile() {
        String filePath = SystemConstants.THRESHOLDS_DIR + File.separator
                + SystemConstants.THRESHOLD_FILE;

        if (!Files.exists(Paths.get(filePath))) {
            return SystemConstants.DEFAULT_THRESHOLD;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                double loaded = Double.parseDouble(line.trim());

                // Load adjustment count if present
                String countLine = reader.readLine();
                if (countLine != null && !countLine.trim().isEmpty()) {
                    this.adjustmentCount = Integer.parseInt(countLine.trim());
                }

                return clampThreshold(loaded);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("  [!] Could not load threshold, using default.");
        }

        return SystemConstants.DEFAULT_THRESHOLD;
    }

    /**
     * Clamps a threshold value to the allowed range [MIN_THRESHOLD, MAX_THRESHOLD].
     *
     * @param threshold the value to clamp
     * @return the clamped threshold
     */
    private double clampThreshold(double threshold) {
        return Math.max(SystemConstants.MIN_THRESHOLD,
                Math.min(SystemConstants.MAX_THRESHOLD, threshold));
    }
}
