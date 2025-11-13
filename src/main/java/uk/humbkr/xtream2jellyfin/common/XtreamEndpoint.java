package uk.humbkr.xtream2jellyfin.common;

public enum XtreamEndpoint {
    PLAYER("player_api"),
    EPG("xmltv");

    private final String value;

    XtreamEndpoint(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
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
