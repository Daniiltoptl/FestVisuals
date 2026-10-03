package com.fest.visuals.client.features.modules.render;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import lombok.Getter;

import java.awt.*;

@ModuleRegister(name = "HitColor", desc = "Изменяет цвет при получении урона.", category = Category.RENDER)
public class HitColorModule extends Module {
    @Getter private static final HitColorModule instance = new HitColorModule();

    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 0, 0, 150));
    public final ModeSetting target = new ModeSetting("Цель")
            .values("Скин", "Броня", "Скин и броня")
            .value("Скин и броня");

    public HitColorModule() {
        addSettings(color, target);
    }
    
    @Override
    public void onEvent() {}
}
