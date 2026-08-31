package com.fest.visuals.inject.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.client.features.modules.render.AnimationsModule;

/**
 * Slides the chat in from the left edge whenever a new line arrives.
 *
 * <p>The animation is restarted on every message rather than run once on open, so a busy chat
 * keeps sliding instead of sitting still after the first line.
 */
@Mixin(ChatComponent.class)
public class MixinChatComponent {
    @Unique private final AnimationUtil festvisuals$animation = new AnimationUtil();
    @Unique private boolean festvisuals$transformed = false;

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("HEAD"))
    private void festvisuals$onMessage(CallbackInfo ci) {
        festvisuals$animation.setValue(0.0);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("HEAD"))
    private void festvisuals$push(GuiGraphicsExtractor context, CallbackInfo ci) {
        festvisuals$transformed = false;

        AnimationsModule module = AnimationsModule.getInstance();
        if (module == null || !module.isEnabled() || !module.chat.getValue()) return;

        festvisuals$animation.update();
        festvisuals$animation.run(1.0, module.duration(), module.openingCurve(), true);

        float progress = (float) festvisuals$animation.getValue();
        if (progress >= 0.999f) return;

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        festvisuals$transformed = true;

        matrices.translate((1f - progress) * -120f * module.strength(), 0f);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("RETURN"))
    private void festvisuals$pop(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (!festvisuals$transformed) return;

        context.pose().popMatrix();
        festvisuals$transformed = false;
    }
}
