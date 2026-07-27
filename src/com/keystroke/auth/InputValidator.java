package com.keystroke.auth;

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * InputValidator.java - Centralized input validation and security utilities.
 *
 * Provides validation for:
 *   - Usernames (alphanumeric, length limits)
 *   - Timing data (realistic ranges)
 *   - File paths (path traversal prevention)
 *   - Text input (injection sanitization)
 *
 * Week 3 - Phase 2 Finalization
 */
public class InputValidator {

    /** Minimum username length */
    private static final int MIN_USERNAME_LENGTH = 3;

    /** Maximum username length */
    private static final int MAX_USERNAME_LENGTH = 20;

    /** Minimum realistic hold/flight time in milliseconds */
    private static final double MIN_TIMING_MS = 10.0;

    /** Maximum realistic hold/flight time in milliseconds */
    private static final double MAX_TIMING_MS = 2000.0;

    /**
     * Validates a username string.
     * Rules: alphanumeric + underscores only, 3-20 characters.
     *
     * @param username the username to validate
     * @return true if the username is valid
     */
    public static boolean validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String trimmed = username.trim();
        if (trimmed.length() < MIN_USERNAME_LENGTH || trimmed.length() > MAX_USERNAME_LENGTH) {
            return false;
        }
        return trimmed.matches("[a-zA-Z0-9_]+");
    }

    /**
     * Returns a detailed error message for invalid usernames.
     *
     * @param username the username that failed validation
     * @return an error message describing the problem, or null if valid
     */
    public static String getUsernameError(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username cannot be empty.";
        }
        String trimmed = username.trim();
        if (trimmed.length() < MIN_USERNAME_LENGTH) {
            return "Username must be at least " + MIN_USERNAME_LENGTH + " characters.";
        }
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            return "Username must be " + MAX_USERNAME_LENGTH + " characters or fewer.";
        }
        if (!trimmed.matches("[a-zA-Z0-9_]+")) {
            return "Username may only contain letters, numbers, and underscores.";
        }
        return null; // Valid
    }

    /**
     * Validates an array of timing data.
     * Each value must be within a realistic range (10ms - 2000ms).
     *
     * @param timings the timing array to validate
     * @return true if all values are within realistic bounds
     */
    public static boolean validateTimingData(double[] timings) {
        if (timings == null || timings.length == 0) {
            return false;
        }
        for (double t : timings) {
            if (t < MIN_TIMING_MS || t > MAX_TIMING_MS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a validation result for timing data with details about invalid values.
     *
     * @param timings the timing array to validate
     * @return error message or null if valid
     */
    public static String getTimingDataError(double[] timings) {
        if (timings == null || timings.length == 0) {
            return "Timing data is null or empty.";
        }
        for (int i = 0; i < timings.length; i++) {
            if (timings[i] < MIN_TIMING_MS) {
                return String.format("Timing[%d] = %.2f ms is below minimum (%.0f ms). Possible bot activity.",
                        i, timings[i], MIN_TIMING_MS);
            }
            if (timings[i] > MAX_TIMING_MS) {
                return String.format("Timing[%d] = %.2f ms exceeds maximum (%.0f ms). Suspicious delay.",
                        i, timings[i], MAX_TIMING_MS);
            }
        }
        return null; // Valid
    }

    /**
     * Sanitizes text input to prevent injection attacks.
     * Removes control characters, limits length, and escapes special characters.
     *
     * @param input the raw user input
     * @return the sanitized string
     */
    public static String sanitizeInput(String input) {
        if (input == null) return "";

        // Remove control characters (except newline and tab)
        String sanitized = input.replaceAll("[\\p{Cc}&&[^\\n\\t]]", "");

        // Remove potential file path separators in username contexts
        sanitized = sanitized.replace("..", "");
        sanitized = sanitized.replace("/", "");
        sanitized = sanitized.replace("\\", "");

        // Trim excessive whitespace
        sanitized = sanitized.trim();

        // Limit length to prevent buffer-style attacks
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 500);
        }

        return sanitized;
    }

    /**
     * Validates a file access path to prevent path traversal attacks.
     * Ensures the resolved path stays within the allowed base directory.
     *
     * @param filePath the file path to validate
     * @param allowedBaseDir the base directory that the path must stay within
     * @return true if the path is safe
     */
    public static boolean validateFileAccess(String filePath, String allowedBaseDir) {
        if (filePath == null || allowedBaseDir == null) return false;

        try {
            // Resolve both paths to absolute canonical form
            Path resolved = Paths.get(filePath).toAbsolutePath().normalize();
            Path base = Paths.get(allowedBaseDir).toAbsolutePath().normalize();

            // Check that the resolved path starts with the base directory
            return resolved.startsWith(base);
        } catch (Exception e) {
            return false; // Any exception means the path is suspicious
        }
    }

    /**
     * Validates a menu choice is within a valid range.
     *
     * @param input the user input string
     * @param minOption the minimum valid option number
     * @param maxOption the maximum valid option number
     * @return the parsed option number, or -1 if invalid
     */
    public static int validateMenuChoice(String input, int minOption, int maxOption) {
        if (input == null || input.trim().isEmpty()) return -1;
        try {
            int choice = Integer.parseInt(input.trim());
            if (choice >= minOption && choice <= maxOption) {
                return choice;
            }
        } catch (NumberFormatException e) {
            // Invalid number format
        }
        return -1;
    }

    /**
     * Validates a threshold value is within the allowed range.
     *
     * @param threshold the threshold to validate
     * @return true if the threshold is within bounds
     */
    public static boolean validateThreshold(double threshold) {
        return threshold >= SystemConstants.MIN_THRESHOLD
                && threshold <= SystemConstants.MAX_THRESHOLD;
    }

    /**
     * Validates a password meets minimum requirements.
     *
     * @param password the password to validate
     * @return true if the password meets requirements
     */
    public static boolean validatePassword(String password) {
        return password != null && password.length() >= 4;
    }
}
