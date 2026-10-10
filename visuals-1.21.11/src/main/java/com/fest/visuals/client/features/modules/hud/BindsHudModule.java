package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

@ModuleRegister(name = "Binds", desc = "Список включённых функций и их клавиш", category = Category.HUD)
public class BindsHudModule extends HudModule {
    @Getter private static final BindsHudModule instance = new BindsHudModule();

    public final BooleanSetting showKeyPill = new BooleanSetting("Обрамление клавиши").value(true);
    public final BooleanSetting alwaysShow = new BooleanSetting("Всегда показывать").value(false);
    public final SliderSetting fontScale = new SliderSetting("Масштаб шрифта").value(1.0f).range(0.6f, 1.6f).step(0.05f);

    public BindsHudModule() {
        addSettings(showKeyPill, alwaysShow, fontScale);
    }

    @Override
    protected String widgetName() { return "Keybinds"; }
}
