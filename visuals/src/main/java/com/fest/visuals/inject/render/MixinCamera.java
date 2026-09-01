package com.fest.visuals.inject.render;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.fest.visuals.client.features.modules.utility.ZoomModule;

/**
 * Applies the zoom to the camera field of view.
 *
 * <p>This is where the level projection reads its angle from, so scaling it here zooms as far as
 * asked. Driving the FOV option instead runs into its own validation, which refuses anything below
 * its floor and keeps the previous value — the reason deep zoom used to snap back out.
 */
@Mixin(Camera.class)
public class MixinCamera {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void festvisuals$zoom(CallbackInfoReturnable<Float> cir) {
        float multiplier = ZoomModule.getInstance().fovMultiplier();
        if (multiplier == 1f) return;

        cir.setReturnValue(cir.getReturnValue() * multiplier);
    }
}
