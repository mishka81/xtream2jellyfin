package uk.humbkr.xtream2jellyfin.config;

import lombok.Data;

@Data
public class GlobalSettings {

    private boolean runOnce = false;

    private String fileManagerType = "simple";

    private String mediaDir = "media";

    private boolean writeMetadataJson = false;

}
