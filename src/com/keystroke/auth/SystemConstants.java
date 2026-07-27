package com.keystroke.auth;

/**
 * SystemConstants.java - Centralized constants for the BioType keystroke authentication system.
 *
 * All file paths, system settings, and default values are defined here to avoid
 * magic numbers/strings scattered throughout the codebase.
 *
 * Week 2-3 Enhancement - Phase 2
 */
public final class SystemConstants {

    // Prevent instantiation
    private SystemConstants() {}

    // ==================== Directory Paths ====================

    /** Root directory for all persistent data */
    public static final String DATA_DIR = "profiles";

    /** Directory for user profile files */
    public static final String USERS_DIR = DATA_DIR + "/users";

    /** Directory for threshold configuration files */
    public static final String THRESHOLDS_DIR = DATA_DIR + "/thresholds";

    /** Directory for authentication log files */
    public static final String LOGS_DIR = DATA_DIR + "/logs";

    /** Directory for admin settings files */
    public static final String ADMIN_DIR = DATA_DIR + "/admin";

    /** Directory for ML learning data files */
    public static final String ML_DIR = DATA_DIR + "/ml";

    // ==================== File Names ====================

    /** System threshold configuration file */
    public static final String THRESHOLD_FILE = "system_threshold.txt";

    /** Admin settings file */
    public static final String ADMIN_SETTINGS_FILE = "admin_settings.txt";

    /** Profile file extension */
    public static final String PROFILE_EXTENSION = ".profile";

    /** Log file prefix */
    public static final String LOG_FILE_PREFIX = "auth_";

    /** Log file extension */
    public static final String LOG_FILE_EXTENSION = ".txt";

    /** Mouse dynamics profile file extension */
    public static final String MOUSE_PROFILE_EXTENSION = ".mouse";

    // ==================== Authentication Settings ====================

    /** Default authentication threshold (percentage, 0-100 scale) */
    public static final double DEFAULT_THRESHOLD = 70.0;

    /** Minimum allowed threshold */
    public static final double MIN_THRESHOLD = 40.0;

    /** Maximum allowed threshold */
    public static final double MAX_THRESHOLD = 80.0;

    /** Threshold adjustment step size for adaptive learning */
    public static final double THRESHOLD_ADJUST_STEP = 2.0;

    /** Number of enrollment samples required */
    public static final int ENROLLMENT_SAMPLES = 3;

    /** Maximum enrollment samples allowed */
    public static final int MAX_ENROLLMENT_SAMPLES = 5;

    // ==================== Similarity Scoring Weights ====================

    /** Weight for hold timing similarity in combined score */
    public static final double HOLD_WEIGHT = 0.6;

    /** Weight for flight timing similarity in combined score */
    public static final double FLIGHT_WEIGHT = 0.4;

    // ==================== Impostor Detection Settings ====================

    /** Maximum failed attempts before session lockout */
    public static final int MAX_FAILED_ATTEMPTS = 3;

    /** Minimum standard deviation threshold - below this indicates bot-like consistency */
    public static final double BOT_DETECTION_STD_DEV = 0.5;

    /** Maximum reasonable typing speed (ms per character) - faster is suspicious */
    public static final double MIN_TYPING_SPEED_MS = 10.0;

    /** Minimum reasonable typing speed (ms per character) - slower is suspicious */
    public static final double MAX_TYPING_SPEED_MS = 5000.0;

    /** Impostor risk score threshold for flagging */
    public static final double IMPOSTOR_FLAG_THRESHOLD = 70.0;

    // ==================== Admin Credentials ====================

    /** Default admin username */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    /** Default admin password */
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    // ==================== Standard Phrase ====================

    /** Standard phrase for enrollment and authentication */
    public static final String STANDARD_PHRASE = "The quick brown fox jumps over the lazy dog";

    // ==================== Display Formatting ====================

    /** Width of the console box borders */
    public static final int BOX_WIDTH = 56;

    /** Separator line for console output */
    public static final String SEPARATOR = "  ════════════════════════════════════════════════════";

    // ==================== System Metadata ====================

    /** System version string */
    public static final String SYSTEM_VERSION = "2.0.0";

    /** Build date */
    public static final String BUILD_DATE = "2026-04-01";

    /** Application name */
    public static final String APP_NAME = "BioType";

    // ==================== Backup Settings ====================

    /** Directory for system backups */
    public static final String BACKUP_DIR = DATA_DIR + "/backups";

    /** Backup file prefix */
    public static final String BACKUP_PREFIX = "keystroke_backup_";

    // ==================== Two-Factor Authentication ====================

    /** OTP validity window in seconds */
    public static final int OTP_EXPIRY_SECONDS = 30;
}
