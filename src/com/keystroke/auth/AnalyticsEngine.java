package com.keystroke.auth;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * AnalyticsEngine.java - Advanced analytics for system-wide authentication metrics.
 *
 * Provides:
 *   - System accuracy (overall success/failure rates)
 *   - Weak profile identification (high false-reject users)
 *   - Anomalous pattern detection across authentication history
 *   - Comprehensive security report generation
 *
 * Week 3 - Phase 2 Finalization
 */
public class AnalyticsEngine {

    private final FileManager fileManager;
    private final AuthLogger authLogger;
    private final ProfileAnalyzer profileAnalyzer;

    /**
     * Constructs the analytics engine.
     *
     * @param fileManager the file manager for loading profiles
     * @param authLogger the logger for reading authentication history
     * @param profileAnalyzer the analyzer for profile quality checks
     */
    public AnalyticsEngine(FileManager fileManager, AuthLogger authLogger,
                           ProfileAnalyzer profileAnalyzer) {
        this.fileManager = fileManager;
        this.authLogger = authLogger;
        this.profileAnalyzer = profileAnalyzer;
    }

    /**
     * Calculates overall system authentication accuracy from log files.
     * Reads all daily logs and computes success/failure rates.
     *
     * @return the system accuracy percentage (0-100), or -1 if no logs
     */
    public double calculateSystemAccuracy() {
        Path logsDir = Paths.get(SystemConstants.LOGS_DIR);
        if (!Files.exists(logsDir)) return -1;

        int totalAttempts = 0;
        int totalSuccesses = 0;

        try {
            File dir = logsDir.toFile();
            File[] logFiles = dir.listFiles((d, n) ->
                    n.startsWith(SystemConstants.LOG_FILE_PREFIX)
                            && n.endsWith(SystemConstants.LOG_FILE_EXTENSION));

            if (logFiles == null || logFiles.length == 0) return -1;

            for (File file : logFiles) {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    totalAttempts++;
                    if (line.contains("- SUCCESS -")) totalSuccesses++;
                }
            }
        } catch (IOException e) {
            MenuSystem.printError("Failed to read logs: " + e.getMessage());
            return -1;
        }

        if (totalAttempts == 0) return -1;
        return ((double) totalSuccesses / totalAttempts) * 100.0;
    }

    /**
     * Identifies user profiles with high false-reject rates.
     * Users who frequently fail authentication despite being enrolled are flagged.
     *
     * @return list of weak profile usernames with their failure rates
     */
    public Map<String, Double> identifyWeakProfiles() {
        Map<String, int[]> userStats = getUserStats(); // [successes, failures]
        Map<String, Double> weakProfiles = new LinkedHashMap<>();

        for (Map.Entry<String, int[]> entry : userStats.entrySet()) {
            String username = entry.getKey();
            int[] stats = entry.getValue();
            int total = stats[0] + stats[1];
            if (total >= 2) { // Need at least 2 attempts
                double failRate = ((double) stats[1] / total) * 100.0;
                if (failRate >= 40.0) { // 40%+ failure rate is concerning
                    weakProfiles.put(username, failRate);
                }
            }
        }

        return weakProfiles;
    }

    /**
     * Detects anomalous authentication patterns across all users.
     * Flags patterns like: all-failures, suspicious timing, and session floods.
     *
     * @return list of anomaly descriptions
     */
    public List<String> detectAnomalousPatterns() {
        List<String> anomalies = new ArrayList<>();
        Map<String, int[]> userStats = getUserStats();

        for (Map.Entry<String, int[]> entry : userStats.entrySet()) {
            String username = entry.getKey();
            int[] stats = entry.getValue();
            int total = stats[0] + stats[1];

            // 100% failure rate
            if (total >= 3 && stats[0] == 0) {
                anomalies.add("[HIGH] " + username + ": 100% failure rate over "
                        + total + " attempts — possible compromise or stale profile.");
            }
            // Very high failure rate
            else if (total >= 3 && ((double) stats[1] / total) > 0.7) {
                anomalies.add("[MEDIUM] " + username + ": "
                        + String.format("%.0f%%", (double) stats[1] / total * 100)
                        + " failure rate — profile may need re-enrollment.");
            }
            // Excessive attempts in short time (flood detection)
            if (total >= 10) {
                anomalies.add("[MEDIUM] " + username + ": "
                        + total + " attempts detected — possible brute-force activity.");
            }
        }

        // Check for orphan profiles (no auth attempts)
        File usersDir = new File(SystemConstants.USERS_DIR);
        if (usersDir.exists()) {
            File[] profiles = usersDir.listFiles((d, n) ->
                    n.endsWith(SystemConstants.PROFILE_EXTENSION));
            if (profiles != null) {
                for (File f : profiles) {
                    String name = f.getName().replace(SystemConstants.PROFILE_EXTENSION, "");
                    if (!userStats.containsKey(name)) {
                        anomalies.add("[LOW] " + name
                                + ": Profile enrolled but no authentication attempts on record.");
                    }
                }
            }
        }

        return anomalies;
    }

    /**
     * Generates a comprehensive security report including accuracy, weak profiles,
     * anomalies, and per-user breakdowns.
     */
    public void generateSecurityReport() {
        MenuSystem.printHeader("SYSTEM SECURITY REPORT");

        // 1. Overall accuracy
        double accuracy = calculateSystemAccuracy();
        System.out.println("  ──── SYSTEM ACCURACY ────");
        if (accuracy >= 0) {
            System.out.printf("  Overall Auth Accuracy: %.1f%%\n", accuracy);
            String rating = accuracy >= 90 ? "EXCELLENT" : accuracy >= 70 ? "GOOD"
                    : accuracy >= 50 ? "NEEDS IMPROVEMENT" : "POOR";
            System.out.println("  Rating: " + rating);
        } else {
            System.out.println("  No authentication data available.");
        }
        System.out.println();

        // 2. Profile inventory
        System.out.println("  ──── PROFILE INVENTORY ────");
        int profileCount = fileManager.getProfileCount();
        System.out.printf("  Enrolled Users: %d\n", profileCount);

        // Profile quality assessment
        File usersDir = new File(SystemConstants.USERS_DIR);
        if (usersDir.exists()) {
            File[] profiles = usersDir.listFiles((d, n) ->
                    n.endsWith(SystemConstants.PROFILE_EXTENSION));
            if (profiles != null) {
                int[] widths = {5, 18, 10, 10, 10};
                MenuSystem.printTableRow(
                        new String[]{"#", "Username", "Quality", "Speed", "Rhythm"}, widths);
                MenuSystem.printTableSeparator(widths);

                int num = 1;
                for (File f : profiles) {
                    String name = f.getName().replace(SystemConstants.PROFILE_EXTENSION, "");
                    KeystrokeProfile profile = fileManager.loadUserProfile(name);
                    if (profile != null) {
                        double quality = profileAnalyzer.calculateQualityScore(
                                profile.getHoldTimings(), profile.getFlightTimings());
                        double speed = profileAnalyzer.calculateTypingSpeed(
                                profile.getHoldTimings(), profile.getFlightTimings());
                        double rhythm = profileAnalyzer.calculateRhythmConsistency(
                                profile.getHoldTimings());
                        MenuSystem.printTableRow(
                                new String[]{
                                        String.valueOf(num++), name,
                                        String.format("%.0f", quality),
                                        String.format("%.0f cpm", speed),
                                        String.format("%.0f%%", rhythm)
                                }, widths);
                    }
                }
            }
        }
        System.out.println();

        // 3. Weak profiles
        System.out.println("  ──── WEAK PROFILES ────");
        Map<String, Double> weakProfiles = identifyWeakProfiles();
        if (weakProfiles.isEmpty()) {
            System.out.println("  No weak profiles detected.");
        } else {
            for (Map.Entry<String, Double> entry : weakProfiles.entrySet()) {
                System.out.printf("  ⚠ %s — %.1f%% failure rate (recommend re-enrollment)\n",
                        entry.getKey(), entry.getValue());
            }
        }
        System.out.println();

        // 4. Anomalies
        System.out.println("  ──── ANOMALY DETECTION ────");
        List<String> anomalies = detectAnomalousPatterns();
        if (anomalies.isEmpty()) {
            System.out.println("  No anomalous patterns detected.");
        } else {
            for (String anomaly : anomalies) {
                System.out.println("  " + anomaly);
            }
        }
        System.out.println();

        // 5. Summary
        System.out.println("  ──── RECOMMENDATIONS ────");
        if (accuracy >= 0 && accuracy < 70) {
            System.out.println("  • Consider lowering the authentication threshold.");
        }
        if (!weakProfiles.isEmpty()) {
            System.out.println("  • Re-enroll users with high failure rates.");
        }
        if (profileCount == 0) {
            System.out.println("  • No users enrolled. System is not operational.");
        }
        if (anomalies.stream().anyMatch(a -> a.startsWith("[HIGH]"))) {
            System.out.println("  • Investigate HIGH severity anomalies immediately.");
        }
        if (accuracy >= 90 && weakProfiles.isEmpty() && anomalies.isEmpty()) {
            System.out.println("  ✓ System is healthy. No action required.");
        }

        System.out.println("\n" + SystemConstants.SEPARATOR + "\n");
    }

    // ==================== Internal Helpers ====================

    /**
     * Aggregates per-user success/failure counts from all log files.
     *
     * @return map of username → [successes, failures]
     */
    private Map<String, int[]> getUserStats() {
        Map<String, int[]> stats = new LinkedHashMap<>();
        Path logsDir = Paths.get(SystemConstants.LOGS_DIR);
        if (!Files.exists(logsDir)) return stats;

        try {
            File dir = logsDir.toFile();
            File[] files = dir.listFiles((d, n) ->
                    n.startsWith(SystemConstants.LOG_FILE_PREFIX)
                            && n.endsWith(SystemConstants.LOG_FILE_EXTENSION));
            if (files == null) return stats;

            for (File file : files) {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    // Parse: [timestamp] username - SUCCESS/FAILED - ...
                    int startBracket = line.indexOf("] ");
                    int dash = line.indexOf(" - ", startBracket);
                    if (startBracket >= 0 && dash > startBracket) {
                        String user = line.substring(startBracket + 2, dash).trim();
                        stats.putIfAbsent(user, new int[]{0, 0});
                        if (line.contains("- SUCCESS -")) stats.get(user)[0]++;
                        else if (line.contains("- FAILED -")) stats.get(user)[1]++;
                    }
                }
            }
        } catch (IOException e) {
            MenuSystem.printError("Failed to aggregate user stats: " + e.getMessage());
        }

        return stats;
    }
}
