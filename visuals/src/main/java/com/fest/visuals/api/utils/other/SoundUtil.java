package com.fest.visuals.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import com.fest.visuals.api.system.backend.ClientInfo;
import com.fest.visuals.api.system.interfaces.QuickImports;

@UtilityClass
public class SoundUtil implements QuickImports {
    // smooth toggle sound
    private final Identifier ENABLE_SMOOTH_SOUND = Identifier.parse(path() + "smooth_on");
    public SoundEvent ENABLE_SMOOTH_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_SMOOTH_SOUND);
    private final Identifier DISABLE_SMOOTH_SOUND = Identifier.parse(path() + "smooth_off");
    public SoundEvent DISABLE_SMOOTH_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_SMOOTH_SOUND);

    // cel toggle sound
    private final Identifier ENABLE_CEL_SOUND = Identifier.parse(path() + "celestial_on");
    public SoundEvent ENABLE_CEL_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_CEL_SOUND);
    private final Identifier DISABLE_CEL_SOUND = Identifier.parse(path() + "celestial_off");
    public SoundEvent DISABLE_CEL_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_CEL_SOUND);

    // nur toggle sound
    private final Identifier ENABLE_NU_SOUND = Identifier.parse(path() + "nursultan_on");
    public SoundEvent ENABLE_NU_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_NU_SOUND);
    private final Identifier DISABLE_NU_SOUND = Identifier.parse(path() + "nursultan_off");
    public SoundEvent DISABLE_NU_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_NU_SOUND);

    // akrien toggle sound
    private final Identifier ENABLE_AK_SOUND = Identifier.parse(path() + "akrien_on");
    public SoundEvent ENABLE_AK_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_AK_SOUND);
    private final Identifier DISABLE_AK_SOUND = Identifier.parse(path() + "akrien_off");
    public SoundEvent DISABLE_AK_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_AK_SOUND);

    // tech toggle sound
    private final Identifier ENABLE_TECH_SOUND = Identifier.parse(path() + "tech_on");
    public SoundEvent ENABLE_TECH_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_TECH_SOUND);
    private final Identifier DISABLE_TECH_SOUND = Identifier.parse(path() + "tech_off");
    public SoundEvent DISABLE_TECH_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_TECH_SOUND);

    // blop toggle sound
    private final Identifier ENABLE_BLOP_SOUND = Identifier.parse(path() + "blop_on");
    public SoundEvent ENABLE_BLOP_EVENT = SoundEvent.createVariableRangeEvent(ENABLE_BLOP_SOUND);
    private final Identifier DISABLE_BLOP_SOUND = Identifier.parse(path() + "blop_off");
    public SoundEvent DISABLE_BLOP_EVENT = SoundEvent.createVariableRangeEvent(DISABLE_BLOP_SOUND);

    public void load() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_SMOOTH_SOUND, ENABLE_SMOOTH_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_SMOOTH_SOUND, DISABLE_SMOOTH_EVENT);

        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_CEL_SOUND, ENABLE_CEL_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_CEL_SOUND, DISABLE_CEL_EVENT);

        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_NU_SOUND, ENABLE_NU_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_NU_SOUND, DISABLE_NU_EVENT);

        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_AK_SOUND, ENABLE_AK_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_AK_SOUND, DISABLE_AK_EVENT);

        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_TECH_SOUND, ENABLE_TECH_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_TECH_SOUND, DISABLE_TECH_EVENT);

        Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE_BLOP_SOUND, ENABLE_BLOP_EVENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE_BLOP_SOUND, DISABLE_BLOP_EVENT);
    }

    public void playSound(SoundEvent sound) {
        if (mc.player != null && mc.level != null && mc.getCameraEntity() != null)
            mc.level.playSound(mc.player, mc.getCameraEntity().blockPosition(), sound, SoundSource.BLOCKS, 1.0f, 1f);
    }

    private String path() {
        return ClientInfo.NAME.toLowerCase() + ":";
    }
}
