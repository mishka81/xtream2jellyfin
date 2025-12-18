package uk.humbkr.xtream2jellyfin.streamhandler;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.GlobalSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;

import java.time.Instant;
import java.util.*;

@Slf4j
public class LiveStreamsHandler extends BaseStreamsHandler {

    private String epgData;

    public LiveStreamsHandler(XtreamProviderConfig providerConfig, FileManager fileManager, GlobalSettings globalSettings) {
        super(providerConfig, fileManager, globalSettings, log);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.LIVE;
    }

    @Override
    protected void processStreams() {
        try {
            long startTime = System.currentTimeMillis();
            logInfo("Loading live streams");

            List<String> liveStreamsData = new ArrayList<>();
            liveStreamsData.add("#EXTM3U");

            for (Map<String, Object> liveStream : getStreams()) {
                boolean canProcess = canProcess(liveStream);

                if (canProcess) {
                    List<String> lines = processLiveStream(liveStream);

                    if (lines != null) {
                        liveStreamsData.addAll(lines);
                    }
                }
            }

            String m3uContent = String.join("\r\n", liveStreamsData);

            Instant date = Instant.now();

            addFile(mediaDir + "/live.m3u", m3uContent, date);
            addFile(mediaDir + "/epg.xml", epgData, date);

            long executionTime = System.currentTimeMillis() - startTime;

            logInfo(String.format("Processed live streams [%d], Duration: %.3f seconds",
                    getStreams().size(), executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to load live streams: " + ex.getMessage(), ex);
        }
    }

    private List<String> processLiveStream(Map<String, Object> liveStream) {
        try {
            String channelName = (String) liveStream.get("name");
            String channelCategoryId = String.valueOf(liveStream.get("category_id"));
            String channelGroup = categories.get(channelCategoryId);

            channelName = cleanNameRegex(channelName);

            String channelUniqueId = (String) liveStream.get("epg_channel_id");
            Object channelNumberObj = liveStream.get("stream_id");
            String channelNumber = String.valueOf(channelNumberObj);
            String channelLogo = (String) liveStream.get("stream_icon");

            String streamType = (String) liveStream.getOrDefault("stream_type", MediaType.LIVE.toString());

            String streamUrl = buildStreamUrl(channelNumber, "m3u8");

            Map<String, String> tagsInfo = new LinkedHashMap<>();
            tagsInfo.put("name", channelName);
            tagsInfo.put("id", channelUniqueId);
            tagsInfo.put("logo", channelLogo);
            tagsInfo.put("type", streamType);

            List<String> tags = new ArrayList<>();
            for (Map.Entry<String, String> entry : tagsInfo.entrySet()) {
                if (entry.getValue() != null) {
                    tags.add(String.format("tvg-%s=\"%s\"", entry.getKey(), entry.getValue()));
                }
            }

            if (channelGroup != null) {
                tags.add(String.format("group-title=\"%s\"", channelGroup));
                tags.add(String.format("tag-group=\"%s\"", channelGroup));
            }

            String extinf = "#EXTINF:-1," + String.join(" ", tags) + "," + channelName;

            return Arrays.asList(extinf, streamUrl);

        } catch (Exception ex) {
            logError("Failed to load stream lines, Data: " + liveStream + ", Error: " + ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    protected List<Object[]> getDataPoints() {
        List<Object[]> args = super.getDataPoints();
        args.add(new Object[]{XtreamEndpoint.EPG, XtreamEndpoint.EPG.toString(), null});
        return args;
    }

    @Override
    protected void extraDataLoading(XtreamEndpoint endpoint, Object data) {
        if (endpoint == XtreamEndpoint.EPG) {
            this.epgData = (String) data;
        }
    }

}
