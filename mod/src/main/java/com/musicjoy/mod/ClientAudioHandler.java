package com.musicjoy.mod;

import com.mojang.logging.LogUtils;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ClientAudioHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<byte[]> chunks = new ArrayList<>();
    private static int totalSize = 0;
    private static int expectedSize = 0;
    private static String currentSong = "";
    private static Thread playThread;
    private static SourceDataLine audioLine;

    // Pre-created OpenAL objects (from SoundEngineMixin)
    private static int alBuffer = -1;
    private static int alSource = -1;
    private static volatile boolean alPlaying = false;

    private static volatile List<byte[]> pendingPcmChunks = null;
    private static volatile int pendingRate = 44100;
    private static volatile int pendingChannels = 2;
    private static volatile String pendingSongName = "";

    public static void setAlBuffer(int buf, int src) {
        alBuffer = buf;
        alSource = src;
    }

    public static void onStart(AudioPacket.StartPacket packet) {
        chunks.clear();
        totalSize = 0;
        expectedSize = packet.totalSize;
        currentSong = packet.songName;
        LOGGER.info("MusicJoy: 开始接收 - {} ({} bytes)", currentSong, expectedSize);
    }

    public static void onData(AudioPacket.DataPacket packet) {
        chunks.add(packet.data);
        totalSize += packet.data.length;
    }

    public static void onStop() {
        if (playThread != null && playThread.isAlive()) {
            stop();
            LOGGER.info("MusicJoy: 强制中止 - {}", currentSong);
            return;
        }
        if (totalSize > 0) {
            LOGGER.info("MusicJoy: 接收完毕 ({} bytes), 开始播放 - {}", totalSize, currentSong);
            playMp3();
        } else {
            LOGGER.info("MusicJoy: 收到停止信号, 无数据");
        }
    }

    public static void onSoundEngineTick(Object soundEngine) {
        if (alBuffer == -1 || alSource == -1) return;

        if (pendingPcmChunks != null) {
            List<byte[]> pcmChunks = pendingPcmChunks;
            int rate = pendingRate;
            int ch = pendingChannels;
            String name = pendingSongName;
            pendingPcmChunks = null;

            try {
                int fmt = ch == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                int totalBytes = 0;
                for (byte[] p : pcmChunks) totalBytes += p.length;
                byte[] allPcm = new byte[totalBytes];
                int offset = 0;
                for (byte[] p : pcmChunks) {
                    System.arraycopy(p, 0, allPcm, offset, p.length);
                    offset += p.length;
                }
                pcmChunks.clear();

                AL10.alSourceStop(alSource);
                AL10.alBufferData(alBuffer, fmt, ByteBuffer.wrap(allPcm), rate);
                AL10.alSourcei(alSource, AL10.AL_BUFFER, alBuffer);
                AL10.alSourcePlay(alSource);
                alPlaying = true;
                LOGGER.info("MusicJoy: OpenAL 播放中 - {} ({} bytes)", name, totalBytes);
            } catch (Throwable e) {
                LOGGER.error("MusicJoy: OpenAL 播放失败", e);
            }
        }

        // Check if playback ended
        if (alPlaying && alSource != -1) {
            int state = AL10.alGetSourcei(alSource, AL10.AL_SOURCE_STATE);
            if (state != AL10.AL_PLAYING) {
                alPlaying = false;
                LOGGER.info("MusicJoy: OpenAL 播放结束");
            }
        }
    }

    private static void playMp3() {
        byte[][] savedChunks = chunks.toArray(new byte[0][]);
        int savedTotal = totalSize;
        chunks.clear();
        totalSize = 0;
        stop();

        byte[] allData = new byte[savedTotal];
        int offset = 0;
        for (byte[] chunk : savedChunks) {
            System.arraycopy(chunk, 0, allData, offset, chunk.length);
            offset += chunk.length;
        }

        LOGGER.info("MusicJoy: 头字节: {} {} {} {} {}",
                String.format("%02X", allData[0] & 0xFF),
                String.format("%02X", allData[1] & 0xFF),
                String.format("%02X", allData[2] & 0xFF),
                String.format("%02X", allData[3] & 0xFF),
                String.format("%02X", allData[4] & 0xFF));

        byte[] audioData = allData;
        String songName = currentSong;

        try {
            playThread = new Thread(() -> {
                if (isJavaSoundSupported()) {
                    try {
                        playWithSourceDataLine(audioData, songName);
                        return;
                    } catch (Throwable e) {
                        LOGGER.error("MusicJoy: JavaSound 播放失败", e);
                    }
                }
                try {
                    decodeAndQueueForOpenAL(audioData, songName);
                } catch (Throwable e) {
                    LOGGER.error("MusicJoy: 解码失败", e);
                }
            }, "MusicJoy-Player");
            playThread.setDaemon(true);
            playThread.start();
        } catch (Exception e) {
            LOGGER.error("MusicJoy: 初始化失败", e);
        }
    }

    private static boolean isJavaSoundSupported() {
        try {
            AudioFormat fmt = new AudioFormat(44100.0f, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            return AudioSystem.isLineSupported(info);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void playWithSourceDataLine(byte[] audioData, String songName) throws Exception {
        Bitstream bitstream = new Bitstream(new ByteArrayInputStream(audioData));
        Decoder decoder = new Decoder();
        SourceDataLine line = null;
        int frameCount = 0;
        while (true) {
            Header header = bitstream.readFrame();
            if (header == null) { LOGGER.info("MusicJoy: 无更多帧, 共 {} 帧", frameCount); break; }
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            if (line == null) {
                int sr = header.frequency(), ch = output.getChannelCount();
                AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sr, 16, ch, ch * 2, sr, false);
                line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, fmt));
                line.open(fmt, 65536); line.start(); audioLine = line;
            }
            short[] buf = output.getBuffer();
            int len = output.getBufferLength();
            float vol = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
            byte[] bytes = new byte[len * 2];
            for (int i = 0; i < len; i++) {
                int s = (int) (buf[i] * vol);
                if (s > 32767) s = 32767; if (s < -32768) s = -32768;
                bytes[i * 2] = (byte) (s & 0xFF); bytes[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            }
            line.write(bytes, 0, len * 2);
            if (frameCount == 0) LOGGER.info("MusicJoy: 音量={}", vol);
            bitstream.closeFrame(); frameCount++;
        }
        if (line != null) { line.drain(); line.stop(); line.close(); audioLine = null; }
        LOGGER.info("MusicJoy: 播放结束 - {} ({} frames)", songName, frameCount);
    }

    // 解码 PCM, 排队等 onSoundEngineTick 播放
    private static void decodeAndQueueForOpenAL(byte[] audioData, String songName) throws Exception {
        Bitstream bitstream = new Bitstream(new ByteArrayInputStream(audioData));
        Decoder decoder = new Decoder();
        int sampleRate = 44100, channels = 2, frameCount = 0;
        int chunkBytes = 44100 * 2 * 2;
        List<byte[]> pcmChunks = new ArrayList<>();
        ByteArrayOutputStream chunk = new ByteArrayOutputStream(chunkBytes);
        float vol = Math.max(0f, Math.min(Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS), 1f));

        while (true) {
            Header header = bitstream.readFrame();
            if (header == null) break;
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            if (frameCount == 0) { sampleRate = header.frequency(); channels = output.getChannelCount(); }
            short[] buf = output.getBuffer();
            int len = output.getBufferLength();
            for (int i = 0; i < len; i++) {
                int s = (int) (buf[i] * vol);
                if (s > 32767) s = 32767; if (s < -32768) s = -32768;
                chunk.write(s & 0xFF);
                chunk.write((s >> 8) & 0xFF);
            }
            if (chunk.size() >= chunkBytes) {
                pcmChunks.add(chunk.toByteArray());
                chunk = new ByteArrayOutputStream(chunkBytes);
            }
            bitstream.closeFrame(); frameCount++;
        }
        byte[] last = chunk.toByteArray();
        if (last.length > 0) pcmChunks.add(last);

        int totalBytes = 0;
        for (byte[] p : pcmChunks) totalBytes += p.length;
        LOGGER.info("MusicJoy: 解码完成 {} 帧, {} 分片, {} bytes, rate={}, ch={}", frameCount, pcmChunks.size(), totalBytes, sampleRate, channels);

        pendingPcmChunks = pcmChunks;
        pendingRate = sampleRate;
        pendingChannels = channels;
        pendingSongName = songName;
    }

    public static void stop() {
        if (audioLine != null) { audioLine.stop(); audioLine.close(); audioLine = null; }
        if (playThread != null) { playThread.interrupt(); playThread = null; }
        if (alSource != -1) {
            AL10.alSourceStop(alSource);
        }
        chunks.clear(); totalSize = 0;
        alPlaying = false;
    }
}