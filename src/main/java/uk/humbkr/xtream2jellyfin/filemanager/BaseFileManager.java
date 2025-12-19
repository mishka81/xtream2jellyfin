package uk.humbkr.xtream2jellyfin.filemanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import uk.humbkr.xtream2jellyfin.common.JsonUtils;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseFileManager {

    protected final ObjectMapper objectMapper = JsonUtils.initializeJsonMapper();

    @NonNull
    protected final String rootDir;

}
