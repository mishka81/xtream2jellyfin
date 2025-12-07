package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.humbkr.xtream2jellyfin.common.Constants;

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

    @JsonProperty("category_name_regex")
    private Map<String, String> categoryNameRegex = new HashMap<>();

    @JsonProperty("write_metadata_json")
    private boolean writeMetadataJson = false;

    private JellyfinConfig libraryRefresh;

    private MediaSettings live;

    private MediaSettings movie;

    private MediaSettings series;

    @JsonProperty("settings")
    public void setMediaSettings(Map<String, MediaSettings> settings) {
        this.live = settings.get("live");
        this.movie = settings.get("movie");
        this.series = settings.get("series");
    }

}
