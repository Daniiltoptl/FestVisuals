package com.fest.visuals.inject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.client.features.modules.render.AnimationsModule;

/** Drops the player list down from the top edge, and scales it up as it lands. */
@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
    @Unique private final AnimationUtil festvisuals$animation = new AnimationUtil();
    @Unique private boolean festvisuals$wasVisible = false;
    @Unique private boolean festvisuals$transformed = false;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void festvisuals$push(GuiGraphicsExtractor context, CallbackInfo ci) {
        festvisuals$transformed = false;

        AnimationsModule module = AnimationsModule.getInstance();
        if (module == null || !module.isEnabled() || !module.tabList.getValue()) return;

        Minecraft mc = Minecraft.getInstance();
        boolean visible = mc.options.keyPlayerList.isDown();
        if (visible && !festvisuals$wasVisible) festvisuals$animation.setValue(0.0);
        festvisuals$wasVisible = visible;

        festvisuals$animation.update();
        festvisuals$animation.run(1.0, module.duration(), module.openingCurve(), true);

        float progress = (float) festvisuals$animation.getValue();
        if (progress >= 0.999f) return;

        float strength = module.strength();
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        festvisuals$transformed = true;

        if (module.slides()) {
            matrices.translate(0f, (1f - progress) * -40f * strength);
        }
        if (module.scales()) {
            float centreX = mc.getWindow().getGuiScaledWidth() / 2f;
            float scale = 1f - (1f - progress) * 0.15f * strength;
            matrices.translate(centreX, 0f);
            matrices.scale(scale, scale);
            matrices.translate(-centreX, 0f);
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void festvisuals$pop(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (!festvisuals$transformed) return;

        context.pose().popMatrix();
        festvisuals$transformed = false;
    }
}
