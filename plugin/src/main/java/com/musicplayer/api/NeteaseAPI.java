package com.musicplayer.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.musicplayer.MusicPlayerPlugin;
import com.musicplayer.model.Song;
import com.musicplayer.model.User;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeteaseAPI {

    private static final String API_BASE = "https://music.163.com/api";
    private static final String OPENAPI_BASE = "http://openapi.music.163.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final Gson GSON = new Gson();

    private static final String APP_ID = "b3010d00000000005b985090cfa8e803";
    private static final String APP_SECRET = "dec91fc3bc7f904958eb5dc4d2350efc";
    private static final String PRIVATE_KEY_B64 =
            "MIIEpAIBAAKCAQEA4+5Z84hbw34xYrhyoYP7w6MTrbqLEKnzgX7i7bEy5mgP8NXyM2+BP9" +
            "DkyJs+9aCykMrkmSxcC1ILD5XoGMXrqmOHqW73935K3PoL3eD3CWxICe6h/wvFmd6Kt1YV" +
            "+wxS2JAMoHXD0NyMyZCncW2l6i7hCF00x1m2mJPgkXPC521Mq+xe28iSjMKdd534xCGlGn" +
            "Vk59o3NSJYkOlDICTF7TrWL7O4LWjFNzxgR3sIOg4G2OyDnfYWJ6SkHt0yHZgQwJRKw+sS" +
            "ndx7M/IWcJk0VzyKeZjkC0LHHhUEL6i9aLYDOBAW1Bju+Vd7F+0FL3z+jx2eMm1XaKsHfE" +
            "ml2hhwSQIDAQABAoIBAAt7dumXTXQgN9Gu1AuB8B1vai7ezmGlBiDYaQsrWZKU3HS3+m8P" +
            "53V1Cwl2FKpgEY7Mwc5blzl0xRzgYuRYmCIpyGxHLFO594CBUja3vgop+8kQTfXgvHAfb2" +
            "KEAjB28vd47kHpHNe5PJhs4pqKuQh+mxSGSYHfaZzvGimJqOBqn0o6SRt0HZLLLyuq5+8G" +
            "rUWg7lUhp5e/WWaEklii09cxejdlvPwI46gvL33zzp2SCSmj2jmAq/tATSheudgkq+oqPSw" +
            "BkoMfX9RYD9VCBnvuMJAk3lN3qJRCO2HCnBlx45d24CrBq3+4eoJOR3a+QgUZqlZzd+SS/" +
            "HhEjJjzONkCgYEA/tnOnaH1meOBEYKHolayaoIl32J5n26sM+pgqQDBzrEw5MKtlkpp8EM" +
            "/QntlbOXxQuoD4dxRYMMbSbD4iWsw9b1FRGmxBZkNw3CxZMM+eDlnSjhvFWjkQ38iU4632" +
            "RH5+WV1creNHTv79dRsojf7tL6zIk+JiKCkxW7zargYmh0CgYEA5PV4BlWuwgCL8xkFdvY" +
            "DdpZvFWSASiBCBkPTog3tLvwLEwylBaQK1fSVHvMaVNqOTiyhOkBGa6hc0DM0PNkiDRtpc" +
            "KQ4Yd8rnXWkoUqULG/5ROGPqLDe1w+XGoy0HG9kr6eIaKqc75nsWTLYwwmrzIUIPrKizVf" +
            "y8m/jrOKB9x0CgYEAvCIWARAauqvUEBUIA7N0qmPBiZKsriGJRhnzmU9Rwz/TFE0S0CvG4" +
            "PMDtBRb33QwEnGmxAlcmc3uSww1rVxQHFjI1aQgZj2Ch5CdpADuENpvgb7/YhK4z4r+0Cg" +
            "/WCOJIa7gcPtHVRl++w8B0w745YkRvHZy1UWh3Y32bAXTQ5ECgYEArgMF+sKKNHYX52W/6" +
            "n7leO6ydG7StgO4SmyoXwT4m/oBLLQyg4v+lEf0QfZ/r7f/6dgvnUcWfmISUEZqUEiygYW" +
            "w6GRXWcM0sdKkkdVNskxUBg7skUgDxS3bKNmBVg8vVfUdTndFGTFCGTT0257xDFqbPpOj+" +
            "i76vVlej5oDLt0CgYBNcMOSL/6IUh18+OkwiutfCADlZTd59ZKUlOfAbooe0SYQHwctJon" +
            "RG1KcTFRCzVfgZkXwlSh2jNtQu1mSYBTa1RODiKM8GH6zmVbuf8kbPXsviP4gblZJ4bRhM" +
            "ka95h8xsL+u0DIpeRyACAgqrEMMk7VWVbaIPEaAJBpknNqL7Q==";

    

    private final CloseableHttpClient client;
    private final Map<String, String> cookies;

    private MusicPlayerPlugin plugin;
    private boolean loggedIn = false;
    private String currentUsername = "";
    private boolean isVip = false;
    private String qrKey = null;

    public interface QrLoginCallback {
        void onQrCodeUrl(String url);
        void onScanSuccess(String nickname, boolean vip);
        void onExpired();
        void onError(String msg);
    }

    public NeteaseAPI() {
        this.cookies = new HashMap<>();
        this.client = HttpClients.custom()
                .disableRedirectHandling()
                .build();
    }

    public boolean loginByEmail(String email, String password) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", email);
            data.addProperty("password", NeteaseEncrypt.md5(password));
            data.addProperty("rememberLogin", "true");

            NeteaseEncrypt.EncryptedData encrypted = NeteaseEncrypt.encrypt(GSON.toJson(data));
            JsonObject body = new JsonObject();
            body.addProperty("params", encrypted.getParams());
            body.addProperty("encSecKey", encrypted.getEncSecKey());

            String response = weapiPost("/login", body.toString());
            JsonObject json = GSON.fromJson(response, JsonObject.class);

            if (json != null && json.has("code") && json.get("code").getAsInt() == 200) {
                JsonObject profile = json.getAsJsonObject("profile");
                if (profile != null) {
                    this.loggedIn = true;
                    this.currentUsername = profile.get("nickname").getAsString();
                    this.isVip = profile.get("vipType").getAsInt() > 0;
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loginByPhone(String phone, String password) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("phone", phone);
            data.addProperty("password", NeteaseEncrypt.md5(password));
            data.addProperty("rememberLogin", "true");

            NeteaseEncrypt.EncryptedData encrypted = NeteaseEncrypt.encrypt(GSON.toJson(data));
            JsonObject body = new JsonObject();
            body.addProperty("params", encrypted.getParams());
            body.addProperty("encSecKey", encrypted.getEncSecKey());

            String response = weapiPost("/login/cellphone", body.toString());
            JsonObject json = GSON.fromJson(response, JsonObject.class);

            if (json != null && json.has("code") && json.get("code").getAsInt() == 200) {
                JsonObject profile = json.getAsJsonObject("profile");
                if (profile != null) {
                    this.loggedIn = true;
                    this.currentUsername = profile.get("nickname").getAsString();
                    this.isVip = profile.get("vipType").getAsInt() > 0;
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getQrKey() {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                plugin.getLogger().info("QR码: 第" + (attempt + 1) + "次请求...");

                // 方式1: weapi加密 -> /weapi/ 端点
                JsonObject data = new JsonObject();
                NeteaseEncrypt.EncryptedData encrypted = NeteaseEncrypt.encrypt(GSON.toJson(data));
                JsonObject body = new JsonObject();
                body.addProperty("params", encrypted.getParams());
                body.addProperty("encSecKey", encrypted.getEncSecKey());

                String response = weapiPost("/login/qrcode/unikey", body.toString());
                if (isEmptyResponse(response)) {
                    response = weapiPost("/login/qrcode/unikey", body.toString(), "https://music.163.com/api");
                }
                if (isEmptyResponse(response)) {
                    response = rawPost("/login/qrcode/unikey", body.toString());
                }
                if (isEmptyResponse(response)) {
                    response = simplePost("/login/qrcode/unikey", "{}");
                }
                if (isEmptyResponse(response)) {
                    response = simplePost("/login/qrcode/unikey", "{}", "https://music.163.com/weapi");
                }
                if (isEmptyResponse(response)) {
                    // 方式4: GET请求
                    response = apiGet("https://music.163.com/api/login/qrcode/unikey");
                }

                plugin.getLogger().info("QR码: 响应长度=" + (response != null ? response.length() : 0));
                if (isEmptyResponse(response)) {
                    plugin.getLogger().warning("QR码: 空响应, 尝试" + (attempt + 1) + "次");
                    Thread.sleep(1000);
                    continue;
                }
                JsonObject json = GSON.fromJson(response, JsonObject.class);
                if (json != null && json.has("unikey")) {
                    this.qrKey = json.get("unikey").getAsString();
                    plugin.getLogger().info("QR码: 成功 key=" + qrKey);
                    return this.qrKey;
                }
                plugin.getLogger().warning("QR码失败: " + response);
            } catch (Exception e) {
                plugin.getLogger().warning("QR码异常: " + e.getMessage());
            }
        }
        return null;
    }

    private String signRSA256(String data) throws Exception {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (!tmpDir.endsWith(java.io.File.separator)) tmpDir += java.io.File.separator;
        String dataFile = tmpDir + "musicjoy_sign_" + System.nanoTime() + ".txt";
        String sigFile = tmpDir + "musicjoy_sig_" + System.nanoTime() + ".bin";
        String keyFile = tmpDir + "musicjoy_key_" + System.nanoTime() + ".pem";

        try {
            // Write private key PEM (PKCS1 format)
            String pem = "-----BEGIN RSA PRIVATE KEY-----\r\n";
            String b64 = PRIVATE_KEY_B64;
            for (int i = 0; i < b64.length(); i += 64) {
                pem += b64.substring(i, Math.min(i + 64, b64.length())) + "\r\n";
            }
            pem += "-----END RSA PRIVATE KEY-----\r\n";
            java.io.FileWriter fw = new java.io.FileWriter(keyFile);
            fw.write(pem);
            fw.close();

            // Write data to sign
            java.io.FileOutputStream fos = new java.io.FileOutputStream(dataFile);
            fos.write(data.getBytes("UTF-8"));
            fos.close();

            // Run openssl to sign
            ProcessBuilder pb = new ProcessBuilder(
                    "openssl", "dgst", "-sha256", "-sign", keyFile,
                    "-out", sigFile, dataFile);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                java.util.Scanner s = new java.util.Scanner(p.getInputStream()).useDelimiter("\\A");
                String err = s.hasNext() ? s.next() : "";
                throw new Exception("openssl exit " + exitCode + ": " + err);
            }

            // Read signature
            java.io.FileInputStream fis = new java.io.FileInputStream(sigFile);
            byte[] sigBytes = new byte[(int) new java.io.File(sigFile).length()];
            fis.read(sigBytes);
            fis.close();

            return java.util.Base64.getEncoder().encodeToString(sigBytes);
        } finally {
            new java.io.File(dataFile).delete();
            new java.io.File(sigFile).delete();
            new java.io.File(keyFile).delete();
        }
    }

    private String deviceJson = "{\"netStatus\":\"wifi\",\"flowFlag\":\"init\",\"channel\":\"netease\",\"deviceId\":\"musicjoy_mc_server\",\"deviceType\":\"andrcar\",\"appVer\":\"1.0.0\",\"os\":\"andrcar\",\"osVer\":\"1.0\",\"brand\":\"netease\",\"model\":\"musicjoy\",\"clientIp\":\"127.0.0.1\"}";
    private String anonymousToken = null;

    private String openApiRequest(String path, JsonObject bizContent) throws Exception {
        return openApiRequest(path, bizContent, anonymousToken != null ? anonymousToken : "");
    }

    private String openApiRequest(String path, JsonObject bizContent, String accessToken) throws Exception {
        long timestamp = System.currentTimeMillis();
        String bizStr = URLEncoder.encode(bizContent.toString(), "UTF-8");
        String deviceStr = URLEncoder.encode(deviceJson, "UTF-8");

        // Build sorted params for URL (all values URL-encoded)
        java.util.TreeMap<String, String> urlParams = new java.util.TreeMap<>();
        if (accessToken != null && !accessToken.isEmpty()) urlParams.put("accessToken", accessToken);
        urlParams.put("appId", APP_ID);
        urlParams.put("appSecret", APP_SECRET);
        urlParams.put("bizContent", bizStr);
        urlParams.put("device", deviceStr);
        urlParams.put("signType", "RSA_SHA256");
        urlParams.put("timestamp", String.valueOf(timestamp));

        // Build sign string from raw (unencoded) values, sorted, excluding sign itself
        StringBuilder signSb = new StringBuilder();
        java.util.TreeMap<String, String> rawParams = new java.util.TreeMap<>();
        if (accessToken != null && !accessToken.isEmpty()) rawParams.put("accessToken", accessToken);
        rawParams.put("appId", APP_ID);
        rawParams.put("appSecret", APP_SECRET);
        rawParams.put("bizContent", bizContent.toString());
        rawParams.put("device", deviceJson);
        rawParams.put("signType", "RSA_SHA256");
        rawParams.put("timestamp", String.valueOf(timestamp));
        for (java.util.Map.Entry<String, String> e : rawParams.entrySet()) {
            if (signSb.length() > 0) signSb.append("&");
            signSb.append(e.getKey()).append("=").append(e.getValue());
        }
        String sign = URLEncoder.encode(signRSA256(signSb.toString()), "UTF-8");

        // Build URL
        StringBuilder url = new StringBuilder(OPENAPI_BASE + path + "?");
        for (java.util.Map.Entry<String, String> e : urlParams.entrySet()) {
            url.append(e.getKey()).append("=").append(e.getValue()).append("&");
        }
        url.append("sign=").append(sign);

        plugin.getLogger().info("OpenAPI请求: " + path);
        HttpGet get = new HttpGet(url.toString());
        get.setHeader("User-Agent", USER_AGENT);
        try (CloseableHttpResponse resp = client.execute(get)) {
            String body = EntityUtils.toString(resp.getEntity(), "UTF-8");
            plugin.getLogger().info("OpenAPI响应: " + (body.length() < 200 ? body : body.substring(0, 200) + "..."));
            return body;
        }
    }

    private boolean loginAnonymous() {
        try {
            JsonObject biz = new JsonObject();
            biz.addProperty("clientId", APP_ID);
            String resp = openApiRequest("/openapi/music/basic/oauth2/login/anonymous", biz, "");
            if (resp == null || resp.isEmpty()) return false;
            JsonObject json = GSON.fromJson(resp, JsonObject.class);
            if (json != null && json.has("code") && json.get("code").getAsInt() == 200
                    && json.has("data")) {
                JsonObject data = json.getAsJsonObject("data");
                if (data.has("accessToken")) {
                    anonymousToken = data.get("accessToken").getAsString();
                    plugin.getLogger().info("匿名登录成功, token=" + anonymousToken.substring(0, 20) + "...");
                    return true;
                }
            }
            plugin.getLogger().warning("匿名登录失败: " + resp);
        } catch (Exception e) {
            plugin.getLogger().warning("匿名登录异常: " + e.getMessage());
        }
        return false;
    }

    public String getQrKeyOpenApi() {
        try {
            // Ensure anonymous login first
            if (anonymousToken == null) {
                if (!loginAnonymous()) {
                    plugin.getLogger().warning("匿名登录失败, 无法获取QR码");
                    return null;
                }
            }
            JsonObject biz = new JsonObject();
            biz.addProperty("type", 2);
            biz.addProperty("expiredKey", "300");
            String resp = openApiRequest("/openapi/music/basic/user/oauth2/qrcodekey/get/v2", biz);
            if (resp == null || resp.isEmpty()) return null;
            JsonObject json = GSON.fromJson(resp, JsonObject.class);
            if (json != null && json.has("code") && json.get("code").getAsInt() == 200
                    && json.has("data")) {
                JsonObject data = json.getAsJsonObject("data");
                if (data.has("uniKey")) {
                    this.qrKey = data.get("uniKey").getAsString();
                    plugin.getLogger().info("OpenAPI QR码: 成功 key=" + qrKey);
                    return this.qrKey;
                }
            }
            plugin.getLogger().warning("OpenAPI QR码失败: " + resp);
        } catch (Exception e) {
            plugin.getLogger().warning("OpenAPI QR码异常: " + e.getMessage());
        }
        return null;
    }

    public int checkQrLoginOpenApi() {
        try {
            JsonObject biz = new JsonObject();
            biz.addProperty("key", qrKey);
            biz.addProperty("clientId", APP_ID);
            String resp = openApiRequest("/openapi/music/basic/oauth2/device/login/qrcode/get", biz);
            if (resp == null || resp.isEmpty()) return 0;
            JsonObject json = GSON.fromJson(resp, JsonObject.class);
            if (json != null && json.has("code") && json.get("code").getAsInt() == 200
                    && json.has("data")) {
                JsonObject data = json.getAsJsonObject("data");
                if (data.has("status")) {
                    int status = data.get("status").getAsInt();
                    if (status == 800) { qrKey = null; return -1; }
                    if (status == 803) {
                        this.loggedIn = true;
                        this.currentUsername = "扫码用户";
                        return 1;
                    }
                    return status;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("OpenAPI轮询异常: " + e.getMessage());
        }
        return 0;
    }

    public void setPlugin(MusicPlayerPlugin p) { this.plugin = p; }

    public String getQrCodeUrl() {
        if (qrKey == null) return null;
        return "https://music.163.com/login?codekey=" + qrKey;
    }

    public String getQrCodeUrlOpenApi() {
        if (qrKey == null) return null;
        return "https://163cn.tv/" + qrKey;
    }

    public void startQrLogin(QrLoginCallback callback) {
        boolean useOpenApi = true;
        qrKey = getQrKeyOpenApi();
        if (qrKey == null) {
            useOpenApi = false;
            qrKey = getQrKey();
        }
        if (qrKey == null) {
            callback.onError("获取二维码Key失败");
            return;
        }
        String qrUrl = useOpenApi ? getQrCodeUrlOpenApi() : getQrCodeUrl();
        callback.onQrCodeUrl(qrUrl);
    }

    public int checkQrLogin() {
        int status = checkQrLoginOpenApi();
        if (status > 0) return status;
        try {
            JsonObject data = new JsonObject();
            data.addProperty("key", qrKey);
            NeteaseEncrypt.EncryptedData encrypted = NeteaseEncrypt.encrypt(GSON.toJson(data));
            JsonObject body = new JsonObject();
            body.addProperty("params", encrypted.getParams());
            body.addProperty("encSecKey", encrypted.getEncSecKey());

            String response = weapiPost("/login/qrcode/client/login", body.toString());
            if (isEmptyResponse(response)) {
                response = rawPost("/login/qrcode/client/login", body.toString());
            }
            if (isEmptyResponse(response)) {
                response = jsonPost("/login/qrcode/client/login", body.toString());
            }
            if (isEmptyResponse(response)) {
                JsonObject keyBody = new JsonObject();
                keyBody.addProperty("key", qrKey);
                response = simplePost("/login/qrcode/client/login", keyBody.toString());
            }
            JsonObject json = GSON.fromJson(response, JsonObject.class);
            if (json != null && json.has("code")) {
                int code = json.get("code").getAsInt();
                if (code == 800) { qrKey = null; return -1; }
                if (code == 803) {
                    String nickname = json.has("nickname") ? json.get("nickname").getAsString() : "未知用户";
                    this.loggedIn = true;
                    this.currentUsername = nickname;
                    return 1;
                }
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Song> searchSongs(String keyword, int limit) {
        List<Song> songs = new ArrayList<>();
        try {
            String url = API_BASE + "/search/get/web?csrf_token=&s="
                    + URLEncoder.encode(keyword, "UTF-8")
                    + "&type=1&offset=0&limit=" + Math.min(limit, 100);

            String response = apiGet(url);
            JsonObject json = GSON.fromJson(response, JsonObject.class);

            if (json != null && json.has("code") && json.get("code").getAsInt() == 200) {
                JsonObject result = json.getAsJsonObject("result");
                if (result != null && result.has("songs")) {
                    JsonArray arr = result.getAsJsonArray("songs");
                    for (int i = 0; i < arr.size(); i++) {
                        songs.add(parseSong(arr.get(i).getAsJsonObject()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songs;
    }

    public String getSongUrl(long songId) {
        try {
            String url = API_BASE + "/song/enhance/player/url?id=" + songId
                    + "&ids=%5B" + songId + "%5D&br=320000";

            String response = apiGet(url);
            JsonObject json = GSON.fromJson(response, JsonObject.class);

            if (json != null && json.get("code").getAsInt() == 200) {
                JsonArray dataArray = json.getAsJsonArray("data");
                if (dataArray != null && dataArray.size() > 0) {
                    JsonObject songData = dataArray.get(0).getAsJsonObject();
                    if (songData.get("url") != null && !songData.get("url").isJsonNull()) {
                        return songData.get("url").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Song getSongDetail(long songId) {
        try {
            String url = API_BASE + "/v3/song/detail?c=%5B%7B%22id%22%3A" + songId + "%7D%5D";
            String response = apiGet(url);
            JsonObject json = GSON.fromJson(response, JsonObject.class);

            if (json != null && json.get("code").getAsInt() == 200) {
                JsonArray arr = json.getAsJsonArray("songs");
                if (arr != null && arr.size() > 0) {
                    return parseSong(arr.get(0).getAsJsonObject());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getLyric(long songId) {
        try {
            String url = API_BASE + "/song/lyric?id=" + songId + "&lv=-1&kv=-1&tv=-1";
            String response = apiGet(url);
            JsonObject json = GSON.fromJson(response, JsonObject.class);

            if (json != null && json.get("code").getAsInt() == 200 && json.has("lrc")) {
                JsonObject lrc = json.getAsJsonObject("lrc");
                if (lrc != null && lrc.has("lyric") && !lrc.get("lyric").isJsonNull()) {
                    return lrc.get("lyric").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getCurrentUser() {
        if (!loggedIn) return null;
        return new User(currentUsername, isVip, loggedIn);
    }

    public boolean isLoggedIn() { return loggedIn; }
    public boolean isVip() { return isVip; }
    public String getCurrentUsername() { return currentUsername; }

    public void logout() {
        this.loggedIn = false;
        this.currentUsername = "";
        this.isVip = false;
        this.cookies.clear();
        this.qrKey = null;
    }

    public void setLoginCookies(String cookieStr) {
        cookies.clear();
        if (cookieStr == null || cookieStr.trim().isEmpty()) {
            this.loggedIn = false;
            this.currentUsername = "";
            this.isVip = false;
            return;
        }
        boolean hasLogin = false;
        String[] lines = cookieStr.split("\r?\n");
        boolean isNetscape = false;
        for (String line : lines) {
            if (line.contains("MUSIC_U") && line.contains("\t")) isNetscape = true;
        }

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (isNetscape) {
                // Netscape format: domain \t includeSubdomains \t path \t secure \t expiry \t name \t value
                String[] fields = line.split("\\t");
                if (fields.length >= 7) {
                    String name = fields[5].trim();
                    String value = fields[6].trim();
                    if (!name.isEmpty() && !value.isEmpty()) {
                        cookies.put(name, value);
                        if (name.equalsIgnoreCase("MUSIC_U")) hasLogin = true;
                    }
                } else {
                    String[] kv = line.split("=", 2);
                    if (kv.length == 2) {
                        cookies.put(kv[0].trim(), kv[1].trim());
                        if (kv[0].trim().equalsIgnoreCase("MUSIC_U")) hasLogin = true;
                    }
                }
            } else {
                String[] kv = line.split("=", 2);
                if (kv.length == 2) {
                    cookies.put(kv[0].trim(), kv[1].trim());
                    if (kv[0].trim().equalsIgnoreCase("MUSIC_U")) hasLogin = true;
                }
            }
        }
        // Netease requires these defaults for some /api/ calls
        if (!cookies.containsKey("os")) cookies.put("os", "pc");
        if (!cookies.containsKey("appver")) cookies.put("appver", "2.9.7");

        this.loggedIn = hasLogin;
        this.currentUsername = hasLogin ? "Cookie用户" : "";
        this.isVip = hasLogin; // actual verification happens via song fetch
    }

    public boolean hasLoginCookies() { return launched_risky_check(); }

    public String getCookiesString() {
        if (cookies.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : cookies.entrySet()) {
            if (e.getKey().equalsIgnoreCase("os") || e.getKey().equalsIgnoreCase("appver")) continue;
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    private boolean launched_risky_check() {
        // RISKY_API login status check via /api/nuser/account/get
        try {
            String url = API_BASE + "/nuser/account/get";
            String resp = apiGet(url);
            if (resp == null || resp.isEmpty()) return false;
            JsonObject json = GSON.fromJson(resp, JsonObject.class);
            if (json != null && json.has("code") && json.get("code").getAsInt() == 200) {
                if (json.has("account") && !json.get("account").isJsonNull()) {
                    JsonObject acc = json.getAsJsonObject("account");
                    this.loggedIn = true;
                    if (json.has("profile") && !json.get("profile").isJsonNull()) {
                        JsonObject profile = json.getAsJsonObject("profile");
                        if (profile.has("nickname") && !profile.get("nickname").isJsonNull()) {
                            this.currentUsername = profile.get("nickname").getAsString();
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private Song parseSong(JsonObject obj) {
        long id = obj.get("id").getAsLong();
        String name = obj.get("name").getAsString();
        String artist = "";
        String album = "";
        long duration = 0;

        if (obj.has("artists") || obj.has("ar")) {
            JsonArray artists = obj.has("artists") ? obj.getAsJsonArray("artists") : obj.getAsJsonArray("ar");
            if (artists != null && artists.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < artists.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(artists.get(i).getAsJsonObject().get("name").getAsString());
                }
                artist = sb.toString();
            }
        }
        if (obj.has("album")) {
            JsonObject albumObj = obj.getAsJsonObject("album");
            if (albumObj != null && albumObj.has("name")) {
                album = albumObj.get("name").getAsString();
            }
        }
        if (obj.has("duration")) {
            duration = obj.get("duration").getAsLong();
        }
        return new Song(id, name, artist, album, duration);
    }

    private String weapiPost(String endpoint, String encryptedBody) throws IOException {
        return weapiPost(endpoint, encryptedBody, "https://music.163.com/weapi");
    }

    private String weapiPost(String endpoint, String encryptedBody, String baseUrl) throws IOException {
        String url = baseUrl + endpoint;
        HttpPost post = new HttpPost(url);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("Referer", "https://music.163.com/");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setHeader("X-Real-IP", "118.118.118.118");
        applyCookies(post);

        JsonObject json = GSON.fromJson(encryptedBody, JsonObject.class);
        String params = json.get("params").getAsString();
        String encSecKey = json.get("encSecKey").getAsString();
        String formBody = "params=" + java.net.URLEncoder.encode(params, "UTF-8")
                + "&encSecKey=" + java.net.URLEncoder.encode(encSecKey, "UTF-8");
        post.setEntity(new StringEntity(formBody, "UTF-8"));

        try (CloseableHttpResponse resp = client.execute(post)) {
            saveCookies(resp);
            int statusCode = resp.getStatusLine().getStatusCode();
            String respBody = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity(), "UTF-8") : "";
            plugin.getLogger().info("weapiPost " + endpoint + " -> " + statusCode + " (len=" + respBody.length() + ")");
            if (respBody.length() > 0 && respBody.length() < 500) {
                plugin.getLogger().info("weapiPost 响应: " + respBody);
            }
            return respBody;
        }
    }

    private String rawPost(String endpoint, String encryptedBody) throws Exception {
        String url = "https://music.163.com/weapi" + endpoint;
        plugin.getLogger().info("rawPost " + endpoint + " 尝试 HttpURLConnection...");

        // 先获取首页 Cookie
        java.net.HttpURLConnection pre = (java.net.HttpURLConnection) new java.net.URL("https://music.163.com/").openConnection();
        pre.setRequestProperty("User-Agent", USER_AGENT);
        pre.connect();
        for (java.util.Map.Entry<String, java.util.List<String>> h : pre.getHeaderFields().entrySet()) {
            if (h.getKey() != null && h.getKey().equalsIgnoreCase("Set-Cookie")) {
                for (String val : h.getValue()) {
                    String[] parts = val.split(";")[0].split("=", 2);
                    if (parts.length == 2) cookies.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        pre.disconnect();

        JsonObject json = GSON.fromJson(encryptedBody, JsonObject.class);
        String params = json.get("params").getAsString();
        String encSecKey = json.get("encSecKey").getAsString();
        String formBody = "params=" + java.net.URLEncoder.encode(params, "UTF-8")
                + "&encSecKey=" + java.net.URLEncoder.encode(encSecKey, "UTF-8");

        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Referer", "https://music.163.com/");
        conn.setRequestProperty("Origin", "https://music.163.com");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("X-Real-IP", "118.118.118.118");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        if (!cookies.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, String> e : cookies.entrySet()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
            conn.setRequestProperty("Cookie", sb.toString());
        }
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(formBody.getBytes("UTF-8"));
        }

        int statusCode = conn.getResponseCode();
        String contentEncoding = conn.getHeaderField("Content-Encoding");
        String respBody;
        try (java.io.InputStream raw = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            if (raw == null) respBody = "";
            else {
                java.io.InputStream decompressed = raw;
                if ("gzip".equalsIgnoreCase(contentEncoding)) {
                    decompressed = new java.util.zip.GZIPInputStream(raw);
                }
                java.util.Scanner s = new java.util.Scanner(decompressed, "UTF-8").useDelimiter("\\A");
                respBody = s.hasNext() ? s.next() : "";
            }
        }
        plugin.getLogger().info("rawPost " + endpoint + " -> " + statusCode + " (len=" + respBody.length() + ")" + (contentEncoding != null ? " enc=" + contentEncoding : ""));
        if (respBody.length() > 0 && respBody.length() < 500) {
            plugin.getLogger().info("rawPost 响应: " + respBody);
        }

        for (java.util.Map.Entry<String, java.util.List<String>> h : conn.getHeaderFields().entrySet()) {
            if (h.getKey() != null && h.getKey().equalsIgnoreCase("Set-Cookie")) {
                for (String val : h.getValue()) {
                    String[] parts = val.split(";")[0].split("=", 2);
                    if (parts.length == 2) cookies.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        conn.disconnect();
        return respBody;
    }

    private boolean isEmptyResponse(String r) {
        return r == null || r.isEmpty() || r.contains("\"code\":400");
    }

    private String simplePost(String endpoint, String jsonBody) throws Exception {
        return simplePost(endpoint, jsonBody, "https://music.163.com/api");
    }

    private String simplePost(String endpoint, String jsonBody, String baseUrl) throws Exception {
        String url = baseUrl + endpoint;
        plugin.getLogger().info("simplePost " + endpoint + " @" + baseUrl + " ...");

        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Referer", "https://music.163.com/");
        conn.setRequestProperty("Origin", "https://music.163.com");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int statusCode = conn.getResponseCode();
        String respBody;
        try (java.io.InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            if (is == null) respBody = "";
            else {
                java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                respBody = s.hasNext() ? s.next() : "";
            }
        }
        plugin.getLogger().info("simplePost " + endpoint + " -> " + statusCode + " (len=" + respBody.length() + ")");
        if (respBody.length() > 0 && respBody.length() < 500) {
            plugin.getLogger().info("simplePost 响应: " + respBody);
        }
        conn.disconnect();
        return respBody;
    }

    private String jsonPost(String endpoint, String encryptedBody) throws Exception {
        String url = "https://music.163.com/api" + endpoint;
        plugin.getLogger().info("jsonPost " + endpoint + " 尝试 JSON 格式...");

        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Referer", "https://music.163.com/");
        conn.setRequestProperty("Origin", "https://music.163.com");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-Real-IP", "118.118.118.118");
        if (!cookies.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, String> e : cookies.entrySet()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(e.getKey()).append("=").append(e.getValue());
            }
            conn.setRequestProperty("Cookie", sb.toString());
        }
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(encryptedBody.getBytes("UTF-8"));
        }

        int statusCode = conn.getResponseCode();
        String respBody;
        try (java.io.InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream()) {
            if (is == null) respBody = "";
            else {
                java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
                respBody = s.hasNext() ? s.next() : "";
            }
        }
        plugin.getLogger().info("jsonPost " + endpoint + " -> " + statusCode + " (len=" + respBody.length() + ")");
        if (respBody.length() > 0 && respBody.length() < 500) {
            plugin.getLogger().info("jsonPost 响应: " + respBody);
        }
        conn.disconnect();
        return respBody;
    }

    private String apiGet(String url) throws IOException {
        HttpGet get = new HttpGet(url);
        get.setHeader("User-Agent", USER_AGENT);
        get.setHeader("Referer", "https://music.163.com/");
        applyCookies(get);

        try (CloseableHttpResponse resp = client.execute(get)) {
            saveCookies(resp);
            return EntityUtils.toString(resp.getEntity(), "UTF-8");
        }
    }

    private void applyCookies(HttpGet get) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : cookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        if (sb.length() > 0) get.setHeader("Cookie", sb.toString());
    }

    private void applyCookies(HttpPost post) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : cookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        if (sb.length() > 0) post.setHeader("Cookie", sb.toString());
    }

    private void saveCookies(CloseableHttpResponse resp) {
        if (resp.getHeaders("Set-Cookie") != null) {
            for (org.apache.http.Header h : resp.getHeaders("Set-Cookie")) {
                String[] parts = h.getValue().split(";")[0].split("=", 2);
                if (parts.length == 2) {
                    cookies.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }
}