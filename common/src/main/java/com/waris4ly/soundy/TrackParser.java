package com.waris4ly.soundy;

import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;

public class TrackParser {

    public TrackData parse(JsonBrowser track) {
        long id = track.get("id").asLong(0);
        String title = track.get("title").text();
        String permalinkUrl = track.get("permalink_url").text();
        long duration = track.get("duration").asLong(0);
        String artworkUrl = track.get("artwork_url").text();
        String username = track.get("user").get("username").text();
        boolean streamable = track.get("streamable").asBoolean(false);

        return new TrackData(id, title, permalinkUrl, duration,
                artworkUrl != null ? artworkUrl : "",
                username != null ? username : "Unknown",
                streamable);
    }
}
