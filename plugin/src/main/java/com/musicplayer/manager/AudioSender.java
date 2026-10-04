package com.musicplayer.manager;

import com.musicplayer.MusicPlayerPlugin;
import com.musicplayer.model.Song;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AudioSender {

    private static final int CHUNK_SIZE = 30000;
    private final MusicPlayerPlugin plugin;
    private Object channel;
    private boolean ready = false;

    public AudioSender(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
        try {
            Class<?> modClass = Class.forName("com.musicjoy.mod.MusicJoyMod");
            Field channelField = modClass.getField("CHANNEL");
            channel = channelField.get(null);
            ready = true;
            plugin.getLogger().info("AudioSender: 已连接到Mod的SimpleChannel");
        } catch (Exception e) {
            plugin.getLogger().warning("AudioSender: 无法连接Mod通道: " + e.getMessage());
        }
    }

    public void sendToAllPlayers(File mp3File, Song song) {
        if (!ready) { plugin.getLogger().warning("Mod通道未就绪"); return; }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                byte[] mp3Data = readFile(mp3File);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendToPlayer(player, mp3Data, song);
                }
                plugin.getLogger().info("已发送给 " + Bukkit.getOnlinePlayers().size() + " 位玩家");
            } catch (Exception e) {
                plugin.getLogger().warning("发送失败: " + e.getMessage());
            }
        });
    }

    private void sendToPlayer(Player player, byte[] mp3Data, Song song) {
        try {
            Object sp = ((CraftPlayer) player).getHandle();
            Object listener = sp.getClass().getField("f_8906_").get(sp);
            Field connField = listener.getClass().getDeclaredField("f_9742_");
            connField.setAccessible(true);
            Object conn = connField.get(listener);

            Class<?> netDirClass = Class.forName("net.minecraftforge.network.NetworkDirection");
            Object playToClient = netDirClass.getField("PLAY_TO_CLIENT").get(null);

            Class<?> startPktClass = Class.forName("com.musicjoy.mod.AudioPacket$StartPacket");
            Object startPkt = startPktClass.getConstructor(int.class, String.class).newInstance(mp3Data.length, song.getName());
            Method sendTo = channel.getClass().getMethod("sendTo", Object.class, Class.forName("net.minecraft.network.Connection"), netDirClass);
            sendTo.invoke(channel, startPkt, conn, playToClient);

            int offset = 0;
            int seq = 0;
            Class<?> dataPktClass = Class.forName("com.musicjoy.mod.AudioPacket$DataPacket");
            while (offset < mp3Data.length) {
                int size = Math.min(CHUNK_SIZE, mp3Data.length - offset);
                byte[] chunk = new byte[size];
                System.arraycopy(mp3Data, offset, chunk, 0, size);
                Object dataPkt = dataPktClass.getConstructor(int.class, byte[].class).newInstance(seq, chunk);
                sendTo.invoke(channel, dataPkt, conn, playToClient);
                offset += size;
                seq++;
                Thread.sleep(1);
            }

            Class<?> stopPktClass = Class.forName("com.musicjoy.mod.AudioPacket$StopPacket");
            Object stopPkt = stopPktClass.getConstructor().newInstance();
            sendTo.invoke(channel, stopPkt, conn, playToClient);
        } catch (Exception e) {
            plugin.getLogger().warning("发送给 " + player.getName() + " 失败: " + e.getMessage());
        }
    }

    public void stopAll() {
        if (!ready) return;
        try {
            Class<?> stopPktClass = Class.forName("com.musicjoy.mod.AudioPacket$StopPacket");
            Object stopPkt = stopPktClass.getConstructor().newInstance();
            Class<?> netDirClass = Class.forName("net.minecraftforge.network.NetworkDirection");
            Object playToClient = netDirClass.getField("PLAY_TO_CLIENT").get(null);
            Method sendTo = channel.getClass().getMethod("sendTo", Object.class, Class.forName("net.minecraft.network.Connection"), netDirClass);

            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    Object sp = ((CraftPlayer) player).getHandle();
                    Object listener = sp.getClass().getField("f_8906_").get(sp);
                    Field connField = listener.getClass().getDeclaredField("f_9742_");
                    connField.setAccessible(true);
                    Object conn = connField.get(listener);
                    sendTo.invoke(channel, stopPkt, conn, playToClient);
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("停止发送失败: " + e.getMessage());
        }
    }

    private byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) baos.write(buf, 0, len);
            return baos.toByteArray();
        }
    }
}