package com.keystroke.auth;

/**
 * AuthenticationEngine.java - Advanced authentication engine that orchestrates
 * similarity scoring, threshold checking, impostor detection, and logging.
 *
 * This is the central authentication manager that coordinates:
 *   - EuclideanSimilarityScorer for similarity calculation
 *   - ThresholdManager for adaptive threshold decisions
 *   - ImpostorDetector for anomaly detection
 *   - AuthLogger for audit trail logging
 *   - AdaptiveThresholdML for per-user personalized thresholds (Phase 2)
 *   - MouseDynamicsProfile for combined biometric scoring (Phase 2)
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class AuthenticationEngine {

    // Core components
    private final SimilarityScorer scorer;
    private final ThresholdManager thresholdManager;
    private final ImpostorDetector impostorDetector;
    private final AuthLogger authLogger;
    private final FileManager fileManager;
    private final AdaptiveThresholdML adaptiveML;

    // Feature flags (set by ConfigManager)
    private boolean mlThresholdEnabled = false;
    private boolean mouseDynamicsEnabled = false;

    /**
     * Constructs the authentication engine with all required subsystems.
     *
     * @param fileManager the file manager for profile loading
     */
    public AuthenticationEngine(FileManager fileManager) {
        this.scorer = new EuclideanSimilarityScorer();
        this.thresholdManager = new ThresholdManager();
        this.impostorDetector = new ImpostorDetector();
        this.authLogger = new AuthLogger();
        this.fileManager = fileManager;
        this.adaptiveML = new AdaptiveThresholdML();
    }

    /**
     * Performs full authentication for a user with given keystroke timing data.
     *
     * Workflow:
     *   1. Check if session is locked for this user
     *   2. Load stored profile from file
     *   3. Build a temporary profile from new timing data
     *   4. Run impostor detection on new timing data
     *   5. Compute similarity using Euclidean scorer
     *   6. Compare similarity against adaptive threshold
     *   7. Log the attempt and adjust threshold
     *   8. Return AuthResult with full details
     *
     * @param username       the username to authenticate
     * @param newHoldTimes   the hold timings from the current typing sample
     * @param newFlightTimes the flight timings from the current typing sample
     * @return an AuthResult object containing the authentication decision and details
     * @throws AuthenticationException if authentication cannot proceed
     */
    public AuthResult authenticate(String username, double[] newHoldTimes, double[] newFlightTimes)
            throws AuthenticationException {
        return authenticate(username, newHoldTimes, newFlightTimes, null);
    }

    /**
     * Performs full authentication with optional mouse dynamics scoring.
     *
     * @param username          the username to authenticate
     * @param newHoldTimes      the hold timings from the current typing sample
     * @param newFlightTimes    the flight timings from the current typing sample
     * @param mouseProfile      the live mouse dynamics profile (null if not captured)
     * @return an AuthResult object with authentication decision and details
     * @throws AuthenticationException if authentication cannot proceed
     */
    public AuthResult authenticate(String username, double[] newHoldTimes, double[] newFlightTimes,
                                    MouseDynamicsProfile mouseProfile) throws AuthenticationException {

        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Username cannot be empty.",
                    AuthenticationException.ErrorCode.INVALID_INPUT);
        }
        if (newHoldTimes == null || newHoldTimes.length == 0) {
            throw new AuthenticationException("Hold timing data is missing or empty.",
                    AuthenticationException.ErrorCode.INVALID_INPUT);
        }

        // Step 1: Check session lock
        if (impostorDetector.isSessionLocked(username)) {
            throw new AuthenticationException(
                    "Session locked for user '" + username + "' due to multiple failed attempts.",
                    AuthenticationException.ErrorCode.SESSION_LOCKED);
        }

        // Step 2: Load stored profile
        KeystrokeProfile storedProfile = fileManager.loadUserProfile(username);
        if (storedProfile == null) {
            throw new AuthenticationException(
                    "No enrolled profile found for user '" + username + "'.",
                    AuthenticationException.ErrorCode.PROFILE_NOT_FOUND);
        }

        // Step 3: Build temporary profile from new timing data
        KeystrokeProfile inputProfile = new KeystrokeProfile(username);
        inputProfile.setHoldTimings(newHoldTimes);
        inputProfile.setFlightTimings(newFlightTimes != null ? newFlightTimes : new double[0]);
        inputProfile.setAverageHoldTime(KeystrokeCapture.calculateMean(newHoldTimes));
        inputProfile.setAverageFlightTime(
                newFlightTimes != null ? KeystrokeCapture.calculateMean(newFlightTimes) : 0.0);

        // Step 4: Run impostor detection
        double impostorRisk = impostorDetector.analyzePattern(newHoldTimes, newFlightTimes);

        // If impostor risk is extremely high, reject immediately
        if (impostorRisk >= 90.0) {
            AuthResult result = new AuthResult(false, 0.0, thresholdManager.getCurrentThreshold(),
                    impostorRisk, "Rejected: Extreme impostor risk detected", username);

            authLogger.logAuthAttempt(username, false, 0.0,
                    thresholdManager.getCurrentThreshold(), impostorRisk);
            impostorDetector.recordFailedAttempt(username);

            return result;
        }

        // Step 5: Compute keystroke similarity score
        System.out.println("\n  Computing similarity using " + scorer.getAlgorithmName() + "...");
        double keystrokeScore = scorer.calculateSimilarity(storedProfile, inputProfile);

        // Step 5b: Compute mouse dynamics score if available
        double mouseScore = -1;
        double combinedScore = keystrokeScore;

        if (mouseDynamicsEnabled && mouseProfile != null) {
            MouseDynamicsProfile storedMouseProfile = fileManager.loadMouseProfile(username);
            if (storedMouseProfile != null) {
                mouseScore = storedMouseProfile.calculateSimilarity(mouseProfile);
                // Combined: 70% keystroke + 30% mouse
                combinedScore = 0.7 * keystrokeScore + 0.3 * mouseScore;
                System.out.printf("  [Mouse] Score: %.2f%% | Combined: %.2f%% (70/30 split)%n",
                        mouseScore, combinedScore);
            } else {
                System.out.println("  [Mouse] No stored mouse profile — using keystroke only.");
            }
        }

        // Step 6: Apply threshold (use ML personalized if enabled)
        double threshold;
        if (mlThresholdEnabled && adaptiveML.hasEnoughData(username)) {
            threshold = adaptiveML.getPersonalizedThreshold(username);
            System.out.printf("  [ML] Using personalized threshold: %.2f%%%n", threshold);
        } else {
            threshold = thresholdManager.getCurrentThreshold();
        }

        boolean isAuthenticated = combinedScore >= threshold;

        // If impostor risk is elevated, apply stricter check
        if (impostorRisk >= SystemConstants.IMPOSTOR_FLAG_THRESHOLD) {
            double adjustedThreshold = threshold + 10.0; // Raise threshold for suspicious patterns
            isAuthenticated = combinedScore >= adjustedThreshold;
            System.out.printf("  [!] Elevated impostor risk (%.1f%%) — threshold raised to %.1f%%\n",
                    impostorRisk, adjustedThreshold);
        }

        // Build reason string
        String reason;
        if (isAuthenticated) {
            reason = String.format("Similarity %.1f%% >= Threshold %.1f%%", combinedScore, threshold);
        } else {
            reason = String.format("Similarity %.1f%% < Threshold %.1f%%", combinedScore, threshold);
        }

        // Step 7: Create result
        AuthResult result = new AuthResult(isAuthenticated, combinedScore, threshold,
                impostorRisk, reason, username);
        if (mouseScore >= 0) {
            result.setMouseScore(mouseScore);
        }

        // Step 8: Log the attempt
        authLogger.logAuthAttempt(username, isAuthenticated, combinedScore, threshold, impostorRisk);

        // Step 9: Update session tracking
        if (isAuthenticated) {
            impostorDetector.recordSuccessfulAttempt(username);
        } else {
            impostorDetector.recordFailedAttempt(username);
        }

        // Step 10: Adaptively adjust threshold
        thresholdManager.adjustThreshold(isAuthenticated, combinedScore);

        // Step 11: Record in ML learning dataset
        if (mlThresholdEnabled) {
            adaptiveML.recordAuthentication(username, combinedScore, isAuthenticated);
        }

        return result;
    }

    // ==================== Feature Flag Setters ====================

    /**
     * Enables or disables ML-based adaptive threshold.
     */
    public void setMLThresholdEnabled(boolean enabled) {
        this.mlThresholdEnabled = enabled;
    }

    /**
     * Enables or disables mouse dynamics for combined scoring.
     */
    public void setMouseDynamicsEnabled(boolean enabled) {
        this.mouseDynamicsEnabled = enabled;
    }

    // ==================== Accessor Methods ====================

    /**
     * Returns the threshold manager for external configuration.
     *
     * @return the ThresholdManager instance
     */
    public ThresholdManager getThresholdManager() {
        return thresholdManager;
    }

    /**
     * Returns the impostor detector for session management.
     *
     * @return the ImpostorDetector instance
     */
    public ImpostorDetector getImpostorDetector() {
        return impostorDetector;
    }

    /**
     * Returns the auth logger for report generation.
     *
     * @return the AuthLogger instance
     */
    public AuthLogger getAuthLogger() {
        return authLogger;
    }

    /**
     * Returns the similarity scorer for algorithm info.
     *
     * @return the SimilarityScorer instance
     */
    public SimilarityScorer getScorer() {
        return scorer;
    }

    /**
     * Returns the adaptive ML threshold engine.
     *
     * @return the AdaptiveThresholdML instance
     */
    public AdaptiveThresholdML getAdaptiveML() {
        return adaptiveML;
    }
}
