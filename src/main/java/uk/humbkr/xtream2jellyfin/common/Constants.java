package uk.humbkr.xtream2jellyfin.common;

import uk.humbkr.xtream2jellyfin.streamhandler.MediaType;
import uk.humbkr.xtream2jellyfin.streamhandler.XtreamAction;

import java.util.Map;

public class Constants {

    // Configuration
    public static final String CONFIG_FILE = "config/config.yaml";

    public static final String LOGBACK_CONFIG_FILE = "config/logback.xml";

    // Directories
    public static final String CACHE_DIR = "cache";

    public static final String MEDIA_DIR = "media";

    // Scan Settings
    public static final int DEFAULT_SCAN_INTERVAL = 360; // 6 hours in minutes

    public static final boolean USE_CACHE = false;

    // Context Parameters
    public static final Map<XtreamAction, String> CONTEXT_PARAMETER = Map.of(
            XtreamAction.SERIES_INFO, "series_id",
            XtreamAction.VOD_INFO, "vod_id",
            XtreamAction.EPG_INFO, "stream_id"
    );

    // Media Resolver Keys
    public static final String MEDIA_RESOLVER_CATEGORIES = "categories";

    public static final String MEDIA_RESOLVER_STREAMS = "streams";

    public static final String MEDIA_RESOLVER_INFO = "info";

    // Load Data Actions
    public static final String[] LOAD_DATA_ACTIONS = {
            MEDIA_RESOLVER_STREAMS,
            MEDIA_RESOLVER_CATEGORIES
    };

    // Media Resolvers
    public static final Map<MediaType, Map<String, XtreamAction>> MEDIA_RESOLVERS = Map.of(
            MediaType.LIVE, Map.of(
                    MEDIA_RESOLVER_STREAMS, XtreamAction.LIVE_STREAMS,
                    MEDIA_RESOLVER_CATEGORIES, XtreamAction.LIVE_CATEGORIES,
                    MEDIA_RESOLVER_INFO, XtreamAction.EPG_INFO
            ),
            MediaType.MOVIE, Map.of(
                    MEDIA_RESOLVER_STREAMS, XtreamAction.VOD_STREAMS,
                    MEDIA_RESOLVER_CATEGORIES, XtreamAction.VOD_CATEGORIES,
                    MEDIA_RESOLVER_INFO, XtreamAction.VOD_INFO
            ),
            MediaType.SERIES, Map.of(
                    MEDIA_RESOLVER_STREAMS, XtreamAction.SERIES_STREAMS,
                    MEDIA_RESOLVER_CATEGORIES, XtreamAction.SERIES_CATEGORIES,
                    MEDIA_RESOLVER_INFO, XtreamAction.SERIES_INFO
            )
    );

    // HTTP Headers
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/117.0.0.0 Safari/537.36";

    public static final Map<String, String> HEADERS = Map.of(
            "Upgrade-Insecure-Requests", "1",
            "User-Agent", DEFAULT_USER_AGENT
    );

    private Constants() {
        // Utility class
    }

}
