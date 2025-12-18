package uk.humbkr.xtream2jellyfin.streamhandler;

import lombok.Getter;

@Getter
public enum XtreamAction {

    AUTHENTICATE(""),
    LIVE_CATEGORIES("get_live_categories"),
    LIVE_STREAMS("get_live_streams"),
    SERIES_CATEGORIES("get_series_categories"),
    SERIES_STREAMS("get_series"),
    VOD_CATEGORIES("get_vod_categories"),
    VOD_STREAMS("get_vod_streams"),
    SERIES_INFO("get_series_info"),
    VOD_INFO("get_vod_info"),
    EPG_INFO("get_short_epg");

    private final String value;

    XtreamAction(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
