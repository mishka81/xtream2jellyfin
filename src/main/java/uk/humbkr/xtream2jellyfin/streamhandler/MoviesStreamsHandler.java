package uk.humbkr.xtream2jellyfin.streamhandler;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.GlobalSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.streamhandler.nameformat.StreamNameFormatContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class MoviesStreamsHandler extends BaseStreamsHandler {

    public MoviesStreamsHandler(XtreamProviderConfig providerConfig,
                                FileManager fileManager,
                                GlobalSettings globalSettings) {
        super(providerConfig, fileManager, globalSettings, log);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.MOVIE;
    }

    @Override
    protected void processItem(Map<String, Object> stream) throws Exception {
        processMovieStream(stream);
    }

    private void processMovieStream(Map<String, Object> movieStream) {
        String movieName = (String) movieStream.get("name");
        Object addedObj = movieStream.get("added");
        String categoryId = String.valueOf(movieStream.get("category_id"));

        Object movieIdObj = movieStream.get("stream_id");
        String movieId = String.valueOf(movieIdObj);
        String containerExtension = (String) movieStream.get("container_extension");
        String movieCategory = categories.get(categoryId);

        // Format movie name with Jellyfin-compatible naming
        String tmdbId = extractTmdbId(movieStream);
        String imdbId = extractImdbId(movieStream);

        // Prefer TMDB ID for movies, fallback to IMDB
        String externalProviderId = null;
        String externalId = null;
        if (tmdbId != null && !tmdbId.isEmpty()) {
            externalProviderId = "tmdbid";
            externalId = tmdbId;
        } else if (imdbId != null && !imdbId.isEmpty()) {
            externalProviderId = "imdbid";
            externalId = imdbId;
        }

        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year(extractYear(movieStream))
                .externalProviderId(externalProviderId)
                .externalId(externalId)
                .build();

        String movieNameClean = movieNameFormat.format(movieName, context);

        List<String> baseFilePathParts = new ArrayList<>();
        baseFilePathParts.add(mediaDir + "s");

        if (categoryFolder) {
            baseFilePathParts.add(movieCategory);
        }

        baseFilePathParts.add(movieNameClean);
        baseFilePathParts.add(movieNameClean);

        String baseFilePath = String.join("/", baseFilePathParts);

        String streamFile = baseFilePath + ".strm";
        String streamUrl = buildStreamUrl(movieId, containerExtension);

        String streamDataFile = baseFilePath + ".json";

        long addedTimestamp = Long.parseLong(String.valueOf(addedObj));
        Instant date = Instant.ofEpochSecond(addedTimestamp);

        addFile(streamFile, streamUrl, date);
        if (writeMetadataJson) {
            addFile(streamDataFile, movieStream, date);
        }
    }

}
