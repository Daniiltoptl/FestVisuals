package com.fest.visuals.inject.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fest.visuals.client.features.modules.render.wings.WingsLayer;

@Mixin(AvatarRenderer.class)
public class MixinAvatarRenderer {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void festvisuals$addWingsLayer(EntityRendererProvider.Context context, boolean useSmallArms, CallbackInfo ci) {
        ((MixinLivingEntityRendererInvoker) this).festvisuals$addLayer(new WingsLayer((AvatarRenderer) (Object) this));
    }
}
