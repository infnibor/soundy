package com.waris4ly.soundy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SoundCloudService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SoundCloudHttpClient client;
    private final TrackParser trackParser = new TrackParser();
    private final PlaylistParser playlistParser = new PlaylistParser();

    public SoundCloudService(SoundCloudHttpClient client) {
        this.client = client;
    }

    // returns TrackData or PlaylistData depending on what the URL points to
    public Object resolveUrl(String url) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(url, StandardCharsets.UTF_8);
        String json = client.get("/resolve?url=" + encoded);
        JsonNode node = MAPPER.readTree(json);
        String kind = node.path("kind").asText();
        return switch (kind) {
            case "track" -> trackParser.parse(node);
            case "playlist" -> playlistParser.parse(node);
            default -> throw new IOException("Unsupported resource kind '" + kind + "' for: " + url);
        };
    }

    public List<TrackData> search(String query) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String json = client.get("/search/tracks?q=" + encoded + "&limit=10");
        JsonNode root = MAPPER.readTree(json);

        List<TrackData> results = new ArrayList<>();
        JsonNode collection = root.path("collection");
        if (collection.isArray()) {
            for (JsonNode node : collection) {
                results.add(trackParser.parse(node));
            }
        }
        return results;
    }

    // prefers progressive MP3 for seeking support, falls back to HLS
    // skips snipped transcodings which are preview only clips
    public StreamData getStream(long trackId) throws IOException, InterruptedException {
        String json = client.get("/tracks/" + trackId);
        JsonNode track = MAPPER.readTree(json);
        JsonNode transcodings = track.path("media").path("transcodings");

        if (!transcodings.isArray() || transcodings.isEmpty()) {
            throw new IOException("No transcodings found for track: " + trackId);
        }

        String progressiveUrl = null;
        String hlsUrl = null;

        for (JsonNode t : transcodings) {
            if (t.path("snipped").asBoolean(false)) continue;

            String protocol = t.path("format").path("protocol").asText();
            String mime = t.path("format").path("mime_type").asText();
            String url = t.path("url").asText();

            if ("progressive".equals(protocol) && mime.contains("mpeg")) {
                progressiveUrl = url;
                break;
            }
            if ("hls".equals(protocol) && mime.contains("mpeg") && hlsUrl == null) {
                hlsUrl = url;
            }
        }

        if (progressiveUrl != null) {
            return new StreamData(resolveStreamUrl(progressiveUrl), StreamData.Protocol.PROGRESSIVE);
        }
        if (hlsUrl != null) {
            return new StreamData(resolveStreamUrl(hlsUrl), StreamData.Protocol.HLS);
        }

        throw new IOException("No playable stream found for track: " + trackId);
    }

    // hits the transcoding URL to get the signed CDN URL
    private String resolveStreamUrl(String transcodingUrl) throws IOException, InterruptedException {
        String json = client.getAbsolute(transcodingUrl);
        JsonNode node = MAPPER.readTree(json);
        String url = node.path("url").asText(null);
        if (url == null || url.isEmpty()) {
            throw new IOException("Empty stream URL from: " + transcodingUrl);
        }
        return url;
    }
}
