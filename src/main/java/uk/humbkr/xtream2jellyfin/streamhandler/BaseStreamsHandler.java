package uk.humbkr.xtream2jellyfin.streamhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import uk.humbkr.xtream2jellyfin.config.GlobalSettings;
import uk.humbkr.xtream2jellyfin.config.MediaSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.constant.Constants;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.filemanager.FileManagerUtils;
import uk.humbkr.xtream2jellyfin.streamhandler.nameformat.CategoryNameFormat;
import uk.humbkr.xtream2jellyfin.streamhandler.nameformat.StreamNameFormat;
import uk.humbkr.xtream2jellyfin.util.JsonUtils;
import uk.humbkr.xtream2jellyfin.util.RegexUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public abstract class BaseStreamsHandler {

    protected final ObjectMapper objectMapper;

    protected final HttpClient httpClient;

    protected final FileManager fileManager;

    protected final String providerName;

    protected final String username;

    protected final String password;

    protected final Map<String, String> categoryNameCleanupPatterns;

    protected final Map<String, String> nameCleanupPatterns;

    protected final List<String> includeCategoryIds;

    protected final List<String> excludeCategoryIds;

    protected final boolean categoryFolder;

    protected final boolean writeMetadataJson;

    protected final boolean useServerInfo;

    protected final boolean enabled;

    protected final String cacheDir;

    protected final String mediaDir;

    protected final Map<String, XtreamAction> resolvers;

    protected final boolean useCache = Constants.USE_CACHE;

    protected final StreamNameFormat movieNameFormat;

    protected final StreamNameFormat seriesNameFormat;

    protected final CategoryNameFormat categoryNameFormat;

    private final Logger log;

    protected String providerUrl;

    protected Map<String, Object> data;

    protected Map<String, String> categories;

    protected int processNumber = 0;

    protected int streamsCount = 0;

    protected int processedCount = 0;

    protected int streamsSkipped = 0;

    protected long processingStartTime = 0;

    public BaseStreamsHandler(XtreamProviderConfig providerConfig, FileManager fileManager,
                              GlobalSettings globalSettings, Logger log) {
        this.log = log;
        this.objectMapper = JsonUtils.getObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        this.fileManager = fileManager;
        this.providerName = Objects.requireNonNull(providerConfig.getName());
        this.writeMetadataJson = globalSettings.isWriteMetadataJson();

        this.username = providerConfig.getUsername();
        this.password = providerConfig.getPassword();
        this.providerUrl = providerConfig.getUrl();
        this.categoryNameCleanupPatterns = providerConfig.getCategoryNameCleanupPatterns();

        this.cacheDir = Constants.CACHE_DIR + "/" + providerName;

        String baseMediaDir = globalSettings.getMediaDir();
        this.mediaDir = baseMediaDir + "/" + providerName + "/" + getMediaType();

        MediaSettings mediaSettings = getMediaSettingsForType(providerConfig);

        this.useServerInfo = mediaSettings.isUseServerInfo();
        this.nameCleanupPatterns = mediaSettings.getNameCleanupPatterns();
        this.includeCategoryIds = mediaSettings.getIncludeCategoryIds();
        this.excludeCategoryIds = mediaSettings.getExcludeCategoryIds();
        this.categoryFolder = mediaSettings.isCategoryFolder();
        this.enabled = mediaSettings.isEnabled();

        this.resolvers = Constants.MEDIA_RESOLVERS.get(getMediaType());
        this.data = new HashMap<>();
        this.categories = new HashMap<>();

        // Initialize name formatters with default templates
        String movieTemplate = "${name} (${year}) [${externalProviderId}-${externalId}]";
        String seriesTemplate = "${name} (${year}) [${externalProviderId}-${externalId}]";

        this.movieNameFormat = new StreamNameFormat(movieTemplate, mediaSettings.getNameCleanupPatterns());
        this.seriesNameFormat = new StreamNameFormat(seriesTemplate, mediaSettings.getNameCleanupPatterns());
        this.categoryNameFormat = new CategoryNameFormat(this.categoryNameCleanupPatterns);
    }

    private MediaSettings getMediaSettingsForType(XtreamProviderConfig config) {
        MediaType type = getMediaType();
        return switch (type) {
            case LIVE -> config.getLive();
            case MOVIE -> config.getMovies();
            case SERIES -> config.getSeries();
        };
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

            logInfo(String.format("Complete processing, Total: %d, Processed: %d, Skipped: %d, Duration: %.3f seconds",
                    streamsCount, processedCount, streamsSkipped, executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to process: " + ex.getMessage(), ex);
        }
    }

    protected void processStreams() {

        // Get reference to streams list before removing from data map
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allStreams = (List<Map<String, Object>>) data.remove(Constants.MEDIA_RESOLVER_STREAMS);

        // Count total streams
        int totalStreamsCount = allStreams.size();

        logInfo("Total streams available: " + totalStreamsCount);

        resetCounters(totalStreamsCount);

        // Process streams one at a time, allowing GC to collect each stream after processing
        Iterator<Map<String, Object>> streamsIterator = allStreams.iterator();
        while (streamsIterator.hasNext()) {
            Map<String, Object> stream = streamsIterator.next();
            Object streamName = stream.get("name");
            if (!canProcess(stream)) {
                logDebug("Skipping stream: " + streamName);
                streamsSkipped++;
            } else {
                try {
                    processItem(stream);
                } catch (Exception ex) {
                    logError("Failed to process " + getMediaType() + " stream, ID: " + streamName + ", Error: " + ex.getMessage(), ex);
                }
                updateCounters();
            }
            streamsIterator.remove();
        }

        // Clear the entire list to release all references
        allStreams.clear();
    }

    protected boolean canProcess(Map<String, Object> streamInfo) {
        String streamName = (String) streamInfo.get("name");
        String categoryId = String.valueOf(streamInfo.get("category_id"));

        if (streamName == null) {
            return false;
        }

        // If include_category_ids is set, it takes precedence over exclude_category_ids
        if (!includeCategoryIds.isEmpty()) {
            return includeCategoryIds.contains(categoryId);
        }

        // Otherwise, use exclude_category_ids logic
        return !excludeCategoryIds.contains(categoryId);
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

            logInfo(String.format("Loaded %d lists, Duration: %.3f seconds", allDataPoints.size(), executionTime / 1000.0));

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

                categoryName = cleanCategoryName(categoryName);

                categories.put(categoryId, capitalize(categoryName));
            }

            long executionTime = System.currentTimeMillis() - startTime;

            logDebug("Categories loaded: " + categories);

            logInfo(String.format("Loaded %d categories, Duration: %.3f seconds", getCategories().size(), executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to load categories: " + ex.getMessage(), ex);
        }
    }

    protected String cleanCategoryName(String categoryName) {
        if (StringUtils.isBlank(categoryName)) {
            return categoryName;
        }
        String cleanedCategoryName = categoryNameFormat.format(categoryName);
        if (!categoryName.equals(cleanedCategoryName)) {
            logDebug(String.format("Cleaned category name: '%s' -> '%s'", categoryName, cleanedCategoryName));
        }
        return cleanedCategoryName;
    }

    protected String cleanNameRegex(String text) {
        if (text != null) {
            for (Map.Entry<String, String> entry : nameCleanupPatterns.entrySet()) {
                String regexFind = entry.getKey();
                String regexReplace = entry.getValue();

                text = RegexUtils.replaceAll(text, regexFind, regexReplace);
            }
            text = text.trim();
        }
        return text;
    }

    protected String buildUrl(XtreamEndpoint endpoint, XtreamAction action, String contextId) {
        String credentials = "username" + "=" + username + "&" +
                "password" + "=" + password;
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
                    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(30))
                            .GET();

                    for (Map.Entry<String, String> header : Constants.HEADERS.entrySet()) {
                        requestBuilder.header(header.getKey(), header.getValue());
                    }

                    HttpResponse<String> response = httpClient.send(
                            requestBuilder.build(),
                            HttpResponse.BodyHandlers.ofString()
                    );

                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        String responseBody = response.body();

                        if (endpoint.isJson()) {
                            result = objectMapper.readValue(responseBody, Object.class);
                        } else {
                            result = responseBody;
                        }
                        break;
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
        this.streamsSkipped = 0;
    }

    protected void updateCounters() {
        long executionTime = System.currentTimeMillis() - processingStartTime;

        processedCount++;

        int totalHandled = processedCount + streamsSkipped;
        double progress = (double) totalHandled / streamsCount;
        double progressLeftRatio = 1.0 / progress;
        double expectedDuration = progressLeftRatio * executionTime;
        double timeLeft = expectedDuration - executionTime;

        logInfo(String.format("Progress: %d / %d (%.1f%%), Processed: %d, Skipped: %d, Estimated time left: %.2f seconds",
                totalHandled, streamsCount, progress * 100, processedCount, streamsSkipped, timeLeft / 1000.0));
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

    protected void logTrace(String message) {
        log.trace("[{}::{}] {}", providerName, getMediaType(), message);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    protected String extractYear(Map<String, Object> streamData) {
        if (streamData == null) {
            return null;
        }

        // Try multiple field names
        String[] yearFields = {"year", "releaseDate", "release_year", "release_date"};

        for (String field : yearFields) {
            Object value = streamData.get(field);
            if (value != null) {
                String yearStr = value.toString();
                // Extract year from date strings like "2024-12-18" or just "2024"
                if (RegexUtils.matches(yearStr, "\\d{4}.*")) {
                    return yearStr.substring(0, 4);
                }
            }
        }

        return null;
    }

    protected String extractTmdbId(Map<String, Object> streamData) {
        if (streamData == null) {
            return null;
        }

        // Try multiple field names
        String[] tmdbFields = {"tmdb_id", "tmdb", "tmdbId", "movie_data.tmdb_id", "info.tmdb_id"};

        for (String field : tmdbFields) {
            Object value = getNestedValue(streamData, field);
            if (value != null) {
                return value.toString();
            }
        }

        return null;
    }

    protected String extractImdbId(Map<String, Object> streamData) {
        if (streamData == null) {
            return null;
        }

        // Try multiple field names
        String[] imdbFields = {"imdb_id", "imdb", "imdbId", "movie_data.imdb_id", "info.imdb_id"};

        for (String field : imdbFields) {
            Object value = getNestedValue(streamData, field);
            if (value != null) {
                String imdbStr = value.toString();
                // Ensure IMDB ID format (tt followed by numbers)
                if (RegexUtils.matches(imdbStr, "tt\\d+")) {
                    return imdbStr;
                }
                // If it's just numbers, prepend "tt"
                if (RegexUtils.matches(imdbStr, "\\d+")) {
                    return "tt" + imdbStr;
                }
                return imdbStr;
            }
        }

        return null;
    }

    protected String extractTvdbId(Map<String, Object> streamData) {
        if (streamData == null) {
            return null;
        }

        // Try multiple field names
        String[] tvdbFields = {"tvdb_id", "tvdb", "tvdbId", "series_data.tvdb_id", "info.tvdb_id"};

        for (String field : tvdbFields) {
            Object value = getNestedValue(streamData, field);
            if (value != null) {
                return value.toString();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String key) {
        if (key.contains(".")) {
            String[] parts = RegexUtils.split(key, "\\.");
            Object current = map;

            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                    if (current == null) {
                        return null;
                    }
                } else {
                    return null;
                }
            }

            return current;
        } else {
            return map.get(key);
        }
    }

}
