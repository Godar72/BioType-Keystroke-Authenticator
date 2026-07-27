package com.keystroke.auth;

/**
 * AuthResult.java - Immutable data object encapsulating an authentication attempt result.
 *
 * Contains the authentication decision, confidence score, threshold used,
 * impostor risk assessment, and descriptive reason for the outcome.
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class AuthResult {

    // Private fields - immutable after construction
    private final boolean authenticated;
    private final double confidenceScore;   // 0-100 scale
    private final double thresholdUsed;     // 0-100 scale
    private final double impostorRisk;      // 0-100 scale
    private final String reason;
    private final String username;
    private final long timestamp;
    private double mouseScore = -1;         // -1 means not used

    /**
     * Constructs a full AuthResult with all fields.
     *
     * @param authenticated   whether the user was authenticated
     * @param confidenceScore the similarity confidence score (0-100)
     * @param thresholdUsed   the threshold value used for this decision (0-100)
     * @param impostorRisk    the impostor risk score (0-100)
     * @param reason          human-readable reason for the result
     * @param username        the username that was authenticated
     */
    public AuthResult(boolean authenticated, double confidenceScore, double thresholdUsed,
                      double impostorRisk, String reason, String username) {
        this.authenticated = authenticated;
        this.confidenceScore = confidenceScore;
        this.thresholdUsed = thresholdUsed;
        this.impostorRisk = impostorRisk;
        this.reason = reason;
        this.username = username;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Simplified constructor without impostor risk.
     *
     * @param authenticated   whether the user was authenticated
     * @param confidenceScore the similarity confidence score (0-100)
     * @param thresholdUsed   the threshold value used (0-100)
     * @param reason          human-readable reason for the result
     * @param username        the username that was authenticated
     */
    public AuthResult(boolean authenticated, double confidenceScore, double thresholdUsed,
                      String reason, String username) {
        this(authenticated, confidenceScore, thresholdUsed, 0.0, reason, username);
    }

    // ==================== Getter Methods ====================

    /**
     * Returns whether the authentication was successful.
     *
     * @return true if authenticated, false if rejected
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Returns the confidence score of the match (0-100 scale).
     * Higher values indicate stronger match with stored profile.
     *
     * @return the confidence score
     */
    public double getConfidenceScore() {
        return confidenceScore;
    }

    /**
     * Returns the threshold value that was used for this authentication decision.
     *
     * @return the threshold used (0-100 scale)
     */
    public double getThresholdUsed() {
        return thresholdUsed;
    }

    /**
     * Returns the impostor risk score (0-100 scale).
     * Higher values indicate more suspicious patterns.
     *
     * @return the impostor risk score
     */
    public double getImpostorRisk() {
        return impostorRisk;
    }

    /**
     * Returns the human-readable reason for the authentication decision.
     *
     * @return the reason string
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the username that was being authenticated.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the timestamp when this result was created (epoch milliseconds).
     *
     * @return the creation timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the mouse dynamics confidence score, or -1 if not used.
     *
     * @return the mouse dynamics score (0-100) or -1
     */
    public double getMouseScore() {
        return mouseScore;
    }

    /**
     * Sets the mouse dynamics score for this result.
     *
     * @param score the mouse similarity score (0-100)
     */
    public void setMouseScore(double score) {
        this.mouseScore = score;
    }

    // ==================== Display Methods ====================

    /**
     * Displays the authentication result in a formatted console box.
     */
    public void displayResult() {
        System.out.println("\n  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║            AUTHENTICATION RESULT                     ║");
        System.out.println("  ╠══════════════════════════════════════════════════════╣");
        System.out.printf("  ║  User              : %-32s ║\n", username);
        System.out.printf("  ║  Confidence Score  : %6.2f%%                         ║\n", confidenceScore);
        System.out.printf("  ║  Threshold Used    : %6.2f%%                         ║\n", thresholdUsed);
        System.out.printf("  ║  Impostor Risk     : %6.2f%%                         ║\n", impostorRisk);
        System.out.printf("  ║  Reason            : %-32s ║\n", reason);
        System.out.println("  ╠══════════════════════════════════════════════════════╣");

        if (authenticated) {
            System.out.println("  ║                                                      ║");
            System.out.println("  ║      ✅  AUTHENTICATION SUCCESSFUL                   ║");
            System.out.println("  ║      Identity verified — Access Granted               ║");
            System.out.println("  ║                                                      ║");
        } else {
            System.out.println("  ║                                                      ║");
            System.out.println("  ║      ❌  AUTHENTICATION FAILED                       ║");
            System.out.println("  ║      Identity not verified — Access Denied            ║");
            System.out.println("  ║                                                      ║");
        }
        System.out.println("  ╚══════════════════════════════════════════════════════╝\n");
    }

    /**
     * Returns a string representation of this result.
     *
     * @return formatted result summary
     */
    @Override
    public String toString() {
        return String.format("AuthResult{user='%s', auth=%s, confidence=%.2f%%, threshold=%.2f%%, risk=%.2f%%}",
                username, authenticated ? "SUCCESS" : "FAILED", confidenceScore, thresholdUsed, impostorRisk);
    }
}
