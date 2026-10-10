package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

@ModuleRegister(name = "Armor", desc = "Показывает броню и её прочность", category = Category.HUD)
public class ArmorHudModule extends HudModule {
    @Getter private static final ArmorHudModule instance = new ArmorHudModule();

    public static final String HORIZONTAL = "Горизонтально";
    public static final String VERTICAL = "Вертикально";

    public final ModeSetting orientation = new ModeSetting("Ориентация").values(HORIZONTAL, VERTICAL);
    public final BooleanSetting showDurability = new BooleanSetting("Прочность").value(true);
    public final SliderSetting scale = new SliderSetting("Масштаб").value(1.0f).range(0.6f, 1.6f).step(0.05f);

    public ArmorHudModule() {
        addSettings(orientation, showDurability, scale);
    }

    public boolean isVertical() { return orientation.is(VERTICAL); }

    @Override
    protected String widgetName() { return "Armor"; }
}
