package uk.humbkr.xtream2jellyfin.filemanager;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class CachedFileManager extends BaseFileManager implements FileManager {

    private final String filesDbPath;

    private Map<String, Map<String, String>> filesDb;

    // Stale file tracking
    private Set<String> trackedFiles;

    private Set<String> staleFiles;

    public CachedFileManager(String rootDir, @NonNull String cacheDir) {
        super(rootDir);
        this.filesDbPath = cacheDir + "/files.json";
        this.filesDb = new HashMap<>();
        this.trackedFiles = new HashSet<>();
        this.staleFiles = new HashSet<>();
    }

    @Override
    public void initialize() {
        // Load existing database
        Object fileDb = FileManagerUtils.get(filesDbPath, null);
        if (fileDb != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> db = (Map<String, Map<String, String>>) fileDb;
                this.filesDb = db;
            } catch (ClassCastException e) {
                log.warn("Failed to cast files db, initializing empty db", e);
                this.filesDb = new HashMap<>();
            }
        }

        // Initialize stale file tracking
        // Mark all previously known files as potentially stale
        this.staleFiles = new HashSet<>(filesDb.keySet());
        this.trackedFiles = new HashSet<>();

        log.debug("Loaded {} files from cache database, {} marked as potentially stale",
                filesDb.size(), staleFiles.size());
    }

    @Override
    public void complete() {
        // Clean up stale files first
        cleanupStaleFiles();

        // Update database to contain only files from current run
        Map<String, Map<String, String>> updatedDb = new HashMap<>();
        for (String trackedFile : trackedFiles) {
            if (filesDb.containsKey(trackedFile)) {
                updatedDb.put(trackedFile, filesDb.get(trackedFile));
            }
        }
        this.filesDb = updatedDb;

        // Save updated database
        try {
            Path path = Paths.get(filesDbPath);
            FileManagerUtils.prepareDirectory(path.getParent().toString());

            String json = objectMapper.writeValueAsString(filesDb);
            Files.writeString(path, json, StandardCharsets.UTF_8);

            log.debug("Saved {} files to cache database", filesDb.size());
        } catch (IOException e) {
            log.error("Failed to update database", e);
        }

        // Reset tracking sets for next run
        trackedFiles.clear();
        staleFiles.clear();
        filesDb = new HashMap<>();
    }

    @Override
    public void save(String path, Object content, String date) {
        // Mark file as active in current run
        trackedFiles.add(path);
        staleFiles.remove(path);

        try {
            String contentStr = objectMapper.writeValueAsString(content);
            byte[] contentBytes = contentStr.getBytes(StandardCharsets.UTF_8);
            String contentHash = md5Hash(contentBytes);

            Map<String, String> fileHistory = filesDb.getOrDefault(path, new HashMap<>());
            String itemHash = fileHistory.get("hash");

            if (!contentHash.equals(itemHash)) {
                Path filePath = Paths.get(path);
                FileManagerUtils.prepareDirectory(filePath.getParent().toString());

                String fileContent;
                if (path.endsWith(".json")) {
                    fileContent = objectMapper.writeValueAsString(content);
                } else {
                    if (content instanceof String) {
                        fileContent = (String) content;
                    } else {
                        fileContent = objectMapper.writeValueAsString(content);
                    }
                }

                log.debug("Writing file: {}", path);
                Files.writeString(filePath, fileContent, StandardCharsets.UTF_8);

                Map<String, String> metadata = new HashMap<>();
                metadata.put("hash", contentHash);
                metadata.put("added", date);
                filesDb.put(path, metadata);
            }
        } catch (IOException e) {
            log.error("Failed to save file: {}", path, e);
        }
    }

    private void cleanupStaleFiles() {
        if (staleFiles.isEmpty()) {
            return;
        }

        int deletedCount = 0;
        int failedCount = 0;

        log.info("Cleaning up {} stale files...", staleFiles.size());

        for (String stalePath : staleFiles) {
            try {
                Path file = Paths.get(stalePath);
                if (Files.exists(file)) {
                    Files.delete(file);
                    deletedCount++;
                    log.debug("Deleted stale file: {}", stalePath);
                } else {
                    log.debug("Stale file already missing: {}", stalePath);
                }
            } catch (IOException e) {
                failedCount++;
                log.warn("Failed to delete stale file: {}", stalePath, e);
            }
        }

        if (deletedCount > 0) {
            log.info("Successfully deleted {} stale files", deletedCount);
        }
        if (failedCount > 0) {
            log.warn("Failed to delete {} stale files", failedCount);
        }

        // Clean up empty directories
        cleanupEmptyDirectories();
    }

    private void cleanupEmptyDirectories() {
        try {
            Path rootPath = Paths.get(rootDir);
            if (!Files.exists(rootPath)) {
                return;
            }

            // Walk the directory tree and collect directories in reverse depth order
            // This ensures we process child directories before parent directories
            Files.walk(rootPath)
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(rootPath)) // Don't delete the root directory
                    .sorted((p1, p2) -> Integer.compare(p2.getNameCount(), p1.getNameCount()))
                    .forEach(this::deleteIfEmpty);

        } catch (IOException e) {
            log.warn("Failed to cleanup empty directories", e);
        }
    }

    private void deleteIfEmpty(Path directory) {
        try {
            // Check if directory is empty
            try (var stream = Files.list(directory)) {
                if (stream.findFirst().isEmpty()) {
                    Files.delete(directory);
                    log.debug("Deleted empty directory: {}", directory);
                }
            }
        } catch (IOException e) {
            log.debug("Could not delete directory: {}", directory, e);
        }
    }

    private String md5Hash(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not found", e);
            return "";
        }
    }

}
