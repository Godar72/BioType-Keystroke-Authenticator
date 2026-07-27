package com.keystroke.auth;

import java.util.Scanner;

/**
 * KeystrokeCapture.java - Captures keystroke timing data from user input.
 * Uses System.nanoTime() for precise timing measurements.
 *
 * Captures two key biometric features:
 *   1. Hold Duration  - estimated per-character dwell time
 *   2. Flight Time    - estimated inter-key transition time
 *
 * Week 1 Implementation - Updated for Phase 2 Accuracy Fix
 *
 * APPROACH: The user types the entire phrase 3 separate times. For each attempt:
 *   - Total time and per-segment timing are measured
 *   - The phrase is split into SEGMENTS (groups of 3-4 characters)
 *   - Each segment is typed + Enter pressed, measuring real typing rhythm
 *   - This gives us ~12 granular timing measurements per attempt
 *   - Natural variation between segments captures typing dynamics
 *
 * This approach captures REAL timing differences between people because:
 *   - Fast typists complete segments in ~300-600ms
 *   - Slow typists take ~800-2000ms per segment
 *   - The PATTERN of fast/slow segments is unique to each person
 */
public class KeystrokeCapture {

    // The standard phrase used for enrollment and authentication
    public static final String STANDARD_PHRASE = "The quick brown fox jumps over the lazy dog";

    // Minimum number of characters required for meaningful pattern analysis
    private static final int MIN_CHARACTERS = 10;

    // Segment size for grouping characters (3-4 chars per segment)
    private static final int SEGMENT_SIZE = 4;

    /**
     * Captures keystroke timing data by having the user type the phrase in segments.
     *
     * Each segment is a short group of characters (3-4 chars). The user types each
     * segment and presses Enter. The time for each segment is measured precisely.
     * This captures the RHYTHM and SPEED pattern unique to each typist.
     *
     * Hold durations = time to type each segment (raw typing speed per group)
     * Flight times = time between finishing one segment and starting the next
     *
     * @param inputText the text the user should type
     * @param scanner   the Scanner object for reading user input
     * @return a double[][] array where [0] = hold durations, [1] = flight times;
     *         null if input is too short
     */
    public double[][] captureKeystrokeTiming(String inputText, Scanner scanner) {

        // Validate input length
        if (inputText == null || inputText.length() < MIN_CHARACTERS) {
            System.out.println("[ERROR] Input text must be at least " + MIN_CHARACTERS + " characters.");
            return null;
        }

        // Split phrase into segments
        String[] segments = splitIntoSegments(inputText, SEGMENT_SIZE);
        int numSegments = segments.length;

        double[] holdDurations = new double[numSegments];
        double[] flightTimes = new double[numSegments - 1];

        System.out.println("\n  ╔══════════════════════════════════════════════════╗");
        System.out.println("  ║           KEYSTROKE TIMING CAPTURE               ║");
        System.out.println("  ╠══════════════════════════════════════════════════╣");
        System.out.println("  ║  Type each segment exactly as shown, then ENTER ║");
        System.out.println("  ║  Type naturally — your rhythm is being measured  ║");
        System.out.println("  ╚══════════════════════════════════════════════════╝\n");

        System.out.println("  Full phrase: \"" + inputText + "\"");
        System.out.println("  Segments: " + numSegments + "\n");

        long previousReleaseTime = 0;

        for (int i = 0; i < numSegments; i++) {
            String segment = segments[i];
            String display = segment.replace(" ", "·"); // Show spaces visibly

            System.out.printf("  [%2d/%2d] Type \"%s\" : ", (i + 1), numSegments, display);

            // Record the time just before user starts typing
            long pressTime = System.nanoTime();

            // Read user input — user types the segment and presses Enter
            String userInput = scanner.nextLine();

            // Record the time right after Enter is pressed
            long releaseTime = System.nanoTime();

            // Hold duration: time to type this segment (ms)
            holdDurations[i] = (releaseTime - pressTime) / 1_000_000.0;

            // Flight time: gap between previous segment's Enter and this segment's start
            if (i > 0) {
                flightTimes[i - 1] = (pressTime - previousReleaseTime) / 1_000_000.0;
                if (flightTimes[i - 1] < 0) flightTimes[i - 1] = 0.0;
            }

            previousReleaseTime = releaseTime;

            // Validate input accuracy
            if (!userInput.equalsIgnoreCase(segment.trim())) {
                System.out.printf("         [!] Expected \"%s\" — got \"%s\". Continuing...\n",
                        segment, userInput);
            }
        }

        // Display timing data
        System.out.println("\n  ──────────── TIMING DATA ────────────");
        System.out.println("  Segment    | Hold (ms)  | Flight (ms)");
        System.out.println("  ───────────┼────────────┼────────────");

        for (int i = 0; i < numSegments; i++) {
            String seg = segments[i].length() > 5
                    ? segments[i].substring(0, 5) + ".."
                    : segments[i];
            if (i > 0) {
                System.out.printf("  %-10s |  %8.2f  |  %8.2f\n",
                        seg, holdDurations[i], flightTimes[i - 1]);
            } else {
                System.out.printf("  %-10s |  %8.2f  |     ---\n",
                        seg, holdDurations[i]);
            }
        }
        System.out.println("  ─────────────────────────────────────\n");

        // Print summary statistics
        System.out.printf("  Average Hold Duration : %.2f ms\n", calculateMean(holdDurations));
        System.out.printf("  Average Flight Time   : %.2f ms\n", calculateMean(flightTimes));
        System.out.printf("  Hold Std Deviation    : %.2f ms\n", calculateStdDev(holdDurations));
        System.out.printf("  Flight Std Deviation  : %.2f ms\n\n", calculateStdDev(flightTimes));

        return new double[][] { holdDurations, flightTimes };
    }

    /**
     * Splits a text string into segments of roughly equal size.
     * Tries to break at word boundaries when possible.
     *
     * @param text the text to split
     * @param charsPerSegment approximate characters per segment
     * @return array of segment strings
     */
    private String[] splitIntoSegments(String text, int charsPerSegment) {
        java.util.List<String> segments = new java.util.ArrayList<>();
        int i = 0;

        while (i < text.length()) {
            int end = Math.min(i + charsPerSegment, text.length());

            // Try to break at a word boundary (space) if we're not at the end
            if (end < text.length()) {
                // Look ahead for a space within the next 2 characters
                int spaceIdx = text.indexOf(' ', end);
                if (spaceIdx >= 0 && spaceIdx <= end + 2) {
                    end = spaceIdx + 1; // Include the space
                }
            }

            segments.add(text.substring(i, end));
            i = end;
        }

        return segments.toArray(new String[0]);
    }

    /**
     * Calculates the arithmetic mean of a double array.
     *
     * @param data the array of values
     * @return the mean value
     */
    public static double calculateMean(double[] data) {
        if (data == null || data.length == 0) return 0.0;
        double sum = 0.0;
        for (double val : data) {
            sum += val;
        }
        return sum / data.length;
    }

    /**
     * Calculates the standard deviation of a double array.
     *
     * @param data the array of values
     * @return the standard deviation
     */
    public static double calculateStdDev(double[] data) {
        if (data == null || data.length <= 1) return 0.0;
        double mean = calculateMean(data);
        double sumSquaredDiffs = 0.0;
        for (double val : data) {
            sumSquaredDiffs += (val - mean) * (val - mean);
        }
        return Math.sqrt(sumSquaredDiffs / (data.length - 1));
    }
}
