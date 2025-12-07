package uk.humbkr.xtream2jellyfin.streamhandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import uk.humbkr.xtream2jellyfin.common.MediaType;
import uk.humbkr.xtream2jellyfin.common.XtreamAction;
import uk.humbkr.xtream2jellyfin.common.XtreamEndpoint;
import uk.humbkr.xtream2jellyfin.config.GlobalSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SeriesStreamsHandler extends BaseStreamsHandler {

    public SeriesStreamsHandler(XtreamProviderConfig providerConfig, FileManager fileManager, GlobalSettings globalSettings) {
        super(providerConfig, fileManager, globalSettings, log);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.SERIES;
    }

    @Override
    protected void processItem(Map<String, Object> stream) throws Exception {
        processSeriesStream(stream);
    }

    private String getStreamInfoPath(Map<String, Object> stream) {
        String seriesName = (String) stream.get("name");
        String categoryId = String.valueOf(stream.get("category_id"));

        String seriesCategory = categories.get(categoryId);

        seriesName = cleanNameRegex(seriesName);
        String seriesNameClean = cleanName(seriesName);

        if (!Strings.CS.equals(seriesName, seriesNameClean)) {
            logInfo("Cleaned series name: '" + seriesName + "' to '" + seriesNameClean + "'");
        }

        List<String> basePathParts = new ArrayList<>();
        basePathParts.add(mediaDir);

        if (categoryFolder) {
            basePathParts.add(seriesCategory);
        }

        basePathParts.add(seriesNameClean);
        String basePath = String.join("/", basePathParts);

        return basePath + "/" + seriesNameClean + ".json";
    }

    private void processSeriesStream(Map<String, Object> stream) {
        try {
            Object seriesIdObj = stream.get("series_id");
            String seriesId = String.valueOf(seriesIdObj);

            logDebug("Updating stream for #" + seriesId);

            Object dataResult = getData(XtreamEndpoint.PLAYER, XtreamAction.SERIES_INFO, seriesId);

            if (dataResult != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) dataResult;
                stream.putAll(dataMap);

                @SuppressWarnings("unchecked")
                Map<String, Object> streamInfo = (Map<String, Object>) stream.get("info");

                Object addedObj = streamInfo.get("last_modified");
                long addedTimestamp = Long.parseLong(String.valueOf(addedObj));
                Instant date = Instant.ofEpochSecond(addedTimestamp);

                String streamInfoPath = getStreamInfoPath(stream);

                log.debug("processing series stream: {}", streamInfoPath);

                if (writeMetadataJson) {
                    addFile(streamInfoPath, stream, date);
                }

                String basePath = StringUtils.substringBeforeLast(streamInfoPath, "/");

                @SuppressWarnings("unchecked")
                Map<String, List<Map<String, Object>>> episodesData =
                        (Map<String, List<Map<String, Object>>>) stream.get("episodes");

                if (episodesData != null) {
                    for (Map.Entry<String, List<Map<String, Object>>> seasonEntry : episodesData.entrySet()) {
                        List<Map<String, Object>> seasonData = seasonEntry.getValue();

                        for (Map<String, Object> episode : seasonData) {
                            processEpisode(basePath, episode);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            logError("Failed to process stream, Stream: " + stream + ", Error: " + ex.getMessage(), ex);
        }
    }

    private void processEpisode(String basePath, Map<String, Object> episode) {
        String seriesName = StringUtils.substringAfterLast(basePath, "/");

        try {
            Object streamIdObj = episode.get("id");
            String streamId = String.valueOf(streamIdObj);

            Object episodeNumberObj = episode.get("episode_num");
            int episodeNumber = Integer.parseInt(String.valueOf(episodeNumberObj));

            Object seasonNumberObj = episode.get("season");
            int seasonNumber = Integer.parseInt(String.valueOf(seasonNumberObj));

            String containerExtension = (String) episode.get("container_extension");

            Object addedObj = episode.get("added");
            long addedTimestamp = Long.parseLong(String.valueOf(addedObj));

            String seasonPad = String.format("%02d", seasonNumber);
            String episodeShort = String.format("%02d", episodeNumber);

            String episodeFile = String.format("%s - S%sE%s", seriesName, seasonPad, episodeShort);

            String seasonDir = "Season " + seasonPad;

            String episodeFilePath = basePath + "/" + seasonDir + "/" + episodeFile + ".strm";

            String episodeStreamUrl = buildStreamUrl(streamId, containerExtension);

            Instant date = Instant.ofEpochSecond(addedTimestamp);

            addFile(episodeFilePath, episodeStreamUrl, date);

        } catch (Exception ex) {
            logError("Failed to process series, Series: " + seriesName +
                    ", Episode: " + episode + ", Error: " + ex.getMessage(), ex);
        }
    }

}
