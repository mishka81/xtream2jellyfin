package uk.humbkr.xtream2jellyfin.streamhandler;

import uk.humbkr.xtream2jellyfin.common.MediaType;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoviesStreamsHandler extends BaseStreamsHandler {

    public MoviesStreamsHandler(Map<String, Object> appConfig,
                                Map<String, Object> providerConfig,
                                FileManager fileManager) {
        super(appConfig, providerConfig, fileManager);
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

        movieName = cleanNameRegex(movieName);

        Object movieIdObj = movieStream.get("stream_id");
        String movieId = String.valueOf(movieIdObj);
        String containerExtension = (String) movieStream.get("container_extension");
        String movieCategory = categories.get(categoryId);

        String movieNameClean = cleanName(movieName);

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
