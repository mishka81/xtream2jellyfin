package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AppConfig {

    @JsonProperty("app")
    private GlobalSettings globalSettings = new GlobalSettings();

    private Map<String, XtreamProviderConfig> providers = new HashMap<>();

    @JsonProperty("providers")
    public void setProviders(Map<String, XtreamProviderConfig> providers) {
        this.providers = providers;
        // Set the name for each provider based on the key
        for (Map.Entry<String, XtreamProviderConfig> entry : providers.entrySet()) {
            entry.getValue().setName(entry.getKey());
        }
    }

}
