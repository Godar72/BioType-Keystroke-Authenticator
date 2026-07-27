package com.keystroke.auth;

import java.util.Random;

/**
 * DemoScript.java - Automated demo script showcasing all system features.
 *
 * Demonstrates:
 *   1. System startup and configuration
 *   2. Admin creating users (via DemoDataGenerator)
 *   3. User enrollment with timing display
 *   4. Successful authentication with confidence scores
 *   5. Failed impostor attempts with detection alerts
 *   6. Admin viewing analytics and adjusting thresholds
 *   7. Profile analysis and quality scoring
 *   8. Backup creation
 *
 * Week 3 - Phase 2 Finalization
 */
public class DemoScript {

    private final FileManager fileManager;
    private final AuthenticationEngine authEngine;
    private final ProfileAnalyzer profileAnalyzer;
    private final AnalyticsEngine analyticsEngine;
    private final DemoDataGenerator dataGenerator;
    private final BackupManager backupManager;
    private final Random random;

    /**
     * Constructs the demo script with all required components.
     *
     * @param fileManager the file manager
     */
    public DemoScript(FileManager fileManager) {
        this.fileManager = fileManager;
        this.authEngine = new AuthenticationEngine(fileManager);
        this.profileAnalyzer = new ProfileAnalyzer();
        this.analyticsEngine = new AnalyticsEngine(fileManager,
                authEngine.getAuthLogger(), profileAnalyzer);
        this.dataGenerator = new DemoDataGenerator(fileManager);
        this.backupManager = new BackupManager();
        this.random = new Random(42);
    }

    /**
     * Runs the complete automated demo.
     */
    public void runDemo() {
        MenuSystem.printHeader("AUTOMATED DEMO SCRIPT");
        System.out.println("  This demo showcases all major features of the");
        System.out.println("  BioType Keystroke Authentication System.\n");
        pause(500);

        // Step 1: Generate demo data
        step1_GenerateDemoData();

        // Step 2: Genuine user authentication
        step2_GenuineAuthentication();

        // Step 3: Impostor attempt
        step3_ImpostorAttempt();

        // Step 4: Profile analysis
        step4_ProfileAnalysis();

        // Step 5: Threshold management
        step5_ThresholdManagement();

        // Step 6: Analytics report
        step6_AnalyticsReport();

        // Step 7: Backup creation
        step7_SystemBackup();

        // Summary
        printDemoSummary();
    }

    // ==================== Demo Steps ====================

    private void step1_GenerateDemoData() {
        printStep(1, "GENERATING DEMO DATA");

        dataGenerator.createSampleUsers();
        System.out.println();
        dataGenerator.populateAuthLogs();

        MenuSystem.printSuccess("Demo data generation complete.\n");
        pause(300);
    }

    private void step2_GenuineAuthentication() {
        printStep(2, "GENUINE USER AUTHENTICATION");

        System.out.println("  Scenario: 'jane_smith' (fast typist) authenticates");
        System.out.println("  with typing patterns similar to her enrolled profile.\n");

        // Load the enrolled profile to create similar test data
        KeystrokeProfile enrolled = fileManager.loadUserProfile("jane_smith");
        if (enrolled == null) {
            MenuSystem.printError("Demo profile not found for jane_smith.");
            return;
        }

        // Generate test data close to enrolled profile (genuine user)
        double[] genuineHold = addSmallNoise(enrolled.getHoldTimings(), 3.0);
        double[] genuineFlight = addSmallNoise(enrolled.getFlightTimings(), 4.0);

        System.out.println("  Typing sample captured. Computing similarity...\n");

        try {
            AuthResult result = authEngine.authenticate("jane_smith",
                    genuineHold, genuineFlight);
            result.displayResult();
        } catch (AuthenticationException e) {
            System.out.println("  Auth Exception: " + e.getMessage());
        }

        pause(300);
    }

    private void step3_ImpostorAttempt() {
        printStep(3, "IMPOSTOR ATTEMPT DETECTION");

        System.out.println("  Scenario: An impostor tries to authenticate as");
        System.out.println("  'john_doe' with very different typing patterns.\n");

        // Create impostor timings (very different from john_doe)
        double[] impostorHold = generateTimings(43, 250.0, 60.0);
        double[] impostorFlight = generateTimings(42, 350.0, 80.0);

        System.out.println("  Impostor typing sample captured. Analyzing...\n");

        try {
            AuthResult result = authEngine.authenticate("john_doe",
                    impostorHold, impostorFlight);
            result.displayResult();

            if (!result.isAuthenticated()) {
                MenuSystem.printSuccess("Impostor correctly rejected!\n");
            }
        } catch (AuthenticationException e) {
            System.out.println("  Auth Exception: " + e.getMessage());
        }

        pause(300);
    }

    private void step4_ProfileAnalysis() {
        printStep(4, "PROFILE QUALITY ANALYSIS");

        String[] demoUsers = {"typing_expert", "bob_wilson"};
        for (String username : demoUsers) {
            System.out.println("  Analyzing profile: " + username);
            KeystrokeProfile profile = fileManager.loadUserProfile(username);
            if (profile != null) {
                profileAnalyzer.analyzeProfile(profile);
            }
        }

        pause(300);
    }

    private void step5_ThresholdManagement() {
        printStep(5, "ADAPTIVE THRESHOLD MANAGEMENT");

        ThresholdManager tm = authEngine.getThresholdManager();
        tm.displayStatus();

        System.out.println("  Admin adjusts threshold to 65%...");
        tm.setThreshold(65.0);
        tm.displayStatus();

        System.out.println("  Resetting to default...");
        tm.resetToDefault();

        pause(300);
    }

    private void step6_AnalyticsReport() {
        printStep(6, "SECURITY ANALYTICS REPORT");
        analyticsEngine.generateSecurityReport();
        pause(300);
    }

    private void step7_SystemBackup() {
        printStep(7, "SYSTEM BACKUP");
        String backupPath = backupManager.createSystemBackup();
        if (backupPath != null) {
            System.out.println("  Backup saved to: " + backupPath);
        }
        System.out.println();
        backupManager.listBackups();
        pause(300);
    }

    // ==================== Display Helpers ====================

    private void printStep(int stepNum, String title) {
        System.out.println("\n  " + MenuSystem.BOLD + MenuSystem.CYAN
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                + MenuSystem.RESET);
        System.out.println("  " + MenuSystem.BOLD + "  STEP " + stepNum + ": " + title
                + MenuSystem.RESET);
        System.out.println("  " + MenuSystem.BOLD + MenuSystem.CYAN
                + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                + MenuSystem.RESET + "\n");
    }

    private void printDemoSummary() {
        MenuSystem.printHeader("DEMO COMPLETE");
        System.out.println("  Features demonstrated:");
        System.out.println("    ✓ User enrollment with demo data");
        System.out.println("    ✓ Genuine user authentication (high similarity)");
        System.out.println("    ✓ Impostor detection (low similarity, rejection)");
        System.out.println("    ✓ Profile quality analysis");
        System.out.println("    ✓ Adaptive threshold management");
        System.out.println("    ✓ Security analytics and reporting");
        System.out.println("    ✓ System backup creation");
        System.out.println();
        System.out.println("  System version: " + SystemConstants.SYSTEM_VERSION);
        System.out.println("  Build date: " + SystemConstants.BUILD_DATE);
        System.out.println();
    }

    // ==================== Utility ====================

    private double[] generateTimings(int length, double mean, double stdDev) {
        double[] t = new double[length];
        for (int i = 0; i < length; i++) {
            t[i] = Math.max(10.0, mean + random.nextGaussian() * stdDev);
        }
        return t;
    }

    private double[] addSmallNoise(double[] original, double noiseLevel) {
        if (original == null) return new double[0];
        double[] noisy = new double[original.length];
        for (int i = 0; i < original.length; i++) {
            noisy[i] = Math.max(10.0, original[i] + random.nextGaussian() * noiseLevel);
        }
        return noisy;
    }

    private void pause(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
