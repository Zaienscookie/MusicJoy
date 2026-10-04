package com.musicplayer.manager;

import com.musicplayer.MusicPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackManager {

    private final MusicPlayerPlugin plugin;
    private final File packDir;
    private final File packFile;
    private final String ffmpegPath;
    private byte[] currentHash;

    private static final int PACK_FORMAT = 15;

    public ResourcePackManager(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
        this.packDir = new File(plugin.getDataFolder(), "resourcepack");
        this.packFile = new File(packDir, "MusicJoy.zip");

        this.ffmpegPath = findFfmpeg();

        if (!packDir.exists()) packDir.mkdirs();
    }

    private String findFfmpeg() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String ffmpegName = isWindows ? "ffmpeg.exe" : "ffmpeg";

        // 1. Check TEMP/plugins/ffmpeg/ (Windows)
        if (isWindows) {
            String tempFfmpeg = System.getenv("TEMP") + File.separator + "ffmpeg" + File.separator + ffmpegName;
            if (new File(tempFfmpeg).exists()) return tempFfmpeg;
        }

        // 2. Check plugins/ffmpeg/ (both platforms)
        String pluginFfmpeg = new File(plugin.getDataFolder().getParentFile(), "ffmpeg" + File.separator + ffmpegName).getAbsolutePath();
        if (new File(pluginFfmpeg).exists()) return pluginFfmpeg;

        // 3. Check common Linux paths
        if (!isWindows) {
            String[] linuxPaths = {"/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg", "/usr/bin/ffmpeg/ffmpeg"};
            for (String p : linuxPaths) {
                if (new File(p).exists()) return p;
            }
            // Try `which ffmpeg`
            try {
                Process which = Runtime.getRuntime().exec(new String[]{"which", "ffmpeg"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(which.getInputStream()));
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) return line.trim();
            } catch (Exception ignored) {}
        }

        return pluginFfmpeg;
    }

    public boolean convertToOgg(File mp3File, File oggFile) {
        try {
            if (oggFile.exists()) oggFile.delete();

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath, "-y", "-i", mp3File.getAbsolutePath(),
                    "-acodec", "libvorbis", "-ab", "128k",
                    "-ar", "44100", "-ac", "2",
                    oggFile.getAbsolutePath());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                plugin.getLogger().fine("[ffmpeg] " + line);
            }
            int exitCode = process.waitFor();
            return exitCode == 0 && oggFile.exists();
        } catch (Exception e) {
            plugin.getLogger().warning("FFmpeg转换失败: " + e.getMessage());
            return false;
        }
    }

    public boolean generatePack(File oggFile, String soundName) {
        try {
            if (packFile.exists()) packFile.delete();

            File assetsDir = new File(packDir, "assets/musicjoy");
            assetsDir.mkdirs();

            File soundsDir = new File(assetsDir, "sounds");
            soundsDir.mkdirs();

            File targetOgg = new File(soundsDir, soundName + ".ogg");
            if (targetOgg.exists()) targetOgg.delete();
            Files.copy(oggFile.toPath(), targetOgg.toPath());

            String mcmeta = "{\"pack\":{\"pack_format\":" + PACK_FORMAT + ",\"description\":\"MusicJoy - 全服音乐\"}}";
            writeFile(new File(packDir, "pack.mcmeta"), mcmeta);

            String soundsJson = "{\"music.song\":{\"sounds\":[{\"name\":\"musicjoy:sounds/" + soundName + "\",\"stream\":true}]}}";
            writeFile(new File(assetsDir, "sounds.json"), soundsJson);

            zipPack();

            this.currentHash = sha1(packFile);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("资源包生成失败: " + e.getMessage());
            return false;
        }
    }

    public void applyToAllPlayers() {
        if (!packFile.exists() || currentHash == null) return;

        String url = plugin.getHttpServerManager().getResourcePackUrl();
        String hash = bytesToHex(currentHash);

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                player.setResourcePack(url, currentHash, "§bMusicJoy\n§7音乐资源包, 接受后听到音乐", true);
            } catch (NoSuchMethodError e) {
                player.setResourcePack(url, currentHash);
            }
        }

        plugin.getLogger().info("资源包已应用到 " + Bukkit.getOnlinePlayers().size() + " 位玩家");
    }

    public void playSoundToAll(String soundName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), "musicjoy:music.song", 1.0f, 1.0f);
        }
    }

    public boolean isAvailable() {
        return new File(ffmpegPath).exists();
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    private void zipPack() throws IOException {
        File[] files = packDir.listFiles();
        if (files == null) return;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(packFile))) {
            addToZip(zos, packDir, "");
        }
    }

    private void addToZip(ZipOutputStream zos, File dir, String base) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String entryName = base + f.getName();
            if (f.isDirectory()) {
                if (!entryName.endsWith("/")) entryName += "/";
                zos.putNextEntry(new ZipEntry(entryName));
                zos.closeEntry();
                addToZip(zos, f, entryName);
            } else {
                if (f.getName().equals("MusicJoy.zip")) continue;
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(f.toPath(), zos);
                zos.closeEntry();
            }
        }
    }

    private void writeFile(File file, String content) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            w.write(content);
        }
    }

    private byte[] sha1(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
        }
        return md.digest();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}