package com.keystroke.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * KeystrokeAuthSystem.java - Enhanced main class for the BioType keystroke authentication system.
 *
 * Phase 2 Production Release — Fully integrated with all Week 3 components:
 *   - Role-based login: Admin (password) vs User (biometric)
 *   - Admin menu: user management, logs, threshold, reports, analytics, backup
 *   - User menu: authenticate, re-enroll, history, profile analysis
 *   - Demo mode: automated feature showcase with synthetic data
 *   - Test mode: comprehensive test suite and performance benchmarks
 *   - Help system with ANSI-colored output
 *   - Configuration management via ConfigManager
 *   - Backup/restore via BackupManager
 *
 * @author Keystroke Biometrics Team
 * @version 2.0.0
 */
public class KeystrokeAuthSystem {

    // Core system components
    private final KeystrokeCapture capture;
    private final FileManager fileManager;
    private final AuthenticationEngine authEngine;
    private final ProfileAnalyzer profileAnalyzer;
    private final ConfigManager configManager;
    private final BackupManager backupManager;
    private final Scanner scanner;

    // Admin user
    private final Admin admin;

    /**
     * Constructor initializes all core components.
     */
    public KeystrokeAuthSystem() {
        this.fileManager = new FileManager();
        this.capture = new KeystrokeCapture();
        this.authEngine = new AuthenticationEngine(fileManager);
        this.profileAnalyzer = new ProfileAnalyzer();
        this.configManager = new ConfigManager();
        this.backupManager = new BackupManager();
        this.scanner = new Scanner(System.in);
        this.admin = new Admin();
    }

    /**
     * Main entry point for the application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        KeystrokeAuthSystem system = new KeystrokeAuthSystem();
        system.run();
    }

    /**
     * Runs the main application loop.
     */
    public void run() {
        MenuSystem.printSplash();

        // Display system status
        int profileCount = fileManager.getProfileCount();
        double threshold = authEngine.getThresholdManager().getCurrentThreshold();
        System.out.printf("  System Status: %d enrolled users | Threshold: %.1f%% | Scorer: %s\n\n",
                profileCount, threshold, authEngine.getScorer().getAlgorithmName());

        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print("  Enter your choice: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    enrollUser();
                    break;
                case "2":
                    userLogin();
                    break;
                case "3":
                    adminLogin();
                    break;
                case "4":
                    viewProfiles();
                    break;
                case "5":
                    runDemoMode();
                    break;
                case "6":
                    runTestMode();
                    break;
                case "7":
                    MenuSystem.printHelp();
                    break;
                case "8":
                    running = false;
                    shutdown();
                    break;
                default:
                    MenuSystem.printWarning("Invalid option. Please enter 1-8.\n");
            }
        }

        scanner.close();
    }

    // ═══════════════════════════════════════════════════════════════
    //                        DISPLAY METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Prints the main menu.
     */
    private void printMainMenu() {
        System.out.println("  ┌──────────────────────────────────────────┐");
        System.out.println("  │              MAIN MENU                   │");
        System.out.println("  ├──────────────────────────────────────────┤");
        System.out.println("  │  1) Enroll New User                     │");
        System.out.println("  │  2) User Login (Biometric Auth)         │");
        System.out.println("  │  3) Admin Login                         │");
        System.out.println("  │  4) View Saved Profiles                 │");
        System.out.println("  │  5) Run Demo                            │");
        System.out.println("  │  6) Run Tests                           │");
        System.out.println("  │  7) Help                                │");
        System.out.println("  │  8) Exit                                │");
        System.out.println("  └──────────────────────────────────────────┘");
    }

    /**
     * Performs graceful shutdown with resource cleanup.
     */
    private void shutdown() {
        System.out.println("\n  Saving configuration...");
        configManager.saveConfig();
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║  Thank you for using BioType v"
                + SystemConstants.SYSTEM_VERSION + "!       ║");
        System.out.println("  ║  Stay secure. Goodbye.                   ║");
        System.out.println("  ╚══════════════════════════════════════════╝\n");
    }

    // ═══════════════════════════════════════════════════════════════
    //                     ENROLLMENT WORKFLOW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Handles the user enrollment process.
     */
    private void enrollUser() {
        MenuSystem.printHeader("USER ENROLLMENT");

        System.out.print("  Enter username: ");
        String username = scanner.nextLine().trim();

        String error = InputValidator.getUsernameError(username);
        if (error != null) {
            MenuSystem.printWarning(error);
            return;
        }

        if (fileManager.profileExists(username)) {
            System.out.print("  [!] Profile already exists for '" + username + "'. Overwrite? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if (!confirm.equals("y") && !confirm.equals("yes")) {
                MenuSystem.printInfo("Enrollment cancelled.\n");
                return;
            }
        }

        RegularUser user = new RegularUser(username);
        user.enroll();

        List<double[]> holdSamples = new ArrayList<>();
        List<double[]> flightSamples = new ArrayList<>();
        int sampleCount = SystemConstants.ENROLLMENT_SAMPLES;

        for (int sample = 1; sample <= sampleCount; sample++) {
            MenuSystem.printProgressBar(sample - 1, sampleCount, "Enrollment");
            System.out.println("\n  SAMPLE " + sample + " of " + sampleCount);

            double[][] timingData = capture.captureKeystrokeTiming(
                    SystemConstants.STANDARD_PHRASE, scanner);

            if (timingData != null) {
                holdSamples.add(timingData[0]);
                flightSamples.add(timingData[1]);
                MenuSystem.printSuccess("Sample " + sample + " captured.");
            } else {
                MenuSystem.printWarning("Sample failed. Retrying...");
                sample--;
            }

            if (sample < sampleCount) {
                System.out.println("\n  Press ENTER when ready for next sample...");
                scanner.nextLine();
            }
        }

        MenuSystem.printProgressBar(sampleCount, sampleCount, "Enrollment");

        KeystrokeProfile profile = new KeystrokeProfile(username);
        profile.buildProfile(holdSamples, flightSamples);

        String filename = username + SystemConstants.PROFILE_EXTENSION;
        fileManager.saveUserProfile(profile, filename);

        user.setEnrolled(true);
        user.displayDashboard();

        System.out.println("  Running profile quality analysis...");
        profileAnalyzer.analyzeProfile(profile);

        MenuSystem.printSuccess("Enrollment complete for '" + username + "'.\n");
    }

    // ═══════════════════════════════════════════════════════════════
    //                    USER LOGIN WORKFLOW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Handles the user login workflow.
     */
    private void userLogin() {
        MenuSystem.printHeader("USER LOGIN");

        System.out.print("  Enter username: ");
        String username = scanner.nextLine().trim();

        String error = InputValidator.getUsernameError(username);
        if (error != null) {
            MenuSystem.printWarning(error);
            return;
        }

        if (!fileManager.profileExists(username)) {
            MenuSystem.printWarning("No profile found for '" + username + "'.");
            MenuSystem.printInfo("Please enroll first (Main Menu Option 1).\n");
            return;
        }

        RegularUser user = new RegularUser(username);
        user.setEnrolled(true);

        if (!user.login()) return;

        System.out.println("  Type the standard phrase for biometric verification:");
        double[][] timingData = capture.captureKeystrokeTiming(
                SystemConstants.STANDARD_PHRASE, scanner);

        if (timingData == null) {
            MenuSystem.printWarning("Failed to capture timing data.\n");
            return;
        }

        try {
            AuthResult result = authEngine.authenticate(username, timingData[0], timingData[1]);
            result.displayResult();

            if (result.isAuthenticated()) {
                runUserMenu(user);
            } else {
                MenuSystem.printError("Access denied. Returning to main menu.\n");
            }
        } catch (AuthenticationException e) {
            MenuSystem.printError(e.getErrorCode().getDescription() + ": " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Runs the user menu loop after successful authentication.
     *
     * @param user the authenticated RegularUser
     */
    private void runUserMenu(RegularUser user) {
        boolean inUserMenu = true;

        while (inUserMenu) {
            user.displayDashboard();
            System.out.print("  Enter your choice: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    performReAuthentication(user.getUsername());
                    break;
                case "2":
                    reEnrollUser(user);
                    break;
                case "3":
                    user.viewMyAuthHistory(authEngine.getAuthLogger());
                    break;
                case "4":
                    user.viewMyProfileAnalysis(fileManager, profileAnalyzer);
                    break;
                case "5":
                    MenuSystem.printSuccess("Logged out successfully.\n");
                    inUserMenu = false;
                    break;
                default:
                    MenuSystem.printWarning("Invalid option. Please enter 1-5.\n");
            }
        }
    }

    /**
     * Performs re-authentication for a logged-in user.
     */
    private void performReAuthentication(String username) {
        MenuSystem.printHeader("RE-AUTHENTICATION");
        System.out.println("  Type the standard phrase:");

        double[][] timingData = capture.captureKeystrokeTiming(
                SystemConstants.STANDARD_PHRASE, scanner);

        if (timingData == null) {
            MenuSystem.printWarning("Failed to capture timing data.\n");
            return;
        }

        try {
            AuthResult result = authEngine.authenticate(username, timingData[0], timingData[1]);
            result.displayResult();
        } catch (AuthenticationException e) {
            MenuSystem.printError(e.getErrorCode().getDescription() + ": " + e.getMessage());
        }
    }

    /**
     * Re-enrolls a user by capturing new samples.
     */
    private void reEnrollUser(RegularUser user) {
        MenuSystem.printHeader("RE-ENROLLMENT");
        System.out.print("  This will replace your existing profile. Continue? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("y") && !confirm.equals("yes")) {
            MenuSystem.printInfo("Re-enrollment cancelled.\n");
            return;
        }

        user.enroll();

        List<double[]> holdSamples = new ArrayList<>();
        List<double[]> flightSamples = new ArrayList<>();
        int sampleCount = SystemConstants.ENROLLMENT_SAMPLES;

        for (int sample = 1; sample <= sampleCount; sample++) {
            MenuSystem.printProgressBar(sample - 1, sampleCount, "Re-enroll");
            System.out.println("\n  SAMPLE " + sample + " of " + sampleCount);

            double[][] timingData = capture.captureKeystrokeTiming(
                    SystemConstants.STANDARD_PHRASE, scanner);

            if (timingData != null) {
                holdSamples.add(timingData[0]);
                flightSamples.add(timingData[1]);
                MenuSystem.printSuccess("Sample " + sample + " captured.");
            } else {
                MenuSystem.printWarning("Sample failed. Retrying...");
                sample--;
            }

            if (sample < sampleCount) {
                System.out.println("\n  Press ENTER for next sample...");
                scanner.nextLine();
            }
        }

        MenuSystem.printProgressBar(sampleCount, sampleCount, "Re-enroll");

        KeystrokeProfile newProfile = new KeystrokeProfile(user.getUsername());
        newProfile.buildProfile(holdSamples, flightSamples);
        user.updateProfile(fileManager, newProfile);

        MenuSystem.printSuccess("Re-enrollment complete.\n");
        profileAnalyzer.analyzeProfile(newProfile);
    }

    // ═══════════════════════════════════════════════════════════════
    //                    ADMIN LOGIN WORKFLOW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Handles admin login and admin menu.
     */
    private void adminLogin() {
        MenuSystem.printHeader("ADMIN LOGIN");

        System.out.print("  Enter admin username: ");
        String inputUser = scanner.nextLine().trim();

        System.out.print("  Enter admin password: ");
        String inputPass = scanner.nextLine().trim();

        if (!inputUser.equals(admin.getUsername()) || !inputPass.equals(admin.getPassword())) {
            MenuSystem.printError("Invalid admin credentials.\n");
            return;
        }

        MenuSystem.printSuccess("Admin login successful. Welcome, " + admin.getUsername() + "!\n");
        runAdminMenu();
    }

    /**
     * Runs the admin menu loop with extended options.
     */
    private void runAdminMenu() {
        boolean inAdminMenu = true;

        while (inAdminMenu) {
            System.out.println("\n  ╔══════════════════════════════════════════╗");
            System.out.println("  ║          ADMIN DASHBOARD                 ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.printf("  ║  Welcome : %-30s ║\n", admin.getUsername());
            System.out.println("  ║  Role    : ADMINISTRATOR                 ║");
            System.out.println("  ╠══════════════════════════════════════════╣");
            System.out.println("  ║  1) View All Users                       ║");
            System.out.println("  ║  2) Delete User Profile                  ║");
            System.out.println("  ║  3) View Auth Logs                       ║");
            System.out.println("  ║  4) Adjust Threshold                     ║");
            System.out.println("  ║  5) Generate Daily Report                ║");
            System.out.println("  ║  6) View Threshold Status                ║");
            System.out.println("  ║  7) Security Analytics Report            ║");
            System.out.println("  ║  8) System Configuration                 ║");
            System.out.println("  ║  9) Create Backup                        ║");
            System.out.println("  ║  10) Restore Backup                      ║");
            System.out.println("  ║  11) Logout                              ║");
            System.out.println("  ╚══════════════════════════════════════════╝");

            System.out.print("  Enter your choice: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    admin.viewAllUsers();
                    break;
                case "2":
                    handleDeleteUser();
                    break;
                case "3":
                    admin.viewAuthLogs(authEngine.getAuthLogger());
                    break;
                case "4":
                    handleAdjustThreshold();
                    break;
                case "5":
                    admin.generateReport(authEngine.getAuthLogger());
                    break;
                case "6":
                    authEngine.getThresholdManager().displayStatus();
                    break;
                case "7":
                    AnalyticsEngine analytics = new AnalyticsEngine(
                            fileManager, authEngine.getAuthLogger(), profileAnalyzer);
                    analytics.generateSecurityReport();
                    break;
                case "8":
                    handleConfigMenu();
                    break;
                case "9":
                    backupManager.createSystemBackup();
                    break;
                case "10":
                    handleRestoreBackup();
                    break;
                case "11":
                    MenuSystem.printSuccess("Admin logged out.\n");
                    inAdminMenu = false;
                    break;
                default:
                    MenuSystem.printWarning("Invalid option. Please enter 1-11.\n");
            }
        }
    }

    /**
     * Handles admin user deletion with confirmation.
     */
    private void handleDeleteUser() {
        System.out.print("\n  Enter username to delete: ");
        String deleteUser = scanner.nextLine().trim();

        if (deleteUser.isEmpty()) {
            MenuSystem.printWarning("Username cannot be empty.\n");
            return;
        }

        if (!fileManager.profileExists(deleteUser)) {
            MenuSystem.printWarning("No profile found for '" + deleteUser + "'.\n");
            return;
        }

        System.out.print("  " + MenuSystem.YELLOW + "Are you sure you want to delete '"
                + deleteUser + "'? This cannot be undone. (y/n): " + MenuSystem.RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            admin.deleteUser(deleteUser, fileManager);
        } else {
            MenuSystem.printInfo("Deletion cancelled.\n");
        }
    }

    /**
     * Handles admin threshold adjustment with validation.
     */
    private void handleAdjustThreshold() {
        authEngine.getThresholdManager().displayStatus();

        System.out.printf("  Enter new threshold (%.0f-%.0f) or 'reset' for default: ",
                SystemConstants.MIN_THRESHOLD, SystemConstants.MAX_THRESHOLD);
        String input = scanner.nextLine().trim();

        if (input.equalsIgnoreCase("reset")) {
            authEngine.getThresholdManager().resetToDefault();
        } else {
            try {
                double newThreshold = Double.parseDouble(input);
                if (InputValidator.validateThreshold(newThreshold)) {
                    admin.adjustSystemThreshold(authEngine.getThresholdManager(), newThreshold);
                } else {
                    MenuSystem.printWarning(String.format(
                            "Threshold must be between %.0f and %.0f.\n",
                            SystemConstants.MIN_THRESHOLD, SystemConstants.MAX_THRESHOLD));
                }
            } catch (NumberFormatException e) {
                MenuSystem.printWarning("Invalid number.\n");
            }
        }
    }

    /**
     * Handles configuration viewing and modification.
     */
    private void handleConfigMenu() {
        configManager.displayConfig();
        System.out.print("  Reset to defaults? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("y") || confirm.equals("yes")) {
            configManager.resetToDefaults();
        }
    }

    /**
     * Handles backup restoration with confirmation.
     */
    private void handleRestoreBackup() {
        backupManager.listBackups();
        System.out.print("  Enter backup filename to restore (or ENTER to cancel): ");
        String filename = scanner.nextLine().trim();

        if (filename.isEmpty()) {
            MenuSystem.printInfo("Restore cancelled.\n");
            return;
        }

        String fullPath = SystemConstants.BACKUP_DIR + "/" + filename;
        System.out.print("  " + MenuSystem.RED + "WARNING: This will overwrite current data. Proceed? (y/n): "
                + MenuSystem.RESET);
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            backupManager.restoreFromBackup(fullPath);
        } else {
            MenuSystem.printInfo("Restore cancelled.\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //                    DEMO & TEST MODES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Runs the automated demo showcasing all features.
     */
    private void runDemoMode() {
        System.out.print("\n  Run automated demo? This will create sample data. (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y") || confirm.equals("yes")) {
            DemoScript demo = new DemoScript(fileManager);
            demo.runDemo();
        } else {
            MenuSystem.printInfo("Demo cancelled.\n");
        }
    }

    /**
     * Runs the test suite and performance benchmarks.
     */
    private void runTestMode() {
        System.out.println("\n  ┌──────────────────────────────────────────┐");
        System.out.println("  │          TEST OPTIONS                    │");
        System.out.println("  ├──────────────────────────────────────────┤");
        System.out.println("  │  1) Run Unit Tests                      │");
        System.out.println("  │  2) Run Performance Benchmarks          │");
        System.out.println("  │  3) Run All Tests                       │");
        System.out.println("  │  4) Back                                │");
        System.out.println("  └──────────────────────────────────────────┘");

        System.out.print("  Enter your choice: ");
        String input = scanner.nextLine().trim();

        switch (input) {
            case "1":
                new TestSuite().runAllTests();
                break;
            case "2":
                new PerformanceTester(fileManager).runAllBenchmarks();
                break;
            case "3":
                new TestSuite().runAllTests();
                System.out.println();
                new PerformanceTester(fileManager).runAllBenchmarks();
                break;
            case "4":
                break;
            default:
                MenuSystem.printWarning("Invalid option.\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //                    PROFILE VIEWING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Displays saved profiles with optional detailed analysis.
     */
    private void viewProfiles() {
        MenuSystem.printHeader("SAVED PROFILES");
        fileManager.listProfiles();

        System.out.print("  Enter username to view details (or ENTER to skip): ");
        String username = scanner.nextLine().trim();

        if (!username.isEmpty()) {
            KeystrokeProfile profile = fileManager.loadUserProfile(username);
            if (profile != null) {
                profile.displayProfile();

                System.out.print("  Run profile quality analysis? (y/n): ");
                String analyze = scanner.nextLine().trim().toLowerCase();
                if (analyze.equals("y") || analyze.equals("yes")) {
                    profileAnalyzer.analyzeProfile(profile);
                }
            }
        }
    }
}
