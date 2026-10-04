package com.musicjoy.mod.mixin;

import com.musicjoy.mod.ClientAudioHandler;
import com.musicjoy.mod.MusicJoyMod;
import net.minecraft.client.sounds.SoundEngine;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Unique
    private static boolean musicjoy_alInitialized = false;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void onInit(CallbackInfo ci) {
        if (!musicjoy_alInitialized) {
            musicjoy_alInitialized = true;
            try {
                int buf = AL10.alGenBuffers();
                int src = AL10.alGenSources();
                AL10.alSourcei(src, AL10.AL_BUFFER, buf);
                ClientAudioHandler.setAlBuffer(buf, src);
                MusicJoyMod.LOGGER.info("MusicJoy: OpenAL 预创建 buffer={} source={}", buf, src);
            } catch (Throwable e) {
                MusicJoyMod.LOGGER.error("MusicJoy: OpenAL 预创建失败", e);
            }
        }
    }

    @Inject(method = "m_120302_", at = @At("HEAD"), remap = false)
    private void onTick(boolean p_120425_, CallbackInfo ci) {
        ClientAudioHandler.onSoundEngineTick(this);
    }
}