package com.fest.visuals.inject.render;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.fest.visuals.client.features.modules.render.HitColorModule;

/**
 * Lets armour flash on hit with Hit Color, which vanilla 26.2 never does: every armour piece is
 * submitted with {@code NO_OVERLAY}. With the module aimed at armour, the piece gets the same
 * hurt overlay the body uses, so it picks up the module's colour too.
 */
@Mixin(EquipmentLayerRenderer.class)
public class MixinEquipmentLayerRenderer {
    @ModifyArgs(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void festvisuals$armourHurtOverlay(Args args) {
        HitColorModule module = HitColorModule.getInstance();
        if (!module.isEnabled() || module.target.is("Скин")) return;

        Object state = args.get(1);
        if (state instanceof LivingEntityRenderState living && living.hasRedOverlay) {
            args.set(5, OverlayTexture.pack(0f, true));
        }
    }
}
