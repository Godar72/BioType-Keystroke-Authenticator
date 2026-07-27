package com.keystroke.auth;

/**
 * ProfileException.java - Custom exception for profile-related errors.
 *
 * Thrown when profile operations fail due to I/O errors, corrupt data,
 * invalid profile formats, or missing required fields.
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class ProfileException extends Exception {

    /** Error code for categorizing the type of profile failure */
    private final ErrorCode errorCode;

    /**
     * Enum defining specific profile error categories.
     */
    public enum ErrorCode {
        /** Profile file not found on disk */
        FILE_NOT_FOUND("Profile file not found"),

        /** Profile data is corrupt or malformed */
        CORRUPT_DATA("Profile data is corrupt or unreadable"),

        /** Failed to write profile to disk */
        WRITE_FAILURE("Failed to save profile"),

        /** Failed to read profile from disk */
        READ_FAILURE("Failed to load profile"),

        /** Profile already exists when not expected */
        DUPLICATE_PROFILE("Profile already exists"),

        /** Required profile field is missing or empty */
        MISSING_FIELD("Required profile field is missing"),

        /** Profile directory cannot be created */
        DIRECTORY_ERROR("Cannot create or access profile directory");

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
     * Constructs a ProfileException with a message and error code.
     *
     * @param message   detailed error message
     * @param errorCode the category of profile failure
     */
    public ProfileException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a ProfileException with a message, error code, and cause.
     *
     * @param message   detailed error message
     * @param errorCode the category of profile failure
     * @param cause     the underlying exception that caused this error
     */
    public ProfileException(String message, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return the profile error code
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
        return String.format("ProfileException[%s]: %s", errorCode.name(), getMessage());
    }
}
