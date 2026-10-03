package com.fest.visuals.inject.client;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.Minecraft;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    // Aggressive Entity Culling
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void festvisuals(Entity entity, Frustum frustum, double d, double e, double f, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null && Minecraft.getInstance().player != null) {
            double distSqr = entity.distanceToSqr(Minecraft.getInstance().player);
            
            // Cull FallingBlocks (TNT, Sand) at 48 blocks
            if (entity instanceof FallingBlockEntity && distSqr > com.fest.visuals.client.features.modules.render.FpsBoostModule.getInstance().fallingBlockCutoffSqr()) {
                cir.setReturnValue(false);
            }
            
            // Cull Item Entities (dropped loot) at 32 blocks
            if (entity instanceof ItemEntity && distSqr > com.fest.visuals.client.features.modules.render.FpsBoostModule.getInstance().itemCutoffSqr()) {
                cir.setReturnValue(false);
            }
        }
    }
}
