package uk.humbkr.xtream2jellyfin.filemanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.util.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@Slf4j
public class SimpleFileManager implements FileManager {

    private final ObjectMapper objectMapper;

    private final String mediaPath;

    public SimpleFileManager(String providerName, String mediaDir) {
        this.objectMapper = JsonUtils.getObjectMapper();
        this.mediaPath = mediaDir + "/" + providerName;
    }

    @Override
    public void onProcessStart() {
        deleteDirectory(mediaPath);
        log.info("Cleaned up library directory: {}", mediaPath);
    }

    @Override
    public void onProcessEnd() {
        // No-op: SimpleFileManager doesn't maintain a database
    }

    @Override
    public void save(String path, Object content, String date) {
        try {
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

        } catch (IOException e) {
            log.error("Failed to save file: {}", path, e);
        }
    }

    private void deleteDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (Files.exists(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    paths.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException e) {
                                    log.error("Failed to delete: {}", p, e);
                                }
                            });
                }
                log.debug("Deleted directory: {}", directoryPath);
            }
        } catch (IOException e) {
            log.error("Failed to delete directory: {}", directoryPath, e);
        }
    }

}
