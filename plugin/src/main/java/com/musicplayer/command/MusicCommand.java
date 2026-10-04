package com.musicplayer.command;

import com.musicplayer.MusicPlayerPlugin;
import com.musicplayer.api.NeteaseAPI;
import com.musicplayer.manager.LocalSongManager;
import com.musicplayer.manager.MusicManager;
import com.musicplayer.manager.PlayerManager;
import com.musicplayer.model.Song;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class MusicCommand implements CommandExecutor, TabCompleter {

    private final MusicPlayerPlugin plugin;
    private final NeteaseAPI api;
    private final MusicManager musicManager;
    private final PlayerManager playerManager;
    private final LocalSongManager localSongManager;

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "login", "qrcode", "cookie", "logout", "search", "play", "stop", "skip",
            "queue", "list", "clear", "remove", "volume", "info", "status",
            "loop", "loopsingle", "vip", "local", "help", "reload"
    );

    public MusicCommand(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
        this.api = plugin.getNeteaseAPI();
        this.musicManager = plugin.getMusicManager();
        this.playerManager = plugin.getPlayerManager();
        this.localSongManager = plugin.getLocalSongManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "login": return handleLogin(sender, args);
            case "cookie": return handleCookie(sender, args);
            case "qrcode": return handleQrcode(sender);
            case "logout": return handleLogout(sender);
            case "search": return handleSearch(sender, args);
            case "play": return handlePlay(sender, args);
            case "stop": return handleStop(sender);
            case "skip": return handleSkip(sender);
            case "queue": return handleQueue(sender, args);
            case "list": return handleList(sender);
            case "clear": return handleClear(sender);
            case "remove": return handleRemove(sender, args);
            case "volume": return handleVolume(sender, args);
            case "info": return handleInfo(sender);
            case "status": return handleStatus(sender);
            case "loop": return handleLoop(sender);
            case "loopsingle": return handleLoopSingle(sender);
            
            case "vip": return handleVip(sender, args);
            case "local": return handleLocal(sender);
            case "reload": return handleReload(sender);
            case "help":
            default: sendHelp(sender); return true;
        }
    }

    private boolean handleLogin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c[音乐] §7控制台不能登录"); return true;
        }
        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage("§c用法: /music login <email> <password>");
            player.sendMessage("§c用法: /music login phone <phone> <password>");
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success;
            if (args[1].equalsIgnoreCase("phone") && args.length >= 4) {
                success = api.loginByPhone(args[2], args[3]);
            } else {
                success = api.loginByEmail(args[1], args[2]);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    playerManager.loginPlayer(player, api.getCurrentUsername(), api.isVip());
                    if (api.isVip()) {
                        Bukkit.broadcastMessage("§a[音乐] §6✦ 玩家 §e" + player.getName() + " §6登录了网易云VIP账号!");
                    }
                } else {
                    player.sendMessage("§c[音乐] §7登录失败, 请检查账号密码");
                }
            });
        });
        return true;
    }

    private boolean handleQrcode(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c[音乐] §7控制台不能扫码"); return true;
        }
        Player player = (Player) sender;
        player.sendMessage("§a[音乐] §7正在获取二维码...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            api.startQrLogin(new NeteaseAPI.QrLoginCallback() {
                @Override
                public void onQrCodeUrl(String url) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§a[音乐] §7请打开网易云音乐App扫码登录");
                        player.sendMessage("§a[音乐] §7扫码链接: §e§n" + url);
                        player.sendMessage("§a[音乐] §7二维码渲染: §e§nhttps://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + java.net.URLEncoder.encode(url));
                        player.sendMessage("§a[音乐] §7等待扫码中...(30秒内有效)");
                    });
                }

                @Override
                public void onScanSuccess(String nickname, boolean vip) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        playerManager.loginPlayer(player, nickname, vip);
                        Bukkit.broadcastMessage("§a[音乐] §6✦ 玩家 §e" + player.getName() + " §6扫码登录了网易云!");
                    });
                }

                @Override
                public void onExpired() {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§c[音乐] §7二维码已过期, 请重新扫码");
                    });
                }

                @Override
                public void onError(String msg) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§c[音乐] §7扫码失败: " + msg);
                    });
                }
            });

            for (int i = 0; i < 30; i++) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                int result = api.checkQrLogin();
                if (result == 1) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        playerManager.loginPlayer(player, api.getCurrentUsername(), api.isVip());
                        Bukkit.broadcastMessage("§a[音乐] §6✦ 玩家 §e" + player.getName() + " §6扫码登录了网易云!");
                    });
                    return;
                } else if (result == -1) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§c[音乐] §7二维码已过期");
                    });
                    return;
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§c[音乐] §7扫码超时");
            });
        });
        return true;
    }

    private boolean handleRegister(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage("§c用法: /music register <email> <password>");
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = api.loginByEmail(args[1], args[2]);
            if (!success) success = api.loginByPhone(args[1], args[2]);
            boolean finalSuccess = success;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (finalSuccess) {
                    playerManager.loginPlayer(player, api.getCurrentUsername(), api.isVip());
                    player.sendMessage("§a[音乐] §7登录成功!");
                } else {
                    player.sendMessage("§c[音乐] §7操作失败");
                }
            });
        });
        return true;
    }

    private boolean handleCookie(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /music cookie <Cookie字符串>");
            sender.sendMessage("§7或用 /music cookie load 从 config 重新加载");
            return true;
        }
        if (args[1].equalsIgnoreCase("load")) {
            plugin.loadCookies();
            sender.sendMessage("§a[音乐] §7已从配置文件加载Cookie");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        api.setLoginCookies(sb.toString());
        plugin.saveCookies();
        sender.sendMessage("§a[音乐] §aCookie已设置并保存!");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean ok = api.hasLoginCookies();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ok) {
                    sender.sendMessage("§a[音乐] §a登录校验成功! 用户: §e" + api.getCurrentUsername());
                } else {
                    sender.sendMessage("§c[音乐] §cCookie 无效或已过期, 请重新获取");
                }
            });
        });
        return true;
    }

    private boolean handleLogout(CommandSender sender) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        playerManager.logoutPlayer(player);
        api.logout();
        return true;
    }

    private boolean handleSearch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /music search <关键词>");
            return true;
        }
        StringBuilder keyword = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (keyword.length() > 0) keyword.append(" ");
            keyword.append(args[i]);
        }
        String searchKeyword = keyword.toString();
        sender.sendMessage("§a[音乐] §7正在搜索: §e" + searchKeyword);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            musicManager.search(searchKeyword, 10);
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<Song> results = musicManager.getSearchResults();
                if (results.isEmpty()) {
                    sender.sendMessage("§c[音乐] §7未找到相关歌曲");
                    return;
                }
                sender.sendMessage("§a[音乐] §7搜索结果:");
                for (int i = 0; i < results.size(); i++) {
                    Song s = results.get(i);
                    sender.sendMessage("§e" + (i + 1) + ". §f" + s.getName() + " §7- §b" + s.getArtist() + " §7[" + s.getDurationFormatted() + "]");
                }
                sender.sendMessage("§a[音乐] §7使用 §e/music play <序号> §7播放");
            });
        });
        return true;
    }

    private boolean handlePlay(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /music play <歌曲ID/序号>");
            sender.sendMessage("§c用法: /music play local <本地歌曲ID>");
            return true;
        }

        if (args[1].equalsIgnoreCase("local") && args.length >= 3) {
            try {
                long localId = Long.parseLong(args[2]);
                LocalSongManager.LocalSong local = localSongManager.getLocalSong(localId);
                if (local == null) {
                    sender.sendMessage("§c[音乐] §7未找到本地歌曲");
                    return true;
                }
                Song song = localSongManager.toBukkitSong(local);
                sender.sendMessage("§a[音乐] §7正在播放本地歌曲: §e" + song.getName());
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    musicManager.playLocal(local);
                });
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("§c[音乐] §7无效的本地歌曲ID");
                return true;
            }
        }

        try {
            int index = Integer.parseInt(args[1]) - 1;
            if (index >= 0 && index < musicManager.getSearchResults().size()) {
                Song song = musicManager.getSearchResults().get(index);
                sender.sendMessage("§a[音乐] §7正在播放: §e" + song.getName());
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> musicManager.play(song));
                return true;
            }
        } catch (NumberFormatException ignored) {}

        try {
            long songId = Long.parseLong(args[1]);
            sender.sendMessage("§a[音乐] §7正在通过ID播放: §e" + songId);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> musicManager.playById(songId));
            return true;
        } catch (NumberFormatException ignored) {}

        sender.sendMessage("§c[音乐] §7无效的歌曲ID或序号");
        return true;
    }

    private boolean handleLocal(CommandSender sender) {
        List<LocalSongManager.LocalSong> songs = localSongManager.getLocalSongs();
        if (songs.isEmpty()) {
            sender.sendMessage("§a[音乐] §7没有本地歌曲, 使用 §e/music upload <歌名> <歌手> §7上传MP3");
            return true;
        }
        sender.sendMessage("§a[音乐] §7本地歌曲列表 (§e" + songs.size() + "§7首):");
        for (LocalSongManager.LocalSong s : songs) {
            sender.sendMessage("§e" + s.id + ". §f" + s.name + " §7- §b" + s.artist + " §7(上传者: " + s.uploader + ")");
        }
        sender.sendMessage("§a[音乐] §7使用 §e/music play local <ID> §7播放");
        return true;
    }

    private boolean handleUpload(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c[音乐] §7控制台不能上传"); return true;
        }
        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage("§c用法: /music upload <歌名> <歌手>");
            player.sendMessage("§c然后通过插件消息通道发送MP3文件数据");
            player.sendMessage("§c或手动将MP3放入 plugins/MusicJoy/music/ 目录");
            player.sendMessage("§c再用 §e/music upload <歌名> <歌手> §7关联");
            return true;
        }

        StringBuilder name = new StringBuilder();
        StringBuilder artist = new StringBuilder();
        boolean isArtist = false;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-a")) { isArtist = true; continue; }
            if (!isArtist) { if (name.length() > 0) name.append(" "); name.append(args[i]); }
            else { if (artist.length() > 0) artist.append(" "); artist.append(args[i]); }
        }
        if (artist.length() == 0 && name.length() > 0) {
            artist.append("未知歌手");
        }

        localSongManager.cleanupTemp(player);
        boolean success = localSongManager.uploadSong(player, name.toString(), artist.toString());
        if (success) {
            player.sendMessage("§a[音乐] §7上传成功! 使用 §e/music local §7查看");
        } else {
            player.sendMessage("§c[音乐] §7上传失败, 请先将MP3放入 plugins/MusicJoy/music/ 目录后重试");
        }
        return true;
    }

    private boolean handleDelocal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /music delocal <本地歌曲ID>");
            return true;
        }
        try {
            long id = Long.parseLong(args[1]);
            if (localSongManager.deleteLocalSong(id)) {
                sender.sendMessage("§a[音乐] §7已删除本地歌曲");
            } else {
                sender.sendMessage("§c[音乐] §7未找到该本地歌曲");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[音乐] §7请输入有效的ID");
        }
        return true;
    }

    private boolean handleStop(CommandSender sender) {
        musicManager.stop();
        sender.sendMessage("§a[音乐] §7已停止播放");
        return true;
    }

    private boolean handleSkip(CommandSender sender) {
        musicManager.skip();
        sender.sendMessage("§a[音乐] §7已跳过");
        return true;
    }

    private boolean handleQueue(CommandSender sender, String[] args) {
        if (args.length > 1) {
            try {
                int index = Integer.parseInt(args[1]) - 1;
                if (index >= 0 && index < musicManager.getSearchResults().size()) {
                    musicManager.addToQueueFromSearch(index);
                    sender.sendMessage("§a[音乐] §7已添加到队列");
                    return true;
                }
            } catch (NumberFormatException ignored) {}
            try {
                long songId = Long.parseLong(args[1]);
                musicManager.addToQueueById(songId);
                return true;
            } catch (NumberFormatException ignored) {}
        }
        List<Song> queue = musicManager.getQueue();
        if (queue.isEmpty()) {
            sender.sendMessage("§a[音乐] §7队列为空");
            return true;
        }
        sender.sendMessage("§a[音乐] §7播放队列 (§e" + queue.size() + "§7首):");
        for (int i = 0; i < queue.size(); i++) {
            Song s = queue.get(i);
            sender.sendMessage("§e" + (i + 1) + ". §f" + s.getName() + " §7- §b" + s.getArtist());
        }
        return true;
    }

    private boolean handleList(CommandSender sender) {
        return handleQueue(sender, new String[]{"queue"});
    }

    private boolean handleClear(CommandSender sender) {
        musicManager.clearQueue();
        sender.sendMessage("§a[音乐] §7队列已清空");
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /music remove <序号>");
            return true;
        }
        try {
            int index = Integer.parseInt(args[1]) - 1;
            musicManager.removeFromQueue(index);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[音乐] §7请输入有效的序号");
        }
        return true;
    }

    private boolean handleVolume(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§a[音乐] §7当前音量: §e" + musicManager.getVolume() + "%");
            return true;
        }
        try {
            int vol = Integer.parseInt(args[1]);
            musicManager.setVolume(vol);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[音乐] §7请输入0-100的数字");
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        Song current = musicManager.getCurrentSong();
        if (current == null) {
            sender.sendMessage("§a[音乐] §7当前没有播放歌曲");
            return true;
        }
        sender.sendMessage("§a[音乐] §7当前播放:");
        sender.sendMessage("§e  歌曲: §f" + current.getName());
        sender.sendMessage("§e  歌手: §b" + current.getArtist());
        sender.sendMessage("§e  专辑: §d" + current.getAlbum());
        sender.sendMessage("§e  时长: §7" + current.getDurationFormatted());
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage("§a[音乐] §7====== MusicJoy 系统状态 ======");
        sender.sendMessage("§e  登录状态: " + (api.isLoggedIn() ? "§a已登录" : "§c未登录"));
        if (api.isLoggedIn()) {
            sender.sendMessage("§e  账号: §f" + api.getCurrentUsername());
            sender.sendMessage("§e  VIP: " + (api.isVip() ? "§a是" : "§c否"));
        }
        sender.sendMessage("§e  播放状态: " + (musicManager.isPlaying() ? "§a播放中" : "§c已停止"));
        if (musicManager.isPlaying() && musicManager.getCurrentSong() != null) {
            sender.sendMessage("§e  当前歌曲: §f" + musicManager.getCurrentSong().getName());
        }
        sender.sendMessage("§e  音量: §f" + musicManager.getVolume() + "%");
        sender.sendMessage("§e  循环: " + (musicManager.isLoop() ? "§a开" : "§c关") + " | 单曲循环: " + (musicManager.isLoopSingle() ? "§a开" : "§c关"));
        sender.sendMessage("§e  队列: §f" + musicManager.getQueue().size() + " 首");
        sender.sendMessage("§e  本地歌曲: §f" + localSongManager.getLocalSongs().size() + " 首");
        return true;
    }

    private boolean handleLoop(CommandSender sender) {
        musicManager.toggleLoop();
        return true;
    }

    private boolean handleLoopSingle(CommandSender sender) {
        musicManager.toggleLoopSingle();
        return true;
    }

    private boolean handleVip(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length >= 3) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage("§c[音乐] §7玩家不在线"); return true; }
                try {
                    int days = Integer.parseInt(args[2]);
                    playerManager.addVipDays(target, days);
                    sender.sendMessage("§a[音乐] §7已给予 " + target.getName() + " " + days + " 天VIP");
                } catch (NumberFormatException e) { sender.sendMessage("§c用法: /music vip <玩家> <天数>"); }
                return true;
            }
            sender.sendMessage("§c用法: /music vip <玩家> <天数>"); return true;
        }
        Player player = (Player) sender;
        if (args.length >= 3 && player.hasPermission("musicplayer.admin")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { player.sendMessage("§c[音乐] §7玩家不在线"); return true; }
            try {
                int days = Integer.parseInt(args[2]);
                playerManager.addVipDays(target, days);
            } catch (NumberFormatException e) { player.sendMessage("§c用法: /music vip <玩家> <天数>"); }
            return true;
        }
        if (playerManager.isVip(player)) {
            player.sendMessage("§a[音乐] §6✦ 你是VIP会员! §7享受全部音乐功能");
        } else {
            player.sendMessage("§a[音乐] §7你还不是VIP, 可以使用 §e/music login §7登录网易云VIP账号激活");
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("musicplayer.admin")) {
            sender.sendMessage("§c[音乐] §7你没有权限"); return true;
        }
        plugin.reloadConfig();
        sender.sendMessage("§a[音乐] §7配置文件已重载");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§a[音乐] §7====== MusicJoy v1.0 ======");
        sender.sendMessage("§e/music help §7- 显示帮助");
        sender.sendMessage("§e/music qrcode §7- 扫码登录网易云");
        sender.sendMessage("§e/music logout §7- 注销");
        sender.sendMessage("§e/music search <关键词> §7- 搜索歌曲");
        sender.sendMessage("§e/music play <序号/ID> §7- 播放歌曲");
        sender.sendMessage("§e/music play local <ID> §7- 播放本地歌曲");
        sender.sendMessage("§e/music stop §7- 停止");
        sender.sendMessage("§e/music skip §7- 跳过");
        sender.sendMessage("§e/music queue [序号] §7- 查看/添加队列");
        sender.sendMessage("§e/music local §7- 查看本地歌曲");
        sender.sendMessage("§e/music volume <0-100> §7- 音量");
        sender.sendMessage("§e/music info / status §7- 信息");
        sender.sendMessage("§e/music loop / loopsingle §7- 循环");
        sender.sendMessage("§e/music vip [玩家] [天数] §7- VIP管理");
        sender.sendMessage("§7---------------------------");
        sender.sendMessage("§7本地歌曲放 plugins/MusicJoy/music/ 自动识别");
        sender.sendMessage("§7文件名格式: 歌手 - 歌名.mp3");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return SUB_COMMANDS.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}