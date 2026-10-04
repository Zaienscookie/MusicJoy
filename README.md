# MusicJoy

Minecraft **网易云音乐全服播放**方案，由两部分组成：

- **服务端插件** `plugin/`（Bukkit / Spigot / Paper / Mohist 等）
- **客户端模组** `mod/`（Forge，Minecraft 1.20.1）

服务端负责搜索、点歌、队列、歌词、网易云 API 调用与音频分发；客户端模组通过 Forge 网络通道接收音频并实时播放，全服在线玩家同步收听，无需离开游戏。

> 仅供学习与自建服务器使用。请遵守网易云音乐服务条款与相关版权规定。

---

## ✨ 功能

- 🎵 **全服同步播放**：一人点歌，全服收听
- 🔍 游戏内聊天命令搜索 / 点歌 / 队列 / 切歌 / 停止 / 循环
- 🎤 **LRC 歌词同步显示**（BossBar / 聊天）
- 🔊 客户端可独立调节音量（联动原版唱片机音量）
- 👤 支持网易云 **账号登录 / 二维码扫码登录 / 手动填 Cookie**
- 🎁 VIP 天数、资源包自动下发等管理能力

## 📁 目录结构

```
MusicJoy/
├── plugin/                 # 服务端插件（Maven 项目）
│   ├── src/main/java/com/musicplayer/
│   │   ├── MusicPlayerPlugin.java      # 插件入口（读取 cookies、初始化）
│   │   ├── api/NeteaseAPI.java         # 网易云 API（weapi 加密、登录、搜索、播放地址）
│   │   ├── api/NeteaseEncrypt.java     # 加密工具（MD5 / AES）
│   │   ├── command/MusicCommand.java   # /music 命令
│   │   └── manager/                    # 播放、发送、HTTP、歌词、资源包、玩家管理
│   ├── src/main/resources/config.yml
│   ├── src/main/resources/plugin.yml
│   ├── pom.xml
│   └── build.ps1           # Windows 一键构建脚本
├── mod/                    # 客户端模组（ForgeGradle 项目）
│   └── src/main/java/com/musicjoy/mod/
│       ├── MusicJoyMod.java            # Mod 入口
│       ├── AudioPacket.java            # 网络包（Start / Data / Stop）
│       ├── ClientAudioHandler.java     # 音频接收与播放
│       └── mixin/SoundEngineMixin.java # 音量联动注入
└── dist/                   # 预编译产物（可直接使用）
    ├── MusicJoy-plugin-1.0.0.jar
    └── musicjoy-forge-mc1.20.1-1.0.0.jar
```

## 🧩 环境要求

| 组件 | 要求 |
|------|------|
| 服务端 | 支持 Bukkit API 的服务端（Spigot/Paper/Mohist…），插件 `api-version: 1.13` |
| 客户端 | Minecraft **1.20.1** + **Forge 47.3.22** |
| 构建插件 | JDK 8+（源码 `-source 8`），Maven 或 `build.ps1` |
| 构建模组 | JDK 17 + Gradle（`./gradlew`） |

## 🚀 安装使用

1. **服务端**：把 `dist/MusicJoy-plugin-1.0.0.jar` 放进服务器的 `plugins/` 目录并重启。
2. **客户端**：玩家把 `dist/musicjoy-forge-mc1.20.1-1.0.0.jar` 放进 `.minecraft/mods/`（需安装对应 Forge）。
3. **登录网易云**（首次需要，任选其一）：
   - 游戏内：`/music login <邮箱> <密码>` 或 `/music login phone <手机号> <密码>`
   - 扫码：`/music qrcode`
   - 或在服务端 `plugins/MusicJoy/cookies.txt` 里粘贴浏览器 Cookie（含 `MUSIC_U`）
4. 点歌：`/music search <关键词>` → `/music play <序号或id>`

### 常用命令（`/music`，别名 `musicplayer` / `netease` / `ncm`）

`login` `qrcode` `cookie` `logout` `search` `play` `stop` `skip` `queue` `lyric`（以插件实现为准）

### 权限

- `musicplayer.use`（默认所有人）
- `musicplayer.vip`
- `musicplayer.admin`（默认 OP）

## ⚙️ 配置（`plugin/src/main/resources/config.yml`）

```yaml
netease:
  api-timeout: 10000
  max-songs: 50
music:
  download-path: plugins/NetEaseMusicPlayer/music
  max-volume: 100
  default-volume: 50
vip:
  default-days: 30
http:
  port: 25566          # 资源包/音频 HTTP 服务端口
  host: "0.0.0.0"
settings:
  auto-save: true
  auto-play: false
  lyric-enable: true
  bossbar-enable: true
```

## 🔨 构建

**插件**（二选一）：
```bash
# 方式一：Maven
cd plugin && mvn -q package        # 产物在 target/

# 方式二：Windows 脚本（会编译并复制到服务端 plugins/）
set MUSICJOY_SERVER_DIR=D:\path\to\your\server
pwsh -File plugin\build.ps1
```

**模组**：
```bash
cd mod
./gradlew build                    # 产物在 build/libs/
```

## 📝 说明 / 注意

- 插件通过网易云 **weapi**（`NeteaseEncrypt`）调用非官方接口，`APP_SECRET` 为社区通用常量；接口可能随时变化。
- 请**不要**把 `cookies.txt`、账号密码等隐私文件提交到仓库（已加入 `.gitignore`）。
- 音频经 Forge 网络通道传输并依赖客户端模组播放；请确保服务端与客户端版本匹配。

## 📄 License

MIT
