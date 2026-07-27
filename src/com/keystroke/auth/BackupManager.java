package com.keystroke.auth;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.*;

/**
 * BackupManager.java - Manages system backups and restoration.
 *
 * Creates compressed ZIP backups of all profiles, logs, thresholds, and admin config.
 * Backup files are named: keystroke_backup_YYYY_MM_DD.zip
 *
 * Supports:
 *   - Full system backup (profiles, logs, thresholds, admin settings)
 *   - Restoration from backup archive
 *   - Listing available backups
 *
 * Week 3 - Phase 2 Finalization
 */
public class BackupManager {

    /** Date format for backup file names */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy_MM_dd");

    /**
     * Creates a full system backup as a ZIP archive.
     * Includes: profiles/users/, profiles/thresholds/, profiles/logs/, profiles/admin/
     *
     * @return the path to the created backup file, or null on failure
     */
    public String createSystemBackup() {
        MenuSystem.printHeader("CREATING SYSTEM BACKUP");

        try {
            // Ensure backup directory exists
            Path backupDir = Paths.get(SystemConstants.BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            // Build backup filename
            String today = LocalDate.now().format(DATE_FORMAT);
            String backupName = SystemConstants.BACKUP_PREFIX + today + ".zip";
            String backupPath = SystemConstants.BACKUP_DIR + File.separator + backupName;

            // Create ZIP file
            Path sourceDir = Paths.get(SystemConstants.DATA_DIR);
            if (!Files.exists(sourceDir)) {
                MenuSystem.printWarning("No data directory found. Nothing to backup.");
                return null;
            }

            try (ZipOutputStream zos = new ZipOutputStream(
                    new FileOutputStream(backupPath))) {

                final int[] fileCount = {0};

                Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                        // Skip backup files themselves
                        if (file.toString().contains("backups")) {
                            return FileVisitResult.CONTINUE;
                        }

                        String entryName = sourceDir.relativize(file).toString()
                                .replace("\\", "/");
                        zos.putNextEntry(new ZipEntry(entryName));

                        Files.copy(file, zos);
                        zos.closeEntry();
                        fileCount[0]++;

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        if (dir.toString().contains("backups")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        String entryName = sourceDir.relativize(dir).toString()
                                .replace("\\", "/");
                        if (!entryName.isEmpty()) {
                            zos.putNextEntry(new ZipEntry(entryName + "/"));
                            zos.closeEntry();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

                MenuSystem.printSuccess("Backup created: " + backupPath);
                System.out.printf("  Files archived: %d\n", fileCount[0]);
                System.out.printf("  Backup size: %d bytes\n",
                        Files.size(Paths.get(backupPath)));

                return backupPath;
            }

        } catch (IOException e) {
            MenuSystem.printError("Backup failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Restores the system from a backup ZIP archive.
     * WARNING: This overwrites all current profiles, logs, and settings.
     *
     * @param backupFilePath the path to the backup ZIP file
     * @return true if restoration was successful
     */
    public boolean restoreFromBackup(String backupFilePath) {
        MenuSystem.printHeader("RESTORING FROM BACKUP");

        Path backupPath = Paths.get(backupFilePath);
        if (!Files.exists(backupPath)) {
            MenuSystem.printError("Backup file not found: " + backupFilePath);
            return false;
        }

        try {
            Path targetDir = Paths.get(SystemConstants.DATA_DIR);
            int fileCount = 0;

            try (ZipInputStream zis = new ZipInputStream(
                    new FileInputStream(backupFilePath))) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path outputPath = targetDir.resolve(entry.getName());

                    // Security: prevent zip-slip attacks
                    if (!outputPath.normalize().startsWith(targetDir.normalize())) {
                        MenuSystem.printWarning("Skipping suspicious entry: " + entry.getName());
                        continue;
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(outputPath);
                    } else {
                        Files.createDirectories(outputPath.getParent());
                        try (OutputStream os = new FileOutputStream(outputPath.toFile())) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }
                        }
                        fileCount++;
                    }

                    zis.closeEntry();
                }
            }

            MenuSystem.printSuccess("Restoration complete.");
            System.out.printf("  Files restored: %d\n", fileCount);
            return true;

        } catch (IOException e) {
            MenuSystem.printError("Restore failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lists all available backup files in the backup directory.
     */
    public void listBackups() {
        MenuSystem.printHeader("AVAILABLE BACKUPS");

        Path backupDir = Paths.get(SystemConstants.BACKUP_DIR);
        if (!Files.exists(backupDir)) {
            MenuSystem.printInfo("No backups found.");
            return;
        }

        File dir = backupDir.toFile();
        File[] backups = dir.listFiles((d, name) ->
                name.startsWith(SystemConstants.BACKUP_PREFIX) && name.endsWith(".zip"));

        if (backups == null || backups.length == 0) {
            MenuSystem.printInfo("No backups found.");
            return;
        }

        int[] widths = {5, 35, 12};
        MenuSystem.printTableRow(new String[]{"#", "Filename", "Size"}, widths);
        MenuSystem.printTableSeparator(widths);

        int num = 1;
        for (File backup : backups) {
            MenuSystem.printTableRow(new String[]{
                    String.valueOf(num++),
                    backup.getName(),
                    formatSize(backup.length())
            }, widths);
        }

        System.out.printf("\n  Total backups: %d\n\n", backups.length);
    }

    /**
     * Formats a file size in bytes to a human-readable string.
     *
     * @param bytes the file size in bytes
     * @return formatted string (e.g., "1.5 KB", "2.3 MB")
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
