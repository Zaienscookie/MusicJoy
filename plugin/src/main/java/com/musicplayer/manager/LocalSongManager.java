package com.musicplayer.manager;

import com.musicplayer.MusicPlayerPlugin;
import com.musicplayer.model.Song;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class LocalSongManager {

    private final MusicPlayerPlugin plugin;
    private final File musicDir;
    private final File dataFile;
    private FileConfiguration data;
    private final List<LocalSong> localSongs = new ArrayList<>();
    private long nextId = 1;

    public LocalSongManager(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
        this.musicDir = new File(plugin.getDataFolder(), "music");
        this.dataFile = new File(plugin.getDataFolder(), "localsongs.yml");
        if (!musicDir.exists()) musicDir.mkdirs();
        loadSongs();
        autoScan();
    }

    public void autoScan() {
        File[] files = musicDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        if (files == null) return;

        for (File f : files) {
            String name = f.getName().replaceAll("\\.mp3$", "");
            if (getLocalSongByName(name) == null) {
                long id = nextId++;
                String artist = "未知";
                String[] parts = name.split(" - ", 2);
                if (parts.length == 2) {
                    name = parts[1].trim();
                    artist = parts[0].trim();
                }
                localSongs.add(new LocalSong(id, name, artist, f.getAbsolutePath(), f.lastModified(), "auto"));
                plugin.getLogger().info("自动扫描到本地歌曲: " + name + " - " + artist);
            }
        }
        saveSongs();
    }

    private LocalSong getLocalSongByName(String name) {
        for (LocalSong s : localSongs) {
            if (s.name.equals(name) || new File(s.filePath).getName().replaceAll("\\.mp3$", "").equals(name)) {
                return s;
            }
        }
        return null;
    }

    public boolean uploadSong(Player uploader, String name, String artist) {
        File uploaded = new File(musicDir, "uploading_" + uploader.getUniqueId() + ".tmp");
        if (!uploaded.exists()) return false;

        long id = nextId++;
        String fileName = id + "_" + sanitize(name) + ".mp3";
        File target = new File(musicDir, fileName);

        try {
            Files.move(uploaded.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LocalSong song = new LocalSong(id, name, artist, target.getAbsolutePath(), System.currentTimeMillis(), uploader.getName());
            localSongs.add(song);
            saveSongs();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveUploadedFile(Player player, byte[] data) {
        File tmp = new File(musicDir, "uploading_" + player.getUniqueId() + ".tmp");
        try {
            Files.write(tmp.toPath(), data);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void cleanupTemp(Player player) {
        File tmp = new File(musicDir, "uploading_" + player.getUniqueId() + ".tmp");
        if (tmp.exists()) tmp.delete();
    }

    public List<LocalSong> getLocalSongs() {
        return new ArrayList<>(localSongs);
    }

    public LocalSong getLocalSong(long id) {
        for (LocalSong s : localSongs) {
            if (s.id == id) return s;
        }
        return null;
    }

    public boolean deleteLocalSong(long id) {
        LocalSong song = getLocalSong(id);
        if (song == null) return false;
        File file = new File(song.filePath);
        if (file.exists()) file.delete();
        localSongs.remove(song);
        saveSongs();
        return true;
    }

    public Song toBukkitSong(LocalSong local) {
        return new Song(local.id, local.name, local.artist, "本地音乐",
                readMp3DurationMs(new File(local.filePath)));
    }

    public File getMusicFile(LocalSong local) {
        File f = new File(local.filePath);
        return f.exists() ? f : null;
    }

    public static long readMp3DurationMs(File f) {
        if (f == null || !f.exists()) return 0;
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r")) {
            long fileLen = f.length();
            if (fileLen < 10) return 0;
            int maxScan = (int) Math.min(fileLen, 1024 * 1024);
            byte[] buf = new byte[maxScan];
            raf.readFully(buf);

            long totalSamples = 0;
            boolean synced = false;
            int i = 0;
            while (i < maxScan - 4) {
                int b0 = buf[i] & 0xFF;
                int b1 = buf[i+1] & 0xFF;
                if (b0 != 0xFF || (b1 & 0xE0) != 0xE0) { i++; continue; }
                int b2 = buf[i+2] & 0xFF;
                int b3 = buf[i+3] & 0xFF;
                // MPEG version bits: 11=MPEG1, 10=MPEG2, 00=MPEG2.5
                int ver = (b1 >> 3) & 0x03;
                int layer = (b1 >> 1) & 0x03;
                int bitrateIdx = (b2 >> 4) & 0x0F;
                int sampleIdx = (b2 >> 2) & 0x03;
                if (layer != 1 && layer != 2 && layer != 3) { i++; continue; }
                if (bitrateIdx == 0 || bitrateIdx == 15) { i++; continue; }
                if (sampleIdx == 3) { i++; continue; }

                int[] bitrates;
                if (ver == 3) {
                    bitrates = new int[]{0,32,40,48,56,64,80,96,112,128,160,192,224,256,320,0};
                } else {
                    bitrates = new int[]{0,8,16,24,32,40,48,56,64,80,96,112,128,144,160,0};
                }
                int bitrateKbps = bitrates[bitrateIdx];

                int[] sampleRates;
                if (ver == 3) sampleRates = new int[]{44100,48000,32000};
                else if (ver == 2) sampleRates = new int[]{22050,24000,16000};
                else sampleRates = new int[]{11025,12000,8000};
                int sampleRate = sampleRates[sampleIdx];

                int samplesPerFrame = (ver == 3) ? 1152 : 576;

                // CBR estimate: duration = totalBytes*8 / bitrate(bps)
                long bytes = fileLen - i;
                long seconds = bytes * 8L / (bitrateKbps * 1000L);
                return seconds * 1000L;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadSongs() {
        if (!dataFile.exists()) return;
        data = YamlConfiguration.loadConfiguration(dataFile);
        nextId = data.getLong("next-id", 1);
        org.bukkit.configuration.ConfigurationSection section = data.getConfigurationSection("songs");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String path = "songs." + key;
            long id = data.getLong(path + ".id");
            String name = data.getString(path + ".name", "未知");
            String artist = data.getString(path + ".artist", "未知");
            String filePath = data.getString(path + ".file", "");
            long time = data.getLong(path + ".time", 0);
            String uploader = data.getString(path + ".uploader", "");
            if (new File(filePath).exists()) {
                localSongs.add(new LocalSong(id, name, artist, filePath, time, uploader));
            }
        }
    }

    private void saveSongs() {
        if (data == null) data = new YamlConfiguration();
        data.set("next-id", nextId);
        data.set("songs", null);
        for (LocalSong s : localSongs) {
            String path = "songs." + s.id;
            data.set(path + ".id", s.id);
            data.set(path + ".name", s.name);
            data.set(path + ".artist", s.artist);
            data.set(path + ".file", s.filePath);
            data.set(path + ".time", s.uploadTime);
            data.set(path + ".uploader", s.uploader);
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static class LocalSong {
        public final long id;
        public final String name;
        public final String artist;
        public final String filePath;
        public final long uploadTime;
        public final String uploader;

        public LocalSong(long id, String name, String artist, String filePath, long uploadTime, String uploader) {
            this.id = id;
            this.name = name;
            this.artist = artist;
            this.filePath = filePath;
            this.uploadTime = uploadTime;
            this.uploader = uploader;
        }
    }
}