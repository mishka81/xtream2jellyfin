package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class MediaSettings {

    private boolean enabled;

    @JsonProperty("category_folder")
    private boolean categoryFolder = true;

    @JsonProperty("use_server_info")
    private boolean useServerInfo = false;

    @JsonProperty("name_cleanup_patterns")
    private Map<String, String> nameCleanupPatterns = new HashMap<>();

    @JsonProperty("include_category_ids")
    private List<String> includeCategoryIds = new ArrayList<>();

    @JsonProperty("exclude_category_ids")
    private List<String> excludeCategoryIds = new ArrayList<>();
}
