public class KeystrokeProfile {
    private double[] meanHoldTimes;
    private double[] meanFlightTimes;
    private String username;

    public KeystrokeProfile(String username) {
        this.username = username;
    }

    public double[] getMeanHoldTimes() {
        return meanHoldTimes;
    }

    public void setMeanHoldTimes(double[] holdTimes) {
        this.meanHoldTimes = holdTimes;
    }

    public String getUsername() {
        return username;
    }
}

