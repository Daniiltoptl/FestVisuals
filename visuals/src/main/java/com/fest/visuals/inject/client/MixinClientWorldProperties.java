package com.fest.visuals.inject.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.fest.visuals.client.features.modules.render.AmbienceModule;

@Mixin(ClientClockManager.class)
public class MixinClientWorldProperties {
    @ModifyReturnValue(method = "getTotalTicks", at = @At("RETURN"))
    private long getTimeOfDay(long original, Holder<WorldClock> definition) {
        return AmbienceModule.getInstance().getTime(original);
    }
}
