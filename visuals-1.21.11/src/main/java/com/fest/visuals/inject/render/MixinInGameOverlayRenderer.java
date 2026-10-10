package com.fest.visuals.inject.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.client.features.modules.render.RemovalsModule;

@Mixin(ScreenEffectRenderer.class)
public class MixinInGameOverlayRenderer {
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource queue, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;

        if (RemovalsModule.getInstance().isFireOverlay()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void onRenderUnderwaterOverlay(Minecraft client, PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource queue, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;

        if (RemovalsModule.getInstance().isWaterOverlay()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void onRenderInWallOverlay(TextureAtlasSprite sprite, PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource queue, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;

        if (RemovalsModule.getInstance().isInwallOverlay()) {
            ci.cancel();
        }
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void festvisuals$totem(net.minecraft.world.item.ItemStack stack, net.minecraft.util.RandomSource random, CallbackInfo ci) {
        if (RemovalsModule.getInstance().isTotem()) ci.cancel();
    }
}
