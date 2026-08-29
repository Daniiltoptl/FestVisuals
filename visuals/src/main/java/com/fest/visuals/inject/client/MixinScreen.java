package com.fest.visuals.inject.client;

import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.client.features.modules.render.AnimationsModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
    @Unique private AnimationUtil screenAnimation = new AnimationUtil();
    @Unique private boolean isFirstRender = true;
    @Unique private boolean isClosing = false;
    @Unique private boolean doClose = false;

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        isFirstRender = true;
        isClosing = false;
        doClose = false;
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void onCloseHead(CallbackInfo ci) {
        AnimationsModule module = AnimationsModule.getInstance();
        if (module != null && module.isEnabled() && !doClose) {
            isClosing = true;
            screenAnimation.setValue(1.0);
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void onExtractRenderStateHead(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AnimationsModule module = AnimationsModule.getInstance();
        if (module != null && module.isEnabled()) {
            boolean isChat = ((Screen)(Object)this) instanceof net.minecraft.client.gui.screens.ChatScreen;
            boolean isInventory = ((Screen)(Object)this) instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen;

            if ((isChat && !module.chat.getValue()) || (isInventory && !module.inventory.getValue())) {
                return;
            }
            if (!isChat && !isInventory) return;

            if (isFirstRender) {
                screenAnimation.setValue(0.0);
                isFirstRender = false;
            }

            screenAnimation.update();
            long speed = module.speed.getValue().longValue();
            Easing ease = module.easing.getValue().equals("РЎ РѕС‚СЃРєРѕРєРѕРј") ? Easing.BACK_OUT : Easing.CUBIC_OUT;
            
            screenAnimation.run(isClosing ? 0.0 : 1.0, speed, isClosing ? Easing.CUBIC_IN : ease, true);
            float progress = (float) screenAnimation.getValue();

            if (isClosing && progress <= 0.02f) {
                doClose = true;
                net.minecraft.client.Minecraft.getInstance().gui.setScreen(null);
                return;
            }

            org.joml.Matrix3x2fStack matrices = guiGraphics.pose();
            matrices.pushMatrix();

            float halfW = ((Screen)(Object)this).width / 2f;
            float halfH = ((Screen)(Object)this).height / 2f;

            if (isChat) {
                float offset = (1.0f - progress) * 150f;
                matrices.translate(0, offset);
            } else {
                float scale = 0.5f + progress * 0.5f;
                matrices.translate(halfW, halfH);
                matrices.scale(scale, scale);
                matrices.translate(-halfW, -halfH);
            }
        }
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
    private void onExtractRenderStateReturn(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AnimationsModule module = AnimationsModule.getInstance();
        if (module != null && module.isEnabled()) {
            boolean isChat = ((Screen)(Object)this) instanceof net.minecraft.client.gui.screens.ChatScreen;
            boolean isInventory = ((Screen)(Object)this) instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen;

            if ((isChat && !module.chat.getValue()) || (isInventory && !module.inventory.getValue())) {
                return;
            }
            if (!isChat && !isInventory) return;

            guiGraphics.pose().popMatrix();
        }
    }
}