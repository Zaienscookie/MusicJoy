package com.musicplayer.manager;

import com.musicplayer.MusicPlayerPlugin;
import com.musicplayer.model.Song;
import com.musicplayer.api.NeteaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MusicManager {

    private final MusicPlayerPlugin plugin;
    private final NeteaseAPI api;

    private final Queue<Song> queue = new ConcurrentLinkedQueue<>();
    private Song currentSong;
    private boolean playing = false;
    private boolean loop = false;
    private boolean loopSingle = false;
    private int volume = 50;
    private int taskId = -1;

    private final List<Song> searchResults = new ArrayList<>();
    private final Map<String, Long> playerCooldown = new HashMap<>();

    public MusicManager(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
        this.api = plugin.getNeteaseAPI();
        this.volume = plugin.getConfig().getInt("music.default-volume", 50);
    }

    public void search(String keyword, int limit) {
        searchResults.clear();
        List<Song> songs = api.searchSongs(keyword, limit);
        searchResults.addAll(songs);
    }

    public List<Song> getSearchResults() {
        return new ArrayList<>(searchResults);
    }

    public boolean play(Song song) {
        if (song == null) return false;
        String url = api.getSongUrl(song.getId());
        if (url == null || url.isEmpty()) return false;

        stop();
        currentSong = song;
        playing = true;

        writeCurrentId(String.valueOf(song.getId()));

        Bukkit.broadcastMessage("§a[音乐] §f正在播放: §e" + song.getName() + " §7- §b" + song.getArtist());
        Bukkit.broadcastMessage("§a[音乐] §7时长: " + song.getDurationFormatted());

        downloadAndPlay(url, song);
        return true;
    }

    public boolean playLocal(LocalSongManager.LocalSong local) {
        if (local == null) return false;
        File file = plugin.getLocalSongManager().getMusicFile(local);
        if (file == null) return false;

        stop();
        Song song = plugin.getLocalSongManager().toBukkitSong(local);
        currentSong = song;
        playing = true;

        writeCurrentId("local_" + local.id);

        Bukkit.broadcastMessage("§a[音乐] §f正在播放本地歌曲: §e" + song.getName() + " §7- §b" + song.getArtist());
        if (song.getDuration() > 0) {
            Bukkit.broadcastMessage("§a[音乐] §7时长: " + song.getDurationFormatted());
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getAudioSender().sendToAllPlayers(file, song);
        });

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            int seconds = 0;
            @Override
            public void run() {
                if (!playing) {
                    if (taskId != -1) {
                        Bukkit.getScheduler().cancelTask(taskId);
                        taskId = -1;
                    }
                    return;
                }
                if (plugin.getConfig().getBoolean("settings.bossbar-enable", true)) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        plugin.getLyricManager().sendProgress(p, song, seconds);
                    }
                }
                seconds += 1;
                long totalSec = song.getDuration() / 1000;
                if (totalSec > 0 && seconds >= totalSec) {
                    if (taskId != -1) {
                        Bukkit.getScheduler().cancelTask(taskId);
                        taskId = -1;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (loopSingle) playLocal(local);
                        else skip();
                    });
                }
            }
        }, 0L, 20L);
        return true;
    }

    public boolean playById(long songId) {
        Song song = api.getSongDetail(songId);
        if (song == null) return false;
        return play(song);
    }

    public boolean playFromSearch(int index) {
        if (index < 0 || index >= searchResults.size()) return false;
        return play(searchResults.get(index));
    }

    public void stop() {
        playing = false;
        currentSong = null;
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        plugin.getLyricManager().clear();
        plugin.getAudioSender().stopAll();
    }

    public void skip() {
        if (!queue.isEmpty()) {
            Song next = queue.poll();
            play(next);
        } else if (loop) {
            if (currentSong != null) {
                play(currentSong);
            }
        } else {
            stop();
            Bukkit.broadcastMessage("§a[音乐] §7队列已空, 停止播放");
        }
    }

    public void addToQueue(Song song) {
        queue.offer(song);
        Bukkit.broadcastMessage("§a[音乐] §7已添加到队列: §e" + song.getName());
    }

    public void addToQueueById(long songId) {
        Song song = api.getSongDetail(songId);
        if (song != null) {
            addToQueue(song);
        }
    }

    public void addToQueueFromSearch(int index) {
        if (index >= 0 && index < searchResults.size()) {
            addToQueue(searchResults.get(index));
        }
    }

    public List<Song> getQueue() {
        return new ArrayList<>(queue);
    }

    public void clearQueue() {
        queue.clear();
        Bukkit.broadcastMessage("§a[音乐] §7队列已清空");
    }

    public void removeFromQueue(int index) {
        List<Song> list = new ArrayList<>(queue);
        if (index >= 0 && index < list.size()) {
            queue.clear();
            for (int i = 0; i < list.size(); i++) {
                if (i != index) queue.offer(list.get(i));
            }
            Bukkit.broadcastMessage("§a[音乐] §7已移除队列第 " + (index + 1) + " 首");
        }
    }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        Bukkit.broadcastMessage("§a[音乐] §7音量已设置为: §e" + this.volume + "%");
    }

    public int getVolume() {
        return volume;
    }

    public void toggleLoop() {
        loop = !loop;
        loopSingle = false;
        Bukkit.broadcastMessage("§a[音乐] §7循环播放: " + (loop ? "§a开启" : "§c关闭"));
    }

    public void toggleLoopSingle() {
        loopSingle = !loopSingle;
        loop = false;
        Bukkit.broadcastMessage("§a[音乐] §7单曲循环: " + (loopSingle ? "§a开启" : "§c关闭"));
    }

    public boolean isLoop() { return loop; }
    public boolean isLoopSingle() { return loopSingle; }
    public boolean isPlaying() { return playing; }
    public Song getCurrentSong() { return currentSong; }

    private void downloadAndPlay(String url, Song song) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String downloadPath = plugin.getConfig().getString("music.download-path", "plugins/MusicJoy/music");
                File dir = new File(downloadPath);
                if (!dir.exists()) dir.mkdirs();

                File mp3File = new File(dir, song.getId() + ".mp3");
                if (!mp3File.exists()) {
                    Bukkit.broadcastMessage("§a[音乐] §7正在下载: §e" + song.getName());
                    downloadFile(url, mp3File);
                }

                Bukkit.broadcastMessage("§a[音乐] §7正在发送到客户端...");
                plugin.getAudioSender().sendToAllPlayers(mp3File, song);

                taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                    int duration = 0;
                    @Override
                    public void run() {
                        if (!playing) {
                            if (taskId != -1) {
                                Bukkit.getScheduler().cancelTask(taskId);
                                taskId = -1;
                            }
                            return;
                        }
                        if (plugin.getConfig().getBoolean("settings.bossbar-enable", true)) {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                plugin.getLyricManager().sendProgress(p, song, duration);
                            }
                        }
                        duration += 1;
                        if (duration >= (song.getDuration() / 1000)) {
                            if (taskId != -1) {
                                Bukkit.getScheduler().cancelTask(taskId);
                                taskId = -1;
                            }
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (loopSingle) play(currentSong);
                                else skip();
                            });
                        }
                    }
                }, 0L, 20L);
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.broadcastMessage("§c[音乐] §7播放失败: " + e.getMessage());
            }
        });
    }

    private void downloadFile(String fileUrl, File output) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(output)) {
                byte[] buffer = new byte[8192];
                int len;
                long total = 0;
                long max = conn.getContentLengthLong();
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    total += len;
                    if (max > 0) {
                        int percent = (int) (total * 100 / max);
                        if (percent % 10 == 0) {
                            final int p = percent;
                            Bukkit.getScheduler().runTask(plugin, () ->
                                Bukkit.broadcastMessage("§a[音乐] §7下载进度: §e" + p + "%"));
                        }
                    }
                }
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void writeCurrentId(String id) {
        try {
            String path = plugin.getConfig().getString("music.download-path", "plugins/MusicJoy/music");
            File dir = new File(path).getParentFile();
            if (dir == null) dir = plugin.getDataFolder();
            File currentFile = new File(dir, "current.txt");
            java.nio.file.Files.write(currentFile.toPath(), id.getBytes("UTF-8"));
        } catch (Exception e) {
            // ignore
        }
    }
}