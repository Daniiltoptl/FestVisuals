package com.fest.visuals.inject.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.Minecraft;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {

    /** Distance cut for chests, signs and banners, driven by the FPS Boost module. */
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private void festvisuals(BlockEntity entity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, boolean bl, CallbackInfoReturnable<BlockEntityRenderState> cir) {
        if (entity != null && Minecraft.getInstance().player != null) {
            double distSqr = entity.getBlockPos().distToCenterSqr(Minecraft.getInstance().player.position());
            // 24 blocks squared is 576. Vanilla usually does 64^2 = 4096.
            if (distSqr > com.fest.visuals.client.features.modules.render.FpsBoostModule.getInstance().blockEntityCutoffSqr()) {
                cir.setReturnValue(null);
            }
        }
    }
}
