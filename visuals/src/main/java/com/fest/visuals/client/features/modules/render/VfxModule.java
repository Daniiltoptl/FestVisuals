package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;

import com.bloom.client.config.BloomConfig;
import com.beash.atmospherics.AtmosphericsClient;

@ModuleRegister(name = "VFX", desc = "\u0413\u0440\u0430\u0444\u043E\u043D", category = Category.RENDER)
public class VfxModule extends Module {
    @Getter private static final VfxModule instance = new VfxModule();

    public VfxModule() {
        this.setEnabled(true, false);
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
            t.printStackTrace();
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
            t.printStackTrace();
        }
    }
}
