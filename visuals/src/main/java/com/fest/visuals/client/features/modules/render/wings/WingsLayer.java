package com.fest.visuals.client.features.modules.render.wings;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.fest.visuals.client.features.modules.render.WingsModule;
import net.minecraft.resources.Identifier;
import java.awt.Color;

public class WingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final ModelPart wingTop;
    private final ModelPart wingMid;
    private final ModelPart wingBot;
    
    // Solid white texture for coloring
    private static final Identifier WINGS_TEX = Identifier.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");

    public WingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
        
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 3 flat geometric shapes per wing
        CubeListBuilder builderTop = CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 14.0F, 6.0F, 0.001F);
        CubeListBuilder builderMid = CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 20.0F, 8.0F, 0.001F);
        CubeListBuilder builderBot = CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 12.0F, 6.0F, 0.001F);

        this.wingTop = root.addOrReplaceChild("wing_top", builderTop, PartPose.offset(0.0F, 0.0F, 2.0F)).bake(16, 16);
        this.wingMid = root.addOrReplaceChild("wing_mid", builderMid, PartPose.offset(0.0F, 0.0F, 2.0F)).bake(16, 16);
        this.wingBot = root.addOrReplaceChild("wing_bot", builderBot, PartPose.offset(0.0F, 0.0F, 2.0F)).bake(16, 16);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yaw, float pitch) {
        WingsModule wings = WingsModule.getInstance();
        if (wings == null || !wings.isEnabled()) return;
        if (state.isSpectator || state.isFallFlying || state.isInvisible) return;
        if (!wings.renderOnOthers.getValue() && Minecraft.getInstance().player != null && state.id != Minecraft.getInstance().player.getId()) return;

        poseStack.pushPose();
        
        this.getParentModel().body.translateAndRotate(poseStack);
        
        float flapSpeed = state.isCrouching ? 2.0f : 1.0f;
        float flap = (float) Math.sin((System.currentTimeMillis() % (long)(2000 / flapSpeed)) / (2000.0f / flapSpeed) * Math.PI * 2);
        
        float scale = wings.scale.getValue() * 0.8f; // adjust scale
        poseStack.scale(scale, scale, scale);
        
        // Orange translucent color
        int r = 255;
        int g = 110;
        int b = 20;
        int a = 180;
        int argb = (a << 24) | (r << 16) | (g << 8) | b;

        RenderType renderType = wings.neon.getValue() ? RenderTypes.entityTranslucentEmissive(WINGS_TEX) : RenderTypes.entityTranslucent(WINGS_TEX);

        // Draw left wing
        poseStack.pushPose();
        // Base rotations for the whole left wing
        poseStack.translate(0, 2 / 16f, 2 / 16f); // offset from center back
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-20 - flap * 15));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10));
        
        // Top segment
        this.wingTop.xRot = (float) Math.toRadians(-30);
        this.wingTop.yRot = (float) Math.toRadians(0);
        this.wingTop.zRot = (float) Math.toRadians(-40);
        collector.submitModelPart(this.wingTop, poseStack, renderType, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null, argb, null);
        
        // Mid segment
        this.wingMid.xRot = (float) Math.toRadians(-15);
        this.wingMid.yRot = (float) Math.toRadians(0);
        this.wingMid.zRot = (float) Math.toRadians(0);
        collector.submitModelPart(this.wingMid, poseStack, renderType, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null, argb, null);

        // Bot segment
        this.wingBot.xRot = (float) Math.toRadians(0);
        this.wingBot.yRot = (float) Math.toRadians(0);
        this.wingBot.zRot = (float) Math.toRadians(35);
        collector.submitModelPart(this.wingBot, poseStack, renderType, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null, argb, null);
        poseStack.popPose();

        // Draw right wing
        poseStack.pushPose();
        poseStack.scale(-1.0f, 1.0f, 1.0f);
        poseStack.translate(0, 2 / 16f, 2 / 16f); // offset from center back
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-20 - flap * 15));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10));
        
        collector.submitModelPart(this.wingTop, poseStack, renderType, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null, argb, null);
        collector.submitModelPart(this.wingMid, poseStack, renderType, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null, argb, null);
        collector.submitModelPart(this.wingBot, poseStack, renderType, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, null, argb, null);
        poseStack.popPose();

        poseStack.popPose();
    }
}
