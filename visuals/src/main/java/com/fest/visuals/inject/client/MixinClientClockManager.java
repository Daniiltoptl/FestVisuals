package com.fest.visuals.inject.client;

import com.fest.visuals.client.features.modules.render.AmbienceModule;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public class MixinClientClockManager {
    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void onGetTotalTicks(Holder<WorldClock> holder, CallbackInfoReturnable<Long> cir) {
        AmbienceModule module = AmbienceModule.getInstance();
        if (module != null && module.isEnabled()) {
            long customTime = module.getTime(-1);
            if (customTime != -1) {
                cir.setReturnValue(customTime);
            }
        }
    }
}