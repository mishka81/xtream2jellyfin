package uk.humbkr.xtream2jellyfin;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.AppConfig;
import uk.humbkr.xtream2jellyfin.config.GlobalSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.constant.Constants;
import uk.humbkr.xtream2jellyfin.streamhandler.XtreamProcessor;
import uk.humbkr.xtream2jellyfin.util.YamlUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Xtream2JellyfinApp {

    public static void main(String[] args) {
        new Xtream2JellyfinApp().run();
    }

    public void run() {
        log.info("Starting xtream2jellyfin");

        AppConfig appConfig = this.readConfig();
        List<Thread> threads = new ArrayList<>();

        for (XtreamProviderConfig providerConfig : appConfig.getProviders().values()) {
            String providerName = providerConfig.getName();

            Thread thread = new Thread(() -> processProviderStreams(providerConfig, appConfig.getGlobalSettings()));
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

    private AppConfig readConfig() {
        File configFile = new File(Constants.CONFIG_FILE);
        if (configFile.exists()) {
            try {
                return YamlUtils.getObjectMapper().readValue(configFile, AppConfig.class);
            } catch (IOException e) {
                log.error("Failed to load config file: {}", e.getMessage());
                log.debug("", e);
            }
        }
        log.error("Config file not found: {}", Constants.CONFIG_FILE);
        return new AppConfig();
    }

    private void processProviderStreams(XtreamProviderConfig config, GlobalSettings globalSettings) {
        new XtreamProcessor(config, globalSettings).processStreams();
    }

}
