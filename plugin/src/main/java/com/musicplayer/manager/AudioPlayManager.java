package com.musicplayer.manager;

import com.musicplayer.MusicPlayerPlugin;
import java.io.File;

public class AudioPlayManager {

    private final MusicPlayerPlugin plugin;
    private boolean isPlaying = false;

    public AudioPlayManager(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean playMp3(File mp3File) {
        stopAll();
        if (!mp3File.exists()) return false;
        isPlaying = true;
        plugin.getLogger().info("Playing: " + mp3File.getName());
        return true;
    }

    public void stopAll() {
        isPlaying = false;
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}