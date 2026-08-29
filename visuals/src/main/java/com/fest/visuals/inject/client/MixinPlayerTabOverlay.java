package com.fest.visuals.inject.client;

import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.client.features.modules.render.AnimationsModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
    @Unique private AnimationUtil tabAnimation = new AnimationUtil();
    @Unique private boolean wasVisible = false;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractRenderStateHead(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
        AnimationsModule module = AnimationsModule.getInstance();
        if (module != null && module.isEnabled() && module.tabList.getValue()) {
            boolean isVisible = net.minecraft.client.Minecraft.getInstance().options.keyPlayerList.isDown();
            
            if (isVisible && !wasVisible) {
                tabAnimation.setValue(0.0);
            }
            wasVisible = isVisible;

            tabAnimation.update();
            long speed = module.speed.getValue().longValue();
            Easing ease = module.easing.getValue().equals("РЎ РѕС‚СЃРєРѕРєРѕРј") ? Easing.BACK_OUT : Easing.CUBIC_OUT;
            tabAnimation.run(1.0, speed, ease, true);
            
            float progress = (float) tabAnimation.getValue();
            float offset = (1.0f - progress) * -300f; 

            org.joml.Matrix3x2fStack matrices = guiGraphics.pose();
            matrices.pushMatrix();
            matrices.translate(0, offset); 
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onExtractRenderStateReturn(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
        AnimationsModule module = AnimationsModule.getInstance();
        if (module != null && module.isEnabled() && module.tabList.getValue()) {
            guiGraphics.pose().popMatrix();
        }
    }
}