package com.fest.visuals.client.features.modules.utility;

import lombok.Getter;
import net.minecraft.sounds.SoundEvent;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.other.SoundUtil;

/**
 * Sound feedback for the client interface: modules toggling, the GUI opening and closing, and the
 * small interactions inside it.
 *
 * <p>Every cue comes from the same pair of clips per theme. Pitch separates them — a card opening
 * is the same sample as a module switching on, played higher — which keeps the asset list short
 * while still making the sounds distinguishable.
 */
@ModuleRegister(name = "Sounds", desc = "Звуки интерфейса и модулей", category = Category.OTHER)
public class SoundsModule extends Module {
    @Getter private static final SoundsModule instance = new SoundsModule();

    public final ModeSetting theme = new ModeSetting("Тема").value("Smooth").values("Smooth", "Blop", "Tech");
    public final BooleanSetting modules = new BooleanSetting("Модули").value(true);
    public final BooleanSetting screens = new BooleanSetting("Открытие меню").value(true);
    public final BooleanSetting interactions = new BooleanSetting("Действия в меню").value(true);
    public final SliderSetting volume = new SliderSetting("Громкость").value(60f).range(5f, 100f).step(5f);

    public SoundsModule() {
        addSettings(theme, modules, screens, interactions, volume);
    }

    /** Called from {@code Module#setEnabled} whenever any other module is toggled. */
    public void playToggleSound(boolean enabling) {
        if (!isEnabled() || !modules.getValue()) return;
        play(enabling, 1f);
    }

    /** The click GUI appearing or going away. */
    public void playScreenSound(boolean opening) {
        if (!isEnabled() || !screens.getValue()) return;
        play(opening, opening ? 0.9f : 0.8f);
    }

    /** Buttons, switches and cards inside the GUI. */
    public void playClickSound(boolean positive) {
        if (!isEnabled() || !interactions.getValue()) return;
        play(positive, 1.25f);
    }

    private void play(boolean on, float pitch) {
        SoundUtil.playSound(event(on), volume.getValue() / 100f, pitch);
    }

    private SoundEvent event(boolean on) {
        return switch (theme.getValue()) {
            case "Blop" -> on ? SoundUtil.ENABLE_BLOP_EVENT : SoundUtil.DISABLE_BLOP_EVENT;
            case "Tech" -> on ? SoundUtil.ENABLE_TECH_EVENT : SoundUtil.DISABLE_TECH_EVENT;
            default -> on ? SoundUtil.ENABLE_SMOOTH_EVENT : SoundUtil.DISABLE_SMOOTH_EVENT;
        };
    }

    @Override
    public void onEvent() {
    }
}
