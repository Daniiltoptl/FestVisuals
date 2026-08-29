package com.fest.visuals.inject.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.client.features.modules.render.RemovalsModule;

@Mixin(BossHealthOverlay.class)
public class MixinBossBarHud {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;
        if (RemovalsModule.getInstance().isBossBar()) {
            ci.cancel();
        }
    }
}
