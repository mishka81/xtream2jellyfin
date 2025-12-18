package uk.humbkr.xtream2jellyfin.filemanager;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Slf4j
public class SimpleFileManager extends BaseFileManager implements FileManager {

    public SimpleFileManager(String rootDir) {
        super(rootDir);
    }

    @Override
    public void initialize() {
        this.deleteDirectory(rootDir);
        log.info("Cleaned up directory: {}", rootDir);
    }

    @Override
    public void complete() {
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
                Files.walkFileTree(path,
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult postVisitDirectory(
                                    Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(
                                    Path file, BasicFileAttributes attrs)
                                    throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                log.debug("Deleted directory: {}", directoryPath);
            }
        } catch (IOException e) {
            log.error("Failed to delete directory: {}", directoryPath, e);
        }
    }

}
