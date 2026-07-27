package com.keystroke.auth;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AdaptiveThresholdML.java — Per-user ML-based adaptive threshold learning system.
 *
 * Tracks authentication history per user and computes personalized thresholds
 * using a moving average of the last 10 successful authentications. Detects
 * typing pattern changes (fatigue, injury) and auto-adjusts thresholds.
 *
 * Learning data is stored in profiles/ml/{username}_learning.dat
 * Format per line: timestamp,score,success(true/false)
 *
 * Phase 2 Enhancement — ML-Based Adaptive Threshold
 */
public class AdaptiveThresholdML {

    /** Minimum successful authentications before using personalized threshold */
    private static final int MIN_DATA_POINTS = 5;

    /** Number of recent successes to average for threshold */
    private static final int MOVING_WINDOW = 10;

    /** Margin subtracted from moving average to form the threshold */
    private static final double THRESHOLD_MARGIN = 12.0;

    /** Timestamp formatter for learning records */
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Records an authentication attempt to the user's learning dataset.
     *
     * @param username the username
     * @param score    the similarity score achieved (0-100)
     * @param success  whether the authentication was successful
     */
    public void recordAuthentication(String username, double score, boolean success) {
        try {
            Path mlDir = Paths.get(SystemConstants.ML_DIR);
            if (!Files.exists(mlDir)) Files.createDirectories(mlDir);

            String filePath = SystemConstants.ML_DIR + File.separator
                    + username + "_learning.dat";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                String ts = LocalDateTime.now().format(TS_FORMAT);
                writer.write(String.format("%s,%.4f,%s", ts, score, success));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("  [ML] Failed to record authentication: " + e.getMessage());
        }
    }

    /**
     * Computes a personalized threshold for a user based on their
     * authentication history. Uses the moving average of the last N
     * successful scores minus a margin.
     *
     * @param username the username
     * @return personalized threshold, or system default if insufficient data
     */
    public double getPersonalizedThreshold(String username) {
        List<Double> successScores = getSuccessScores(username);

        if (successScores.size() < MIN_DATA_POINTS) {
            return SystemConstants.DEFAULT_THRESHOLD; // Not enough data
        }

        // Take the last MOVING_WINDOW scores
        int from = Math.max(0, successScores.size() - MOVING_WINDOW);
        List<Double> recent = successScores.subList(from, successScores.size());

        double avg = recent.stream().mapToDouble(Double::doubleValue).average().orElse(70.0);
        double personalized = Math.max(SystemConstants.MIN_THRESHOLD,
                Math.min(SystemConstants.MAX_THRESHOLD, avg - THRESHOLD_MARGIN));

        System.out.printf("  [ML] Personalized threshold for '%s': %.2f%% (avg=%.2f%%, data=%d)%n",
                username, personalized, avg, successScores.size());

        return personalized;
    }

    /**
     * Calculates the confidence interval (mean ± 1σ) for a user's recent scores.
     *
     * @param username the username
     * @return array of [lower, mean, upper], or null if insufficient data
     */
    public double[] getConfidenceInterval(String username) {
        List<Double> scores = getSuccessScores(username);
        if (scores.size() < MIN_DATA_POINTS) return null;

        int from = Math.max(0, scores.size() - MOVING_WINDOW);
        List<Double> recent = scores.subList(from, scores.size());

        double mean = recent.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = recent.stream().mapToDouble(d -> (d - mean) * (d - mean))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        return new double[]{mean - stdDev, mean, mean + stdDev};
    }

    /**
     * Detects whether a user's typing pattern has changed significantly
     * by comparing recent scores to historical scores.
     *
     * @param username the username
     * @return a human-readable status string
     */
    public String detectPatternChange(String username) {
        List<Double> all = getSuccessScores(username);
        if (all.size() < MOVING_WINDOW) return "Insufficient data";

        // Compare first half vs last half
        int mid = all.size() / 2;
        double earlyAvg = all.subList(0, mid).stream().mapToDouble(Double::doubleValue)
                .average().orElse(0);
        double recentAvg = all.subList(mid, all.size()).stream().mapToDouble(Double::doubleValue)
                .average().orElse(0);

        double change = Math.abs(recentAvg - earlyAvg);

        if (change > 15) return "⚠ Significant pattern change detected (Δ=" + String.format("%.1f%%", change) + ")";
        if (change > 8)  return "Minor drift detected (Δ=" + String.format("%.1f%%", change) + ")";
        return "✓ Pattern stable";
    }

    /**
     * Generates a threshold recommendation report for a specific user.
     *
     * @param username the username
     * @return a formatted report string
     */
    public String generateReport(String username) {
        List<Double> successScores = getSuccessScores(username);
        List<LearningRecord> allRecords = loadRecords(username);

        StringBuilder sb = new StringBuilder();
        sb.append("═══ ML Threshold Report: ").append(username).append(" ═══\n\n");

        int total = allRecords.size();
        long successes = allRecords.stream().filter(r -> r.success).count();
        long failures = total - successes;

        sb.append(String.format("Total attempts    : %d%n", total));
        sb.append(String.format("Successful        : %d%n", successes));
        sb.append(String.format("Failed            : %d%n", failures));
        sb.append(String.format("Success rate      : %.1f%%%n",
                total > 0 ? successes * 100.0 / total : 0));
        sb.append("\n");

        if (successScores.size() >= MIN_DATA_POINTS) {
            double personalized = getPersonalizedThreshold(username);
            double[] ci = getConfidenceInterval(username);
            sb.append(String.format("Personalized threshold : %.1f%%%n", personalized));
            if (ci != null) {
                sb.append(String.format("Confidence interval    : [%.1f%% — %.1f%%]%n", ci[0], ci[2]));
                sb.append(String.format("Average score          : %.1f%%%n", ci[1]));
            }
            sb.append("Pattern status         : ").append(detectPatternChange(username)).append("\n");
        } else {
            sb.append("Not enough data for personalized threshold.\n");
            sb.append(String.format("Need %d more successful authentications.%n",
                    MIN_DATA_POINTS - successScores.size()));
        }

        return sb.toString();
    }

    /**
     * Returns the number of learning data points for a user.
     */
    public int getDataPointCount(String username) {
        return loadRecords(username).size();
    }

    /**
     * Checks if a user has enough data for personalized thresholds.
     */
    public boolean hasEnoughData(String username) {
        return getSuccessScores(username).size() >= MIN_DATA_POINTS;
    }

    // ==================== Internal Helpers ====================

    /**
     * Extracts all successful authentication scores for a user.
     */
    private List<Double> getSuccessScores(String username) {
        List<Double> scores = new ArrayList<>();
        for (LearningRecord r : loadRecords(username)) {
            if (r.success) scores.add(r.score);
        }
        return scores;
    }

    /**
     * Loads all learning records from the user's data file.
     */
    private List<LearningRecord> loadRecords(String username) {
        List<LearningRecord> records = new ArrayList<>();
        String filePath = SystemConstants.ML_DIR + File.separator
                + username + "_learning.dat";

        if (!Files.exists(Paths.get(filePath))) return records;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        double score = Double.parseDouble(parts[1].trim());
                        boolean success = Boolean.parseBoolean(parts[2].trim());
                        records.add(new LearningRecord(parts[0].trim(), score, success));
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            System.out.println("  [ML] Failed to load learning data: " + e.getMessage());
        }

        return records;
    }

    /** Internal record for a learning data point */
    private static class LearningRecord {
        final String timestamp;
        final double score;
        final boolean success;

        LearningRecord(String timestamp, double score, boolean success) {
            this.timestamp = timestamp;
            this.score = score;
            this.success = success;
        }
    }
}
