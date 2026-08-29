package com.fest.visuals.client.features.modules.render;

import lombok.Getter;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.MultiBooleanSetting;

import java.util.Arrays;

@ModuleRegister(name = "Removals", desc = "Убирает лишние элементы игры", category = Category.RENDER)
public class RemovalsModule extends Module {
    @Getter private static final RemovalsModule instance = new RemovalsModule();

    private final String[] elements = {
            "Fire overlay", "Inwall overlay", "Water overlay", "Scoreboard", "Boss bar"
    };

    private final MultiBooleanSetting remove = new MultiBooleanSetting("Remove").value(
            Arrays.stream(elements)
                    .map(name -> new BooleanSetting(name).value(false))
                    .toArray(BooleanSetting[]::new)
    );

    public RemovalsModule() {
        addSettings(remove);
    }

    public boolean isFireOverlay()   { return isEnabled() && remove.isEnabled("Fire overlay"); }
    public boolean isInwallOverlay() { return isEnabled() && remove.isEnabled("Inwall overlay"); }
    public boolean isWaterOverlay()  { return isEnabled() && remove.isEnabled("Water overlay"); }
    public boolean isScoreboard()    { return isEnabled() && remove.isEnabled("Scoreboard"); }
    public boolean isBossBar()       { return isEnabled() && remove.isEnabled("Boss bar"); }

    @Override
    public void onEvent() {

    }
}
