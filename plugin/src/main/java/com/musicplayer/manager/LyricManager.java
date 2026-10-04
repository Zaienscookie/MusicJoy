package com.musicplayer.manager;

import com.musicplayer.MusicPlayerPlugin;
import com.musicplayer.model.Song;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricManager {

    private final MusicPlayerPlugin plugin;
    private final Map<Long, String> lyrics = new TreeMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private String currentLyric = "";
    private String lastBroadcast = "";

    private static final Pattern LYRIC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");

    public LyricManager(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadLyric(String lrcContent) {
        lyrics.clear();
        currentLyric = "";
        lastBroadcast = "";

        if (lrcContent == null || lrcContent.isEmpty()) return;

        for (String line : lrcContent.split("\n")) {
            Matcher m = LYRIC_PATTERN.matcher(line.trim());
            if (m.matches()) {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                long millis = m.group(3).length() == 2 ?
                        Long.parseLong(m.group(3)) * 10 : Long.parseLong(m.group(3));
                long time = min * 60000 + sec * 1000 + millis;
                String text = m.group(4).trim();
                if (!text.isEmpty()) {
                    lyrics.put(time, text);
                }
            }
        }
    }

    public void sendProgress(Player player, Song song, int secondsElapsed) {
        long ms = secondsElapsed * 1000L;

        BossBar bar = bossBars.get(player.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar(
                    "§b" + song.getName() + " §7- §a" + song.getArtist(),
                    BarColor.BLUE, BarStyle.SOLID);
            bossBars.put(player.getUniqueId(), bar);
            bar.addPlayer(player);
        }

        double progress = song.getDuration() > 0 ?
                Math.min(1.0, (double) ms / song.getDuration()) : 0;
        bar.setProgress(progress);
        bar.setTitle("§b" + song.getName() + " §7- §a" + song.getArtist() +
                " §7[" + formatTime(ms) + "/" + song.getDurationFormatted() + "]");
    }

    public void removeBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    public void clear() {
        lyrics.clear();
        currentLyric = "";
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();
    }

    private String getLyricAt(long ms) {
        String result = "";
        long closest = 0;
        for (Map.Entry<Long, String> entry : lyrics.entrySet()) {
            if (entry.getKey() <= ms && entry.getKey() >= closest) {
                closest = entry.getKey();
                result = entry.getValue();
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String formatTime(long ms) {
        long totalSec = ms / 1000;
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60);
    }
}