package com.fest.visuals.inject.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the yellow rotated splash text. It only ever appears on the title screen, where it
 * clashes with the client wordmark, so the renderer is suppressed outright.
 */
@Mixin(SplashRenderer.class)
public class MixinSplashRenderer {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void festvisuals$hide(GuiGraphicsExtractor context, int screenWidth, Font font, float alpha, CallbackInfo ci) {
        ci.cancel();
    }
}
