package com.fest.visuals.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import com.fest.visuals.api.system.backend.ClientInfo;
import com.fest.visuals.api.system.interfaces.QuickImports;

/**
 * Client interface sounds.
 *
 * <p>Played straight through the sound manager as UI sounds, not through the world: a world sound
 * is positional, needs a loaded level, and {@code Level#playSound} deliberately skips the player
 * passed to it — which is why routing these through the level left the player hearing nothing.
 * UI sounds also keep working on menus, where half of these are triggered.
 */
@UtilityClass
public class SoundUtil implements QuickImports {
    private final Identifier ENABLE_SMOOTH_SOUND = Identifier.parse(path() + "smooth_on");
    public SoundEvent ENABLE_SMOOTH_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_SMOOTH_SOUND);
    private final Identifier DISABLE_SMOOTH_SOUND = Identifier.parse(path() + "smooth_off");
    public SoundEvent DISABLE_SMOOTH_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_SMOOTH_SOUND);

    private final Identifier ENABLE_TECH_SOUND = Identifier.parse(path() + "tech_on");
    public SoundEvent ENABLE_TECH_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_TECH_SOUND);
    private final Identifier DISABLE_TECH_SOUND = Identifier.parse(path() + "tech_off");
    public SoundEvent DISABLE_TECH_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_TECH_SOUND);

    private final Identifier ENABLE_BLOP_SOUND = Identifier.parse(path() + "blop_on");
    public SoundEvent ENABLE_BLOP_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_BLOP_SOUND);
    private final Identifier DISABLE_BLOP_SOUND = Identifier.parse(path() + "blop_off");
    public SoundEvent DISABLE_BLOP_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_BLOP_SOUND);

    public void load() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_SMOOTH_SOUND, ENABLE_SMOOTH_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_SMOOTH_SOUND, DISABLE_SMOOTH_EVENT);

        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_TECH_SOUND, ENABLE_TECH_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_TECH_SOUND, DISABLE_TECH_EVENT);

        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_BLOP_SOUND, ENABLE_BLOP_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_BLOP_SOUND, DISABLE_BLOP_EVENT);
    }

    public void playSound(SoundEvent sound) {
        playSound(sound, 1f, 1f);
    }

    /** Pitch shifts the same clip for a related-but-different cue (opening versus closing). */
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (sound == null || volume <= 0f) return;
        if (mc.getSoundManager() == null) return;

        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private String path() {
        return ClientInfo.NAME.toLowerCase() + ":";
    }
}
