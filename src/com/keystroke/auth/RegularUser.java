package com.keystroke.auth;

import java.util.List;

/**
 * RegularUser.java - Standard user with keystroke biometric authentication.
 *
 * Extends User to implement user-specific functionality:
 *   - Keystroke-based login (biometric authentication)
 *   - Profile update (re-enrollment)
 *   - View personal authentication history
 *
 * RegularUser authenticates by typing the standard phrase and matching
 * keystroke timing patterns against their enrolled profile.
 *
 * Week 2-3 Enhancement - Phase 2
 */
public class RegularUser extends User {

    /**
     * Constructs a RegularUser with the given username.
     *
     * @param username the user's unique identifier
     */
    public RegularUser(String username) {
        super(username, Role.USER);
    }

    /**
     * Polymorphic login for RegularUser - uses keystroke biometric authentication.
     * This method checks if the user is enrolled; actual biometric verification
     * is delegated to the AuthenticationEngine by the main system.
     *
     * @return true if the user is enrolled (eligible for biometric auth)
     */
    @Override
    public boolean login() {
        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║          USER LOGIN                      ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println("  User: " + username);

        if (!isEnrolled) {
            System.out.println("  [!] You are not enrolled yet.");
            System.out.println("  [!] Please enroll first to set up your biometric profile.\n");
            return false;
        }

        System.out.println("  [✓] Enrollment verified. Proceeding to biometric check...\n");
        return true;
    }

    /**
     * Displays the regular user dashboard with available options.
     */
    @Override
    public void displayDashboard() {
        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║          USER DASHBOARD                  ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf("  ║  Welcome : %-30s ║\n", username);
        System.out.println("  ║  Role    : REGULAR USER                  ║");
        System.out.printf("  ║  Status  : %-30s ║\n", isEnrolled ? "ENROLLED" : "NOT ENROLLED");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║  1) Authenticate                        ║");
        System.out.println("  ║  2) Re-enroll (Update Profile)          ║");
        System.out.println("  ║  3) View My Auth History                ║");
        System.out.println("  ║  4) View My Profile Analysis            ║");
        System.out.println("  ║  5) Logout                              ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
    }

    // ==================== User Functions ====================

    /**
     * Updates the user's keystroke profile by initiating a re-enrollment process.
     * The old profile is replaced with new timing data.
     *
     * @param fileManager the FileManager to save the updated profile
     * @param profile     the new KeystrokeProfile from re-enrollment
     */
    public void updateProfile(FileManager fileManager, KeystrokeProfile profile) {
        if (profile == null) {
            System.out.println("  [!] Cannot update profile: null profile provided.");
            return;
        }

        System.out.println("\n  ──── PROFILE UPDATE ────");
        System.out.println("  Replacing existing profile for user: " + username);

        // Save the new profile (overwrites existing)
        String filename = username + SystemConstants.PROFILE_EXTENSION;
        fileManager.saveUserProfile(profile, filename);

        System.out.println("  [✓] Profile updated successfully.");
        System.out.println("  ─────────────────────────\n");
    }

    /**
     * Displays the user's personal authentication history.
     *
     * @param logger the AuthLogger instance
     */
    public void viewMyAuthHistory(AuthLogger logger) {
        System.out.println("\n  ──── MY AUTHENTICATION HISTORY ────");
        logger.displayUserHistory(username);
    }

    /**
     * Displays a detailed analysis of the user's keystroke profile.
     *
     * @param fileManager the FileManager to load the profile
     * @param analyzer    the ProfileAnalyzer to run the analysis
     */
    public void viewMyProfileAnalysis(FileManager fileManager, ProfileAnalyzer analyzer) {
        KeystrokeProfile profile = fileManager.loadUserProfile(username);
        if (profile == null) {
            System.out.println("  [!] No profile found. Please enroll first.");
            return;
        }
        analyzer.analyzeProfile(profile);
    }
}
