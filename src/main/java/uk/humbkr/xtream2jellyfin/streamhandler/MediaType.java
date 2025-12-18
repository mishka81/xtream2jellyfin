package uk.humbkr.xtream2jellyfin.streamhandler;

public enum MediaType {
    LIVE("live"),
    SERIES("series"),
    MOVIE("movie");

    private final String value;

    MediaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
