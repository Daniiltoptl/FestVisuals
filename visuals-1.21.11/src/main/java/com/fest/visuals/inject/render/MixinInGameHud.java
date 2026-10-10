package com.fest.visuals.inject.render;

import com.fest.visuals.api.utils.render.RenderUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
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
import com.fest.visuals.client.features.modules.render.CrosshairModule;
import com.fest.visuals.client.features.modules.render.RemovalsModule;

@Mixin(Gui.class)
public class MixinInGameHud {
    @Inject(method = "render", at = @At("HEAD"))
    public void renderHook(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        RenderUtil.matrices().setIdentity();

        Render2DEvent.getInstance().call(new Render2DEvent.Render2DEventData(context, RenderUtil.matrices(), tickCounter.getGameTimeDeltaPartialTick(false)));
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (PotionsHudModule.getInstance().isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void renderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (CrosshairModule.getInstance().isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebar(GuiGraphics context, Objective objective, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;

        // Vanilla sidebar is only hidden by the Removals switch.
        if (RemovalsModule.getInstance().isScoreboard()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFood", at = @At("TAIL"))
    private void festvisuals$saturation(GuiGraphics context, Player player, int top, int right, CallbackInfo ci) {
        SaturationModule.getInstance().drawOutline(context, player, top, right);
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void festvisuals$vignette(GuiGraphics context, Entity camera, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isVignette()) ci.cancel();
    }

    /** Pumpkin blur and the powder snow frost both come through here. */
    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void festvisuals$textureOverlay(GuiGraphics context, Identifier texture, float alpha, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isPumpkin()) ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void festvisuals$portal(GuiGraphics context, float progress, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isPortal()) ci.cancel();
    }

    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void festvisuals$confusion(GuiGraphics context, float strength, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isPortal()) ci.cancel();
    }
}
