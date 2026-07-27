package com.keystroke.auth;

/**
 * ImpostorDetector.java - Detects suspicious typing patterns that may indicate
 * an impostor or automated bot attempting authentication.
 *
 * Detection Heuristics:
 *   1. Too-consistent timing (bot detection): Standard deviation below threshold
 *   2. Extremely fast typing: Average hold time below minimum
 *   3. Extremely slow typing: Average hold time above maximum
 *   4. Multiple failed attempts: Session-based failure tracking
 *   5. Rhythm anomalies: Unusual variance patterns in timing data
 *
 * Risk Score: 0 (no risk) to 100 (very suspicious)
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class ImpostorDetector {

    /** Tracks consecutive failed attempts in the current session */
    private int sessionFailedAttempts;

    /** Username of the current session being tracked */
    private String trackedUser;

    /**
     * Constructs an ImpostorDetector with cleared session state.
     */
    public ImpostorDetector() {
        this.sessionFailedAttempts = 0;
        this.trackedUser = null;
    }

    /**
     * Analyzes a set of timing data for suspicious patterns and returns a risk score.
     *
     * The risk score is the weighted sum of individual anomaly scores:
     *   - Bot consistency check   : 30% weight
     *   - Speed anomaly check     : 25% weight
     *   - Rhythm anomaly check    : 20% weight
     *   - Session failure penalty  : 25% weight
     *
     * @param holdTimes   the hold duration timings from the authentication attempt
     * @param flightTimes the flight time timings from the authentication attempt
     * @return impostor risk score between 0.0 and 100.0
     */
    public double analyzePattern(double[] holdTimes, double[] flightTimes) {
        if (holdTimes == null || holdTimes.length == 0) {
            return 100.0; // No data = maximum suspicion
        }

        double botScore = checkBotConsistency(holdTimes, flightTimes);
        double speedScore = checkSpeedAnomaly(holdTimes);
        double rhythmScore = checkRhythmAnomaly(holdTimes, flightTimes);
        double failureScore = checkSessionFailures();

        // Weighted combination
        double riskScore = (0.30 * botScore)
                + (0.25 * speedScore)
                + (0.20 * rhythmScore)
                + (0.25 * failureScore);

        // Clamp to 0-100
        riskScore = Math.max(0.0, Math.min(100.0, riskScore));

        // Display analysis details
        System.out.println("\n  ──── IMPOSTOR DETECTION ANALYSIS ────");
        System.out.printf("  Bot Consistency Score  : %6.2f / 100\n", botScore);
        System.out.printf("  Speed Anomaly Score    : %6.2f / 100\n", speedScore);
        System.out.printf("  Rhythm Anomaly Score   : %6.2f / 100\n", rhythmScore);
        System.out.printf("  Session Failure Score  : %6.2f / 100\n", failureScore);
        System.out.printf("  Combined Impostor Risk : %6.2f / 100\n", riskScore);

        if (riskScore >= SystemConstants.IMPOSTOR_FLAG_THRESHOLD) {
            System.out.println("  ⚠  WARNING: High impostor risk detected!");
        } else if (riskScore >= 40.0) {
            System.out.println("  ⚡ NOTICE: Moderate suspicion level.");
        } else {
            System.out.println("  ✓  Pattern appears normal.");
        }
        System.out.println("  ─────────────────────────────────────\n");

        return riskScore;
    }

    /**
     * Checks for bot-like consistency in timing data.
     * Bots/automated tools produce unnaturally consistent timing with very low variance.
     *
     * @param holdTimes   hold timing data
     * @param flightTimes flight timing data
     * @return risk score for bot consistency (0-100)
     */
    private double checkBotConsistency(double[] holdTimes, double[] flightTimes) {
        double holdStdDev = KeystrokeCapture.calculateStdDev(holdTimes);
        double flightStdDev = (flightTimes != null && flightTimes.length > 0)
                ? KeystrokeCapture.calculateStdDev(flightTimes) : 0.0;

        double avgStdDev = (holdStdDev + flightStdDev) / 2.0;

        // If standard deviation is extremely low, it's suspicious (bot-like)
        if (avgStdDev < SystemConstants.BOT_DETECTION_STD_DEV) {
            // Very consistent timing = high suspicion
            double ratio = avgStdDev / SystemConstants.BOT_DETECTION_STD_DEV;
            return 100.0 * (1.0 - ratio); // Lower std dev = higher risk
        }

        return 0.0; // Normal variation, no suspicion
    }

    /**
     * Checks for abnormal typing speed (too fast or too slow).
     *
     * @param holdTimes hold timing data
     * @return risk score for speed anomaly (0-100)
     */
    private double checkSpeedAnomaly(double[] holdTimes) {
        double avgHold = KeystrokeCapture.calculateMean(holdTimes);

        // Check if typing is impossibly fast (likely automated)
        if (avgHold < SystemConstants.MIN_TYPING_SPEED_MS) {
            return 100.0; // Maximum suspicion for superhuman speed
        }

        // Check if typing is extremely slow (possible deliberate manipulation)
        if (avgHold > SystemConstants.MAX_TYPING_SPEED_MS) {
            return 60.0; // High suspicion for extremely slow typing
        }

        // Check for borderline fast typing (within 2x of minimum)
        if (avgHold < SystemConstants.MIN_TYPING_SPEED_MS * 2) {
            double ratio = avgHold / (SystemConstants.MIN_TYPING_SPEED_MS * 2);
            return 80.0 * (1.0 - ratio);
        }

        return 0.0; // Normal speed
    }

    /**
     * Checks for rhythm anomalies - unusual patterns in timing variance.
     * Natural typing has characteristic rhythm variations; artificial input does not.
     *
     * @param holdTimes   hold timing data
     * @param flightTimes flight timing data
     * @return risk score for rhythm anomaly (0-100)
     */
    private double checkRhythmAnomaly(double[] holdTimes, double[] flightTimes) {
        if (holdTimes.length < 5) return 0.0; // Not enough data for rhythm analysis

        // Check coefficient of variation (CV)
        // Human typing typically has CV between 0.1 and 0.8
        double holdMean = KeystrokeCapture.calculateMean(holdTimes);
        double holdStdDev = KeystrokeCapture.calculateStdDev(holdTimes);

        if (holdMean <= 0) return 50.0; // Invalid data is somewhat suspicious

        double holdCV = holdStdDev / holdMean;

        double riskScore = 0.0;

        // Very low CV = too consistent (bot-like)
        if (holdCV < 0.05) {
            riskScore += 80.0;
        } else if (holdCV < 0.10) {
            riskScore += 40.0;
        }

        // Very high CV = erratic typing (possible random input)
        if (holdCV > 1.0) {
            riskScore += 60.0;
        } else if (holdCV > 0.8) {
            riskScore += 30.0;
        }

        // Check for repeating patterns (identical adjacent timings)
        int repeats = 0;
        for (int i = 1; i < holdTimes.length; i++) {
            if (Math.abs(holdTimes[i] - holdTimes[i - 1]) < 0.1) {
                repeats++;
            }
        }
        double repeatRatio = (double) repeats / (holdTimes.length - 1);
        if (repeatRatio > 0.5) {
            riskScore += 50.0; // Many identical timings is suspicious
        }

        return Math.min(100.0, riskScore);
    }

    /**
     * Calculates a risk score based on consecutive failed attempts in this session.
     *
     * @return risk score for session failures (0-100)
     */
    private double checkSessionFailures() {
        if (sessionFailedAttempts == 0) return 0.0;
        if (sessionFailedAttempts >= SystemConstants.MAX_FAILED_ATTEMPTS) return 100.0;

        // Linear scaling: each fail adds ~33% risk
        return (double) sessionFailedAttempts / SystemConstants.MAX_FAILED_ATTEMPTS * 100.0;
    }

    // ==================== Session Management ====================

    /**
     * Records a failed authentication attempt for the current session.
     *
     * @param username the username of the failed attempt
     */
    public void recordFailedAttempt(String username) {
        if (trackedUser == null || !trackedUser.equals(username)) {
            trackedUser = username;
            sessionFailedAttempts = 0;
        }
        sessionFailedAttempts++;

        if (sessionFailedAttempts >= SystemConstants.MAX_FAILED_ATTEMPTS) {
            System.out.println("  ⚠  SESSION LOCKED: Too many failed attempts for user '" + username + "'");
        }
    }

    /**
     * Records a successful authentication, resetting the failure counter.
     *
     * @param username the username of the successful attempt
     */
    public void recordSuccessfulAttempt(String username) {
        if (trackedUser != null && trackedUser.equals(username)) {
            sessionFailedAttempts = 0;
        }
    }

    /**
     * Checks if the session is locked due to excessive failed attempts.
     *
     * @param username the username to check
     * @return true if the session is locked
     */
    public boolean isSessionLocked(String username) {
        return trackedUser != null
                && trackedUser.equals(username)
                && sessionFailedAttempts >= SystemConstants.MAX_FAILED_ATTEMPTS;
    }

    /**
     * Resets the session state (e.g., admin override).
     */
    public void resetSession() {
        sessionFailedAttempts = 0;
        trackedUser = null;
        System.out.println("  [✓] Session state cleared.");
    }

    /**
     * Returns the number of failed attempts for the tracked user.
     *
     * @return the number of consecutive failed attempts
     */
    public int getFailedAttemptCount() {
        return sessionFailedAttempts;
    }
}
