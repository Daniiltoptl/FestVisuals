package com.fest.visuals.inject.render;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.fest.visuals.client.features.modules.render.HitColorModule;

/** With Hit Color set to armour only, the body itself stops flashing on hit. */
@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {
    @Inject(method = "getOverlayCoords", at = @At("RETURN"), cancellable = true)
    private static void festvisuals$bodyOverlay(LivingEntityRenderState state, float whiteOverlayProgress,
                                                CallbackInfoReturnable<Integer> cir) {
        if (HitColorModule.getInstance().isEnabled() && HitColorModule.getInstance().target.is("Броня")) {
            cir.setReturnValue(OverlayTexture.NO_OVERLAY);
        }
    }
}
