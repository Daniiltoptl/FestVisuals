package com.fest.visuals.inject.client;

import com.fest.visuals.client.features.modules.render.AmbienceModule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class MixinLevel {
    @Inject(method = "getDefaultClockTime", at = @At("HEAD"), cancellable = true)
    private void onGetDefaultClockTime(CallbackInfoReturnable<Long> cir) {
        AmbienceModule module = AmbienceModule.getInstance();
        if (module != null && module.isEnabled()) {
            if (((Level)(Object)this).isClientSide()) {
                long customTime = module.getTime(-1);
                if (customTime != -1) {
                    cir.setReturnValue(customTime);
                }
            }
        }
    }
}