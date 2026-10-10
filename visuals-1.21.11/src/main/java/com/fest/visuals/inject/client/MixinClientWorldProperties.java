package com.fest.visuals.inject.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.fest.visuals.client.features.modules.render.AmbienceModule;

@Mixin(ClientLevel.ClientLevelData.class)
public class MixinClientWorldProperties {
    @ModifyReturnValue(method = "getDayTime", at = @At("RETURN"))
    private long getTimeOfDay(long original) {
        return AmbienceModule.getInstance().getTime(original);
    }
}
