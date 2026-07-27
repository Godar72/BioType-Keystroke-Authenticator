package com.keystroke.auth;

import java.security.SecureRandom;

/**
 * TwoFactorAuth.java — Cryptographic OTP generator and validator for
 * two-factor authentication as a secondary verification layer after
 * successful keystroke biometric authentication.
 *
 * Uses java.security.SecureRandom for cryptographic-grade OTP generation.
 * OTPs are 6-digit numeric codes valid for 30 seconds.
 *
 * Phase 2 Enhancement — Two-Factor Authentication Module
 */
public class TwoFactorAuth {

    /** Cryptographic random number generator */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Number of digits in the OTP */
    private static final int OTP_LENGTH = 6;

    /** Current OTP (session-only, not persisted) */
    private String currentOTP;

    /** Timestamp when OTP was generated */
    private long otpGeneratedAt;

    /** OTP validity window in milliseconds */
    private final long expiryMillis;

    /**
     * Constructs a TwoFactorAuth module with the default 30-second expiry.
     */
    public TwoFactorAuth() {
        this.expiryMillis = SystemConstants.OTP_EXPIRY_SECONDS * 1000L;
        this.currentOTP = null;
        this.otpGeneratedAt = 0;
    }

    /**
     * Generates a new 6-digit OTP using SecureRandom.
     * Stores the OTP and records the generation timestamp.
     *
     * @return the generated OTP string (zero-padded to 6 digits)
     */
    public String generateOTP() {
        int otp = SECURE_RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH));
        this.currentOTP = String.format("%0" + OTP_LENGTH + "d", otp);
        this.otpGeneratedAt = System.currentTimeMillis();

        System.out.printf("  [2FA] OTP generated: %s (valid for %d seconds)%n",
                currentOTP, SystemConstants.OTP_EXPIRY_SECONDS);
        return currentOTP;
    }

    /**
     * Validates a user-entered OTP against the current stored OTP.
     * Also checks whether the OTP has expired.
     *
     * @param input the user-entered OTP string
     * @return true if the OTP matches and is within the validity window
     */
    public boolean validateOTP(String input) {
        if (currentOTP == null || input == null) {
            return false;
        }
        if (isExpired()) {
            System.out.println("  [2FA] OTP expired.");
            invalidate();
            return false;
        }
        boolean valid = currentOTP.equals(input.trim());
        if (valid) {
            System.out.println("  [2FA] OTP verified successfully.");
            invalidate(); // One-time use
        } else {
            System.out.println("  [2FA] OTP mismatch.");
        }
        return valid;
    }

    /**
     * Checks whether the current OTP has expired.
     *
     * @return true if the OTP is older than the expiry window
     */
    public boolean isExpired() {
        if (otpGeneratedAt == 0) return true;
        return (System.currentTimeMillis() - otpGeneratedAt) > expiryMillis;
    }

    /**
     * Returns how many seconds remain before the OTP expires.
     *
     * @return remaining seconds (0 if expired)
     */
    public int getRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - otpGeneratedAt;
        long remaining = expiryMillis - elapsed;
        return (remaining > 0) ? (int) (remaining / 1000) : 0;
    }

    /**
     * Invalidates the current OTP after use or expiry.
     */
    public void invalidate() {
        this.currentOTP = null;
        this.otpGeneratedAt = 0;
    }

    /**
     * Returns the current OTP for display purposes.
     *
     * @return the OTP string, or null if not generated
     */
    public String getCurrentOTP() {
        return currentOTP;
    }
}
