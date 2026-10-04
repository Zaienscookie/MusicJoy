package com.musicplayer.manager;

import com.musicplayer.MusicPlayerPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerManager {

    private final MusicPlayerPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;

    private final Map<UUID, String> loggedInPlayers = new HashMap<>();
    private final Map<UUID, Boolean> vipPlayers = new HashMap<>();
    private final Map<UUID, Boolean> mutePlayers = new HashMap<>();

    public PlayerManager(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }

    public void loginPlayer(Player player, String nickname, boolean isVip) {
        UUID uuid = player.getUniqueId();
        loggedInPlayers.put(uuid, nickname);
        vipPlayers.put(uuid, isVip);

        data.set("players." + uuid.toString() + ".nickname", nickname);
        data.set("players." + uuid.toString() + ".vip", isVip);
        data.set("players." + uuid.toString() + ".last-login", System.currentTimeMillis());
        saveData();

        player.sendMessage("§a[音乐] §7登录成功! 欢迎: §e" + nickname);
        if (isVip) {
            player.sendMessage("§a[音乐] §6✦ 网易云VIP会员 §7已激活!");
        }
    }

    public void logoutPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        loggedInPlayers.remove(uuid);
        vipPlayers.remove(uuid);
        player.sendMessage("§a[音乐] §7已注销");
    }

    public boolean isLoggedIn(Player player) {
        return loggedInPlayers.containsKey(player.getUniqueId());
    }

    public boolean isVip(Player player) {
        return vipPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public String getNickname(Player player) {
        return loggedInPlayers.get(player.getUniqueId());
    }

    public String getNickname(UUID uuid) {
        return loggedInPlayers.get(uuid);
    }

    public void setMute(Player player, boolean mute) {
        mutePlayers.put(player.getUniqueId(), mute);
    }

    public boolean isMuted(Player player) {
        return mutePlayers.getOrDefault(player.getUniqueId(), false);
    }

    public void setLocalVolume(Player player, int volume) {
        data.set("players." + player.getUniqueId().toString() + ".volume", Math.max(0, Math.min(100, volume)));
        saveData();
    }

    public int getLocalVolume(Player player) {
        return data.getInt("players." + player.getUniqueId().toString() + ".volume",
                plugin.getConfig().getInt("music.default-volume", 50));
    }

    public boolean hasVipPermission(Player player) {
        if (player.hasPermission("musicplayer.vip")) return true;
        return isVip(player);
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addVipDays(Player player, int days) {
        UUID uuid = player.getUniqueId();
        long expiry = data.getLong("players." + uuid.toString() + ".vip-expiry", 0);
        long now = System.currentTimeMillis();
        if (expiry < now) expiry = now;
        expiry += days * 86400000L;
        data.set("players." + uuid.toString() + ".vip-expiry", expiry);
        data.set("players." + uuid.toString() + ".vip", true);
        vipPlayers.put(uuid, true);
        saveData();
        player.sendMessage("§a[音乐] §7已获得 §e" + days + "天 §7VIP, 到期: " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date(expiry)));
    }

    public boolean isVipExpired(Player player) {
        UUID uuid = player.getUniqueId();
        long expiry = data.getLong("players." + uuid.toString() + ".vip-expiry", 0);
        return expiry > 0 && System.currentTimeMillis() > expiry;
    }
}