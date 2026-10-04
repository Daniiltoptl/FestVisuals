package com.fest.visuals.inject.render;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.fest.visuals.client.features.modules.render.AspectRatioModule;
import com.fest.visuals.client.features.modules.utility.ZoomModule;

/**
 * Applies the zoom to the camera field of view.
 *
 * <p>{@code Camera.update} stores {@code calculateFov}'s result in its {@code fov} field and builds
 * the level projection (and the culling frustum) from that field directly; {@code getFov} is only
 * a getter for other callers. Scaling the value where it is calculated therefore zooms the actual
 * view — hooking {@code getFov} changed nothing on screen. Driving the FOV option instead runs into
 * its own validation, which refuses anything below its floor — the reason deep zoom used to snap
 * back out.
 */
@Mixin(Camera.class)
public class MixinCamera {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void festvisuals$zoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        float multiplier = ZoomModule.getInstance().fovMultiplier();
        if (multiplier == 1f) return;

        cir.setReturnValue(cir.getReturnValue() * multiplier);
    }

    /** Stretches the level projection to the chosen aspect ratio; the HUD keeps its own. */
    @ModifyArgs(method = "setupPerspective", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V"))
    private void festvisuals$aspect(Args args) {
        float ratio = AspectRatioModule.getInstance().currentRatio();
        if (ratio <= 0f) return;

        float height = args.get(4);
        args.set(3, height * ratio);
    }

    /** Culling keeps at least the real window's width, so a wider ratio does not clip the edges. */
    @ModifyArgs(method = "createProjectionMatrixForCulling", at = @At(value = "INVOKE",
            target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"))
    private void festvisuals$cullingAspect(Args args) {
        float ratio = AspectRatioModule.getInstance().currentRatio();
        if (ratio <= 0f) return;

        float aspect = args.get(1);
        args.set(1, Math.max(aspect, ratio));
    }
}
