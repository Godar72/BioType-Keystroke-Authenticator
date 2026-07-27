package com.keystroke.auth;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Admin.java - Administrator user with elevated privileges.
 *
 * Extends User to implement admin-specific functionality:
 *   - View all enrolled users
 *   - Delete user profiles
 *   - View authentication logs
 *   - Adjust system-wide authentication threshold
 *   - Generate daily reports
 *
 * Admin authenticates via password (not keystroke biometrics).
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class Admin extends User {

    /** Admin password for login authentication */
    private String password;

    /**
     * Constructs an Admin with the given username and password.
     *
     * @param username the admin's username
     * @param password the admin's password
     */
    public Admin(String username, String password) {
        super(username, Role.ADMIN);
        this.password = password;
        this.isEnrolled = true; // Admins don't require keystroke enrollment
    }

    /**
     * Default admin constructor using system default credentials.
     */
    public Admin() {
        this(SystemConstants.DEFAULT_ADMIN_USERNAME, SystemConstants.DEFAULT_ADMIN_PASSWORD);
    }

    /**
     * Polymorphic login for Admin - uses password-based authentication.
     * Admin login does not use keystroke biometrics.
     *
     * @return true if login is successful
     */
    @Override
    public boolean login() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║          ADMIN LOGIN                     ║");
        System.out.println("  ╚══════════════════════════════════════════╝");

        System.out.print("  Enter admin username: ");
        String inputUser = scanner.nextLine().trim();

        System.out.print("  Enter admin password: ");
        String inputPass = scanner.nextLine().trim();

        if (inputUser.equals(username) && inputPass.equals(password)) {
            System.out.println("  [✓] Admin login successful. Welcome, " + username + "!\n");
            return true;
        } else {
            System.out.println("  [✗] Invalid admin credentials.\n");
            return false;
        }
    }

    /**
     * Displays the admin dashboard with available admin options.
     */
    @Override
    public void displayDashboard() {
        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║          ADMIN DASHBOARD                 ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf("  ║  Welcome : %-30s ║\n", username);
        System.out.println("  ║  Role    : ADMINISTRATOR                 ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║  1) View All Users                       ║");
        System.out.println("  ║  2) Delete User Profile                  ║");
        System.out.println("  ║  3) View Auth Logs                       ║");
        System.out.println("  ║  4) Adjust Threshold                     ║");
        System.out.println("  ║  5) Generate Daily Report                ║");
        System.out.println("  ║  6) View Threshold Status                ║");
        System.out.println("  ║  7) Logout                               ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    // ==================== Admin Functions ====================

    /**
     * Displays all enrolled user profiles found in the users directory.
     */
    public void viewAllUsers() {
        System.out.println("\n  ──── ALL ENROLLED USERS ────");

        File usersDir = new File(SystemConstants.USERS_DIR);
        if (!usersDir.exists() || !usersDir.isDirectory()) {
            System.out.println("  [!] No users directory found.");
            System.out.println("  ─────────────────────────────\n");
            return;
        }

        File[] profileFiles = usersDir.listFiles((dir, name) ->
                name.endsWith(SystemConstants.PROFILE_EXTENSION));

        if (profileFiles == null || profileFiles.length == 0) {
            System.out.println("  [!] No enrolled users found.");
        } else {
            System.out.printf("  %-5s %-20s %-15s\n", "#", "Username", "File Size");
            System.out.println("  ──── ──────────────────── ───────────────");
            int count = 1;
            for (File file : profileFiles) {
                String name = file.getName().replace(SystemConstants.PROFILE_EXTENSION, "");
                long sizeBytes = file.length();
                System.out.printf("  %-5d %-20s %d bytes\n", count++, name, sizeBytes);
            }
            System.out.printf("\n  Total users: %d\n", profileFiles.length);
        }

        System.out.println("  ─────────────────────────────\n");
    }

    /**
     * Deletes a user's profile by username.
     *
     * @param username the username whose profile to delete
     * @param fileManager the FileManager instance to use for deletion
     * @return true if the profile was successfully deleted
     */
    public boolean deleteUser(String username, FileManager fileManager) {
        System.out.println("\n  Attempting to delete profile for: " + username);

        if (!fileManager.profileExists(username)) {
            System.out.println("  [!] No profile found for user: " + username);
            return false;
        }

        boolean deleted = fileManager.deleteProfile(username);
        if (deleted) {
            System.out.println("  [✓] User '" + username + "' has been removed from the system.");
        }
        return deleted;
    }

    /**
     * Views authentication logs using the provided logger.
     *
     * @param logger the AuthLogger instance
     */
    public void viewAuthLogs(AuthLogger logger) {
        logger.viewTodaysLogs();
    }

    /**
     * Adjusts the system-wide authentication threshold.
     *
     * @param thresholdManager the ThresholdManager instance
     * @param newThreshold     the desired threshold value (will be clamped to valid range)
     */
    public void adjustSystemThreshold(ThresholdManager thresholdManager, double newThreshold) {
        System.out.printf("\n  Admin '%s' adjusting system threshold...\n", username);
        thresholdManager.setThreshold(newThreshold);
        thresholdManager.displayStatus();
    }

    /**
     * Generates a daily authentication report.
     *
     * @param logger the AuthLogger instance
     */
    public void generateReport(AuthLogger logger) {
        System.out.printf("\n  Admin '%s' generating daily report...\n", username);
        logger.generateDailyReport();
    }

    /**
     * Gets the admin password (for internal use only).
     *
     * @return the admin password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets a new admin password.
     *
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
