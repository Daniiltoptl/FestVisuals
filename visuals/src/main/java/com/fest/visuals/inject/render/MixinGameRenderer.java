package com.fest.visuals.inject.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.KawaseBlurProgram;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void prepareInterfaceBlur(CallbackInfo ci) {
        // Loading screens are rendered before the resource reload has installed
        // custom shader sources. Compiling the blur pipeline there permanently
        // caches it as invalid for the session.
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            KawaseBlurProgram.render(RenderUtil.matrices());
        }

        // Backdrops sample the blur targets that were just built, and must land before the GUI
        // draws the DrawContext content stacked on top of them. Flushed even without a level so
        // the main menu wallpaper still reaches the screen.
        FestRenderer.flushBackdrop();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
    private void renderInterface(CallbackInfo ci) {
        FestRenderer.getInstance().flush();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "bobHurt", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void festvisuals$hurtCam(net.minecraft.client.renderer.state.level.CameraRenderState camera,
                                     com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (com.fest.visuals.client.features.modules.render.RemovalsModule.getInstance().isHurtCam()) ci.cancel();
    }
}
