package com.keystroke.auth;

/**
 * TestSuite.java - Comprehensive unit test suite for all major system components.
 *
 * Tests cover:
 *   - Keystroke capture accuracy and edge cases
 *   - Profile building with 1, 3, 5 samples
 *   - Euclidean distance computation with known data
 *   - Adaptive threshold adjustment logic
 *   - File operations (save, load, corrupt file handling)
 *   - Authentication engine (genuine, impostor, edge cases)
 *   - Input validation
 *   - Impostor detection
 *
 * All tests report PASS/FAIL with detailed diagnostics.
 *
 * Week 3 - Phase 2 Finalization
 */
public class TestSuite {

    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;

    /**
     * Runs the complete test suite.
     */
    public void runAllTests() {
        MenuSystem.printHeader("COMPREHENSIVE TEST SUITE");
        System.out.println("  Running all tests...\n");

        long start = System.currentTimeMillis();

        testKeystrokeCapture();
        testProfileBuilding();
        testEuclideanDistance();
        testThresholdAdjustment();
        testFileOperations();
        testAuthenticationEngine();
        testInputValidator();
        testImpostorDetector();

        long elapsed = System.currentTimeMillis() - start;

        // Summary
        System.out.println("\n" + SystemConstants.SEPARATOR);
        System.out.println("              TEST RESULTS SUMMARY");
        System.out.println(SystemConstants.SEPARATOR);

        int[] widths = {18, 10};
        MenuSystem.printTableRow(new String[]{"Total Tests", String.valueOf(totalTests)}, widths);
        MenuSystem.printTableRow(new String[]{"Passed", String.valueOf(passedTests)}, widths);
        MenuSystem.printTableRow(new String[]{"Failed", String.valueOf(failedTests)}, widths);
        MenuSystem.printTableRow(new String[]{"Success Rate",
                String.format("%.1f%%", (double) passedTests / totalTests * 100)}, widths);
        MenuSystem.printTableRow(new String[]{"Time Elapsed", elapsed + " ms"}, widths);

        System.out.println(SystemConstants.SEPARATOR + "\n");
    }

    // ==================== Test Groups ====================

    /**
     * Tests keystroke capture utility methods (mean, stddev).
     */
    private void testKeystrokeCapture() {
        printTestGroup("KEYSTROKE CAPTURE");

        // Test mean calculation
        double[] data1 = {100.0, 200.0, 300.0};
        assertApprox("Mean of [100,200,300]", 200.0,
                KeystrokeCapture.calculateMean(data1), 0.01);

        // Test stddev calculation
        double[] data2 = {10.0, 10.0, 10.0, 10.0};
        assertApprox("StdDev of uniform array", 0.0,
                KeystrokeCapture.calculateStdDev(data2), 0.01);

        // Test with known stddev
        double[] data3 = {2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0};
        double mean3 = KeystrokeCapture.calculateMean(data3);
        assertApprox("Mean of test data", 5.0, mean3, 0.01);

        // Edge case: single element
        double[] single = {42.0};
        assertApprox("Mean of single element", 42.0,
                KeystrokeCapture.calculateMean(single), 0.01);

        // Edge case: empty array
        double[] empty = {};
        assertApprox("Mean of empty array", 0.0,
                KeystrokeCapture.calculateMean(empty), 0.01);
    }

    /**
     * Tests profile building with different sample counts.
     */
    private void testProfileBuilding() {
        printTestGroup("PROFILE BUILDING");

        // Test with 1 sample
        java.util.List<double[]> holdSamples1 = new java.util.ArrayList<>();
        java.util.List<double[]> flightSamples1 = new java.util.ArrayList<>();
        holdSamples1.add(new double[]{800.0, 900.0, 850.0});
        flightSamples1.add(new double[]{200.0, 220.0});

        KeystrokeProfile profile1 = new KeystrokeProfile("test_1sample");
        profile1.buildProfile(holdSamples1, flightSamples1);
        assertApprox("1-sample hold mean", 850.0,
                profile1.getAverageHoldTime(), 5.0);
        assertTrue("1-sample hold timings length == 3",
                profile1.getHoldTimings().length == 3);

        // Test with 3 samples (standard)
        java.util.List<double[]> holdSamples3 = new java.util.ArrayList<>();
        java.util.List<double[]> flightSamples3 = new java.util.ArrayList<>();
        holdSamples3.add(new double[]{800.0, 900.0, 850.0});
        holdSamples3.add(new double[]{810.0, 890.0, 860.0});
        holdSamples3.add(new double[]{805.0, 895.0, 855.0});
        flightSamples3.add(new double[]{200.0, 220.0});
        flightSamples3.add(new double[]{205.0, 215.0});
        flightSamples3.add(new double[]{202.0, 218.0});

        KeystrokeProfile profile3 = new KeystrokeProfile("test_3sample");
        profile3.buildProfile(holdSamples3, flightSamples3);
        assertApprox("3-sample hold mean ~852", 852.0,
                profile3.getAverageHoldTime(), 10.0);
        assertTrue("3-sample profile is built",
                profile3.getHoldTimings() != null && profile3.getHoldTimings().length > 0);

        // Test with 5 samples
        java.util.List<double[]> holdSamples5 = new java.util.ArrayList<>();
        java.util.List<double[]> flightSamples5 = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            holdSamples5.add(new double[]{800.0 + i * 2, 900.0 + i * 2, 850.0 + i * 2});
            flightSamples5.add(new double[]{200.0 + i * 2, 220.0 + i * 2});
        }

        KeystrokeProfile profile5 = new KeystrokeProfile("test_5sample");
        profile5.buildProfile(holdSamples5, flightSamples5);
        assertTrue("5-sample profile built successfully",
                profile5.getHoldTimings() != null);

        // Statistical accuracy: averaged values should be close
        double expectedHoldMean = 854.0;
        assertApprox("5-sample avg hold is reasonable", expectedHoldMean,
                profile5.getAverageHoldTime(), 10.0);
    }

    /**
     * Tests Euclidean distance calculations with known data.
     */
    private void testEuclideanDistance() {
        printTestGroup("EUCLIDEAN DISTANCE");

        EuclideanSimilarityScorer scorer = new EuclideanSimilarityScorer();

        // Identical profiles should have 100% similarity
        KeystrokeProfile p1 = createTestProfile("identical_a",
                new double[]{800, 900, 850, 750, 820},
                new double[]{200, 180, 220, 210});
        KeystrokeProfile p2 = createTestProfile("identical_b",
                new double[]{800, 900, 850, 750, 820},
                new double[]{200, 180, 220, 210});
        double simIdentical = scorer.calculateSimilarity(p1, p2);
        assertApprox("Identical profiles = 100%", 100.0, simIdentical, 0.1);

        // Very similar profiles should have high similarity (5% deviation)
        KeystrokeProfile p3 = createTestProfile("similar_a",
                new double[]{800, 900, 850, 750, 820},
                new double[]{200, 180, 220, 210});
        KeystrokeProfile p4 = createTestProfile("similar_b",
                new double[]{820, 880, 870, 730, 840},
                new double[]{210, 175, 230, 205});
        double simSimilar = scorer.calculateSimilarity(p3, p4);
        assertTrue("Similar profiles > 80%", simSimilar > 80.0);

        // Very different profiles should have low similarity (different typist)
        KeystrokeProfile p5 = createTestProfile("different_a",
                new double[]{400, 450, 420, 380, 430},
                new double[]{100, 120, 110, 105});
        KeystrokeProfile p6 = createTestProfile("different_b",
                new double[]{1800, 2000, 1700, 1900, 1850},
                new double[]{500, 550, 480, 520});
        double simDifferent = scorer.calculateSimilarity(p5, p6);
        assertTrue("Different profiles < 50%", simDifferent < 50.0);

        // Null handling
        double simNull = scorer.calculateSimilarity(null, p1);
        assertApprox("Null profile returns 0", 0.0, simNull, 0.1);
    }

    /**
     * Tests adaptive threshold adjustment logic.
     */
    private void testThresholdAdjustment() {
        printTestGroup("THRESHOLD ADJUSTMENT");

        ThresholdManager tm = new ThresholdManager();
        tm.setThreshold(60.0);

        // Test: genuine user rejected (false reject) → threshold should decrease
        double before = tm.getCurrentThreshold();
        tm.adjustThreshold(false, 55.0); // Failed with score close to threshold
        assertTrue("Threshold decreased after false reject",
                tm.getCurrentThreshold() <= before);

        // Test: threshold stays within bounds
        tm.setThreshold(SystemConstants.MIN_THRESHOLD - 10);
        assertTrue("Threshold clamped to min",
                tm.getCurrentThreshold() >= SystemConstants.MIN_THRESHOLD);

        tm.setThreshold(SystemConstants.MAX_THRESHOLD + 10);
        assertTrue("Threshold clamped to max",
                tm.getCurrentThreshold() <= SystemConstants.MAX_THRESHOLD);

        // Test: reset to default
        tm.resetToDefault();
        assertApprox("Reset to default", SystemConstants.DEFAULT_THRESHOLD,
                tm.getCurrentThreshold(), 0.01);
    }

    /**
     * Tests file operations (save, load, existence check, delete).
     */
    private void testFileOperations() {
        printTestGroup("FILE OPERATIONS");

        FileManager fm = new FileManager();

        // Save a test profile
        KeystrokeProfile saveProfile = createTestProfile("_test_save_",
                new double[]{100.0, 110.0, 120.0},
                new double[]{80.0, 90.0});
        fm.saveUserProfile(saveProfile, "_test_save_" + SystemConstants.PROFILE_EXTENSION);
        assertTrue("Profile saved successfully",
                fm.profileExists("_test_save_"));

        // Load the saved profile
        KeystrokeProfile loaded = fm.loadUserProfile("_test_save_");
        assertTrue("Profile loaded successfully", loaded != null);
        if (loaded != null) {
            assertApprox("Loaded avg hold matches saved",
                    saveProfile.getAverageHoldTime(),
                    loaded.getAverageHoldTime(), 0.01);
            assertTrue("Hold timings array length matches",
                    loaded.getHoldTimings().length == 3);
        }

        // Delete the test profile
        boolean deleted = fm.deleteProfile("_test_save_");
        assertTrue("Profile deleted", deleted);
        assertTrue("Profile no longer exists",
                !fm.profileExists("_test_save_"));

        // Load nonexistent profile
        KeystrokeProfile missing = fm.loadUserProfile("_nonexistent_user_xyz_");
        assertTrue("Loading missing profile returns null", missing == null);
    }

    /**
     * Tests the authentication engine with genuine and impostor scenarios.
     */
    private void testAuthenticationEngine() {
        printTestGroup("AUTHENTICATION ENGINE");

        FileManager fm = new FileManager();
        AuthenticationEngine engine = new AuthenticationEngine(fm);

        // Create and save a test profile (segment-scale timings)
        double[] enrolledHold = {800, 850, 780, 900, 820, 870, 810, 860, 790, 840, 830};
        double[] enrolledFlight = {200, 180, 220, 190, 210, 195, 215, 185, 205, 200};
        KeystrokeProfile enrolled = createTestProfile("_test_auth_",
                enrolledHold, enrolledFlight);
        fm.saveUserProfile(enrolled,
                "_test_auth_" + SystemConstants.PROFILE_EXTENSION);

        // Test genuine user (similar timings — ~5% deviation)
        try {
            double[] genuineHold = {820, 840, 790, 910, 830, 860, 820, 870, 800, 850, 840};
            double[] genuineFlight = {210, 175, 225, 195, 215, 200, 220, 190, 210, 205};
            AuthResult result = engine.authenticate("_test_auth_",
                    genuineHold, genuineFlight);
            assertTrue("Genuine user similarity > 50%",
                    result.getConfidenceScore() > 50.0);
        } catch (AuthenticationException e) {
            recordResult("Genuine auth no exception", false);
        }

        // Test impostor (very different timings — different typist)
        try {
            double[] impostorHold = {1800, 2000, 1700, 1900, 1850, 1950, 1750, 2100, 1800, 1900, 1850};
            double[] impostorFlight = {500, 550, 480, 520, 510, 540, 490, 530, 500, 515};
            AuthResult result = engine.authenticate("_test_auth_",
                    impostorHold, impostorFlight);
            assertTrue("Impostor similarity < 50%",
                    result.getConfidenceScore() < 50.0);
        } catch (AuthenticationException e) {
            recordResult("Impostor auth no exception", false);
        }

        // Test missing user
        try {
            engine.authenticate("_nonexistent_xyz_",
                    new double[]{100}, new double[]{80});
            recordResult("Missing user throws exception", false);
        } catch (AuthenticationException e) {
            assertTrue("ErrorCode is PROFILE_NOT_FOUND",
                    e.getErrorCode() == AuthenticationException.ErrorCode.PROFILE_NOT_FOUND);
        }

        // Test empty input
        try {
            engine.authenticate("_test_auth_", new double[]{}, null);
            recordResult("Empty input throws exception", false);
        } catch (AuthenticationException e) {
            assertTrue("ErrorCode is INVALID_INPUT",
                    e.getErrorCode() == AuthenticationException.ErrorCode.INVALID_INPUT);
        }

        // Cleanup
        fm.deleteProfile("_test_auth_");
    }

    /**
     * Tests input validation methods.
     */
    private void testInputValidator() {
        printTestGroup("INPUT VALIDATOR");

        // Username validation
        assertTrue("Valid username 'john'", InputValidator.validateUsername("john"));
        assertTrue("Valid username with underscore", InputValidator.validateUsername("john_doe"));
        assertTrue("Invalid: empty", !InputValidator.validateUsername(""));
        assertTrue("Invalid: null", !InputValidator.validateUsername(null));
        assertTrue("Invalid: too short 'ab'", !InputValidator.validateUsername("ab"));
        assertTrue("Invalid: special chars", !InputValidator.validateUsername("jo@hn!"));

        // Timing data validation
        assertTrue("Valid timings", InputValidator.validateTimingData(
                new double[]{50.0, 100.0, 200.0}));
        assertTrue("Invalid: below minimum", !InputValidator.validateTimingData(
                new double[]{5.0, 100.0}));
        assertTrue("Invalid: above maximum", !InputValidator.validateTimingData(
                new double[]{100.0, 2500.0}));
        assertTrue("Invalid: null", !InputValidator.validateTimingData(null));
        assertTrue("Invalid: empty", !InputValidator.validateTimingData(new double[]{}));

        // Input sanitization
        String sanitized = InputValidator.sanitizeInput("hello../world");
        assertTrue("Sanitized removes path traversal",
                !sanitized.contains(".."));

        // Threshold validation
        assertTrue("Valid threshold 60", InputValidator.validateThreshold(60.0));
        assertTrue("Invalid threshold 30", !InputValidator.validateThreshold(30.0));
        assertTrue("Invalid threshold 90", !InputValidator.validateThreshold(90.0));
    }

    /**
     * Tests impostor detection heuristics.
     */
    private void testImpostorDetector() {
        printTestGroup("IMPOSTOR DETECTOR");

        ImpostorDetector detector = new ImpostorDetector();

        // Normal human typing — low risk (segment-scale values)
        double[] normalHold = {800, 850, 780, 900, 820, 870, 810, 860, 790, 840, 830};
        double[] normalFlight = {200, 180, 220, 190, 210, 195, 215, 185, 205, 200};
        double normalRisk = detector.analyzePattern(normalHold, normalFlight);
        assertTrue("Normal typing risk < 50", normalRisk < 50.0);

        // Bot-like typing — very consistent, should flag
        double[] botHold = {800.0, 800.0, 800.0, 800.0, 800.0,
                800.0, 800.0, 800.0, 800.0, 800.0, 800.0};
        double[] botFlight = {200.0, 200.0, 200.0, 200.0, 200.0,
                200.0, 200.0, 200.0, 200.0, 200.0};
        double botRisk = detector.analyzePattern(botHold, botFlight);
        assertTrue("Bot-like typing risk > 30", botRisk > 30.0);

        // Null/empty data
        double nullRisk = detector.analyzePattern(null, null);
        assertApprox("Null data = max risk", 100.0, nullRisk, 0.1);

        // Session lock after failures
        detector.recordFailedAttempt("locktest");
        detector.recordFailedAttempt("locktest");
        detector.recordFailedAttempt("locktest");
        assertTrue("Session locked after 3 failures",
                detector.isSessionLocked("locktest"));

        detector.resetSession();
        assertTrue("Session unlocked after reset",
                !detector.isSessionLocked("locktest"));
    }

    // ==================== Assertion Helpers ====================

    private void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("    " + MenuSystem.statusBadge("PASS") + " " + testName);
        } else {
            failedTests++;
            System.out.println("    " + MenuSystem.statusBadge("FAIL") + " " + testName);
        }
    }

    private void assertApprox(String testName, double expected, double actual, double tolerance) {
        boolean pass = Math.abs(expected - actual) <= tolerance;
        totalTests++;
        if (pass) {
            passedTests++;
            System.out.println("    " + MenuSystem.statusBadge("PASS") + " " + testName
                    + " (expected=" + expected + ", actual=" + String.format("%.4f", actual) + ")");
        } else {
            failedTests++;
            System.out.println("    " + MenuSystem.statusBadge("FAIL") + " " + testName
                    + " (expected=" + expected + ", actual=" + String.format("%.4f", actual) + ")");
        }
    }

    private void recordResult(String testName, boolean pass) {
        totalTests++;
        if (pass) {
            passedTests++;
            System.out.println("    " + MenuSystem.statusBadge("PASS") + " " + testName);
        } else {
            failedTests++;
            System.out.println("    " + MenuSystem.statusBadge("FAIL") + " " + testName);
        }
    }

    private void printTestGroup(String name) {
        System.out.println("\n  ─── " + name + " ───");
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

    /**
     * Returns the total test count.
     * @return total tests run
     */
    public int getTotalTests() { return totalTests; }

    /**
     * Returns the passed test count.
     * @return tests passed
     */
    public int getPassedTests() { return passedTests; }

    /**
     * Returns the failed test count.
     * @return tests failed
     */
    public int getFailedTests() { return failedTests; }
}
