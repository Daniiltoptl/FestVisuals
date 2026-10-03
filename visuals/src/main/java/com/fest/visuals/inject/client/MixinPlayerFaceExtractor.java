package com.fest.visuals.inject.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.fest.visuals.api.utils.network.VisualsUsers;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerFaceExtractor {
    private static final Identifier AVATAR = Identifier.fromNamespaceAndPath("festvisuals", "textures/icon.png");

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerFaceExtractor;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;IIIZZI)V"))
    private void festvisuals$wrapExtractFace(GuiGraphicsExtractor extractor, Identifier texture, int x, int y, int size, boolean hat, boolean upsideDown, int color, Operation<Void> original, @Local PlayerInfo playerInfo) {
        if (playerInfo != null && playerInfo.getProfile() != null && VisualsUsers.hasVisuals(playerInfo.getProfile().name())) {
            extractor.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                AVATAR,
                x, y,
                0f, 0f,
                size, size,
                size, size,
                size, size,
                color
            );
        } else {
            original.call(extractor, texture, x, y, size, hat, upsideDown, color);
        }
    }
}
