package com.fest.visuals.client.features.modules.render;

import lombok.Getter;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.SliderSetting;

/**
 * Chunks fade in as they load instead of popping into existence.
 *
 * <p>Minecraft 26.2 already fades new chunk sections in (0.75 s by default), and Sodium reads the
 * very same option, so there is nothing to replace — this drives that duration, up to the two
 * seconds the game accepts, and hands the player's own value back when switched off. The effect
 * only shows on chunks that load after it is changed.
 */
@ModuleRegister(name = "Chunks Fade In", desc = "Плавное появление чанков при загрузке", category = Category.RENDER)
public class ChunkFadeModule extends Module {
    @Getter private static final ChunkFadeModule instance = new ChunkFadeModule();

    /** The option is stored in 1/20 s steps and capped at two seconds by the game itself. */
    public final SliderSetting duration = new SliderSetting("Длительность, сек").value(2f).range(0.05f, 2f).step(0.05f)
            .onAction(this::apply);

    private Double previous;

    public ChunkFadeModule() {
        addSettings(duration);
    }

    @Override
    public void onEvent() {
    }

    @Override
    public void onEnable() {
        if (mc.options == null) return;
        if (previous == null) previous = mc.options.chunkSectionFadeInTime().get();
        apply();
    }

    @Override
    public void onDisable() {
        if (mc.options != null && previous != null) mc.options.chunkSectionFadeInTime().set(previous);
        previous = null;
    }

    private void apply() {
        if (!isEnabled() || mc.options == null) return;
        double seconds = Math.round(duration.getValue() * 20f) / 20.0;
        mc.options.chunkSectionFadeInTime().set(Math.max(0.0, Math.min(2.0, seconds)));
    }
}
