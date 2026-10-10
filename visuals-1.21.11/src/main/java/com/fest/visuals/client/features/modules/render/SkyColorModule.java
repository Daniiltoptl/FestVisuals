package com.fest.visuals.client.features.modules.render;

import java.awt.Color;
import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;

@ModuleRegister(name = "Sky Color", desc = "\u041C\u0435\u043D\u044F\u0435\u0442 \u0446\u0432\u0435\u0442 \u043D\u0435\u0431\u0430", category = Category.RENDER)
public class SkyColorModule extends Module {
    @Getter private static final SkyColorModule instance = new SkyColorModule();

    public final BooleanSetting themeColor = new BooleanSetting("\u0426\u0432\u0435\u0442 \u0442\u0435\u043C\u044B").value(true);
    public final ColorSetting color = new ColorSetting("\u0426\u0432\u0435\u0442").value(new Color(150, 180, 255, 255))
            .setVisible(() -> !themeColor.getValue());

    public SkyColorModule() {
        addSettings(themeColor, color);
    }
    
    @Override
    public void onEvent() {}
}
