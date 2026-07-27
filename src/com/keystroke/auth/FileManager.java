package com.keystroke.auth;

import java.io.*;
import java.nio.file.*;

/**
 * FileManager.java - Enhanced file-based persistence for keystroke profiles.
 *
 * Updated file structure for Phase 2:
 *   profiles/
 *   ├── users/[username].profile
 *   ├── thresholds/system_threshold.txt
 *   ├── logs/auth_YYYY_MM_DD.txt
 *   └── admin/admin_settings.txt
 *
 * File Format (profiles/users/[username].profile):
 *   Line 1: username
 *   Line 2: averageHoldTime,averageFlightTime,holdStdDev,flightStdDev
 *   Line 3: holdTiming1,holdTiming2,...,holdTimingN  (comma-separated)
 *   Line 4: flightTiming1,flightTiming2,...,flightTimingM  (comma-separated)
 *
 * Week 2-3 Enhancement - Phase 2 (Updated from Week 1)
 */
public class FileManager {

    /**
     * Initializes the file manager and ensures all required directories exist.
     */
    public FileManager() {
        initializeDirectories();
    }

    /**
     * Creates all required directories if they don't already exist.
     */
    private void initializeDirectories() {
        String[] dirs = {
                SystemConstants.USERS_DIR,
                SystemConstants.THRESHOLDS_DIR,
                SystemConstants.LOGS_DIR,
                SystemConstants.ADMIN_DIR,
                SystemConstants.ML_DIR
        };

        for (String dir : dirs) {
            try {
                Path dirPath = Paths.get(dir);
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                }
            } catch (IOException e) {
                System.out.println("  [ERROR] Failed to create directory: " + dir
                        + " — " + e.getMessage());
            }
        }
    }

    // ==================== Profile Save / Load ====================

    /**
     * Saves a user's keystroke profile to a text file.
     *
     * @param profile  the KeystrokeProfile to save
     * @param filename the filename (e.g., "john.profile")
     */
    public void saveUserProfile(KeystrokeProfile profile, String filename) {
        try {
            String filePath = SystemConstants.USERS_DIR + File.separator + filename;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

                // Line 1: Username
                writer.write(profile.getUsername());
                writer.newLine();

                // Line 2: Summary statistics (comma-separated)
                writer.write(String.format("%.6f,%.6f,%.6f,%.6f",
                        profile.getAverageHoldTime(),
                        profile.getAverageFlightTime(),
                        profile.getHoldStdDev(),
                        profile.getFlightStdDev()));
                writer.newLine();

                // Line 3: Hold timings array (comma-separated)
                writer.write(arrayToCSV(profile.getHoldTimings()));
                writer.newLine();

                // Line 4: Flight timings array (comma-separated)
                writer.write(arrayToCSV(profile.getFlightTimings()));
                writer.newLine();

                // Line 5: Custom passphrase
                writer.write(profile.getPhrase() != null ? profile.getPhrase() : SystemConstants.STANDARD_PHRASE);
                writer.newLine();
            }

            System.out.println("  [✓] Profile saved: " + filePath);

        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to save profile: " + e.getMessage());
        }
    }

    /**
     * Loads a user's keystroke profile from a text file.
     *
     * @param username the username whose profile to load
     * @return the loaded KeystrokeProfile, or null if unavailable
     */
    public KeystrokeProfile loadUserProfile(String username) {
        String filePath = SystemConstants.USERS_DIR + File.separator
                + username + SystemConstants.PROFILE_EXTENSION;

        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("  [!] No profile found for user: " + username);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            // Line 1: Username
            String storedUsername = reader.readLine();
            if (storedUsername == null || storedUsername.trim().isEmpty()) {
                System.out.println("  [ERROR] Invalid profile: missing username.");
                return null;
            }
            storedUsername = storedUsername.trim();

            KeystrokeProfile profile = new KeystrokeProfile(storedUsername);

            // Line 2: Summary statistics
            String statsLine = reader.readLine();
            if (statsLine != null && !statsLine.trim().isEmpty()) {
                String[] stats = statsLine.trim().split(",");
                if (stats.length >= 4) {
                    profile.setAverageHoldTime(Double.parseDouble(stats[0]));
                    profile.setAverageFlightTime(Double.parseDouble(stats[1]));
                    profile.setHoldStdDev(Double.parseDouble(stats[2]));
                    profile.setFlightStdDev(Double.parseDouble(stats[3]));
                }
            }

            // Line 3: Hold timings
            String holdLine = reader.readLine();
            if (holdLine != null && !holdLine.trim().isEmpty()) {
                profile.setHoldTimings(csvToArray(holdLine.trim()));
            }

            // Line 4: Flight timings
            String flightLine = reader.readLine();
            if (flightLine != null && !flightLine.trim().isEmpty()) {
                profile.setFlightTimings(csvToArray(flightLine.trim()));
            }

            // Line 5: Custom passphrase (optional, backward-compatible)
            String phraseLine = reader.readLine();
            if (phraseLine != null && !phraseLine.trim().isEmpty()) {
                profile.setPhrase(phraseLine.trim());
            }

            System.out.println("  [✓] Profile loaded for user: " + storedUsername);
            return profile;

        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to load profile: " + e.getMessage());
            return null;
        } catch (NumberFormatException e) {
            System.out.println("  [ERROR] Corrupt profile data: " + e.getMessage());
            return null;
        }
    }

    // ==================== Profile Management ====================

    /**
     * Checks if a profile file exists for the given username.
     *
     * @param username the username to check
     * @return true if a profile exists
     */
    public boolean profileExists(String username) {
        String filePath = SystemConstants.USERS_DIR + File.separator
                + username + SystemConstants.PROFILE_EXTENSION;
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Deletes a user's profile file.
     *
     * @param username the username whose profile to delete
     * @return true if the file was successfully deleted
     */
    public boolean deleteProfile(String username) {
        String filePath = SystemConstants.USERS_DIR + File.separator
                + username + SystemConstants.PROFILE_EXTENSION;
        try {
            boolean deleted = Files.deleteIfExists(Paths.get(filePath));
            if (deleted) {
                System.out.println("  [✓] Profile deleted for user: " + username);
            } else {
                System.out.println("  [!] No profile to delete for: " + username);
            }
            return deleted;
        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to delete profile: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lists all saved user profiles.
     */
    public void listProfiles() {
        File usersDir = new File(SystemConstants.USERS_DIR);
        if (!usersDir.exists()) {
            System.out.println("  [!] No users directory found.");
            return;
        }

        File[] profileFiles = usersDir.listFiles((dir, name) ->
                name.endsWith(SystemConstants.PROFILE_EXTENSION));

        if (profileFiles == null || profileFiles.length == 0) {
            System.out.println("  [!] No saved profiles found.");
            return;
        }

        System.out.println("\n  ──── SAVED PROFILES ────");
        for (File file : profileFiles) {
            String name = file.getName().replace(SystemConstants.PROFILE_EXTENSION, "");
            System.out.println("   • " + name);
        }
        System.out.printf("  Total: %d profiles\n", profileFiles.length);
        System.out.println("  ────────────────────────\n");
    }

    /**
     * Returns the count of enrolled user profiles.
     *
     * @return the number of profile files
     */
    public int getProfileCount() {
        File usersDir = new File(SystemConstants.USERS_DIR);
        if (!usersDir.exists()) return 0;

        File[] profileFiles = usersDir.listFiles((dir, name) ->
                name.endsWith(SystemConstants.PROFILE_EXTENSION));

        return (profileFiles != null) ? profileFiles.length : 0;
    }

    // ==================== Helper Methods ====================

    /**
     * Converts a double array to a comma-separated string.
     *
     * @param array the array to convert
     * @return CSV string representation
     */
    private String arrayToCSV(double[] array) {
        if (array == null || array.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(String.format("%.6f", array[i]));
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Converts a comma-separated string to a double array.
     *
     * @param csv the CSV string to parse
     * @return the parsed double array
     */
    private double[] csvToArray(String csv) {
        if (csv == null || csv.isEmpty()) return new double[0];

        String[] parts = csv.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i].trim());
        }
        return result;
    }

    // ==================== Mouse Profile Persistence ====================

    /**
     * Saves a mouse dynamics profile for a user.
     *
     * @param username the username
     * @param profile  the mouse dynamics profile to save
     */
    public void saveMouseProfile(String username, MouseDynamicsProfile profile) {
        try {
            String filePath = SystemConstants.USERS_DIR + File.separator
                    + username + SystemConstants.MOUSE_PROFILE_EXTENSION;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write(username);
                writer.newLine();
                writer.write(profile.toCSV());
                writer.newLine();
            }
            System.out.println("  [✓] Mouse profile saved for: " + username);
        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to save mouse profile: " + e.getMessage());
        }
    }

    /**
     * Loads a mouse dynamics profile for a user.
     *
     * @param username the username
     * @return the loaded MouseDynamicsProfile, or null if not found
     */
    public MouseDynamicsProfile loadMouseProfile(String username) {
        String filePath = SystemConstants.USERS_DIR + File.separator
                + username + SystemConstants.MOUSE_PROFILE_EXTENSION;

        if (!Files.exists(Paths.get(filePath))) return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine(); // Skip username line
            String csv = reader.readLine();
            if (csv != null && !csv.trim().isEmpty()) {
                return MouseDynamicsProfile.fromCSV(username, csv.trim());
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to load mouse profile: " + e.getMessage());
        }
        return null;
    }
}
