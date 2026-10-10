package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.module.setting.ModeSetting;

@ModuleRegister(name = "Dynamic Island", desc = "Динамический остров в стиле iOS", category = Category.HUD)
public class DynamicIslandModule extends HudModule {
    @Getter private static final DynamicIslandModule instance = new DynamicIslandModule();

    public final ModeSetting displayMode = new ModeSetting("Режим").value("Авто").values("Авто", "Только Остров", "Только Плеер");
    public final BooleanSetting showClock = new BooleanSetting("Часы").value(true);
    public final BooleanSetting showPing = new BooleanSetting("Пинг").value(true);
    public final SliderSetting maxToasts = new SliderSetting("Макс. уведомлений").value(3f).range(1f, 6f).step(1f);
    public final SliderSetting lifetime = new SliderSetting("Время показа (с)").value(3.2f).range(1.0f, 8.0f).step(0.2f);
    public final BooleanSetting showState = new BooleanSetting("Показывать статусы").value(true);
    public final SliderSetting fontScale = new SliderSetting("Размер шрифта").value(1.0f).range(0.6f, 1.6f).step(0.05f);

    public DynamicIslandModule() {
        addSettings(displayMode, showClock, showPing, maxToasts, lifetime, showState, fontScale);
    }

    @Override
    protected String widgetName() { return "Dynamic Island"; }
}