package com.keystroke.auth;

/**
 * AuthenticationException.java - Custom exception for authentication-related errors.
 *
 * Thrown when authentication operations fail due to invalid credentials,
 * missing profiles, threshold violations, or session lockouts.
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class AuthenticationException extends Exception {

    /** Error code for categorizing the type of authentication failure */
    private final ErrorCode errorCode;

    /**
     * Enum defining specific authentication error categories.
     */
    public enum ErrorCode {
        /** User profile not found in the system */
        PROFILE_NOT_FOUND("Profile not found"),

        /** Authentication threshold not met */
        THRESHOLD_NOT_MET("Similarity below threshold"),

        /** Too many failed authentication attempts */
        SESSION_LOCKED("Session locked due to failed attempts"),

        /** Impostor pattern detected */
        IMPOSTOR_DETECTED("Suspicious typing pattern detected"),

        /** Invalid input data provided */
        INVALID_INPUT("Invalid input data"),

        /** System configuration error */
        SYSTEM_ERROR("Internal authentication system error");

        private final String description;

        ErrorCode(String description) {
            this.description = description;
        }

        /**
         * Returns a human-readable description of this error code.
         *
         * @return the error description
         */
        public String getDescription() {
            return description;
        }
    }

    /**
     * Constructs an AuthenticationException with a message and error code.
     *
     * @param message   detailed error message
     * @param errorCode the category of authentication failure
     */
    public AuthenticationException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs an AuthenticationException with a message, error code, and cause.
     *
     * @param message   detailed error message
     * @param errorCode the category of authentication failure
     * @param cause     the underlying exception that caused this error
     */
    public AuthenticationException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return the authentication error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns a formatted string representation of this exception.
     *
     * @return formatted error message with code
     */
    @Override
    public String toString() {
        return String.format("AuthenticationException[%s]: %s", errorCode.name(), getMessage());
    }
}
