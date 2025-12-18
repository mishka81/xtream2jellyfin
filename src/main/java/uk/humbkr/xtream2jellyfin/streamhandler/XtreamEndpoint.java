package uk.humbkr.xtream2jellyfin.streamhandler;

import lombok.Getter;

@Getter
public enum XtreamEndpoint {
    PLAYER("player_api"),
    EPG("xmltv");

    private final String value;

    XtreamEndpoint(String value) {
        this.value = value;
    }

    public boolean isJson() {
        return this == PLAYER;
    }

    public String getExt() {
        return isJson() ? "json" : "xml";
    }

    @Override
    public String toString() {
        return value;
    }
}
