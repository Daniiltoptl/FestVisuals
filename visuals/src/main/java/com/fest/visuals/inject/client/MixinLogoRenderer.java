package com.fest.visuals.inject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla "Minecraft / Java Edition" wordmark on the title screen. The client's own
 * logo is drawn as part of the centre column instead, which sits at a different anchor than the
 * one this renderer is given.
 *
 * <p>Targets the four argument overload because the shorter one just delegates to it. Other
 * screens using the same renderer are left alone.
 */
@Mixin(LogoRenderer.class)
public class MixinLogoRenderer {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void festvisuals$logo(GuiGraphicsExtractor context, int screenWidth, float alpha, int heightOffset, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof TitleScreen) ci.cancel();
    }
}
