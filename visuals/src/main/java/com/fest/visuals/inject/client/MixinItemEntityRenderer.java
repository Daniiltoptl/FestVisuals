package com.fest.visuals.inject.client;

import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemEntityRenderer.class)
public class MixinItemEntityRenderer {

    // Only render ONE item model for dropped stacks instead of 5 overlapping models!
    // This gives a massive FPS boost near mob grinders or explosions.
    @ModifyVariable(
        method = "renderMultipleFromCount(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ItemClusterRenderState;Lnet/minecraft/util/RandomSource;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private static int festvisuals(int originalCount) {
        return com.fest.visuals.client.features.modules.render.FpsBoostModule.getInstance().singleItemModel() ? 1 : originalCount;
    }
}
