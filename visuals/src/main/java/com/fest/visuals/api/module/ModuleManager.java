package com.fest.visuals.api.module;

import lombok.Getter;
import com.fest.visuals.client.features.modules.hud.*;
import com.fest.visuals.client.features.modules.render.*;
import com.fest.visuals.client.features.modules.render.motionblur.MotionBlurModule;
import com.fest.visuals.client.features.modules.render.particles.ParticlesModule;
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
                ScoreboardHudModule.getInstance(),
                SaturationModule.getInstance(),
                DurabilityViewerModule.getInstance(),
                StatsHudModule.getInstance(),
                
                // RENDER
                AspectRatioModule.getInstance(),
                ChinaHatModule.getInstance(),
                ChunkFadeModule.getInstance(),
                CriticalEffectModule.getInstance(),
                DamageNumbersModule.getInstance(),
                HitBubbleModule.getInstance(),
                HitboxModule.getInstance(),
                TrajectoryModule.getInstance(),
                BlockHighlightModule.getInstance(),
                AmbienceModule.getInstance(),
                CrosshairModule.getInstance(),
                JumpCircleModule.getInstance(),
                MotionBlurModule.getInstance(),
                ParticlesModule.getInstance(),
                RemovalsModule.getInstance(),
                SwingAnimationModule.getInstance(),
                TrailsModule.getInstance(),
                ViewModelModule.getInstance(),

                // OTHER (utility modules)
                AuctionHelperModule.getInstance(),
                AutoAcceptModule.getInstance(),
                AutoAuthModule.getInstance(),
                AutoEatModule.getInstance(),
                AutoInvisibleModule.getInstance(),
                AutoResellModule.getInstance(),
                AutoSprintModule.getInstance(),
                DeathCordsModule.getInstance(),
                FastScrollerModule.getInstance(),
                FtHelperModule.getInstance(),
                JarvisModule.getInstance(),
                ZoomModule.getInstance(),
                NameProtectModule.getInstance(),
                SoundsModule.getInstance()
        );

        modules.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        for (Module module : modules) {
            if (module instanceof com.fest.visuals.client.features.modules.hud.HudModule hud) hud.syncWidget();
        }
    }

    public void register(Module... modules) {
        this.modules.addAll(List.of(modules));
    }
}