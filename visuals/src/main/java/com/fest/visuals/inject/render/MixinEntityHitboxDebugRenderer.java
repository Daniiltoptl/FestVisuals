package com.fest.visuals.inject.render;

import com.fest.visuals.client.features.modules.render.HitboxModule;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public class MixinEntityHitboxDebugRenderer {
    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
    private void onShowHitboxes(Entity entity, float partial, boolean bl, CallbackInfo ci) {
        if (HitboxModule.getInstance().isEnabled()) {
            HitboxModule.getInstance().renderHitbox(entity, partial);
            ci.cancel();
        }
    }
}
