package com.keystroke.auth;

import java.util.Random;

/**
 * PerformanceTester.java - Benchmarks system performance under various conditions.
 *
 * Measures:
 *   - Authentication speed (time per attempt)
 *   - Stress testing with 50+ concurrent profile loads
 *   - Memory usage during large profile sets
 *   - Generates a performance report with statistics
 *
 * Week 3 - Phase 2 Finalization
 */
public class PerformanceTester {

    private final FileManager fileManager;
    private final Random random;

    /**
     * Constructs a PerformanceTester.
     *
     * @param fileManager the FileManager for profile operations
     */
    public PerformanceTester(FileManager fileManager) {
        this.fileManager = fileManager;
        this.random = new Random(99);
    }

    /**
     * Runs the complete performance benchmark suite.
     */
    public void runAllBenchmarks() {
        MenuSystem.printHeader("PERFORMANCE BENCHMARKS");

        long totalStart = System.currentTimeMillis();

        double authSpeed = measureAuthenticationSpeed();
        double stressResult = stressTestMultipleUsers();
        long[] memoryStats = memoryUsageTest();

        long totalElapsed = System.currentTimeMillis() - totalStart;

        generatePerformanceReport(authSpeed, stressResult, memoryStats, totalElapsed);
    }

    /**
     * Measures the average time for a single authentication attempt.
     * Creates a temporary profile, runs 20 authentication attempts, and averages.
     *
     * @return average authentication time in milliseconds
     */
    public double measureAuthenticationSpeed() {
        System.out.println("\n  ──── AUTH SPEED BENCHMARK ────");

        // Create a test profile
        String testUser = "_perf_speed_test_";
        double[] holdTimings = generateRandomTimings(43, 100.0, 20.0);
        double[] flightTimings = generateRandomTimings(42, 120.0, 25.0);

        KeystrokeProfile profile = createTestProfile(testUser, holdTimings, flightTimings);
        fileManager.saveUserProfile(profile,
                testUser + SystemConstants.PROFILE_EXTENSION);

        AuthenticationEngine engine = new AuthenticationEngine(fileManager);
        int iterations = 20;
        long totalTime = 0;
        int successCount = 0;

        for (int i = 0; i < iterations; i++) {
            // Generate slightly varied timings for each attempt
            double[] testHold = addNoise(holdTimings, 5.0);
            double[] testFlight = addNoise(flightTimings, 5.0);

            long start = System.nanoTime();
            try {
                AuthResult result = engine.authenticate(testUser, testHold, testFlight);
                if (result.isAuthenticated()) successCount++;
            } catch (AuthenticationException e) {
                // Count as completed attempt
            }
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        double avgMs = (totalTime / 1_000_000.0) / iterations;

        System.out.printf("  Iterations       : %d\n", iterations);
        System.out.printf("  Avg Auth Time    : %.2f ms\n", avgMs);
        System.out.printf("  Total Time       : %.2f ms\n", totalTime / 1_000_000.0);
        System.out.printf("  Success Rate     : %.1f%%\n",
                (double) successCount / iterations * 100.0);

        // Cleanup
        fileManager.deleteProfile(testUser);

        return avgMs;
    }

    /**
     * Stress tests by creating and loading 50+ user profiles.
     *
     * @return average load time per profile in milliseconds
     */
    public double stressTestMultipleUsers() {
        System.out.println("\n  ──── STRESS TEST (50 USERS) ────");

        int userCount = 50;
        String prefix = "_perf_stress_";

        // Phase 1: Create 50 profiles
        long createStart = System.currentTimeMillis();
        for (int i = 0; i < userCount; i++) {
            String username = prefix + String.format("%03d", i);
            double[] hold = generateRandomTimings(43, 80.0 + i, 15.0);
            double[] flight = generateRandomTimings(42, 100.0 + i, 20.0);
            KeystrokeProfile profile = createTestProfile(username, hold, flight);
            fileManager.saveUserProfile(profile,
                    username + SystemConstants.PROFILE_EXTENSION);
        }
        long createTime = System.currentTimeMillis() - createStart;

        MenuSystem.printProgressBar(1, 3, "Stress");

        // Phase 2: Load all 50 profiles
        long loadStart = System.currentTimeMillis();
        for (int i = 0; i < userCount; i++) {
            String username = prefix + String.format("%03d", i);
            fileManager.loadUserProfile(username);
        }
        long loadTime = System.currentTimeMillis() - loadStart;

        MenuSystem.printProgressBar(2, 3, "Stress");

        // Phase 3: Run auth on all 50
        AuthenticationEngine engine = new AuthenticationEngine(fileManager);
        int authSuccessCount = 0;
        long authStart = System.currentTimeMillis();
        for (int i = 0; i < userCount; i++) {
            String username = prefix + String.format("%03d", i);
            double[] testHold = generateRandomTimings(43, 80.0 + i, 15.0);
            double[] testFlight = generateRandomTimings(42, 100.0 + i, 20.0);
            try {
                AuthResult result = engine.authenticate(username, testHold, testFlight);
                if (result.isAuthenticated()) authSuccessCount++;
            } catch (AuthenticationException e) {
                // Expected in some cases
            }
        }
        long authTime = System.currentTimeMillis() - authStart;

        MenuSystem.printProgressBar(3, 3, "Stress");

        // Cleanup
        for (int i = 0; i < userCount; i++) {
            String username = prefix + String.format("%03d", i);
            fileManager.deleteProfile(username);
        }

        double avgLoadTime = (double) loadTime / userCount;

        System.out.printf("\n  Users Created    : %d\n", userCount);
        System.out.printf("  Create Time      : %d ms (%.1f ms/user)\n",
                createTime, (double) createTime / userCount);
        System.out.printf("  Load Time        : %d ms (%.1f ms/user)\n",
                loadTime, avgLoadTime);
        System.out.printf("  Auth Time        : %d ms (%.1f ms/user)\n",
                authTime, (double) authTime / userCount);
        System.out.printf("  Auth Success     : %d/%d (%.1f%%)\n",
                authSuccessCount, userCount,
                (double) authSuccessCount / userCount * 100.0);

        return avgLoadTime;
    }

    /**
     * Monitors memory usage during profile loading.
     *
     * @return array of [usedBefore, usedAfter, delta] in bytes
     */
    public long[] memoryUsageTest() {
        System.out.println("\n  ──── MEMORY USAGE TEST ────");

        Runtime runtime = Runtime.getRuntime();

        // Force GC before measurement
        runtime.gc();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        long beforeTotal = runtime.totalMemory();
        long beforeFree = runtime.freeMemory();
        long beforeUsed = beforeTotal - beforeFree;

        // Load many profiles into memory
        KeystrokeProfile[] profiles = new KeystrokeProfile[100];
        for (int i = 0; i < 100; i++) {
            double[] hold = generateRandomTimings(43, 100.0, 20.0);
            double[] flight = generateRandomTimings(42, 120.0, 25.0);
            profiles[i] = createTestProfile("mem_test_" + i, hold, flight);
        }

        long afterTotal = runtime.totalMemory();
        long afterFree = runtime.freeMemory();
        long afterUsed = afterTotal - afterFree;

        long delta = afterUsed - beforeUsed;

        System.out.printf("  Max Memory       : %.1f MB\n",
                runtime.maxMemory() / (1024.0 * 1024.0));
        System.out.printf("  Used Before      : %.1f KB\n", beforeUsed / 1024.0);
        System.out.printf("  Used After       : %.1f KB\n", afterUsed / 1024.0);
        System.out.printf("  Delta (100 profiles): %.1f KB\n", delta / 1024.0);
        System.out.printf("  Per-Profile Avg  : %.2f KB\n", delta / 100.0 / 1024.0);

        return new long[]{beforeUsed, afterUsed, delta};
    }

    // ==================== Report Generation ====================

    /**
     * Generates the final performance report.
     *
     * @param authSpeed average auth time in ms
     * @param stressAvgLoad average profile load time in ms
     * @param memoryStats memory usage statistics
     * @param totalElapsed total benchmark time in ms
     */
    private void generatePerformanceReport(double authSpeed, double stressAvgLoad,
                                           long[] memoryStats, long totalElapsed) {
        System.out.println("\n" + SystemConstants.SEPARATOR);
        System.out.println("          PERFORMANCE REPORT");
        System.out.println(SystemConstants.SEPARATOR);

        int[] widths = {25, 25};
        MenuSystem.printTableRow(new String[]{"Metric", "Value"}, widths);
        MenuSystem.printTableSeparator(widths);
        MenuSystem.printTableRow(new String[]{
                "Avg Auth Time", String.format("%.2f ms", authSpeed)}, widths);
        MenuSystem.printTableRow(new String[]{
                "Avg Profile Load", String.format("%.2f ms", stressAvgLoad)}, widths);
        MenuSystem.printTableRow(new String[]{
                "Memory per Profile", String.format("%.2f KB",
                        memoryStats[2] / 100.0 / 1024.0)}, widths);
        MenuSystem.printTableRow(new String[]{
                "Total Benchmark Time", totalElapsed + " ms"}, widths);

        // Rating
        String rating;
        if (authSpeed < 5.0 && stressAvgLoad < 10.0) rating = "EXCELLENT";
        else if (authSpeed < 20.0 && stressAvgLoad < 50.0) rating = "GOOD";
        else if (authSpeed < 100.0) rating = "ACCEPTABLE";
        else rating = "NEEDS OPTIMIZATION";

        MenuSystem.printTableRow(new String[]{"Overall Rating", rating}, widths);

        System.out.println(SystemConstants.SEPARATOR + "\n");
    }

    // ==================== Helpers ====================

    private double[] generateRandomTimings(int length, double mean, double stdDev) {
        double[] timings = new double[length];
        for (int i = 0; i < length; i++) {
            timings[i] = Math.max(10.0, mean + random.nextGaussian() * stdDev);
        }
        return timings;
    }

    private double[] addNoise(double[] original, double noiseLevel) {
        double[] noisy = new double[original.length];
        for (int i = 0; i < original.length; i++) {
            noisy[i] = Math.max(10.0, original[i] + random.nextGaussian() * noiseLevel);
        }
        return noisy;
    }

    private KeystrokeProfile createTestProfile(String username,
                                               double[] hold, double[] flight) {
        KeystrokeProfile p = new KeystrokeProfile(username);
        p.setHoldTimings(hold);
        p.setFlightTimings(flight);
        p.setAverageHoldTime(KeystrokeCapture.calculateMean(hold));
        p.setAverageFlightTime(KeystrokeCapture.calculateMean(flight));
        p.setHoldStdDev(KeystrokeCapture.calculateStdDev(hold));
        p.setFlightStdDev(KeystrokeCapture.calculateStdDev(flight));
        return p;
    }
}
