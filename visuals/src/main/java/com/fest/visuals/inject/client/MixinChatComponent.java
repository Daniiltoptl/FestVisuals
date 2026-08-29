package com.fest.visuals.inject.client;

import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.client.features.modules.render.AnimationsModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MixinChatComponent {
    @Unique private AnimationUtil chatAnimation = new AnimationUtil();

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("HEAD"))
    private void onAddMessage(CallbackInfo ci) {
        chatAnimation.setValue(0.0);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("HEAD"))
    private void onExtractRenderStateHead(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
        AnimationsModule module = AnimationsModule.getInstance();
        if (module != null && module.isEnabled() && module.chat.getValue()) {
            chatAnimation.update();
            long speed = module.speed.getValue().longValue();
            Easing ease = module.easing.getValue().equals("РЎ РѕС‚СЃРєРѕРєРѕРј") ? Easing.BACK_OUT : Easing.CUBIC_OUT;
            chatAnimation.run(1.0, speed, ease, true);
            
            float progress = (float) chatAnimation.getValue();
            float offset = (1.0f - progress) * -300f; 
            
            org.joml.Matrix3x2fStack matrices = guiGraphics.pose();
            matrices.pushMatrix();
            matrices.translate(offset, 0);
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent;Z)V", at = @At("RETURN"))
    private void onExtractRenderStateReturn(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
        AnimationsModule module = AnimationsModule.getInstance();
        if (module != null && module.isEnabled() && module.chat.getValue()) {
            guiGraphics.pose().popMatrix();
        }
    }
}