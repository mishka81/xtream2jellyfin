package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.humbkr.xtream2jellyfin.constant.Constants;

import java.util.HashMap;
import java.util.Map;

@Data
public class XtreamProviderConfig {

    @JsonIgnore
    private String name;

    private String url;

    private String username;

    private String password;

    private int interval = Constants.DEFAULT_SCAN_INTERVAL;

    @JsonProperty("category_name_cleanup_patterns")
    private Map<String, String> categoryNameCleanupPatterns = new HashMap<>();

    @JsonProperty("write_metadata_json")
    private boolean writeMetadataJson = false;

    private JellyfinConfig libraryRefresh;

    private MediaSettings live;

    private MediaSettings movies;

    private MediaSettings series;

    @JsonProperty("settings")
    public void setMediaSettings(Map<String, MediaSettings> settings) {
        this.live = settings.get("live");
        this.movies = settings.get("movies");
        this.series = settings.get("series");
    }

}
