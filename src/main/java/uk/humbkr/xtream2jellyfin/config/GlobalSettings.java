package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GlobalSettings {

    @JsonProperty("run_once")
    private boolean runOnce = false;

    @JsonProperty("file_manager_type")
    private String fileManagerType = "simple";

    @JsonProperty("media_dir")
    private String mediaDir = "media";

    @JsonProperty("cache_dir")
    private String cacheDir = "cache";

    @JsonProperty("write_metadata_json")
    private boolean writeMetadataJson = false;

}
