package com.fest.visuals.inject.render;

import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.fest.visuals.client.features.modules.render.AmbienceModule;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void applyCustomFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float darkenWorldAmount,
                                ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        
    }
}
