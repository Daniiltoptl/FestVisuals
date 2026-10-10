package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

@ModuleRegister(name = "Cooldowns", desc = "Откат зелий и способностей", category = Category.HUD)
public class CooldownsHudModule extends HudModule {
    @Getter private static final CooldownsHudModule instance = new CooldownsHudModule();

    public final BooleanSetting showProgress = new BooleanSetting("Полоска прогресса").value(true);
    public final BooleanSetting showSeconds = new BooleanSetting("Показывать секунды").value(true);
    public final BooleanSetting alwaysShow = new BooleanSetting("Всегда показывать").value(false);
    public final SliderSetting fontScale = new SliderSetting("Масштаб шрифта").value(1.0f).range(0.6f, 1.6f).step(0.05f);

    public CooldownsHudModule() {
        addSettings(showProgress, showSeconds, alwaysShow, fontScale);
    }

    @Override
    protected String widgetName() { return "Cooldowns"; }
}
