package com.fest.visuals.client.features.modules.render;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import lombok.Getter;

@ModuleRegister(name = "Animations", desc = "Шикарные анимации интерфейса", category = Category.RENDER)
public class AnimationsModule extends Module {
    @Getter private static final AnimationsModule instance = new AnimationsModule();

    public final BooleanSetting chat = new BooleanSetting("Чат").value(true);
    public final BooleanSetting inventory = new BooleanSetting("Инвентарь").value(true);
    public final BooleanSetting tabList = new BooleanSetting("Таб (Список игроков)").value(true);
    public final ModeSetting easing = new ModeSetting("Тип анимации").value("Плавный").values("Плавный", "С отскоком");
    public final SliderSetting speed = new SliderSetting("Скорость").value(300f).range(100f, 1000f).step(50f);

    public AnimationsModule() {
        addSettings(chat, inventory, tabList, easing, speed);
    }

    @Override
    public void onEvent() {}
}