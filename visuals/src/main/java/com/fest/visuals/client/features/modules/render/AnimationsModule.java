package com.fest.visuals.client.features.modules.render;

import lombok.Getter;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.animation.Easing;

/**
 * Opening and closing animations for the vanilla interface.
 *
 * <p>The mixins that drive chat, the tab list and the screens all read their curve and duration
 * from here, so one setting changes every animation at once.
 */
@ModuleRegister(name = "Animations", desc = "Анимации интерфейса Minecraft", category = Category.RENDER)
public class AnimationsModule extends Module {
    @Getter private static final AnimationsModule instance = new AnimationsModule();

    public final BooleanSetting screens = new BooleanSetting("Экраны").value(true);
    public final BooleanSetting containers = new BooleanSetting("Сундуки и инвентарь").value(true);
    public final BooleanSetting chat = new BooleanSetting("Чат").value(true);
    public final BooleanSetting tabList = new BooleanSetting("Таб-лист").value(true);

    public final ModeSetting style = new ModeSetting("Стиль")
            .value("Масштаб").values("Масштаб", "Сдвиг", "Вместе");
    public final ModeSetting curve = new ModeSetting("Кривая")
            .value("Плавная").values("Плавная", "С отскоком", "Резкая");
    public final SliderSetting speed = new SliderSetting("Длительность (мс)").value(320f).range(80f, 1200f).step(20f);
    public final SliderSetting intensity = new SliderSetting("Сила").value(100f).range(20f, 200f).step(5f);

    public AnimationsModule() {
        addSettings(screens, containers, chat, tabList, style, curve, speed, intensity);
    }

    public long duration() {
        return (long) speed.getValue().floatValue();
    }

    /** How far a screen travels and how small it starts, as a multiplier around 1. */
    public float strength() {
        return intensity.getValue() / 100f;
    }

    public Easing openingCurve() {
        return switch (curve.getValue()) {
            case "С отскоком" -> Easing.BACK_OUT;
            case "Резкая" -> Easing.EXPO_OUT;
            default -> Easing.CUBIC_OUT;
        };
    }

    public Easing closingCurve() {
        return curve.is("Резкая") ? Easing.EXPO_IN : Easing.CUBIC_IN;
    }

    public boolean scales() {
        return !style.is("Сдвиг");
    }

    public boolean slides() {
        return !style.is("Масштаб");
    }

    @Override
    public void onEvent() {
    }
}
