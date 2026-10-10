package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;

/**
 * The vanilla sidebar, untouched, with the client's background behind it and a draggable position.
 * Nothing about the scoreboard's content is read or rebuilt: the game draws it as usual, only
 * shifted to where the widget was dragged, and its own dark fill is swapped for the blur panel.
 */
@ModuleRegister(name = "Scoreboard", desc = "Перемещаемое табло с фоном клиента", category = Category.HUD)
public class ScoreboardHudModule extends HudModule {
    @Getter private static final ScoreboardHudModule instance = new ScoreboardHudModule();

    public final BooleanSetting background = new BooleanSetting("Фон").value(true);

    public ScoreboardHudModule() {
        addSettings(background);
    }

    @Override
    protected String widgetName() {
        return "Scoreboard";
    }
}
