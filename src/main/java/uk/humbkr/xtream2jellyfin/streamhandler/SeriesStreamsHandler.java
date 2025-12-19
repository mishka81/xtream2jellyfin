package uk.humbkr.xtream2jellyfin.streamhandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.humbkr.xtream2jellyfin.config.GlobalSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.metadata.NfoGenerator;
import uk.humbkr.xtream2jellyfin.nameformat.StreamNameFormatContext;

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

        // Format series name with Jellyfin-compatible naming
        String tmdbId = extractTmdbId(stream);
        String tvdbId = extractTvdbId(stream);

        // Prefer TVDB ID for series, fallback to TMDB
        String externalProviderId = null;
        String externalId = null;
        if (tvdbId != null && !tvdbId.isEmpty()) {
            externalProviderId = "tvdbid";
            externalId = tvdbId;
        } else if (tmdbId != null && !tmdbId.isEmpty()) {
            externalProviderId = "tmdbid";
            externalId = tmdbId;
        }

        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year(extractYear(stream))
                .externalProviderId(externalProviderId)
                .externalId(externalId)
                .build();

        String seriesNameClean = seriesNameFormat.format(seriesName, context);

        if (!seriesName.equals(seriesNameClean)) {
            logDebug("Cleaned series name: '" + seriesName + "' to '" + seriesNameClean + "'");
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

                logDebug("processing series stream: " + streamInfoPath);

                if (writeMetadataJson) {
                    addFile(streamInfoPath, stream, date);
                }

                // Generate and write tvshow.nfo
                String basePath = StringUtils.substringBeforeLast(streamInfoPath, "/");
                if (writeMetadataNfo) {
                    String nfoPath = basePath + "/tvshow.nfo";
                    String nfoContent = NfoGenerator.generateTvShowNfo(stream);
                    if (nfoContent != null) {
                        addFile(nfoPath, nfoContent, date);
                    }
                }

                @SuppressWarnings("unchecked")
                Map<String, List<Map<String, Object>>> episodesData = (Map<String, List<Map<String, Object>>>) stream.get("episodes");

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

            // Generate and write episode NFO
            if (writeMetadataNfo) {
                String episodeNfoPath = basePath + "/" + seasonDir + "/" + episodeFile + ".nfo";
                String episodeNfoContent = NfoGenerator.generateEpisodeNfo(episode);
                if (episodeNfoContent != null) {
                    addFile(episodeNfoPath, episodeNfoContent, date);
                }
            }

        } catch (Exception ex) {
            logError("Failed to process series, Series: " + seriesName +
                    ", Episode: " + episode + ", Error: " + ex.getMessage(), ex);
        }
    }

}
