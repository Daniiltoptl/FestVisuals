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
import com.fest.visuals.client.features.modules.render.CrosshairModule;
import com.fest.visuals.client.features.modules.render.RemovalsModule;
import com.fest.visuals.client.features.modules.hud.ScoreboardHudModule;

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

        // Vanilla sidebar is only hidden by the Removals switch.
        if (RemovalsModule.getInstance().isScoreboard()) {
            ci.cancel();
        }
    }

    // --- Scoreboard: vanilla draws it; we only move it and swap its fill for the client background.

    @org.spongepowered.asm.mixin.Unique private boolean festvisuals$shifted;
    @org.spongepowered.asm.mixin.Unique private static float festvisuals$minX, festvisuals$minY, festvisuals$maxX, festvisuals$maxY;
    @org.spongepowered.asm.mixin.Unique private static boolean festvisuals$any;

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V", at = @At("HEAD"))
    private void festvisuals$scoreboardBegin(GuiGraphicsExtractor context, Objective objective, CallbackInfo ci) {
        festvisuals$any = false;
        festvisuals$shifted = false;
        if (!ScoreboardHudModule.getInstance().isEnabled()) return;

        var widget = com.fest.visuals.client.ui.widget.WidgetManager.getInstance().byName("Scoreboard");
        if (widget == null) return;
        float[] shift = com.fest.visuals.client.ui.widget.overlay.ScoreboardWidget.shift(widget.getDraggable());
        context.pose().pushMatrix();
        context.pose().translate(shift[0], shift[1]);
        festvisuals$shifted = true;
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void festvisuals$scoreboardFill(GuiGraphicsExtractor context, int x1, int y1, int x2, int y2, int color,
                                            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (!ScoreboardHudModule.getInstance().isEnabled()) {
            original.call(context, x1, y1, x2, y2, color);
            return;
        }
        festvisuals$minX = festvisuals$any ? Math.min(festvisuals$minX, x1) : x1;
        festvisuals$minY = festvisuals$any ? Math.min(festvisuals$minY, y1) : y1;
        festvisuals$maxX = festvisuals$any ? Math.max(festvisuals$maxX, x2) : x2;
        festvisuals$maxY = festvisuals$any ? Math.max(festvisuals$maxY, y2) : y2;
        festvisuals$any = true;
        // The client's background replaces vanilla's dark fill (vanilla's stays when it is switched off).
        if (!ScoreboardHudModule.getInstance().background.getValue()) {
            original.call(context, x1, y1, x2, y2, color);
        }
    }

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V", at = @At("RETURN"))
    private void festvisuals$scoreboardEnd(GuiGraphicsExtractor context, Objective objective, CallbackInfo ci) {
        if (festvisuals$shifted) {
            context.pose().popMatrix();
            festvisuals$shifted = false;
        }
        if (!ScoreboardHudModule.getInstance().isEnabled()) return;
        if (festvisuals$any) {
            com.fest.visuals.client.ui.widget.overlay.ScoreboardWidget.report(festvisuals$minX, festvisuals$minY,
                    festvisuals$maxX - festvisuals$minX, festvisuals$maxY - festvisuals$minY);
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
