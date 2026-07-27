package com.keystroke.auth;

/**
 * ProfileAnalyzer.java - Analyzes keystroke profiles to extract typing characteristics
 * and generate a profile quality score.
 *
 * Analysis includes:
 *   - Typing speed (characters per minute)
 *   - Rhythm consistency (coefficient of variation)
 *   - Pause pattern identification
 *   - Profile quality scoring based on timing variance
 *   - Unique typing characteristic identification
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class ProfileAnalyzer {

    /**
     * Performs a comprehensive analysis of a keystroke profile.
     * Prints detailed analysis results to the console.
     *
     * @param profile the KeystrokeProfile to analyze
     */
    public void analyzeProfile(KeystrokeProfile profile) {
        if (profile == null) {
            System.out.println("  [!] Cannot analyze null profile.");
            return;
        }

        double[] holdTimings = profile.getHoldTimings();
        double[] flightTimings = profile.getFlightTimings();

        if (holdTimings == null || holdTimings.length == 0) {
            System.out.println("  [!] Profile has no timing data to analyze.");
            return;
        }

        System.out.println("\n  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║            PROFILE ANALYSIS REPORT                   ║");
        System.out.println("  ╠══════════════════════════════════════════════════════╣");
        System.out.printf("  ║  Username: %-43s ║\n", profile.getUsername());
        System.out.println("  ╠══════════════════════════════════════════════════════╣");

        // 1. Typing Speed Analysis
        double typingSpeed = calculateTypingSpeed(holdTimings, flightTimings);
        System.out.printf("  ║  Typing Speed       : %.1f chars/min              ║\n", typingSpeed);
        System.out.printf("  ║  Speed Category     : %-32s ║\n", categorizeSpeed(typingSpeed));

        // 2. Rhythm Consistency
        double rhythmConsistency = calculateRhythmConsistency(holdTimings);
        System.out.printf("  ║  Rhythm Consistency : %.2f%%                        ║\n", rhythmConsistency);
        System.out.printf("  ║  Rhythm Category    : %-32s ║\n", categorizeRhythm(rhythmConsistency));

        // 3. Profile Quality Score
        double qualityScore = calculateQualityScore(holdTimings, flightTimings);
        System.out.printf("  ║  Profile Quality    : %.2f / 100                   ║\n", qualityScore);
        System.out.printf("  ║  Quality Rating     : %-32s ║\n", rateQuality(qualityScore));

        System.out.println("  ╠══════════════════════════════════════════════════════╣");
        System.out.println("  ║  TIMING STATISTICS:                                  ║");
        System.out.printf("  ║    Hold Mean         : %8.2f ms                   ║\n",
                profile.getAverageHoldTime());
        System.out.printf("  ║    Hold Std Dev      : %8.2f ms                   ║\n",
                profile.getHoldStdDev());
        System.out.printf("  ║    Flight Mean       : %8.2f ms                   ║\n",
                profile.getAverageFlightTime());
        System.out.printf("  ║    Flight Std Dev    : %8.2f ms                   ║\n",
                profile.getFlightStdDev());
        System.out.printf("  ║    Hold Features     : %-10d                      ║\n",
                holdTimings.length);
        System.out.printf("  ║    Flight Features   : %-10d                      ║\n",
                flightTimings != null ? flightTimings.length : 0);
        System.out.println("  ╠══════════════════════════════════════════════════════╣");

        // 4. Unique Characteristics
        System.out.println("  ║  UNIQUE CHARACTERISTICS:                             ║");
        identifyCharacteristics(holdTimings, flightTimings);

        // 5. Pause Pattern Analysis
        System.out.println("  ║  PAUSE PATTERNS:                                     ║");
        analyzePausePatterns(holdTimings, flightTimings);

        System.out.println("  ╚══════════════════════════════════════════════════════╝\n");
    }

    /**
     * Calculates typing speed in characters per minute.
     * Speed = total characters / total time (converted to per-minute rate).
     *
     * @param holdTimings   hold duration array (milliseconds)
     * @param flightTimings flight time array (milliseconds)
     * @return typing speed in characters per minute
     */
    public double calculateTypingSpeed(double[] holdTimings, double[] flightTimings) {
        // Total time = sum of all holds + sum of all flights
        double totalTimeMs = 0.0;

        for (double hold : holdTimings) {
            totalTimeMs += hold;
        }
        if (flightTimings != null) {
            for (double flight : flightTimings) {
                totalTimeMs += flight;
            }
        }

        if (totalTimeMs <= 0) return 0.0;

        int totalChars = holdTimings.length;
        // Convert to characters per minute: (chars / totalMs) * 60000
        return (totalChars / totalTimeMs) * 60000.0;
    }

    /**
     * Calculates rhythm consistency as a percentage.
     * Based on the inverse of the coefficient of variation (CV).
     * Higher consistency = lower CV = more regular rhythm.
     *
     * @param holdTimings the hold timing array
     * @return rhythm consistency percentage (0-100, higher = more consistent)
     */
    public double calculateRhythmConsistency(double[] holdTimings) {
        double mean = KeystrokeCapture.calculateMean(holdTimings);
        double stdDev = KeystrokeCapture.calculateStdDev(holdTimings);

        if (mean <= 0) return 0.0;

        double cv = stdDev / mean; // Coefficient of variation

        // Convert CV to consistency: 100% consistency when CV=0
        // Decays as CV increases. Cap CV at 2.0 for scoring.
        double consistency = Math.max(0.0, 100.0 * (1.0 - Math.min(cv, 2.0) / 2.0));
        return consistency;
    }

    /**
     * Calculates a profile quality score based on timing variance and sample adequacy.
     *
     * Factors:
     *   - Sufficient data points (more = better)
     *   - Reasonable variance (not too high, not too low)
     *   - Reasonable speed range
     *
     * @param holdTimings   hold timing data
     * @param flightTimings flight timing data
     * @return quality score (0-100)
     */
    public double calculateQualityScore(double[] holdTimings, double[] flightTimings) {
        double score = 0.0;

        // Factor 1: Data sufficiency (max 30 points)
        int dataPoints = holdTimings.length + (flightTimings != null ? flightTimings.length : 0);
        if (dataPoints >= 80) score += 30.0;
        else if (dataPoints >= 40) score += 20.0;
        else if (dataPoints >= 20) score += 10.0;
        else score += 5.0;

        // Factor 2: Reasonable hold time variance (max 25 points)
        double holdMean = KeystrokeCapture.calculateMean(holdTimings);
        double holdStdDev = KeystrokeCapture.calculateStdDev(holdTimings);
        double holdCV = (holdMean > 0) ? holdStdDev / holdMean : 0;

        if (holdCV >= 0.1 && holdCV <= 0.6) {
            score += 25.0; // Good natural variance
        } else if (holdCV >= 0.05 && holdCV <= 0.8) {
            score += 15.0; // Acceptable variance
        } else {
            score += 5.0;  // Suspicious variance
        }

        // Factor 3: Reasonable flight time variance (max 25 points)
        if (flightTimings != null && flightTimings.length > 0) {
            double flightMean = KeystrokeCapture.calculateMean(flightTimings);
            double flightStdDev = KeystrokeCapture.calculateStdDev(flightTimings);
            double flightCV = (flightMean > 0) ? flightStdDev / flightMean : 0;

            if (flightCV >= 0.1 && flightCV <= 0.6) {
                score += 25.0;
            } else if (flightCV >= 0.05 && flightCV <= 0.8) {
                score += 15.0;
            } else {
                score += 5.0;
            }
        }

        // Factor 4: Speed reasonableness (max 20 points)
        double speed = calculateTypingSpeed(holdTimings, flightTimings);
        if (speed >= 30 && speed <= 300) {
            score += 20.0; // Reasonable human typing speed
        } else if (speed >= 10 && speed <= 500) {
            score += 10.0; // Borderline speed
        } else {
            score += 2.0;  // Unreasonable speed
        }

        return Math.min(100.0, score);
    }

    // ==================== Helper Methods ====================

    /**
     * Identifies unique typing characteristics from the timing data.
     *
     * @param holdTimings   hold timing data
     * @param flightTimings flight timing data
     */
    private void identifyCharacteristics(double[] holdTimings, double[] flightTimings) {
        double holdMean = KeystrokeCapture.calculateMean(holdTimings);
        double holdStdDev = KeystrokeCapture.calculateStdDev(holdTimings);

        // Find the fastest and slowest keystrokes
        double minHold = Double.MAX_VALUE, maxHold = Double.MIN_VALUE;
        int minIdx = 0, maxIdx = 0;
        for (int i = 0; i < holdTimings.length; i++) {
            if (holdTimings[i] < minHold) { minHold = holdTimings[i]; minIdx = i; }
            if (holdTimings[i] > maxHold) { maxHold = holdTimings[i]; maxIdx = i; }
        }

        System.out.printf("  ║    Fastest keystroke : pos %d (%.2f ms)             ║\n",
                minIdx + 1, minHold);
        System.out.printf("  ║    Slowest keystroke : pos %d (%.2f ms)             ║\n",
                maxIdx + 1, maxHold);
        System.out.printf("  ║    Speed range       : %.2f ms                     ║\n",
                maxHold - minHold);

        // Identify if user has a distinct pause pattern
        if (holdStdDev > holdMean * 0.4) {
            System.out.println("  ║    Pattern: Variable rhythm (expressive typist)    ║");
        } else if (holdStdDev < holdMean * 0.15) {
            System.out.println("  ║    Pattern: Very consistent (mechanical typist)    ║");
        } else {
            System.out.println("  ║    Pattern: Moderate rhythm (typical typist)       ║");
        }
    }

    /**
     * Analyzes pause patterns in the flight timing data.
     *
     * @param holdTimings   hold timing data
     * @param flightTimings flight timing data
     */
    private void analyzePausePatterns(double[] holdTimings, double[] flightTimings) {
        if (flightTimings == null || flightTimings.length == 0) {
            System.out.println("  ║    No flight data available for pause analysis.   ║");
            return;
        }

        double flightMean = KeystrokeCapture.calculateMean(flightTimings);
        int longPauses = 0;    // Pauses > 2x average
        int shortPauses = 0;   // Pauses < 0.5x average

        for (double flight : flightTimings) {
            if (flight > flightMean * 2.0) longPauses++;
            if (flight < flightMean * 0.5) shortPauses++;
        }

        System.out.printf("  ║    Long pauses  (>2x avg) : %-4d                   ║\n", longPauses);
        System.out.printf("  ║    Quick transitions       : %-4d                   ║\n", shortPauses);

        double pauseRatio = (double) longPauses / flightTimings.length;
        if (pauseRatio > 0.2) {
            System.out.println("  ║    Style: Deliberate typist (frequent pauses)      ║");
        } else if (pauseRatio < 0.05) {
            System.out.println("  ║    Style: Fluid typist (minimal pauses)            ║");
        } else {
            System.out.println("  ║    Style: Balanced typist (moderate pauses)        ║");
        }
    }

    /**
     * Categorizes typing speed into descriptive labels.
     */
    private String categorizeSpeed(double cpm) {
        if (cpm >= 250) return "Very Fast";
        if (cpm >= 150) return "Fast";
        if (cpm >= 80) return "Average";
        if (cpm >= 40) return "Slow";
        return "Very Slow";
    }

    /**
     * Categorizes rhythm consistency into descriptive labels.
     */
    private String categorizeRhythm(double consistency) {
        if (consistency >= 85) return "Highly Consistent";
        if (consistency >= 65) return "Moderately Consistent";
        if (consistency >= 40) return "Variable";
        return "Erratic";
    }

    /**
     * Rates profile quality with a descriptive label.
     */
    private String rateQuality(double quality) {
        if (quality >= 85) return "Excellent";
        if (quality >= 70) return "Good";
        if (quality >= 50) return "Adequate";
        if (quality >= 30) return "Poor";
        return "Insufficient";
    }
}
