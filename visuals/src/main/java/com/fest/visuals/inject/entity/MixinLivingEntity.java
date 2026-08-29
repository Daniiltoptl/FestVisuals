package com.fest.visuals.inject.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.fest.visuals.api.event.events.player.move.JumpEvent;
import com.fest.visuals.api.system.backend.SharedClass;
import com.fest.visuals.client.features.modules.render.SwingAnimationModule;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {
    @Shadow
    public abstract boolean isFallFlying();

    @Shadow
    public int noJumpDelay;

    @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getArmSwingAnimationEnd(final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        SwingAnimationModule swingAnim = SwingAnimationModule.getInstance();
        if (swingAnim.slow.getValue() && swingAnim.isEnabled())
            callbackInfoReturnable.setReturnValue(swingAnim.speed.getValue().intValue());
    }

    @Inject(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getJumpPower()F"))
    public void onJumping(CallbackInfo ci) {
        if (((Object) this) != SharedClass.player()) {
            return;
        }

        JumpEvent.getInstance().call();
    }
}
