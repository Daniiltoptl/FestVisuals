package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

@ModuleRegister(name = "Target HUD", desc = "Информация о выбранной цели", category = Category.HUD)
public class TargetHudModule extends HudModule {
    @Getter private static final TargetHudModule instance = new TargetHudModule();

    public final BooleanSetting showHead = new BooleanSetting("Голова").value(true);
    public final BooleanSetting showHealth = new BooleanSetting("Полоска HP").value(true);
    public final BooleanSetting showAbsorption = new BooleanSetting("Абсорбция").value(true);
    public final SliderSetting scale = new SliderSetting("Масштаб").value(1.0f).range(0.6f, 1.8f).step(0.05f);

    public TargetHudModule() {
        addSettings(showHead, showHealth, showAbsorption, scale);
    }

    @Override
    protected String widgetName() { return "Target info"; }
}
