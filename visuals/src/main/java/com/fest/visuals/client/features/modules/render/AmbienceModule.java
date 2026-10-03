package com.fest.visuals.client.features.modules.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.fog.FogData;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.module.setting.ModeSetting;

import java.awt.Color;

@ModuleRegister(name = "Ambience", desc = "Управление погодой и временем", category = Category.RENDER)
public class AmbienceModule extends Module {
    @Getter private static final AmbienceModule instance = new AmbienceModule();

    @AllArgsConstructor
    public enum Weather implements ModeSetting.NamedChoice {
        NO_CHANGE("Без изменений"),
        SUNNY("Ясно"),
        RAINY("Дождь"),
        SNOWY("Снег"),
        THUNDER("Гроза");
        private final String name;
        @Override public String getName() { return name; }
    }

    private final SliderSetting time = new SliderSetting("Время").value(-1f).range(-1f, 24000f).step(100f);
    public final ModeSetting weather = new ModeSetting("Погода").value(Weather.SUNNY).values(Weather.values());
    public AmbienceModule() {
        addSettings(time, weather);
    }

    public long getTime(long original) {
        if (mc.level == null || !isEnabled()) return original;
        float val = time.getValue();
        if (val < 0) return original;
        return (long) val;
    }

    

    @Override public void onEvent() {}
}