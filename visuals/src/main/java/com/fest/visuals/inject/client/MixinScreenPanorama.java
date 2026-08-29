package com.fest.visuals.inject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.client.ui.menu.MainMenuTheme;

/**
 * Puts the client wallpaper behind every out-of-world screen: the title screen, the server list,
 * world selection, settings and so on.
 *
 * <p>Both entry points are covered because vanilla picks between them by context — the panorama
 * for screens opened outside a world, the dirt texture as the fallback. Screens opened inside a
 * world go through {@code extractBlurredBackground} instead and are deliberately left alone, so
 * the pause menu and inventory still blur the game behind them.
 *
 * <p>Only the title screen gets the wordmark; that is handled separately in the logo renderer.
 */
@Mixin(Screen.class)
public class MixinScreenPanorama {
    /** False inside a world: the screen there must keep the blurred game behind it. */
    private boolean festvisuals$outOfWorld() {
        return Minecraft.getInstance().level == null;
    }

    private void festvisuals$wallpaper() {
        Minecraft mc = Minecraft.getInstance();
        int scrim = (Object) this instanceof TitleScreen
                ? MainMenuTheme.TITLE_SCRIM
                : MainMenuTheme.MENU_SCRIM;

        MainMenuTheme.renderBackground(RenderUtil.matrices(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), scrim);
    }

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void festvisuals$panorama(GuiGraphicsExtractor context, float delta, CallbackInfo ci) {
        if (!festvisuals$outOfWorld()) return;
        festvisuals$wallpaper();
        ci.cancel();
    }

    @Inject(method = "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), cancellable = true)
    private void festvisuals$menuBackground(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (!festvisuals$outOfWorld()) return;
        festvisuals$wallpaper();
        ci.cancel();
    }
}
