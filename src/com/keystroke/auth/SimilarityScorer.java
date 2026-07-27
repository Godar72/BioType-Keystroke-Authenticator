package com.keystroke.auth;

/**
 * SimilarityScorer.java - Interface defining the contract for similarity scoring algorithms.
 *
 * Implementations of this interface provide different distance/similarity metrics
 * for comparing keystroke profiles. The system can swap scoring algorithms by
 * providing different implementations (Strategy pattern).
 *
 * Week 2-3 Enhancement - Phase 2
 */
public interface SimilarityScorer {

    /**
     * Calculates a similarity score between two keystroke profiles.
     *
     * The similarity score is on a 0-100 scale where:
     *   - 100 = identical profiles (perfect match)
     *   -   0 = completely different profiles (no match)
     *
     * @param profile1 the first keystroke profile (typically the stored enrollment profile)
     * @param profile2 the second keystroke profile (typically the live authentication sample)
     * @return a similarity score between 0.0 and 100.0
     */
    double calculateSimilarity(KeystrokeProfile profile1, KeystrokeProfile profile2);

    /**
     * Returns the name of the scoring algorithm for logging and display purposes.
     *
     * @return the algorithm name (e.g., "Euclidean Distance", "Manhattan Distance")
     */
    String getAlgorithmName();
}
