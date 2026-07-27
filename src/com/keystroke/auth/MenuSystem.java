package com.keystroke.auth;

/**
 * MenuSystem.java - Enhanced console interface with ANSI colors, progress bars,
 * formatted tables, and confirmation dialogs.
 *
 * Provides reusable UI utilities for all system menus and output formatting.
 *
 * Week 3 - Phase 2 Finalization
 */
public class MenuSystem {

    // ==================== ANSI Color Codes ====================
    public static final String RESET   = "\u001B[0m";
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String PURPLE  = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";
    public static final String BOLD    = "\u001B[1m";
    public static final String DIM     = "\u001B[2m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_RED   = "\u001B[41m";

    /**
     * Prints a colored message.
     *
     * @param color the ANSI color code
     * @param message the message to print
     */
    public static void printColored(String color, String message) {
        System.out.println(color + message + RESET);
    }

    /**
     * Prints a success message in green.
     *
     * @param message the success message
     */
    public static void printSuccess(String message) {
        System.out.println(GREEN + "  [✓] " + message + RESET);
    }

    /**
     * Prints an error message in red.
     *
     * @param message the error message
     */
    public static void printError(String message) {
        System.out.println(RED + "  [✗] " + message + RESET);
    }

    /**
     * Prints a warning message in yellow.
     *
     * @param message the warning message
     */
    public static void printWarning(String message) {
        System.out.println(YELLOW + "  [!] " + message + RESET);
    }

    /**
     * Prints an info message in cyan.
     *
     * @param message the info message
     */
    public static void printInfo(String message) {
        System.out.println(CYAN + "  [i] " + message + RESET);
    }

    /**
     * Prints a section header with styled box.
     *
     * @param title the header title
     */
    public static void printHeader(String title) {
        int width = 50;
        String border = "═".repeat(width);
        int padding = (width - title.length() - 2) / 2;
        String padStr = " ".repeat(Math.max(0, padding));

        System.out.println();
        System.out.println(BOLD + CYAN + "  ╔" + border + "╗" + RESET);
        System.out.println(BOLD + CYAN + "  ║" + padStr + " " + title
                + " " + padStr + (title.length() % 2 == 0 ? " " : "") + "║" + RESET);
        System.out.println(BOLD + CYAN + "  ╚" + border + "╝" + RESET);
        System.out.println();
    }

    /**
     * Displays a progress bar for enrollment or loading operations.
     *
     * @param current the current step number
     * @param total the total number of steps
     * @param label the label to display alongside the progress bar
     */
    public static void printProgressBar(int current, int total, String label) {
        int barWidth = 30;
        double progress = (double) current / total;
        int filled = (int) (progress * barWidth);
        int empty = barWidth - filled;

        StringBuilder bar = new StringBuilder();
        bar.append(BOLD).append("  ").append(label).append(" [");
        bar.append(GREEN);
        for (int i = 0; i < filled; i++) bar.append("█");
        bar.append(DIM);
        for (int i = 0; i < empty; i++) bar.append("░");
        bar.append(RESET).append(BOLD);
        bar.append("] ");
        bar.append(String.format("%d/%d (%d%%)", current, total, (int) (progress * 100)));
        bar.append(RESET);

        System.out.println(bar.toString());
    }

    /**
     * Prints a formatted table row with aligned columns.
     *
     * @param columns the column values
     * @param widths the width for each column
     */
    public static void printTableRow(String[] columns, int[] widths) {
        StringBuilder row = new StringBuilder("  ");
        for (int i = 0; i < columns.length && i < widths.length; i++) {
            row.append(String.format("%-" + widths[i] + "s", columns[i]));
            if (i < columns.length - 1) row.append(" │ ");
        }
        System.out.println(row.toString());
    }

    /**
     * Prints a table separator line.
     *
     * @param widths the width for each column
     */
    public static void printTableSeparator(int[] widths) {
        StringBuilder sep = new StringBuilder("  ");
        for (int i = 0; i < widths.length; i++) {
            sep.append("─".repeat(widths[i]));
            if (i < widths.length - 1) sep.append("─┼─");
        }
        System.out.println(sep.toString());
    }

    /**
     * Prints a formatted status badge (PASS/FAIL/WARN).
     *
     * @param status the status string
     * @return colored status badge
     */
    public static String statusBadge(String status) {
        switch (status.toUpperCase()) {
            case "PASS":
            case "SUCCESS":
                return GREEN + BOLD + "[PASS]" + RESET;
            case "FAIL":
            case "FAILED":
                return RED + BOLD + "[FAIL]" + RESET;
            case "WARN":
            case "WARNING":
                return YELLOW + BOLD + "[WARN]" + RESET;
            case "SKIP":
                return DIM + "[SKIP]" + RESET;
            default:
                return "[" + status + "]";
        }
    }

    /**
     * Prints the help screen with command explanations.
     */
    public static void printHelp() {
        printHeader("HELP — BIOTYPE SYSTEM");

        System.out.println(BOLD + "  MAIN MENU OPTIONS:" + RESET);
        System.out.println("  ─────────────────────────────────────────────");
        System.out.println(CYAN + "  1) Enroll New User" + RESET);
        System.out.println("     Register a new user by typing the standard");
        System.out.println("     phrase 3 times to build a biometric profile.");
        System.out.println();
        System.out.println(CYAN + "  2) User Login" + RESET);
        System.out.println("     Authenticate by matching your typing pattern");
        System.out.println("     against your enrolled profile.");
        System.out.println();
        System.out.println(CYAN + "  3) Admin Login" + RESET);
        System.out.println("     Access admin functions (user management,");
        System.out.println("     threshold adjustment, log viewing, reports).");
        System.out.println("     Default credentials: admin / admin123");
        System.out.println();
        System.out.println(CYAN + "  4) View Profiles" + RESET);
        System.out.println("     Browse enrolled user profiles and run");
        System.out.println("     quality analysis on specific profiles.");
        System.out.println();
        System.out.println(CYAN + "  5) Run Demo" + RESET);
        System.out.println("     Execute an automated demonstration showing");
        System.out.println("     all system features with synthetic data.");
        System.out.println();
        System.out.println(CYAN + "  6) Run Tests" + RESET);
        System.out.println("     Run the comprehensive test suite covering");
        System.out.println("     all system components and edge cases.");
        System.out.println();
        System.out.println(CYAN + "  7) Help" + RESET);
        System.out.println("     Display this help screen.");
        System.out.println();
        System.out.println(CYAN + "  8) Exit" + RESET);
        System.out.println("     Save configuration and exit the system.");
        System.out.println();

        System.out.println(BOLD + "  AUTHENTICATION ALGORITHM:" + RESET);
        System.out.println("  ─────────────────────────────────────────────");
        System.out.println("  1. User types the standard phrase");
        System.out.println("  2. Hold duration & flight time are measured");
        System.out.println("  3. Euclidean distance is computed vs. profile");
        System.out.println("  4. Distance is converted to similarity (0-100)");
        System.out.println("  5. If similarity >= threshold → AUTHENTICATED");
        System.out.println("  6. Impostor detection runs in parallel");
        System.out.println("  7. Threshold adapts based on outcomes");
        System.out.println();
    }

    /**
     * Prints the system startup splash with version info.
     */
    public static void printSplash() {
        System.out.println();
        printColored(BOLD + CYAN,
                "  ╔══════════════════════════════════════════════════════════════╗");
        printColored(BOLD + CYAN,
                "  ║                                                              ║");
        printColored(BOLD + GREEN,
                "  ║     ██████╗ ██╗ ██████╗ ████████╗██╗   ██╗██████╗ ███████╗   ║");
        printColored(BOLD + GREEN,
                "  ║     ██╔══██╗██║██╔═══██╗╚══██╔══╝╚██╗ ██╔╝██╔══██╗██╔════╝  ║");
        printColored(BOLD + GREEN,
                "  ║     ██████╔╝██║██║   ██║   ██║    ╚████╔╝ ██████╔╝█████╗    ║");
        printColored(BOLD + GREEN,
                "  ║     ██╔══██╗██║██║   ██║   ██║     ╚██╔╝  ██╔═══╝ ██╔══╝    ║");
        printColored(BOLD + GREEN,
                "  ║     ██████╔╝██║╚██████╔╝   ██║      ██║   ██║     ███████╗   ║");
        printColored(BOLD + GREEN,
                "  ║     ╚═════╝ ╚═╝ ╚═════╝    ╚═╝      ╚═╝   ╚═╝     ╚══════╝  ║");
        printColored(BOLD + CYAN,
                "  ║                                                              ║");
        printColored(BOLD + WHITE,
                "  ║         Keystroke Dynamics Biometric Authentication           ║");
        printColored(BOLD + YELLOW,
                "  ║              Phase 2 — Production Release                    ║");
        printColored(DIM,
                "  ║              Version " + SystemConstants.SYSTEM_VERSION
                        + " | Build " + SystemConstants.BUILD_DATE
                        + "                ║");
        printColored(BOLD + CYAN,
                "  ║                                                              ║");
        printColored(BOLD + CYAN,
                "  ╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
