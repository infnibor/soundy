package com.waris4ly.soundy;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.arbjerg.lavalink.api.AudioPlayerManagerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SoundyPlugin implements AudioPlayerManagerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SoundyPlugin.class);

    private final PluginConfig config;

    public SoundyPlugin(PluginConfig config) {
        this.config = config;
    }

    @Override
    public AudioPlayerManager configure(AudioPlayerManager manager) {
        if (!config.isEnabled()) {
            log.info("soundy is disabled");
            return manager;
        }
        log.info("registering SoundySourceManager");
        manager.registerSourceManager(new SoundySourceManager());
        return manager;
    }
}
