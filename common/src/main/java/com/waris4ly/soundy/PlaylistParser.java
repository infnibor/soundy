package com.waris4ly.soundy;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class PlaylistParser {

    private final TrackParser trackParser = new TrackParser();

    public PlaylistData parse(JsonNode node) {
        long id = node.get("id").asLong();
        String title = node.get("title").asText();
        String permalinkUrl = node.get("permalink_url").asText();
        String username = node.path("user").path("username").asText("Unknown");

        List<TrackData> tracks = new ArrayList<>();
        JsonNode tracksNode = node.path("tracks");
        if (tracksNode.isArray()) {
            for (JsonNode t : tracksNode) {
                // soundcloud sometimes returns stub objects with only an id field
                if (t.has("title") && t.has("user") && t.has("duration")) {
                    tracks.add(trackParser.parse(t));
                }
            }
        }
        return new PlaylistData(id, title, permalinkUrl, username, tracks);
    }
}
