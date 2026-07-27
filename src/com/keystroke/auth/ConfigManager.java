package com.keystroke.auth;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * ConfigManager.java - Manages system configuration using a properties file.
 *
 * Configuration is stored in profiles/admin/config.properties and contains:
 *   - Authentication settings (thresholds, enrollment attempts)
 *   - Session settings (timeout, lockout)
 *   - Logging settings (retention, verbosity)
 *   - System metadata (version, build)
 *
 * Week 3 - Phase 2 Finalization
 */
public class ConfigManager {

    /** Path to the configuration file */
    private static final String CONFIG_FILE = SystemConstants.ADMIN_DIR
            + File.separator + "config.properties";

    /** The loaded properties */
    private Properties properties;

    /**
     * Constructs a ConfigManager and loads configuration from file.
     * Falls back to defaults if the file doesn't exist.
     */
    public ConfigManager() {
        this.properties = new Properties();
        loadConfig();
    }

    /**
     * Loads configuration from the properties file.
     * If the file doesn't exist, creates it with default values.
     */
    public void loadConfig() {
        Path configPath = Paths.get(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try (InputStream in = new FileInputStream(CONFIG_FILE)) {
                properties.load(in);
                System.out.println("  [✓] Configuration loaded from: " + CONFIG_FILE);
            } catch (IOException e) {
                System.out.println("  [!] Failed to load config, using defaults: " + e.getMessage());
                setDefaults();
            }
        } else {
            System.out.println("  [i] No config file found. Creating defaults...");
            setDefaults();
            saveConfig();
        }
    }

    /**
     * Saves the current configuration to the properties file.
     */
    public void saveConfig() {
        try {
            Path dirPath = Paths.get(SystemConstants.ADMIN_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
                properties.store(out, "BioType Keystroke Authentication - System Configuration");
                System.out.println("  [✓] Configuration saved to: " + CONFIG_FILE);
            }
        } catch (IOException e) {
            System.out.println("  [ERROR] Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Resets all settings to default values and saves.
     */
    public void resetToDefaults() {
        setDefaults();
        saveConfig();
        System.out.println("  [✓] Configuration reset to defaults.");
    }

    /**
     * Sets all properties to their default values.
     */
    private void setDefaults() {
        properties.setProperty("default_threshold", "60.0");
        properties.setProperty("max_enrollment_attempts", "5");
        properties.setProperty("enrollment_samples", "3");
        properties.setProperty("session_timeout_minutes", "30");
        properties.setProperty("max_failed_attempts", "3");
        properties.setProperty("log_retention_days", "90");
        properties.setProperty("hold_weight", "0.6");
        properties.setProperty("flight_weight", "0.4");
        properties.setProperty("impostor_flag_threshold", "70.0");
        properties.setProperty("min_threshold", "40.0");
        properties.setProperty("max_threshold", "80.0");
        properties.setProperty("enable_impostor_detection", "true");
        properties.setProperty("enable_adaptive_threshold", "true");
        properties.setProperty("backup_enabled", "true");
        properties.setProperty("enable_2fa", "false");
        properties.setProperty("enable_mouse_dynamics", "false");
        properties.setProperty("enable_ml_threshold", "false");
        properties.setProperty("system_version", SystemConstants.SYSTEM_VERSION);
    }

    // ==================== Getters ====================

    /**
     * Gets a string property value.
     *
     * @param key the property key
     * @param defaultValue fallback value if key is missing
     * @return the property value
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Gets a double property value.
     *
     * @param key the property key
     * @param defaultValue fallback value if key is missing or unparseable
     * @return the property value
     */
    public double getDouble(String key, double defaultValue) {
        try {
            String val = properties.getProperty(key);
            return (val != null) ? Double.parseDouble(val) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets an integer property value.
     *
     * @param key the property key
     * @param defaultValue fallback value if key is missing or unparseable
     * @return the property value
     */
    public int getInt(String key, int defaultValue) {
        try {
            String val = properties.getProperty(key);
            return (val != null) ? Integer.parseInt(val) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a boolean property value.
     *
     * @param key the property key
     * @param defaultValue fallback value if key is missing
     * @return the property value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = properties.getProperty(key);
        return (val != null) ? Boolean.parseBoolean(val) : defaultValue;
    }

    // ==================== Setters ====================

    /**
     * Sets a configuration property and saves.
     *
     * @param key the property key
     * @param value the property value
     */
    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    // ==================== Display ====================

    /**
     * Displays all current configuration settings.
     */
    public void displayConfig() {
        System.out.println("\n  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║            SYSTEM CONFIGURATION                      ║");
        System.out.println("  ╠══════════════════════════════════════════════════════╣");

        properties.stringPropertyNames().stream().sorted().forEach(key -> {
            String value = properties.getProperty(key);
            System.out.printf("  ║  %-28s : %-22s ║\n", key, value);
        });

        System.out.println("  ╚══════════════════════════════════════════════════════╝\n");
    }
}
