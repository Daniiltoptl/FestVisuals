package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;

import com.bloom.client.config.BloomConfig;
import com.beash.atmospherics.AtmosphericsClient;

@ModuleRegister(name = "Atmospherics", desc = "\u0413\u0440\u0430\u0444\u043E\u043D", category = Category.RENDER)
public class VfxModule extends Module {
    @Getter private static final VfxModule instance = new VfxModule();

    private boolean synced;

    /**
     * Off by default: Atmospherics' own presets bring ground fog, a night sky full of stars and
     * heavy haze that players read as "no blocks" and green fog rising from the ground.
     */
    public VfxModule() {
    }

    /** Once, after configs are loaded, makes the two bundled mods match this module's state. */
    public void syncOnce() {
        if (synced) return;
        synced = true;
        try {
            boolean on = isEnabled();
            BloomConfig.get().enabled = on;
            AtmosphericsClient.getConfig().masterEnabled = on;
        } catch (Throwable ignored) {
            // the bundled mods are optional extras
        }
    }

    @Override
    public void onEvent() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        try {
            BloomConfig.get().enabled = true;
            BloomConfig.save();
            AtmosphericsClient.getConfig().masterEnabled = true;
            AtmosphericsClient.saveConfig();
        } catch (Throwable t) {
            // bundled mod unavailable: nothing to toggle
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            BloomConfig.get().enabled = false;
            BloomConfig.save();
            AtmosphericsClient.getConfig().masterEnabled = false;
            AtmosphericsClient.saveConfig();
        } catch (Throwable t) {
            // bundled mod unavailable: nothing to toggle
        }
    }
}
