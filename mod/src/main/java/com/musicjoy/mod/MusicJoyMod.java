package com.musicjoy.mod;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(MusicJoyMod.MODID)
public class MusicJoyMod {
    public static final String MODID = "musicjoymod";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL = "1";
    private static int id = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "audio"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    // Pre-registered SoundEvent for temp audio (Android)
    public static final ResourceLocation TEMP_SOUND_LOC = new ResourceLocation(MODID, "temp_sound");
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final RegistryObject<SoundEvent> TEMP_SOUND = SOUNDS.register("temp_sound",
            () -> SoundEvent.createVariableRangeEvent(TEMP_SOUND_LOC));

    public MusicJoyMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SOUNDS.register(modBus);

        CHANNEL.registerMessage(id++, AudioPacket.StartPacket.class,
                AudioPacket.StartPacket::encode,
                AudioPacket.StartPacket::decode,
                AudioPacket.StartPacket::handle);
        CHANNEL.registerMessage(id++, AudioPacket.DataPacket.class,
                AudioPacket.DataPacket::encode,
                AudioPacket.DataPacket::decode,
                AudioPacket.DataPacket::handle);
        CHANNEL.registerMessage(id++, AudioPacket.StopPacket.class,
                AudioPacket.StopPacket::encode,
                AudioPacket.StopPacket::decode,
                AudioPacket.StopPacket::handle);
        LOGGER.info("MusicJoy Mod 已加载 - SimpleChannel 就绪");
    }
}