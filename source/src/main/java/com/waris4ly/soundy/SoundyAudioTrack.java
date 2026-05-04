package com.waris4ly.soundy;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

import java.net.URI;

public class SoundyAudioTrack extends DelegatedAudioTrack {

    private final SoundySourceManager sourceManager;

    // fetched once per play session, not on every seek
    private volatile StreamData cachedStream;

    public SoundyAudioTrack(SoundyAudioTrackInfo trackInfo, SoundySourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        SoundyAudioTrackInfo info = (SoundyAudioTrackInfo) trackInfo;

        if (cachedStream == null) {
            cachedStream = sourceManager.getService().getStream(info.getTrackId());
        }

        try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
            try (PersistentHttpStream httpStream = new PersistentHttpStream(
                    httpInterface, new URI(cachedStream.getUrl()), Long.MAX_VALUE)) {
                processDelegate(new Mp3AudioTrack(trackInfo, httpStream), executor);
            }
        }
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new SoundyAudioTrack((SoundyAudioTrackInfo) trackInfo, sourceManager);
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }
}
