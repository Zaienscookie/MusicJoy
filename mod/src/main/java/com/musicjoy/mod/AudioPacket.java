package com.musicjoy.mod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AudioPacket {

    public static class StartPacket {
        public final int totalSize;
        public final String songName;

        public StartPacket(int totalSize, String songName) {
            this.totalSize = totalSize;
            this.songName = songName;
        }

        public static void encode(StartPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.totalSize);
            buf.writeUtf(packet.songName);
        }

        public static StartPacket decode(FriendlyByteBuf buf) {
            return new StartPacket(buf.readInt(), buf.readUtf());
        }

        public static void handle(StartPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAudioHandler.onStart(packet))
            );
            ctx.get().setPacketHandled(true);
        }
    }

    public static class DataPacket {
        public final int sequence;
        public final byte[] data;

        public DataPacket(int sequence, byte[] data) {
            this.sequence = sequence;
            this.data = data;
        }

        public static void encode(DataPacket packet, FriendlyByteBuf buf) {
            buf.writeInt(packet.sequence);
            buf.writeInt(packet.data.length);
            buf.writeBytes(packet.data);
        }

        public static DataPacket decode(FriendlyByteBuf buf) {
            int sequence = buf.readInt();
            int len = buf.readInt();
            byte[] data = new byte[len];
            buf.readBytes(data);
            return new DataPacket(sequence, data);
        }

        public static void handle(DataPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAudioHandler.onData(packet))
            );
            ctx.get().setPacketHandled(true);
        }
    }

    public static class StopPacket {
        public static void encode(StopPacket packet, FriendlyByteBuf buf) {
        }

        public static StopPacket decode(FriendlyByteBuf buf) {
            return new StopPacket();
        }

        public static void handle(StopPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientAudioHandler.onStop())
            );
            ctx.get().setPacketHandled(true);
        }
    }
}