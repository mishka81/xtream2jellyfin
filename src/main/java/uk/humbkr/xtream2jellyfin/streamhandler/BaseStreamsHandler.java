package uk.humbkr.xtream2jellyfin.streamhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import uk.humbkr.xtream2jellyfin.common.*;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.filemanager.FileManagerUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class BaseStreamsHandler {

    protected final ObjectMapper objectMapper;

    protected final OkHttpClient httpClient;

    protected final FileManager fileManager;

    protected final String providerName;

    protected final String username;

    protected final String password;

    protected final Map<String, String> categoryNameRegex;

    protected final Map<String, String> nameRegex;

    protected final List<String> excludeCategories;

    protected final boolean categoryFolder;

    protected final boolean extractOnly;

    protected final boolean writeMetadataJson;

    protected final boolean useServerInfo;

    protected final boolean enabled;

    protected final String cacheDir;

    protected final String mediaDir;

    protected final Map<String, XtreamAction> resolvers;

    protected final boolean useCache = Constants.USE_CACHE;

    private final Logger log;

    protected String providerUrl;

    protected Map<String, Object> data;

    protected Map<String, String> categories;

    protected int processNumber = 0;

    protected int streamsCount = 0;

    protected int processedCount = 0;

    protected long processingStartTime = 0;

    @SuppressWarnings("unchecked")
    public BaseStreamsHandler(Map<String, Object> appConfig, Map<String, Object> providerConfig, FileManager fileManager,
                              Logger log) {
        this.log = log;
        this.objectMapper = JsonUtils.getObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        this.fileManager = fileManager;
        this.extractOnly = (boolean) appConfig.getOrDefault("extract_only", false);
        this.writeMetadataJson = (boolean) appConfig.getOrDefault("write_metadata_json", false);
        this.providerName = (String) appConfig.get("name");

        this.username = (String) providerConfig.get(Constants.XTREAM_USERNAME);
        this.password = (String) providerConfig.get(Constants.XTREAM_PASSWORD);
        this.providerUrl = (String) providerConfig.get(Constants.XTREAM_URL);
        this.categoryNameRegex = (Map<String, String>) providerConfig.getOrDefault("category_name_regex", new HashMap<>());

        this.cacheDir = Constants.CACHE_DIR + "/" + providerName;

        String baseMediaDir = System.getenv(Constants.ENV_MEDIA_DIR);
        if (baseMediaDir == null || baseMediaDir.isEmpty()) {
            baseMediaDir = Constants.MEDIA_DIR;
        }
        this.mediaDir = baseMediaDir + "/" + providerName + "/" + getMediaType();

        Map<String, Object> settings = (Map<String, Object>) providerConfig.get(Constants.XTREAM_SETTINGS);
        Map<String, Object> mediaSettings = (Map<String, Object>) settings.get(getMediaType().toString());

        this.useServerInfo = (boolean) mediaSettings.getOrDefault("use_server_info", false);
        this.nameRegex = (Map<String, String>) mediaSettings.getOrDefault("name_regex", new HashMap<>());
        this.excludeCategories = (List<String>) mediaSettings.getOrDefault("exclude_categories", new ArrayList<>());
        this.categoryFolder = (boolean) mediaSettings.getOrDefault("category_folder", true);
        this.enabled = (boolean) mediaSettings.getOrDefault("enabled", false);

        this.resolvers = Constants.MEDIA_RESOLVERS.get(getMediaType());
        this.data = new HashMap<>();
        this.categories = new HashMap<>();
    }

    public abstract MediaType getMediaType();

    protected void processItem(Map<String, Object> stream) throws Exception {
        // To be overridden by subclasses
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getStreams() {
        return (List<Map<String, Object>>) data.get(Constants.MEDIA_RESOLVER_STREAMS);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getCategories() {
        return (List<Map<String, Object>>) data.get(Constants.MEDIA_RESOLVER_CATEGORIES);
    }

    public void setProviderUrl(Map<String, Object> serverInfo) {
        String url = (String) serverInfo.get("url");
        String serverProtocol = (String) serverInfo.get("server_protocol");
        this.providerUrl = serverProtocol + "://" + url;
    }

    public void process() {
        try {
            processNumber++;
            processingStartTime = System.currentTimeMillis();

            loadData();
            loadCategories();

            logInfo("Loading streams");

            processStreams();

            long executionTime = System.currentTimeMillis() - processingStartTime;

            data.clear();
            categories.clear();

            logInfo(String.format("Complete processing, Duration: %.3f seconds", executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to process: " + ex.getMessage(), ex);
        }
    }

    protected void processStreams() {

        // Get reference to streams list before removing from data map
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allStreams = (List<Map<String, Object>>) data.remove(Constants.MEDIA_RESOLVER_STREAMS);

        // Count processable streams first
        int processableStreamsCount = allStreams.size();

        logInfo("Available streams for processing: " + processableStreamsCount);

        resetCounters(processableStreamsCount);

        // Process streams one at a time, allowing GC to collect each stream after processing
        Iterator<Map<String, Object>> streamsIterator = allStreams.iterator();
        while (streamsIterator.hasNext()) {
            Map<String, Object> stream = streamsIterator.next();

            if (!canProcess(stream)) {
                logInfo("Skipping stream: " + stream);
                continue;
            }

            try {
                processItem(stream);
            } catch (Exception ex) {
                logError("Failed to process " + getMediaType() + " stream, ID: " + stream + ", Error: " + ex.getMessage(), ex);
            }

            updateCounters();

            streamsIterator.remove();
        }

        // Clear the entire list to release all references
        allStreams.clear();
    }

    protected boolean canProcess(Map<String, Object> streamInfo) {
        String streamName = (String) streamInfo.get("name");
        String categoryId = String.valueOf(streamInfo.get("category_id"));

        boolean isExcluded = excludeCategories.contains(categoryId);
        return streamName != null && !isExcluded;
    }

    protected void loadData() {
        try {
            long startTime = System.currentTimeMillis();

            data = new HashMap<>();
            categories = new HashMap<>();

            List<Object[]> allDataPoints = getDataPoints();

            for (Object[] dataPoints : allDataPoints) {
                XtreamEndpoint endpoint = (XtreamEndpoint) dataPoints[0];
                String dataPoint = (String) dataPoints[1];
                XtreamAction action = (XtreamAction) dataPoints[2];

                loadDataPoint(endpoint, dataPoint, action);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            logInfo(String.format("Loaded %d lists, Duration: %.3f seconds",
                    allDataPoints.size(), executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to load data: " + ex.getMessage(), ex);
        }
    }

    protected List<Object[]> getDataPoints() {
        List<Object[]> args = new ArrayList<>();

        for (String resolverAction : Constants.LOAD_DATA_ACTIONS) {
            XtreamAction action = resolvers.get(resolverAction);
            if (action != null) {
                args.add(new Object[]{XtreamEndpoint.PLAYER, resolverAction, action});
            }
        }

        return args;
    }

    protected void loadDataPoint(XtreamEndpoint endpoint, String dataPoint, XtreamAction action) {
        try {
            logDebug("Load endpoint data, Endpoint: " + endpoint);

            Object dataResult = getData(endpoint, action, null);

            if (dataResult != null) {
                if (endpoint == XtreamEndpoint.PLAYER) {
                    data.put(dataPoint, dataResult);
                }

                extraDataLoading(endpoint, dataResult);

                logInfo("Endpoint '" + endpoint + "' data loaded, Action: " + dataPoint);
            }

        } catch (Exception ex) {
            logError("Failed to load endpoint data, Endpoint: " + endpoint + ", Error: " + ex.getMessage(), ex);
        }
    }

    protected void extraDataLoading(XtreamEndpoint endpoint, Object data) {
        // To be overridden by subclasses
    }

    protected void loadCategories() {
        try {
            long startTime = System.currentTimeMillis();
            logInfo("Loading categories");

            for (Map<String, Object> item : getCategories()) {
                String categoryId = String.valueOf(item.get("category_id"));
                String categoryName = (String) item.get("category_name");

                String cleanName = cleanCategoryNameRegex(categoryName);
                cleanName = cleanName(cleanName);

                categories.put(categoryId, capitalize(cleanName));
            }

            long executionTime = System.currentTimeMillis() - startTime;

            logInfo(String.format("Loaded %d categories, Duration: %.3f seconds",
                    getCategories().size(), executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to load categories: " + ex.getMessage(), ex);
        }
    }

    protected String cleanName(String title) {
        if (title != null) {
            for (Map.Entry<String, String> entry : Constants.CLEAN_CHARS.entrySet()) {
                if (title.contains(entry.getKey())) {
                    title = title.replace(entry.getKey(), entry.getValue());
                }
            }
        }
        return title;
    }

    protected String cleanCategoryNameRegex(String text) {
        if (text != null) {
            for (Map.Entry<String, String> entry : categoryNameRegex.entrySet()) {
                String regexFind = entry.getKey();
                String regexReplace = entry.getValue();

                String textNew = text.replaceAll(regexFind, regexReplace);
                textNew = textNew.trim();

                if (!text.equals(textNew)) {
                    text = textNew;
                }
            }
        }
        return text;
    }

    protected String cleanNameRegex(String text) {
        if (text != null) {
            for (Map.Entry<String, String> entry : nameRegex.entrySet()) {
                String regexFind = entry.getKey();
                String regexReplace = entry.getValue();

                text = text.replaceAll(regexFind, regexReplace);
            }
            text = text.trim();
        }
        return text;
    }

    protected String buildUrl(XtreamEndpoint endpoint, XtreamAction action, String contextId) {
        String credentials = Constants.XTREAM_USERNAME + "=" + username + "&" +
                Constants.XTREAM_PASSWORD + "=" + password;
        String url = providerUrl + "/" + endpoint + ".php?" + credentials;

        if (action != null && endpoint == XtreamEndpoint.PLAYER) {
            url += "&action=" + action;

            if (contextId != null) {
                String contextKey = Constants.CONTEXT_PARAMETER.get(action);
                if (contextKey != null) {
                    url += "&" + contextKey + "=" + contextId;
                }
            }
        }

        logDebug("Built URL: " + url);

        return url;
    }

    public Object getData(XtreamEndpoint endpoint, XtreamAction action, String contextId) {
        Object result = null;
        String path = getCachePath(endpoint, action, contextId);

        if (useCache) {
            result = FileManagerUtils.get(path, null);
        }

        if (result == null) {
            String url = buildUrl(endpoint, action, contextId);

            log.debug("Fetching data from URL: {}", url);

            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    Request.Builder requestBuilder = new Request.Builder().url(url);
                    for (Map.Entry<String, String> header : Constants.HEADERS.entrySet()) {
                        requestBuilder.addHeader(header.getKey(), header.getValue());
                    }

                    try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();

                            if (endpoint.isJson()) {
                                result = objectMapper.readValue(responseBody, Object.class);
                            } else {
                                result = responseBody;
                            }
                            break;
                        }
                    }

                    Thread.sleep(1000);
                } catch (IOException | InterruptedException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    logError("Attempt " + (attempt + 1) + " failed: " + e.getMessage(), e);
                }
            }

            if (result != null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (useCache && result != null) {
            String date = Instant.now().toString();
            fileManager.save(path, result, date);
        }

        return result;
    }

    protected String getCachePath(XtreamEndpoint endpoint, XtreamAction action, String contextId) {
        List<String> parts = new ArrayList<>();
        parts.add(endpoint.toString());
        if (action != null) parts.add(action.toString());
        if (contextId != null) parts.add(contextId);
        parts.add(endpoint.getExt());

        String fileName = String.join(".", parts);
        return cacheDir + "/" + fileName;
    }

    protected String buildStreamUrl(String streamId, String ext) {
        return providerUrl + "/" + getMediaType() + "/" + username + "/" + password + "/" + streamId + "." + ext;
    }

    protected void addFile(String filePath, Object content, Instant date) {
        fileManager.save(filePath, content, date.toString());
    }

    protected void resetCounters(int streams) {
        this.streamsCount = streams;
        this.processedCount = 0;
    }

    protected void updateCounters() {
        long executionTime = System.currentTimeMillis() - processingStartTime;

        processedCount++;

        double progress = (double) processedCount / streamsCount;
        double progressLeftRatio = 1.0 / progress;
        double expectedDuration = progressLeftRatio * executionTime;
        double timeLeft = expectedDuration - executionTime;

        logInfo(String.format("Processed %d / %d (%.1f%%), Estimated time left: %.2f seconds",
                processedCount, streamsCount, progress * 100, timeLeft / 1000.0));
    }

    protected void logError(String message, Exception ex) {
        log.error("[{}::{}] {}", providerName, getMediaType(), message, ex);
    }

    protected void logWarning(String message) {
        log.warn("[{}::{}] {}", providerName, getMediaType(), message);
    }

    protected void logInfo(String message) {
        log.info("[{}::{}] {}", providerName, getMediaType(), message);
    }

    protected void logDebug(String message) {
        log.debug("[{}::{}] {}", providerName, getMediaType(), message);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
