package com.fest.visuals.inject.client;

import com.fest.visuals.client.features.modules.render.SkyColorModule;
import com.fest.visuals.api.utils.color.UIColors;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public class MixinSkyRenderer {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void customSkyColor(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo ci) {
        SkyColorModule module = SkyColorModule.getInstance();
        if (module != null && module.isEnabled()) {
            java.awt.Color target = module.themeColor.getValue() ? UIColors.primary(255) : module.color.getValue();
            state.skyColor = target.getRGB();
        }
    }
}
