public abstract class BiometricAuthenticator {
    // Template method hiding the internal complexity of verification
    public final boolean verifyUser(KeystrokeProfile input, KeystrokeProfile stored, double threshold) {
        SimilarityScorer scorer = new EuclideanScorer();
        double distance = scorer.calculateDistance(input, stored);

        System.out.println("Verification complete. Distance: " + distance);
        return distance <= threshold; // Accept or Reject
    }
}