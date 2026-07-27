package com.keystroke.auth;

/**
 * User.java - Abstract base class for all users in the keystroke biometric system.
 *
 * Supports ADMIN and USER roles with enrollment status tracking.
 * Subclasses (Admin, RegularUser) implement the abstract login() method
 * to provide role-specific behavior (polymorphism).
 *
 * Week 2-3 Enhancement - Phase 2 (Updated from Week 1)
 */
public abstract class User {
    
    // Enum defining allowed user roles
    public enum Role {
        ADMIN, USER
    }

    // Protected fields - accessible to subclasses
    protected String username;
    protected Role role;
    protected boolean isEnrolled;

    /**
     * Constructor to create a new user with specified username and role.
     * New users are not enrolled by default.
     *
     * @param username the unique identifier for the user
     * @param role     the role assigned to the user (ADMIN or USER)
     */
    public User(String username, Role role) {
        this.username = username;
        this.role = role;
        this.isEnrolled = false;
    }

    /**
     * Overloaded constructor - creates a regular USER by default.
     *
     * @param username the unique identifier for the user
     */
    public User(String username) {
        this(username, Role.USER);
    }

    // ==================== Getter Methods ====================

    /**
     * Returns the username of this user.
     *
     * @return the username string
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the role of this user (ADMIN or USER).
     *
     * @return the user's role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Returns whether the user has completed keystroke enrollment.
     *
     * @return true if the user is enrolled, false otherwise
     */
    public boolean isEnrolled() {
        return isEnrolled;
    }

    // ==================== Setter Methods ====================

    /**
     * Sets the username.
     *
     * @param username the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the role of this user.
     *
     * @param role the new role (ADMIN or USER)
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Sets the enrollment status of this user.
     *
     * @param enrolled true if the user has completed enrollment
     */
    public void setEnrolled(boolean enrolled) {
        this.isEnrolled = enrolled;
    }

    // ==================== Abstract Methods ====================

    /**
     * Polymorphic login method - behaves differently for Admin vs RegularUser.
     * Must be implemented by all concrete subclasses.
     *
     * @return true if login is successful
     */
    public abstract boolean login();

    /**
     * Displays the role-specific dashboard.
     * Must be implemented by all concrete subclasses.
     */
    public abstract void displayDashboard();

    // ==================== Common Methods ====================

    /**
     * Initiates the enrollment process for this user.
     * Prints a formatted enrollment header.
     */
    public void enroll() {
        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║          ENROLLMENT INITIATED            ║");
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.printf("  ║  User    : %-30s ║\n", username);
        System.out.printf("  ║  Role    : %-30s ║\n", role);
        System.out.println("  ╠══════════════════════════════════════════╣");
        System.out.println("  ║  You will type a standard phrase 3 times ║");
        System.out.println("  ║  to build your biometric profile.        ║");
        System.out.println("  ╚══════════════════════════════════════════╝\n");
        this.isEnrolled = true;
    }

    /**
     * Returns a string representation of this user.
     *
     * @return formatted string with user details
     */
    @Override
    public String toString() {
        return "User{username='" + username + "', role=" + role + ", enrolled=" + isEnrolled + "}";
    }
}
