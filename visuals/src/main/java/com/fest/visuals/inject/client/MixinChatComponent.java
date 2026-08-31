package com.fest.visuals.inject.client;

import net.minecraft.client.Minecraft;
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
 * Chat entrance: the log slides in from the left, and the newest message is revealed left to
 * right as if it were being typed.
 *
 * <p>The typing effect is a moving clip rather than a growing substring. Chat lines are built as
 * formatted character sequences long before they reach the renderer, so cutting them apart would
 * mean rebuilding the wrapping every frame; sweeping a scissor across the finished text costs
 * nothing and looks the same.
 */
@Mixin(ChatComponent.class)
public class MixinChatComponent {
    @Unique private final AnimationUtil festvisuals$slide = new AnimationUtil();
    @Unique private long festvisuals$messageAt = 0L;
    @Unique private boolean festvisuals$transformed = false;
    @Unique private boolean festvisuals$clipped = false;

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("HEAD"))
    private void festvisuals$onMessage(CallbackInfo ci) {
        festvisuals$slide.setValue(0.0);
        festvisuals$messageAt = System.currentTimeMillis();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("HEAD"))
    private void festvisuals$push(GuiGraphicsExtractor context, CallbackInfo ci) {
        festvisuals$transformed = false;
        festvisuals$clipped = false;

        AnimationsModule module = AnimationsModule.getInstance();
        if (module == null || !module.isEnabled() || !module.chat.getValue()) return;

        Minecraft mc = Minecraft.getInstance();
        float strength = module.strength();

        festvisuals$slide.update();
        festvisuals$slide.run(1.0, module.duration(), module.openingCurve(), true);
        float progress = (float) festvisuals$slide.getValue();

        if (progress < 0.999f) {
            Matrix3x2fStack matrices = context.pose();
            matrices.pushMatrix();
            festvisuals$transformed = true;
            matrices.translate((1f - progress) * -120f * strength, 0f);
        }

        if (!module.typewriter.getValue()) return;

        // The reveal runs a little longer than the slide so the text keeps appearing after the
        // log has settled, which is what sells it as typing rather than sliding.
        long typing = Math.max(120L, (long) (module.duration() * 1.6f));
        float elapsed = (System.currentTimeMillis() - festvisuals$messageAt) / (float) typing;
        if (elapsed >= 1f) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int reveal = Math.max(1, Math.round(width * Math.min(1f, elapsed * 1.15f)));

        context.enableScissor(0, 0, reveal, height);
        festvisuals$clipped = true;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("RETURN"))
    private void festvisuals$pop(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (festvisuals$clipped) {
            context.disableScissor();
            festvisuals$clipped = false;
        }
        if (festvisuals$transformed) {
            context.pose().popMatrix();
            festvisuals$transformed = false;
        }
    }
}
