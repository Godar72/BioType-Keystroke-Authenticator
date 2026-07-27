package com.keystroke.auth;

import java.util.Random;


/**
 * DemoDataGenerator.java - Generates realistic demo data for testing and demonstration.
 *
 * Creates:
 *   - Sample user profiles with varied typing characteristics
 *   - Impostor attempt data with distinguishable patterns
 *   - Realistic authentication log history
 *
 * Timing values represent SEGMENT timings (not per-character):
 *   - Each segment is ~4 characters of the standard phrase
 *   - ~11 hold timings (time to type each segment)
 *   - ~10 flight timings (gap between segments)
 *
 * Week 3 - Phase 2 Finalization
 */
public class DemoDataGenerator {

    private final FileManager fileManager;
    private final Random random;

    /** Number of segments (phrase split into ~4 char groups) */
    private static final int NUM_SEGMENTS = 11;

    /**
     * Constructs a DemoDataGenerator using the given FileManager.
     *
     * @param fileManager the FileManager for saving generated profiles
     */
    public DemoDataGenerator(FileManager fileManager) {
        this.fileManager = fileManager;
        this.random = new Random(42); // Fixed seed for reproducibility
    }

    /**
     * Creates 10 sample user profiles with diverse typing characteristics.
     * Timing values represent segment typing speed (ms per 4-char group).
     */
    public void createSampleUsers() {
        MenuSystem.printHeader("GENERATING DEMO USERS");

        //                    username          holdMean  holdStd  flightMean  flightStd
        createProfile("john_doe",       800.0,  120.0,   200.0,   50.0);  // Average typist
        MenuSystem.printProgressBar(1, 10, "Users");
        createProfile("jane_smith",     450.0,   60.0,   120.0,   25.0);  // Fast, consistent
        MenuSystem.printProgressBar(2, 10, "Users");
        createProfile("bob_wilson",    1800.0,  400.0,   500.0,  150.0);  // Hunt-and-peck
        MenuSystem.printProgressBar(3, 10, "Users");
        createProfile("alice_chen",     550.0,  150.0,   180.0,   60.0);  // Mobile typist
        MenuSystem.printProgressBar(4, 10, "Users");
        createProfile("security_test", 1000.0,  200.0,   250.0,   70.0);  // Deliberate
        MenuSystem.printProgressBar(5, 10, "Users");
        createProfile("typing_expert",  350.0,   40.0,    80.0,   15.0);  // Professional
        MenuSystem.printProgressBar(6, 10, "Users");
        createProfile("new_user_01",   1100.0,  250.0,   300.0,   80.0);  // Beginner
        MenuSystem.printProgressBar(7, 10, "Users");
        createProfile("new_user_02",   1400.0,  300.0,   400.0,  100.0);  // Casual
        MenuSystem.printProgressBar(8, 10, "Users");
        createProfile("new_user_03",    600.0,  100.0,   200.0,  100.0);  // Programmer
        MenuSystem.printProgressBar(9, 10, "Users");
        createProfile("new_user_04",   1600.0,  350.0,   450.0,  120.0);  // Older user
        MenuSystem.printProgressBar(10, 10, "Users");

        MenuSystem.printSuccess("10 demo user profiles created successfully.");
    }

    /**
     * Creates a single profile with specified timing characteristics.
     * Timings are in ms-per-segment scale.
     *
     * @param username   the username
     * @param holdMean   mean hold time in ms (per segment)
     * @param holdStdDev standard deviation of hold time
     * @param flightMean mean flight time in ms (between segments)
     * @param flightStdDev standard deviation of flight time
     */
    private void createProfile(String username, double holdMean, double holdStdDev,
                               double flightMean, double flightStdDev) {
        double[] holdTimings = generateGaussianTimings(NUM_SEGMENTS, holdMean, holdStdDev);
        double[] flightTimings = generateGaussianTimings(NUM_SEGMENTS - 1, flightMean, flightStdDev);

        KeystrokeProfile profile = new KeystrokeProfile(username);
        profile.setHoldTimings(holdTimings);
        profile.setFlightTimings(flightTimings);
        profile.setAverageHoldTime(KeystrokeCapture.calculateMean(holdTimings));
        profile.setAverageFlightTime(KeystrokeCapture.calculateMean(flightTimings));
        profile.setHoldStdDev(KeystrokeCapture.calculateStdDev(holdTimings));
        profile.setFlightStdDev(KeystrokeCapture.calculateStdDev(flightTimings));

        String filename = username + SystemConstants.PROFILE_EXTENSION;
        fileManager.saveUserProfile(profile, filename);
    }

    /**
     * Generates an array of Gaussian-distributed timing values.
     *
     * @param length  the number of values to generate
     * @param mean    the mean value
     * @param stdDev  the standard deviation
     * @return array of timing values (clamped to [50, 5000] ms for segment timings)
     */
    private double[] generateGaussianTimings(int length, double mean, double stdDev) {
        double[] timings = new double[length];
        for (int i = 0; i < length; i++) {
            double val = mean + random.nextGaussian() * stdDev;
            timings[i] = Math.max(50.0, Math.min(5000.0, val));
        }
        return timings;
    }

    /**
     * Generates impostor attempt data — timing data that mimics a different user.
     * Creates data with shifted means and higher variance to represent impostor typing.
     *
     * @return array of [holdTimings, flightTimings] mimicking impostor behavior
     */
    public double[][] createImpostorAttempts() {
        MenuSystem.printHeader("GENERATING IMPOSTOR DATA");

        // Impostor times differ significantly from any enrolled user
        double[] impostorHold = generateGaussianTimings(NUM_SEGMENTS, 2000.0, 500.0);
        double[] impostorFlight = generateGaussianTimings(NUM_SEGMENTS - 1, 600.0, 200.0);

        MenuSystem.printSuccess("Impostor attempt data generated.");
        System.out.printf("  Impostor Hold Mean: %.2f ms, StdDev: %.2f ms\n",
                KeystrokeCapture.calculateMean(impostorHold),
                KeystrokeCapture.calculateStdDev(impostorHold));
        System.out.printf("  Impostor Flight Mean: %.2f ms, StdDev: %.2f ms\n",
                KeystrokeCapture.calculateMean(impostorFlight),
                KeystrokeCapture.calculateStdDev(impostorFlight));

        return new double[][] { impostorHold, impostorFlight };
    }

    /**
     * Populates authentication log files with realistic historical data.
     * Creates log entries spanning the current day.
     */
    public void populateAuthLogs() {
        MenuSystem.printHeader("POPULATING AUTH LOGS");

        AuthLogger logger = new AuthLogger();
        String[] users = {"john_doe", "jane_smith", "bob_wilson", "alice_chen", "typing_expert"};
        double[] scores = {78.5, 92.3, 45.0, 67.8, 95.1, 30.2, 55.7, 82.4, 71.0, 40.5};
        boolean[] results = {true, true, false, true, true, false, false, true, true, false};
        double threshold = 70.0;

        for (int i = 0; i < scores.length; i++) {
            String user = users[i % users.length];
            logger.logAuthAttempt(user, results[i], scores[i], threshold, results[i] ? 10.0 : 65.0);
        }

        MenuSystem.printSuccess("10 authentication log entries created.");
        MenuSystem.printProgressBar(10, 10, "Logs ");
    }

    /**
     * Runs the complete demo data generation: users + impostor data + logs.
     */
    public void generateAll() {
        System.out.println("\n" + SystemConstants.SEPARATOR);
        System.out.println("          DEMO DATA GENERATION");
        System.out.println(SystemConstants.SEPARATOR + "\n");

        createSampleUsers();
        System.out.println();
        createImpostorAttempts();
        System.out.println();
        populateAuthLogs();

        System.out.println("\n" + SystemConstants.SEPARATOR);
        System.out.println("          DEMO DATA COMPLETE");
        System.out.println(SystemConstants.SEPARATOR + "\n");
    }
}
