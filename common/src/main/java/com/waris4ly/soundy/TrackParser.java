package com.waris4ly.soundy;

import com.fasterxml.jackson.databind.JsonNode;

public class TrackParser {

    public TrackData parse(JsonNode node) {
        long id = node.get("id").asLong();
        String title = node.get("title").asText();
        String permalinkUrl = node.get("permalink_url").asText();
        long duration = node.get("duration").asLong();
        String artworkUrl = node.has("artwork_url") && !node.get("artwork_url").isNull()
                ? node.get("artwork_url").asText() : "";
        String username = node.path("user").path("username").asText("Unknown");
        boolean streamable = node.has("streamable") && node.get("streamable").asBoolean();
        return new TrackData(id, title, permalinkUrl, duration, artworkUrl, username, streamable);
    }
}
