package com.fest.visuals.inject.render;

import com.fest.visuals.api.utils.render.RenderUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.client.features.modules.hud.PotionsHudModule;
import com.fest.visuals.client.features.modules.hud.ScoreboardHudModule;
import com.fest.visuals.client.features.modules.render.RemovalsModule;

@Mixin(Hud.class)
public class MixinInGameHud {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void renderHook(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        RenderUtil.matrices().setIdentity();

        Render2DEvent.getInstance().call(new Render2DEvent.Render2DEventData(context, RenderUtil.matrices(), tickCounter.getGameTimeDeltaPartialTick(false)));
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (PotionsHudModule.getInstance().isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebar(GuiGraphicsExtractor context, Objective objective, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;

        // Vanilla stands down only while the client draws its own copy; with the module off the
        // untouched sidebar comes back.
        if (RemovalsModule.getInstance().isScoreboard() || ScoreboardHudModule.getInstance().isEnabled()) {
            ci.cancel();
        }
    }
}
