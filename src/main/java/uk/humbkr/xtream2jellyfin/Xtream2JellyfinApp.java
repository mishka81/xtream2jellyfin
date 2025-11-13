package uk.humbkr.xtream2jellyfin;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.common.Constants;
import uk.humbkr.xtream2jellyfin.common.JsonUtils;
import uk.humbkr.xtream2jellyfin.streamhandler.XtreamProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class Xtream2JellyfinApp {

    private final boolean extractOnly;

    private final boolean runOnce;

    private final boolean writeMetadataJson;

    public Xtream2JellyfinApp() {
        this.extractOnly = Boolean.parseBoolean(System.getenv(Constants.ENV_EXTRACT_ONLY));
        this.runOnce = Boolean.parseBoolean(System.getenv(Constants.ENV_RUN_ONCE));
        this.writeMetadataJson = Boolean.parseBoolean(System.getenv(Constants.ENV_WRITE_METADATA_JSON));
    }

    public static void main(String[] args) {
        new Xtream2JellyfinApp().run();
    }

    public void run() {
        log.info("Starting xtream2jellyfin");

        Map<String, Object> config = this.readConfig();
        List<Thread> threads = new ArrayList<>();

        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String providerName = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> providerConfig = (Map<String, Object>) entry.getValue();

            Thread thread = new Thread(() -> processProviderStreams(providerName, providerConfig));
            thread.setName("provider-" + providerName);
            threads.add(thread);
            thread.start();
        }

        // Wait for all provider threads (they run indefinitely with scheduled intervals)
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for provider threads", e);
            }
        }

        log.info("xtream2jellyfin stopped");
    }

    private Map<String, Object> readConfig() {
        File configFile = new File(Constants.CONFIG_FILE);
        if (configFile.exists()) {
            try {
                //noinspection unchecked
                return JsonUtils.getObjectMapper().readValue(configFile, Map.class);
            } catch (IOException e) {
                log.error("Failed to load config file", e);
            }
        }
        log.warn("Failed to load config file");
        return Map.of();
    }

    private void processProviderStreams(String providerName, Map<String, Object> config) {
        new XtreamProcessor(providerName, config, extractOnly, runOnce, writeMetadataJson).processStreams();
    }

}
