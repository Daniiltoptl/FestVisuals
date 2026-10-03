package com.fest.visuals.inject.render;

import com.fest.visuals.client.features.modules.render.HitColorModule;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;
import java.lang.reflect.Field;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow private OverlayTexture overlayTexture;

    private static int lastColor = -1;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        OverlayTexture overlay = overlayTexture;
        if (overlay == null) return;
        
        DynamicTexture texture = null;
        try {
            for (Field field : OverlayTexture.class.getDeclaredFields()) {
                if (field.getType() == DynamicTexture.class) {
                    field.setAccessible(true);
                    texture = (DynamicTexture) field.get(overlay);
                    break;
                }
            }
        } catch (Exception ignored) {}

        if (texture == null) return;

        if (!HitColorModule.getInstance().isEnabled()) {
            if (lastColor != -1) {
                lastColor = -1;
                reset(texture);
            }
            return;
        }

        Color c = HitColorModule.getInstance().color.getValue();
        int rgba = (c.getAlpha() << 24) | (c.getBlue() << 16) | (c.getGreen() << 8) | c.getRed();

        if (rgba != lastColor) {
            lastColor = rgba;
            NativeImage image = texture.getPixels();
            if (image != null) {
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 16; j++) {
                        image.setPixel(j, i, rgba);
                    }
                }
                texture.upload();
            }
        }
    }

    private void reset(DynamicTexture texture) {
        NativeImage image = texture.getPixels();
        if (image != null) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 16; j++) {
                    image.setPixel(j, i, -1308622593); // Vanilla red
                }
            }
            texture.upload();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void festvisuals$hurtCam(net.minecraft.client.renderer.state.level.CameraRenderState camera,
                                     com.mojang.blaze3d.vertex.PoseStack poseStack, CallbackInfo ci) {
        if (com.fest.visuals.client.features.modules.render.RemovalsModule.getInstance().isHurtCam()) ci.cancel();
    }
}
