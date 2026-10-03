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

@Mixin(ChatComponent.class)
public class MixinChatComponent {
    @Unique private final AnimationUtil festvisuals$slide = new AnimationUtil();
    @Unique private long festvisuals$messageAt = 0L;
    @Unique private boolean festvisuals$transformed = false;
    @Unique private boolean festvisuals$clipped = false;

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", at = @At("HEAD"))
    private void festvisuals$onMessage(CallbackInfo ci) {
        festvisuals$slide.setValue(0.0);
        festvisuals$messageAt = System.currentTimeMillis();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("HEAD"))
    private void festvisuals$push(GuiGraphicsExtractor context, net.minecraft.client.gui.Font font, int i1, int i2, int i3, net.minecraft.client.gui.components.ChatComponent.DisplayMode mode, boolean b, CallbackInfo ci) {
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

        long typing = Math.max(120L, (long) (module.duration() * 1.6f));
        float elapsed = (System.currentTimeMillis() - festvisuals$messageAt) / (float) typing;
        if (elapsed >= 1f) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int reveal = Math.max(1, Math.round(width * Math.min(1f, elapsed * 1.15f)));

        context.enableScissor(0, 0, reveal, height);
        festvisuals$clipped = true;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("RETURN"))
    private void festvisuals$pop(GuiGraphicsExtractor context, net.minecraft.client.gui.Font font, int i1, int i2, int i3, net.minecraft.client.gui.components.ChatComponent.DisplayMode mode, boolean b, CallbackInfo ci) {
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
