package com.fest.visuals.inject.render;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.fest.visuals.client.features.modules.render.AspectRatioModule;
import com.fest.visuals.client.features.modules.utility.ZoomModule;

/**
 * Zoom and aspect ratio for the level projection.
 *
 * <p>In 1.21.11 the field of view is worked out in {@code GameRenderer.getFov} and the projection
 * matrix is built from it in {@code getProjectionMatrix}, so both are scaled there. Zoom multiplies
 * the field of view instead of driving the FOV option, which refuses anything below its floor and
 * snaps back — the reason deep zoom used to spring out.
 */
@Mixin(GameRenderer.class)
public class MixinCamera {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void festvisuals$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float multiplier = ZoomModule.getInstance().fovMultiplier();
        if (multiplier == 1f) return;

        cir.setReturnValue(cir.getReturnValue() * multiplier);
    }

    /** Stretches the level projection to the chosen aspect ratio; the HUD keeps its own. */
    @ModifyArgs(method = "getProjectionMatrix", at = @At(value = "INVOKE",
            target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"))
    private void festvisuals$aspect(Args args) {
        float ratio = AspectRatioModule.getInstance().currentRatio();
        if (ratio <= 0f) return;

        args.set(1, ratio);
    }
}
