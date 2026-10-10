package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.MultiBooleanSetting;

import java.util.Arrays;

/**
 * Switches off pieces of the vanilla screen and camera that get in the way in a fight.
 *
 * <p>Each option cancels one vanilla draw call through a mixin; the client's own replacements
 * (the draggable scoreboard, for one) honour the same switches, so "remove the scoreboard" means
 * no scoreboard at all rather than swapping vanilla's for ours.
 */
@ModuleRegister(name = "Removals", desc = "Убирает лишние элементы игры", category = Category.RENDER)
public class RemovalsModule extends Module {
    @Getter private static final RemovalsModule instance = new RemovalsModule();

    private static final String FIRE = "Огонь на экране";
    private static final String IN_WALL = "Блок перед глазами";
    private static final String WATER = "Вода на экране";
    private static final String SCOREBOARD = "Табло (скорборд)";
    private static final String BOSS_BAR = "Боссбар";
    private static final String HURT_CAM = "Тряска при уроне";
    private static final String VIGNETTE = "Виньетка";
    private static final String PUMPKIN = "Тыква и снег";
    private static final String PORTAL = "Портал и тошнота";
    private static final String TOTEM = "Анимация тотема";
    private static final String WEATHER = "Дождь и снег";

    private final MultiBooleanSetting remove = new MultiBooleanSetting("Убрать").value(
            Arrays.stream(new String[]{FIRE, IN_WALL, WATER, SCOREBOARD, BOSS_BAR, HURT_CAM, VIGNETTE, PUMPKIN, PORTAL, TOTEM, WEATHER})
                    .map(name -> new BooleanSetting(name).value(false))
                    .toArray(BooleanSetting[]::new)
    );

    public RemovalsModule() {
        addSettings(remove);
    }

    private boolean on(String option) {
        return isEnabled() && remove.isEnabled(option);
    }

    public boolean isFireOverlay()   { return on(FIRE); }
    public boolean isInwallOverlay() { return on(IN_WALL); }
    public boolean isWaterOverlay()  { return on(WATER); }
    public boolean isScoreboard()    { return on(SCOREBOARD); }
    public boolean isBossBar()       { return on(BOSS_BAR); }
    public boolean isHurtCam()       { return on(HURT_CAM); }
    public boolean isVignette()      { return on(VIGNETTE); }
    public boolean isPumpkin()       { return on(PUMPKIN); }
    public boolean isPortal()        { return on(PORTAL); }
    public boolean isTotem()         { return on(TOTEM); }
    public boolean isWeather()       { return on(WEATHER); }

    @Override
    public void onEvent() {
    }
}
