package com.fest.visuals.inject.render;

import com.fest.visuals.api.utils.render.RenderUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.client.features.modules.hud.PotionsHudModule;
import com.fest.visuals.client.features.modules.hud.SaturationModule;
import com.fest.visuals.client.features.modules.hud.ScoreboardHudModule;
import com.fest.visuals.client.features.modules.render.CrosshairModule;
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

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void renderCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (CrosshairModule.getInstance().isEnabled()) {
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

    @Inject(method = "extractFood", at = @At("TAIL"))
    private void festvisuals$saturation(GuiGraphicsExtractor context, Player player, int top, int right, CallbackInfo ci) {
        SaturationModule.getInstance().drawOutline(context, player, top, right);
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void festvisuals$vignette(GuiGraphicsExtractor context, Entity camera, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isVignette()) ci.cancel();
    }

    /** Pumpkin blur and the powder snow frost both come through here. */
    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void festvisuals$textureOverlay(GuiGraphicsExtractor context, Identifier texture, float alpha, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isPumpkin()) ci.cancel();
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void festvisuals$portal(GuiGraphicsExtractor context, float progress, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isPortal()) ci.cancel();
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void festvisuals$confusion(GuiGraphicsExtractor context, float strength, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isPortal()) ci.cancel();
    }
}
