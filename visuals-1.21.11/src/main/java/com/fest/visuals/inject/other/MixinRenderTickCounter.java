package com.fest.visuals.inject.other;

import net.minecraft.client.DeltaTracker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.fest.visuals.api.system.client.TimerManager;

@Mixin(DeltaTracker.Timer.class)
public class MixinRenderTickCounter {
    @Shadow
    private float deltaTicks;

    @Inject(method = "advanceGameTime(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/DeltaTracker$Timer;deltaTicks:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void timer(CallbackInfoReturnable<Integer> callback) {
        float customTimer = TimerManager.getInstance().getTimerSpeed();
        if (customTimer > 0) {
            deltaTicks *= customTimer;
        }
    }
}