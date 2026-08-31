package com.fest.visuals.inject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.client.features.modules.render.AnimationsModule;

/**
 * Opens and closes every screen with an animation instead of a hard cut.
 *
 * <p>Closing is the awkward half: vanilla tears the screen down the moment {@code onClose} runs,
 * so the call is cancelled once, the screen keeps rendering while it shrinks away, and only then
 * is the real close allowed through.
 *
 * <p>The transform is applied to the GUI matrix around the screen centre, which covers the
 * background, the widgets and the container contents in one go.
 */
@Mixin(Screen.class)
public class MixinScreen {
    @Unique private final AnimationUtil festvisuals$animation = new AnimationUtil();
    @Unique private boolean festvisuals$fresh = true;
    @Unique private boolean festvisuals$closing = false;
    @Unique private boolean festvisuals$allowClose = false;
    @Unique private boolean festvisuals$transformed = false;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
    private void festvisuals$onInit(CallbackInfo ci) {
        festvisuals$fresh = true;
        festvisuals$closing = false;
        festvisuals$allowClose = false;
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void festvisuals$onClose(CallbackInfo ci) {
        if (festvisuals$allowClose || !festvisuals$animates()) return;

        festvisuals$closing = true;
        ci.cancel();
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void festvisuals$push(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        festvisuals$transformed = false;
        if (!festvisuals$animates()) return;

        AnimationsModule module = AnimationsModule.getInstance();
        Screen self = (Screen) (Object) this;

        if (festvisuals$fresh) {
            festvisuals$animation.setValue(0.0);
            festvisuals$fresh = false;
        }

        festvisuals$animation.update();
        festvisuals$animation.run(festvisuals$closing ? 0.0 : 1.0, module.duration(),
                festvisuals$closing ? module.closingCurve() : module.openingCurve(), true);

        float progress = (float) festvisuals$animation.getValue();

        if (festvisuals$closing && progress <= 0.02f) {
            festvisuals$allowClose = true;
            Minecraft.getInstance().gui.setScreen(null);
            return;
        }

        float strength = module.strength();
        float centreX = self.width / 2f;
        float centreY = self.height / 2f;

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        festvisuals$transformed = true;

        if (module.slides()) {
            // Chat lives at the bottom edge, so it rises into place; everything else drops in.
            boolean chat = self instanceof ChatScreen;
            float distance = (1f - progress) * 70f * strength;
            matrices.translate(0f, chat ? distance : -distance);
        }

        if (module.scales()) {
            float scale = 1f - (1f - progress) * 0.45f * strength;
            matrices.translate(centreX, centreY);
            matrices.scale(scale, scale);
            matrices.translate(-centreX, -centreY);
        }
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
    private void festvisuals$pop(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // Tracked with a flag rather than recomputed: the settings can change between the two
        // injections, and an unbalanced pop corrupts every later frame.
        if (!festvisuals$transformed) return;

        context.pose().popMatrix();
        festvisuals$transformed = false;
    }

    @Unique
    private boolean festvisuals$animates() {
        AnimationsModule module = AnimationsModule.getInstance();
        if (module == null || !module.isEnabled() || !module.screens.getValue()) return false;

        Screen self = (Screen) (Object) this;
        if (self instanceof com.fest.visuals.client.ui.clickgui.ScreenClickGUI) return false;
        if (self instanceof ChatScreen) return module.chat.getValue();
        if (self instanceof AbstractContainerScreen<?>) return module.containers.getValue();
        return true;
    }
}
