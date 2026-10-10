package com.fest.visuals.client.features.modules.hud;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.client.ui.widget.Widget;
import com.fest.visuals.client.ui.widget.WidgetManager;

/**
 * Combined stats readout — toggles the FPS, BPS and coordinates widgets.
 * Each widget can be shown independently through settings.
 */
@ModuleRegister(name = "Stats", desc = "ФПС, пинг, координаты и скорость", category = Category.HUD)
public class StatsHudModule extends Module {
    @Getter private static final StatsHudModule instance = new StatsHudModule();

    public final BooleanSetting fps = new BooleanSetting("FPS").value(true).onAction(() -> instance.sync());
    public final BooleanSetting bps = new BooleanSetting("BPS").value(true).onAction(() -> instance.sync());
    public final BooleanSetting coords = new BooleanSetting("Координаты").value(true).onAction(() -> instance.sync());
    public final SliderSetting fontScale = new SliderSetting("Масштаб шрифта").value(1.0f).range(0.6f, 1.6f).step(0.05f);

    public StatsHudModule() {
        addSettings(fps, bps, coords, fontScale);
        setEnabled(true, true);
    }

    private void toggle(String name, boolean on) {
        Widget w = WidgetManager.getInstance().byName(name);
        if (w != null) w.setEnabled(on);
    }

    private void sync() {
        boolean parent = isEnabled();
        toggle("FPS", parent && fps.getValue());
        toggle("BPS", parent && bps.getValue());
        toggle("XYZ", parent && coords.getValue());
    }

    @Override public void onEnable()  { sync(); }
    @Override public void onDisable() { sync(); }
    @Override public void onEvent()   {}
}
