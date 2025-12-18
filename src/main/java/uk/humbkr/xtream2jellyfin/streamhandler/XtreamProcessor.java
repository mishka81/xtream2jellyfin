package uk.humbkr.xtream2jellyfin.streamhandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.humbkr.xtream2jellyfin.config.GlobalSettings;
import uk.humbkr.xtream2jellyfin.config.JellyfinConfig;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.CachedFileManager;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.filemanager.SimpleFileManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class XtreamProcessor {

    private final HttpClient httpClient;

    private final String providerName;

    private final String jellyfinToken;

    private final boolean postProcessingEnabled;

    private final String postProcessingUrl;

    private final int scanInterval;

    private final boolean ready;

    private final FileManager fileManager;

    private final List<BaseStreamsHandler> streamHandlers;

    private final boolean runOnce;

    public XtreamProcessor(XtreamProviderConfig config, GlobalSettings globalSettings) {

        this.providerName = config.getName();

        log.info("[{}] Starting", providerName);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        JellyfinConfig jellyfinConfig = config.getLibraryRefresh();

        if (jellyfinConfig != null) {
            this.jellyfinToken = jellyfinConfig.getToken();
            this.postProcessingEnabled = jellyfinConfig.isEnabled();
            this.postProcessingUrl = jellyfinConfig.getProtocol() + "://" +
                    jellyfinConfig.getHostname() + ":" + jellyfinConfig.getPort();
        } else {
            this.jellyfinToken = null;
            this.postProcessingEnabled = false;
            this.postProcessingUrl = null;
        }

        String username = config.getUsername();
        String password = config.getPassword();

        this.scanInterval = config.getInterval();

        this.ready = username != null && password != null;

        String fileManagerType = globalSettings.getFileManagerType();
        if (StringUtils.isBlank(fileManagerType) || "simple".equalsIgnoreCase(fileManagerType)) {
            this.fileManager = new SimpleFileManager(providerName, globalSettings.getMediaDir());
            log.info("[{}] Using SimpleFileManager", providerName);
        } else {
            this.fileManager = new CachedFileManager(providerName, globalSettings.getMediaDir());
            log.info("[{}] Using CachedFileManager", providerName);
        }

        this.streamHandlers = new ArrayList<>();
        streamHandlers.add(new LiveStreamsHandler(config, fileManager, globalSettings));
        streamHandlers.add(new SeriesStreamsHandler(config, fileManager, globalSettings));
        streamHandlers.add(new MoviesStreamsHandler(config, fileManager, globalSettings));

        this.runOnce = globalSettings.isRunOnce();
    }

    public void processStreams() {
        if (!ready) {
            log.error("[{}] Failed to run, please set credentials", providerName);
            return;
        }
        log.info("[{}] Processing", providerName);

        do {
            try {
                authenticate();
                fileManager.onProcessStart();

                List<BaseStreamsHandler> handlers = streamHandlers.stream()
                        .filter(handler -> handler.enabled)
                        .toList();

                for (BaseStreamsHandler streamHandler : handlers) {
                    streamHandler.process();
                }

                fileManager.onProcessEnd();
                postProcessing();

                log.info("{} processing completed", providerName);

            } catch (Exception ex) {
                if (ex.getMessage().contains("Authentication failed")) {
                    log.error("Failed to start processing, Provider: {}, Error: Invalid Credentials", providerName);
                } else {
                    log.error("Failed to start processing, Provider: {}, Error: {}", providerName, ex.getMessage(), ex);
                }
            }

            if (!runOnce) {
                waitForNextIteration();
            }
        } while (!runOnce);
    }

    @SuppressWarnings("unchecked")
    private void authenticate() throws Exception {
        boolean isAuth = false;

        BaseStreamsHandler firstHandler = streamHandlers.get(0);
        Object dataResult = firstHandler.getData(XtreamEndpoint.PLAYER, null, null);

        if (dataResult != null) {
            Map<String, Object> data = (Map<String, Object>) dataResult;
            Map<String, Object> serverInfo = (Map<String, Object>) data.get("server_info");
            Map<String, Object> userInfo = (Map<String, Object>) data.get("user_info");

            int authenticated = ((Number) userInfo.get("auth")).intValue();
            String status = (String) userInfo.get("status");

            isAuth = authenticated == 1 && "Active".equals(status);

            if (isAuth) {
                for (BaseStreamsHandler streamHandler : streamHandlers) {
                    streamHandler.setProviderUrl(serverInfo);
                }
            }
        }

        if (!isAuth) {
            throw new Exception("Authentication failed");
        }
    }

    private void waitForNextIteration() {
        long now = System.currentTimeMillis();
        long nextInterval = 60L * scanInterval * 1000;
        long nextIteration = now + nextInterval;

        LocalDateTime nextIterationTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(nextIteration),
                ZoneId.systemDefault()
        );

        String nextIterationIso = nextIterationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        log.info("Next iteration for {} at {}", providerName, nextIterationIso);

        try {
            Thread.sleep(nextInterval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for next iteration", e);
        }
    }

    private void postProcessing() {
        if (postProcessingEnabled) {
            String url = postProcessingUrl + "/Library/Refresh";
            String serverDetails = "Jellyfin Server: " + postProcessingUrl;

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("X-Jellyfin-Token", jellyfinToken)
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.info("Refresh library triggered, {}", serverDetails);
                } else {
                    log.error("Refresh library failed to trigger, {}, Error: {}",
                            serverDetails, response.statusCode());
                }
            } catch (Exception e) {
                log.error("Failed to trigger library refresh: {}", serverDetails, e);
            }
        }
    }

}
