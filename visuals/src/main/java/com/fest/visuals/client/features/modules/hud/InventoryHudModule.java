package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

@ModuleRegister(name = "Inventory", desc = "Показывает ваш инвентарь на экране", category = Category.HUD)
public class InventoryHudModule extends HudModule {
    @Getter private static final InventoryHudModule instance = new InventoryHudModule();

    public final BooleanSetting showSlots = new BooleanSetting("Ячейки").value(true);
    public final BooleanSetting showCounts = new BooleanSetting("Количество").value(true);
    public final SliderSetting scale = new SliderSetting("Масштаб").value(1.0f).range(0.6f, 1.6f).step(0.05f);

    public InventoryHudModule() {
        addSettings(showSlots, showCounts, scale);
    }

    @Override
    protected String widgetName() { return "Inventory"; }
}
