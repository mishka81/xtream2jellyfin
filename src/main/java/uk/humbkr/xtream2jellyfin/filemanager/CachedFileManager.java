package uk.humbkr.xtream2jellyfin.filemanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.constant.Constants;
import uk.humbkr.xtream2jellyfin.util.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CachedFileManager implements FileManager {

    private final ObjectMapper objectMapper;

    private final String filePath;

    private Map<String, Map<String, String>> filesDb;

    public CachedFileManager(String providerName, String baseMediaDir) {
        this.objectMapper = JsonUtils.getObjectMapper();
        this.filePath = Constants.CACHE_DIR + "/" + providerName + "/files.json";
        this.filesDb = new HashMap<>();
    }

    @Override
    public void onProcessStart() {
        Object fileDb = FileManagerUtils.get(filePath, null);
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
    }

    @Override
    public void onProcessEnd() {
        try {
            Path path = Paths.get(filePath);
            FileManagerUtils.prepareDirectory(path.getParent().toString());

            String json = objectMapper.writeValueAsString(filesDb);
            Files.writeString(path, json, StandardCharsets.UTF_8);

            filesDb = new HashMap<>();
        } catch (IOException e) {
            log.error("Failed to update database", e);
        }
    }

    @Override
    public void save(String path, Object content, String date) {
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
