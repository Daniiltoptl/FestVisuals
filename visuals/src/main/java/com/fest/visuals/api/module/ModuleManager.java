package com.fest.visuals.api.module;

import lombok.Getter;
import com.fest.visuals.client.features.modules.hud.*;
import com.fest.visuals.client.features.modules.render.*;
import com.fest.visuals.client.features.modules.utility.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleManager {
    @Getter private final static ModuleManager instance = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();

    public void load() {
        register(
                ClickGUIModule.getInstance(),

                // HUD
                PotionsHudModule.getInstance(),
                CooldownsHudModule.getInstance(),
                BindsHudModule.getInstance(),
                TargetHudModule.getInstance(),
                ArmorHudModule.getInstance(),
                DynamicIslandModule.getInstance(),
                InventoryHudModule.getInstance(),
                StatsHudModule.getInstance(),
                
                // RENDER
                ChinaHatModule.getInstance(),
                BlockHighlightModule.getInstance(),
                AnimationsModule.getInstance(),
                AmbienceModule.getInstance(),

                // OTHER (utility modules)
                AutoAcceptModule.getInstance(),
                AutoAuthModule.getInstance(),
                AutoEatModule.getInstance(),
                AutoInvisibleModule.getInstance(),
                AutoJoinModule.getInstance(),
                AutoResellModule.getInstance(),
                AutoSprintModule.getInstance(),
                DeathCordsModule.getInstance(),
                InventoryUtilsModule.getInstance(),
                NameProtectModule.getInstance(),
                SoundsModule.getInstance()
        );

        modules.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public void register(Module... modules) {
        this.modules.addAll(List.of(modules));
    }
}