package com.fest.visuals.client.features.modules.render;

import java.awt.Color;

import lombok.Getter;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

/**
 * Translucent faceted wings on the player's own back (third person). Drawn by
 * {@link com.fest.visuals.client.features.modules.render.wings.WingsLayer}, a layer of the player
 * renderer, so they follow the body's rotation, crouching and poses. Other players never get them.
 */
@ModuleRegister(name = "Wings", desc = "3D-крылья на твоей спине (видно с вида от третьего лица)", category = Category.RENDER)
public class WingsModule extends Module {
    @Getter private static final WingsModule instance = new WingsModule();

    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 70, 70, 110));
    public final SliderSetting scale = new SliderSetting("Размер").value(1.0f).range(0.5f, 1.6f).step(0.05f);
    public final BooleanSetting flap = new BooleanSetting("Взмахи").value(true);
    public final SliderSetting flapSpeed = new SliderSetting("Скорость взмахов").value(1.0f).range(0.2f, 3.0f).step(0.1f)
            .setVisible(flap::getValue);
    public final BooleanSetting outline = new BooleanSetting("Контур").value(true);

    public WingsModule() {
        addSettings(color, scale, flap, flapSpeed, outline);
    }

    @Override
    public void onEvent() {
    }
}
