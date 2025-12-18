package uk.humbkr.xtream2jellyfin.streamhandler.nameformat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StreamNameFormatContext {

    private final String year;

    private final String externalProviderId;

    private final String externalId;

}
