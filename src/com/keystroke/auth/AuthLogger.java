package com.keystroke.auth;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AuthLogger.java - Comprehensive logging system for authentication events.
 *
 * Logs are stored in daily files with the format: auth_YYYY_MM_DD.txt
 * Each log entry records: timestamp, username, result, confidence score, and threshold.
 *
 * Features:
 *   - Per-attempt logging with precise timestamps
 *   - Daily log file rotation
 *   - Daily report generation (success rate, failure count)
 *   - Log history retrieval for individual users
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class AuthLogger {

    /** Date formatter for log file names */
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy_MM_dd");

    /** Timestamp formatter for log entries */
    private static final DateTimeFormatter LOG_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Logs an authentication attempt to the daily log file.
     *
     * Format: [TIMESTAMP] USERNAME - SUCCESS/FAILED - Confidence: XX.XX% - Threshold: XX.XX%
     *
     * @param username   the username that was authenticated
     * @param success    whether the authentication succeeded
     * @param confidence the similarity confidence score (0-100)
     * @param threshold  the threshold used for this decision (0-100)
     */
    public void logAuthAttempt(String username, boolean success,
                               double confidence, double threshold) {
        try {
            // Ensure the logs directory exists
            Path logsDir = Paths.get(SystemConstants.LOGS_DIR);
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            // Build the daily log file path
            String today = LocalDate.now().format(FILE_DATE_FORMAT);
            String logFile = SystemConstants.LOGS_DIR + File.separator
                    + SystemConstants.LOG_FILE_PREFIX + today + SystemConstants.LOG_FILE_EXTENSION;

            // Format the log entry
            String timestamp = LocalDateTime.now().format(LOG_TIMESTAMP_FORMAT);
            String result = success ? "SUCCESS" : "FAILED";
            String logEntry = String.format("[%s] %s - %s - Confidence: %.2f%% - Threshold: %.2f%%",
                    timestamp, username, result, confidence, threshold);

            // Append to log file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logEntry);
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to write auth log: " + e.getMessage());
        }
    }

    /**
     * Logs an authentication attempt with impostor risk information.
     *
     * @param username     the username that was authenticated
     * @param success      whether the authentication succeeded
     * @param confidence   the similarity confidence score (0-100)
     * @param threshold    the threshold used for this decision (0-100)
     * @param impostorRisk the impostor risk score (0-100)
     */
    public void logAuthAttempt(String username, boolean success,
                               double confidence, double threshold, double impostorRisk) {
        try {
            Path logsDir = Paths.get(SystemConstants.LOGS_DIR);
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }

            String today = LocalDate.now().format(FILE_DATE_FORMAT);
            String logFile = SystemConstants.LOGS_DIR + File.separator
                    + SystemConstants.LOG_FILE_PREFIX + today + SystemConstants.LOG_FILE_EXTENSION;

            String timestamp = LocalDateTime.now().format(LOG_TIMESTAMP_FORMAT);
            String result = success ? "SUCCESS" : "FAILED";
            String logEntry = String.format(
                    "[%s] %s - %s - Confidence: %.2f%% - Threshold: %.2f%% - ImpostorRisk: %.2f%%",
                    timestamp, username, result, confidence, threshold, impostorRisk);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logEntry);
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to write auth log: " + e.getMessage());
        }
    }

    /**
     * Logs a two-factor authentication (OTP) attempt.
     *
     * @param username the username
     * @param success  whether the OTP was verified successfully
     */
    public void log2FAAttempt(String username, boolean success) {
        try {
            Path logsDir = Paths.get(SystemConstants.LOGS_DIR);
            if (!Files.exists(logsDir)) Files.createDirectories(logsDir);

            String today = LocalDate.now().format(FILE_DATE_FORMAT);
            String logFile = SystemConstants.LOGS_DIR + File.separator
                    + SystemConstants.LOG_FILE_PREFIX + today + SystemConstants.LOG_FILE_EXTENSION;

            String timestamp = LocalDateTime.now().format(LOG_TIMESTAMP_FORMAT);
            String result = success ? "2FA_SUCCESS" : "2FA_FAILED";
            String logEntry = String.format("[%s] %s - %s - Two-Factor OTP Verification",
                    timestamp, username, result);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write(logEntry);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to write 2FA log: " + e.getMessage());
        }
    }

    /**
     * Generates a daily report for the current day's authentication activity.
     *
     * Report includes:
     *   - Total authentication attempts
     *   - Successful attempts and success rate
     *   - Failed attempts and failure rate
     *   - Per-user breakdown
     */
    public void generateDailyReport() {
        String today = LocalDate.now().format(FILE_DATE_FORMAT);
        String logFile = SystemConstants.LOGS_DIR + File.separator
                + SystemConstants.LOG_FILE_PREFIX + today + SystemConstants.LOG_FILE_EXTENSION;

        if (!Files.exists(Paths.get(logFile))) {
            System.out.println("\n  [!] No authentication logs found for today (" + today + ").\n");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(logFile));

            int total = lines.size();
            int successes = 0;
            int failures = 0;
            List<String> userNames = new ArrayList<>();

            for (String line : lines) {
                if (line.contains("- SUCCESS -")) {
                    successes++;
                } else if (line.contains("- FAILED -")) {
                    failures++;
                }

                // Extract username
                int startBracket = line.indexOf("] ");
                int dash = line.indexOf(" - ", startBracket);
                if (startBracket >= 0 && dash > startBracket) {
                    String user = line.substring(startBracket + 2, dash).trim();
                    if (!userNames.contains(user)) {
                        userNames.add(user);
                    }
                }
            }

            double successRate = (total > 0) ? ((double) successes / total * 100.0) : 0.0;

            // Display the report
            System.out.println("\n  ╔══════════════════════════════════════════════════════╗");
            System.out.println("  ║              DAILY AUTHENTICATION REPORT             ║");
            System.out.println("  ╠══════════════════════════════════════════════════════╣");
            System.out.printf("  ║  Date                : %-30s ║\n",
                    today.replace("_", "-"));
            System.out.printf("  ║  Total Attempts      : %-30d ║\n", total);
            System.out.printf("  ║  Successful          : %-30d ║\n", successes);
            System.out.printf("  ║  Failed              : %-30d ║\n", failures);
            System.out.printf("  ║  Success Rate        : %-27.2f%%  ║\n", successRate);
            System.out.printf("  ║  Unique Users        : %-30d ║\n", userNames.size());
            System.out.println("  ╠══════════════════════════════════════════════════════╣");
            System.out.println("  ║  USERS:                                              ║");

            for (String user : userNames) {
                int userSuccess = 0;
                int userFail = 0;
                for (String line : lines) {
                    if (line.contains("] " + user + " - ")) {
                        if (line.contains("- SUCCESS -")) userSuccess++;
                        else if (line.contains("- FAILED -")) userFail++;
                    }
                }
                System.out.printf("  ║    %-15s : %d pass / %d fail              ║\n",
                        user, userSuccess, userFail);
            }

            System.out.println("  ╚══════════════════════════════════════════════════════╝\n");

            // Show the raw log entries
            System.out.println("  ──── RAW LOG ENTRIES ────");
            for (String line : lines) {
                System.out.println("  " + line);
            }
            System.out.println("  ─────────────────────────\n");

        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to generate report: " + e.getMessage());
        }
    }

    /**
     * Retrieves authentication history for a specific user.
     *
     * @param username the username to search for
     * @return a list of log entries related to this user
     */
    public List<String> getUserHistory(String username) {
        List<String> history = new ArrayList<>();

        Path logsDir = Paths.get(SystemConstants.LOGS_DIR);
        if (!Files.exists(logsDir)) {
            return history;
        }

        try {
            // Scan all log files for entries matching this username
            File dir = logsDir.toFile();
            File[] logFiles = dir.listFiles((d, name) ->
                    name.startsWith(SystemConstants.LOG_FILE_PREFIX) &&
                            name.endsWith(SystemConstants.LOG_FILE_EXTENSION));

            if (logFiles != null) {
                for (File file : logFiles) {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        if (line.contains("] " + username + " - ")) {
                            history.add(line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to retrieve user history: " + e.getMessage());
        }

        return history;
    }

    /**
     * Displays the authentication history for a specific user.
     *
     * @param username the username whose history to display
     */
    public void displayUserHistory(String username) {
        List<String> history = getUserHistory(username);

        if (history.isEmpty()) {
            System.out.println("\n  [!] No authentication history found for user: " + username + "\n");
            return;
        }

        System.out.println("\n  ──── AUTH HISTORY: " + username + " ────");
        for (String entry : history) {
            System.out.println("  " + entry);
        }
        System.out.printf("  Total attempts: %d\n", history.size());
        System.out.println("  ──────────────────────────────────\n");
    }

    /**
     * Displays all log entries from today's log file.
     */
    public void viewTodaysLogs() {
        String today = LocalDate.now().format(FILE_DATE_FORMAT);
        String logFile = SystemConstants.LOGS_DIR + File.separator
                + SystemConstants.LOG_FILE_PREFIX + today + SystemConstants.LOG_FILE_EXTENSION;

        if (!Files.exists(Paths.get(logFile))) {
            System.out.println("\n  [!] No logs found for today.\n");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(logFile));
            System.out.println("\n  ──── TODAY'S AUTH LOGS (" + today.replace("_", "-") + ") ────");
            for (String line : lines) {
                System.out.println("  " + line);
            }
            System.out.printf("  Total entries: %d\n", lines.size());
            System.out.println("  ─────────────────────────────────────────\n");
        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to read logs: " + e.getMessage());
        }
    }
}
