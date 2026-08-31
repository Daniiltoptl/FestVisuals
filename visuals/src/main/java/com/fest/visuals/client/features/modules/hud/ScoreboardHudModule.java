package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;

/**
 * Replaces the vanilla sidebar with a draggable copy.
 *
 * <p>The layout stays vanilla — title centred, names left, scores right in red — so it reads the
 * same as on any server; what changes is that it can be moved and that the background is the
 * client's. Turning the module off puts the untouched vanilla sidebar back.
 */
@ModuleRegister(name = "Scoreboard", desc = "Перемещаемая панель счёта", category = Category.HUD)
public class ScoreboardHudModule extends HudModule {
    @Getter private static final ScoreboardHudModule instance = new ScoreboardHudModule();

    public final BooleanSetting background = new BooleanSetting("Фон").value(true);
    public final BooleanSetting numbers = new BooleanSetting("Очки").value(true);
    public final BooleanSetting vanillaNumbers = new BooleanSetting("Красные очки").value(true)
            .setVisible(numbers::getValue);
    public final SliderSetting fontScale = new SliderSetting("Масштаб шрифта").value(1.0f).range(0.6f, 1.6f).step(0.05f);

    public ScoreboardHudModule() {
        addSettings(background, numbers, vanillaNumbers, fontScale);
    }

    @Override
    protected String widgetName() {
        return "Scoreboard";
    }
}
