package com.keystroke.auth;

import java.util.List;

/**
 * KeystrokeProfile.java - Stores and manages a user's keystroke biometric profile.
 * Contains averaged timing data from multiple enrollment samples.
 * 
 * Biometric Features Stored:
 *   - Hold Timings: average key-hold durations across enrollment samples
 *   - Flight Timings: average inter-key flight times across enrollment samples
 *   - Statistical measures: mean and standard deviation for both features
 * 
 * Week 1 Implementation - Core Module
 */
public class KeystrokeProfile {

    // Private fields - encapsulation
    private String username;
    private String phrase;              // Custom passphrase set during enrollment
    private double[] holdTimings;       // Averaged hold durations from enrollment samples
    private double[] flightTimings;     // Averaged flight times from enrollment samples
    private double averageHoldTime;     // Overall mean of hold timings
    private double averageFlightTime;   // Overall mean of flight timings
    private double holdStdDev;          // Standard deviation of hold timings
    private double flightStdDev;        // Standard deviation of flight timings

    /**
     * Constructor to create a profile for a given user.
     *
     * @param username the username this profile belongs to
     */
    public KeystrokeProfile(String username) {
        this.username = username;
        this.phrase = SystemConstants.STANDARD_PHRASE; // default
        this.holdTimings = new double[0];
        this.flightTimings = new double[0];
        this.averageHoldTime = 0.0;
        this.averageFlightTime = 0.0;
        this.holdStdDev = 0.0;
        this.flightStdDev = 0.0;
    }

    // ==================== Getter Methods ====================

    public String getUsername() {
        return username;
    }

    public String getPhrase() {
        return phrase;
    }

    public double[] getHoldTimings() {
        return holdTimings;
    }

    public double[] getFlightTimings() {
        return flightTimings;
    }

    public double getAverageHoldTime() {
        return averageHoldTime;
    }

    public double getAverageFlightTime() {
        return averageFlightTime;
    }

    public double getHoldStdDev() {
        return holdStdDev;
    }

    public double getFlightStdDev() {
        return flightStdDev;
    }

    // ==================== Setter Methods ====================

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public void setHoldTimings(double[] holdTimings) {
        this.holdTimings = holdTimings;
    }

    public void setFlightTimings(double[] flightTimings) {
        this.flightTimings = flightTimings;
    }

    public void setAverageHoldTime(double averageHoldTime) {
        this.averageHoldTime = averageHoldTime;
    }

    public void setAverageFlightTime(double averageFlightTime) {
        this.averageFlightTime = averageFlightTime;
    }

    public void setHoldStdDev(double holdStdDev) {
        this.holdStdDev = holdStdDev;
    }

    public void setFlightStdDev(double flightStdDev) {
        this.flightStdDev = flightStdDev;
    }

    // ==================== Core Methods ====================

    /**
     * Builds a keystroke profile by averaging multiple timing samples.
     * Takes 3-5 enrollment samples and computes the element-wise average
     * for both hold durations and flight times.
     * 
     * Also calculates overall mean and standard deviation statistics.
     *
     * @param holdSamples   list of hold duration arrays from multiple typing sessions
     * @param flightSamples list of flight time arrays from multiple typing sessions
     */
    public void buildProfile(List<double[]> holdSamples, List<double[]> flightSamples) {

        if (holdSamples == null || holdSamples.isEmpty()) {
            System.out.println("[ERROR] No hold timing samples provided.");
            return;
        }
        if (flightSamples == null || flightSamples.isEmpty()) {
            System.out.println("[ERROR] No flight timing samples provided.");
            return;
        }

        int numSamples = holdSamples.size();
        int holdLength = holdSamples.get(0).length;
        int flightLength = flightSamples.get(0).length;

        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║          BUILDING KEYSTROKE PROFILE              ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  User           : %-30s ║\n", username);
        System.out.printf("║  Samples Used   : %-30d ║\n", numSamples);
        System.out.printf("║  Hold Features  : %-30d ║\n", holdLength);
        System.out.printf("║  Flight Features: %-30d ║\n", flightLength);
        System.out.println("╚══════════════════════════════════════════════════╝");

        // Compute element-wise average for hold timings across all samples
        this.holdTimings = computeAverageTimings(holdSamples, holdLength);

        // Compute element-wise average for flight timings across all samples
        this.flightTimings = computeAverageTimings(flightSamples, flightLength);

        // Calculate overall statistics
        this.averageHoldTime = KeystrokeCapture.calculateMean(this.holdTimings);
        this.averageFlightTime = KeystrokeCapture.calculateMean(this.flightTimings);
        this.holdStdDev = KeystrokeCapture.calculateStdDev(this.holdTimings);
        this.flightStdDev = KeystrokeCapture.calculateStdDev(this.flightTimings);

        // Display profile summary
        System.out.println("\n  ──── PROFILE STATISTICS ────");
        System.out.printf("  Average Hold Time    : %.2f ms\n", averageHoldTime);
        System.out.printf("  Average Flight Time  : %.2f ms\n", averageFlightTime);
        System.out.printf("  Hold Std Deviation   : %.2f ms\n", holdStdDev);
        System.out.printf("  Flight Std Deviation : %.2f ms\n", flightStdDev);
        System.out.println("  ────────────────────────────\n");
        System.out.println("  [✓] Profile built successfully for user: " + username);
    }

    /**
     * Computes element-wise average of timing arrays across multiple samples.
     * Handles samples of different lengths by using the minimum length.
     *
     * @param samples the list of timing arrays
     * @param length  the expected length of each array
     * @return the averaged timing array
     */
    private double[] computeAverageTimings(List<double[]> samples, int length) {
        double[] averaged = new double[length];
        int numSamples = samples.size();

        for (int i = 0; i < length; i++) {
            double sum = 0.0;
            int validCount = 0;
            for (double[] sample : samples) {
                if (i < sample.length) {
                    sum += sample[i];
                    validCount++;
                }
            }
            averaged[i] = (validCount > 0) ? sum / validCount : 0.0;
        }
        return averaged;
    }

    /**
     * Displays the full profile details to the console.
     */
    public void displayProfile() {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║            KEYSTROKE PROFILE DETAILS             ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.printf("║  Username          : %-28s ║\n", username);
        System.out.printf("║  Avg Hold Time     : %-28.2f ║\n", averageHoldTime);
        System.out.printf("║  Avg Flight Time   : %-28.2f ║\n", averageFlightTime);
        System.out.printf("║  Hold Std Dev      : %-28.2f ║\n", holdStdDev);
        System.out.printf("║  Flight Std Dev    : %-28.2f ║\n", flightStdDev);
        System.out.printf("║  Hold Features     : %-28d ║\n", holdTimings.length);
        System.out.printf("║  Flight Features   : %-28d ║\n", flightTimings.length);
        System.out.println("╚══════════════════════════════════════════════════╝");

        // Show first few timing values as preview
        System.out.println("\n  Hold Timings (first 10):");
        System.out.print("  ");
        for (int i = 0; i < Math.min(10, holdTimings.length); i++) {
            System.out.printf("%.2f  ", holdTimings[i]);
        }
        System.out.println(holdTimings.length > 10 ? "..." : "");

        System.out.println("\n  Flight Timings (first 10):");
        System.out.print("  ");
        for (int i = 0; i < Math.min(10, flightTimings.length); i++) {
            System.out.printf("%.2f  ", flightTimings[i]);
        }
        System.out.println(flightTimings.length > 10 ? "..." : "");
        System.out.println();
    }

    /**
     * Returns a string representation of the profile.
     */
    @Override
    public String toString() {
        return "KeystrokeProfile{username='" + username +
                "', avgHold=" + String.format("%.2f", averageHoldTime) +
                ", avgFlight=" + String.format("%.2f", averageFlightTime) +
                ", holdFeatures=" + holdTimings.length +
                ", flightFeatures=" + flightTimings.length + "}";
    }
}
