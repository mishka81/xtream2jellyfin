package uk.humbkr.xtream2jellyfin.config;

import lombok.Data;

@Data
public class JellyfinConfig {

    private boolean enabled;

    private String protocol;

    private String hostname;

    private int port;

    private String token;
}
