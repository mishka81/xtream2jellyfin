package uk.humbkr.xtream2jellyfin.streamhandler;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.humbkr.xtream2jellyfin.common.Constants;
import uk.humbkr.xtream2jellyfin.common.XtreamEndpoint;
import uk.humbkr.xtream2jellyfin.filemanager.CachedFileManager;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.filemanager.SimpleFileManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class XtreamProcessor {

    private final OkHttpClient httpClient;

    private final String providerName;

    private final String jellyfinToken;

    private final boolean postProcessingEnabled;

    private final String postProcessingUrl;

    private final int scanInterval;

    private final boolean ready;

    private final FileManager fileManager;

    private final List<BaseStreamsHandler> streamHandlers;

    private final boolean runOnce;

    public XtreamProcessor(String providerName,
                           Map<String, Object> config,
                           boolean extractOnly,
                           boolean runOnce,
                           boolean writeMetadataJson) {

        log.info("[{}] Starting", providerName);

        this.providerName = providerName;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Map<String, Object> jellyfinConfig = getJellyFinConfig(config);

        String jellyFinProtocol = (String) jellyfinConfig.get("protocol");
        String jellyFinHostname = (String) jellyfinConfig.get("hostname");
        String jellyFinPort = String.valueOf(jellyfinConfig.get("port"));

        this.jellyfinToken = (String) jellyfinConfig.get("token");
        this.postProcessingEnabled = (boolean) jellyfinConfig.getOrDefault("enabled", false);

        this.postProcessingUrl = jellyFinProtocol + "://" + jellyFinHostname + ":" + jellyFinPort;

        String username = (String) config.get(Constants.XTREAM_USERNAME);
        String password = (String) config.get(Constants.XTREAM_PASSWORD);

        String scanIntervalStr = String.valueOf(config.getOrDefault(Constants.XTREAM_SCAN_INTERVAL, Constants.DEFAULT_SCAN_INTERVAL));
        this.scanInterval = Integer.parseInt(scanIntervalStr);

        this.ready = username != null && password != null;

        String fileManagerType = System.getenv(Constants.ENV_FILE_MANAGER_TYPE);
        if (StringUtils.isBlank(fileManagerType) || "simple".equalsIgnoreCase(fileManagerType)) {
            this.fileManager = new SimpleFileManager(providerName);
            log.info("[{}] Using SimpleFileManager", providerName);
        } else {
            this.fileManager = new CachedFileManager(providerName);
            log.info("[{}] Using CachedFileManager", providerName);
        }

        Map<String, Object> appConfig = Map.of(
                "name", providerName,
                "extract_only", extractOnly,
                "write_metadata_json", writeMetadataJson
        );

        this.streamHandlers = new ArrayList<>();
        streamHandlers.add(new LiveStreamsHandler(appConfig, config, fileManager));
        streamHandlers.add(new SeriesStreamsHandler(appConfig, config, fileManager));
        streamHandlers.add(new MoviesStreamsHandler(appConfig, config, fileManager));

        this.runOnce = runOnce;
    }

    @NotNull
    private static Map<String, Object> getJellyFinConfig(Map<String, Object> config) {
        Map<String, Object> jellyfCfg = (Map<String, Object>) config.get("libraryRefresh");
        if (jellyfCfg == null) {
            jellyfCfg = Map.of();
        }
        return jellyfCfg;
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
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("X-Jellyfin-Token", jellyfinToken)
                        .post(okhttp3.RequestBody.create(new byte[0]))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        log.info("Refresh library triggered, {}", serverDetails);
                    } else {
                        log.error("Refresh library failed to trigger, {}, Error: {}",
                                serverDetails, response.code());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to trigger library refresh: {}", serverDetails, e);
            }
        }
    }

}
