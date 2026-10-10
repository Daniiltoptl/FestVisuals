package com.fest.visuals.inject.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.fest.visuals.client.features.modules.render.SwingAnimationModule;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer {
    @Inject(method = "renderArmWithItem", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderItem(AbstractClientPlayer player, float tickProgress, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector queue, int light, CallbackInfo ci) {
        if (!item.isEmpty() && !item.has(DataComponents.MAP_ID)) {
            ci.cancel();
            SwingAnimationModule.getInstance().handleRenderItem(player, tickProgress, pitch, hand, swingProgress, item, equipProgress, matrices, queue, light);
        }
    }
}
