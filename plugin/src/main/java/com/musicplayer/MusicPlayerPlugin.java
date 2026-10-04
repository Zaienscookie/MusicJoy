package com.musicplayer;

import com.musicplayer.command.MusicCommand;
import com.musicplayer.manager.MusicManager;
import com.musicplayer.manager.PlayerManager;
import com.musicplayer.manager.LyricManager;
import com.musicplayer.manager.LocalSongManager;
import com.musicplayer.manager.HttpServerManager;
import com.musicplayer.manager.ResourcePackManager;
import com.musicplayer.manager.AudioSender;
import com.musicplayer.api.NeteaseAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MusicPlayerPlugin extends JavaPlugin {

    private static MusicPlayerPlugin instance;
    private NeteaseAPI neteaseAPI;
    private MusicManager musicManager;
    private PlayerManager playerManager;
    private LyricManager lyricManager;
    private LocalSongManager localSongManager;
    private HttpServerManager httpServerManager;
    private ResourcePackManager resourcePackManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfig();

        neteaseAPI = new NeteaseAPI();
        neteaseAPI.setPlugin(this);
        loadCookies();
        musicManager = new MusicManager(this);
        playerManager = new PlayerManager(this);
        lyricManager = new LyricManager(this);
        localSongManager = new LocalSongManager(this);
        httpServerManager = new HttpServerManager(this);
        resourcePackManager = new ResourcePackManager(this);

        getCommand("music").setExecutor(new MusicCommand(this));
        getCommand("music").setTabCompleter(new MusicCommand(this));

        httpServerManager.start();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "musicjoymod:audio");

        getLogger().info("MusicJoy 已加载! (Mohist 1.20.1)");
        getLogger().info("客户端请安装 MusicJoyMod.jar 到 mods/ 目录");
    }

    @Override
    public void onDisable() {
        if (musicManager != null) musicManager.stop();
        if (httpServerManager != null) httpServerManager.stop();
        getLogger().info("MusicJoy 已卸载!");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        config.addDefault("netease.api-timeout", 10000);
        config.addDefault("netease.max-songs", 50);
        config.addDefault("music.download-path", "plugins/MusicJoy/music");
        config.addDefault("music.max-volume", 100);
        config.addDefault("music.default-volume", 50);
        config.addDefault("vip.default-days", 30);
        config.addDefault("settings.auto-save", true);
        config.addDefault("settings.auto-play", false);
        config.addDefault("settings.lyric-enable", true);
        config.addDefault("settings.bossbar-enable", true);
        config.addDefault("http.port", 25566);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private static final java.io.File COOKIE_FILE = new java.io.File("plugins/MusicJoy/cookies.txt");

    public void loadCookies() {
        try {
            if (COOKIE_FILE.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(COOKIE_FILE.toPath()), "UTF-8").trim();
                if (!content.isEmpty()) {
                    neteaseAPI.setLoginCookies(content);
                    getLogger().info("已加载登录Cookie");
                }
            }
        } catch (Exception e) {
            getLogger().warning("加载Cookie失败: " + e.getMessage());
        }
    }

    public void saveCookies() {
        try {
            String cookies = neteaseAPI.getCookiesString();
            COOKIE_FILE.getParentFile().mkdirs();
            java.nio.file.Files.write(COOKIE_FILE.toPath(), cookies.getBytes("UTF-8"));
        } catch (Exception e) {
            getLogger().warning("保存Cookie失败: " + e.getMessage());
        }
    }

    public static MusicPlayerPlugin getInstance() { return instance; }
    public NeteaseAPI getNeteaseAPI() { return neteaseAPI; }
    public MusicManager getMusicManager() { return musicManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public LyricManager getLyricManager() { return lyricManager; }
    public LocalSongManager getLocalSongManager() { return localSongManager; }
    public HttpServerManager getHttpServerManager() { return httpServerManager; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }
    public AudioSender getAudioSender() { return new AudioSender(this); }
}