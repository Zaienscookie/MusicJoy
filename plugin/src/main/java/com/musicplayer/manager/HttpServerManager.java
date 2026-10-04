package com.musicplayer.manager;

import com.musicplayer.MusicPlayerPlugin;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class HttpServerManager {

    private final MusicPlayerPlugin plugin;
    private HttpServer server;
    private int port = 25566;

    public HttpServerManager(MusicPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean start() {
        try {
            port = plugin.getConfig().getInt("http.port", 25566);
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/resourcepack", this::handleResourcePack);
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("HTTP 服务器已启动: 端口 " + port + " (仅资源包)");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("HTTP 服务器启动失败: " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    public int getPort() { return port; }

    public String getResourcePackUrl() {
        try {
            String ip = java.net.InetAddress.getLocalHost().getHostAddress();
            return "http://" + ip + ":" + port + "/resourcepack";
        } catch (Exception e) {
            return "http://localhost:" + port + "/resourcepack";
        }
    }

    private void handleResourcePack(HttpExchange exchange) throws IOException {
        File packFile = new File(plugin.getDataFolder(), "resourcepack/MusicJoy.zip");
        if (!packFile.exists()) {
            sendText(exchange, 404, "资源包未生成");
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.sendResponseHeaders(200, packFile.length());
        try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(packFile)) {
            byte[] buf = new byte[8192]; int len;
            while ((len = fis.read(buf)) != -1) os.write(buf, 0, len);
        }
    }

    private void sendText(HttpExchange exchange, int code, String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}