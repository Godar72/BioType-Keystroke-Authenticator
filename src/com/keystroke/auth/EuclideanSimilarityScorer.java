package com.keystroke.auth;

/**
 * EuclideanSimilarityScorer.java - Implements the SimilarityScorer interface
 * using the Euclidean distance algorithm for comparing keystroke profiles.
 *
 * Algorithm:
 *   1. Compute per-element mean absolute deviation (MAD%) between vectors
 *   2. Convert MAD% to similarity: 100 - MAD%
 *   3. Compute hold and flight similarity separately
 *   4. Combine using weighted average (HOLD_WEIGHT + FLIGHT_WEIGHT)
 *
 * The MAD% approach is more discriminative than exponential decay because
 * it measures the AVERAGE percentage difference per timing element.
 * A 20% average deviation means the typist is genuinely different.
 *
 * Week 2-3 Enhancement - Phase 2 (Accuracy Fix)
 */
public class EuclideanSimilarityScorer implements SimilarityScorer {

    /**
     * Calculates the similarity between two keystroke profiles.
     *
     * Uses Per-Element Mean Absolute Percentage Deviation:
     *   For each element i: deviation[i] = |a[i] - b[i]| / a[i] * 100
     *   MAD% = average of all deviation[i]
     *   Similarity = max(0, 100 - MAD%)
     *
     * This is MUCH more discriminative than raw Euclidean distance because:
     *   - A person who types a segment in 500ms vs 700ms has 40% deviation
     *   - Two different typists easily differ by 30-60% per segment
     *   - The same person typically deviates by only 5-15%
     *
     * @param profile1 the stored enrollment profile
     * @param profile2 the live authentication sample
     * @return similarity score between 0.0 and 100.0
     */
    @Override
    public double calculateSimilarity(KeystrokeProfile profile1, KeystrokeProfile profile2) {

        // Validate inputs
        if (profile1 == null || profile2 == null) {
            System.out.println("  [!] Cannot compute similarity: null profile(s).");
            return 0.0;
        }

        double[] hold1 = profile1.getHoldTimings();
        double[] hold2 = profile2.getHoldTimings();
        double[] flight1 = profile1.getFlightTimings();
        double[] flight2 = profile2.getFlightTimings();

        // Calculate per-element mean absolute percentage deviation
        double holdMAD = computeMADPercent(hold1, hold2);
        double flightMAD = computeMADPercent(flight1, flight2);

        // Also compute Euclidean distance for display
        double holdDistance = computeEuclideanDistance(hold1, hold2);
        double flightDistance = computeEuclideanDistance(flight1, flight2);

        // Convert MAD% to similarity (100 - MAD%, clamped to 0-100)
        double holdSimilarity = Math.max(0.0, 100.0 - holdMAD);
        double flightSimilarity = Math.max(0.0, 100.0 - flightMAD);

        // Combine using weighted average
        double combinedSimilarity = (SystemConstants.HOLD_WEIGHT * holdSimilarity)
                + (SystemConstants.FLIGHT_WEIGHT * flightSimilarity);

        // Clamp to 0-100 range
        combinedSimilarity = Math.max(0.0, Math.min(100.0, combinedSimilarity));

        // Display detailed breakdown
        System.out.println("\n  ──── EUCLIDEAN SIMILARITY ANALYSIS ────");
        System.out.printf("  Hold Distance      : %.4f\n", holdDistance);
        System.out.printf("  Flight Distance    : %.4f\n", flightDistance);
        System.out.printf("  Hold MAD%%          : %.2f%%  → Similarity: %.2f%%\n",
                holdMAD, holdSimilarity);
        System.out.printf("  Flight MAD%%        : %.2f%%  → Similarity: %.2f%%\n",
                flightMAD, flightSimilarity);
        System.out.printf("  Weights            : Hold=%.0f%%  Flight=%.0f%%\n",
                SystemConstants.HOLD_WEIGHT * 100, SystemConstants.FLIGHT_WEIGHT * 100);
        System.out.printf("  Combined Similarity: %.2f%%\n", combinedSimilarity);
        System.out.println("  ───────────────────────────────────────\n");

        return combinedSimilarity;
    }

    /**
     * Computes per-element Mean Absolute Percentage Deviation between two vectors.
     *
     * For each element: |a[i] - b[i]| / max(a[i], minDenom) * 100
     * Returns the average across all elements.
     *
     * @param reference the reference timing vector (from enrolled profile)
     * @param sample    the sample timing vector (from auth attempt)
     * @return MAD% value (0 = identical, 100+ = very different)
     */
    private double computeMADPercent(double[] reference, double[] sample) {
        if (reference == null || sample == null) return 100.0;
        if (reference.length == 0 || sample.length == 0) return 100.0;

        int compareLength = Math.min(reference.length, sample.length);
        double totalDeviation = 0.0;
        double minDenom = 50.0; // Minimum denominator to avoid division by near-zero

        for (int i = 0; i < compareLength; i++) {
            double denom = Math.max(reference[i], minDenom);
            double deviation = Math.abs(reference[i] - sample[i]) / denom * 100.0;
            totalDeviation += deviation;
        }

        return totalDeviation / compareLength;
    }

    /**
     * Computes the Euclidean distance between two double vectors.
     * Used for display purposes.
     *
     * @param vector1 first timing vector
     * @param vector2 second timing vector
     * @return the Euclidean distance (always >= 0)
     */
    private double computeEuclideanDistance(double[] vector1, double[] vector2) {
        if (vector1 == null || vector2 == null) return Double.MAX_VALUE;
        if (vector1.length == 0 || vector2.length == 0) return Double.MAX_VALUE;

        int compareLength = Math.min(vector1.length, vector2.length);
        double sumSquaredDiffs = 0.0;

        for (int i = 0; i < compareLength; i++) {
            double diff = vector1[i] - vector2[i];
            sumSquaredDiffs += diff * diff;
        }

        return Math.sqrt(sumSquaredDiffs);
    }

    /**
     * Returns the name of this scoring algorithm.
     *
     * @return "Euclidean Distance + MAD%"
     */
    @Override
    public String getAlgorithmName() {
        return "Euclidean Distance + MAD%";
    }
}
