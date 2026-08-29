package com.fest.visuals.inject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.client.ui.menu.MainMenuTheme;

/**
 * Paints the title screen's repurposed widgets: the two mode cards and the settings row.
 *
 * <p>Driven by the role the title screen mixin tagged each widget with rather than by type, so
 * untagged buttons — every other screen in the game — keep their vanilla rendering.
 */
@Mixin(AbstractButton.class)
public abstract class MixinAbstractButton {
    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
    private void festvisuals$style(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!(Minecraft.getInstance().gui.screen() instanceof TitleScreen)) return;

        AbstractButton self = (AbstractButton) (Object) this;
        MainMenuTheme.Role role = MainMenuTheme.roleOf(self);
        if (role == null) return;

        switch (role) {
            case CARD_SINGLE, CARD_MULTI -> MainMenuTheme.renderCard(RenderUtil.matrices(), self, role);
            case PILL_SETTINGS, PILL_PACKS -> MainMenuTheme.renderPill(RenderUtil.matrices(), self, role);
        }
        ci.cancel();
    }
}
