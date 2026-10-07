package com.fest.visuals.client.features.modules.render.wings;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import com.fest.visuals.client.features.modules.render.WingsModule;

/**
 * Draws {@link WingsModule}'s wings as a layer of the player renderer, attached to the body part,
 * so they inherit its rotation, crouch tilt and pose.
 *
 * <p>Each wing is a fan of kite-shaped blades hinged between the shoulders — long ones sweeping up
 * and out, shorter ones down and out, all slightly swept back — filled with a translucent unlit
 * colour and optionally outlined, flapping around the vertical hinge. Only the local player gets
 * them. In the body part's space +Z points out of the back and -Y points up.
 */
public class WingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    /**
     * Panels of one wing: angle above the horizontal (degrees), length and half-width, in blocks.
     * Two broad upper panels sweeping up and out, two smaller lower ones out and down.
     */
    private static final float[][] BLADES = {
            {52f, 1.30f, 0.23f},
            {26f, 1.42f, 0.25f},
            {-2f, 1.05f, 0.20f},
            {-28f, 0.80f, 0.16f},
    };
    private static final float SWEEP = 0.18f;     // backward sweep per block of panel length
    private static final float HINGE_X = 0.10f;   // each wing starts at its own shoulder blade

    public WingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yaw, float pitch) {
        WingsModule wings = WingsModule.getInstance();
        if (!wings.isEnabled()) return;
        if (state.isSpectator || state.isFallFlying || state.isInvisible) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || state.id != mc.player.getId()) return; // ours only

        float swing = 0f;
        if (wings.flap.getValue()) {
            double speed = wings.flapSpeed.getValue() * (state.isCrouching ? 0.6 : 1.0);
            swing = (float) Math.sin(System.currentTimeMillis() / 1000.0 * Math.PI * speed) * 14f;
        }

        Color fill = wings.color.getValue();
        int fillColor = fill.getRGB();
        int lineColor = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), Math.min(255, fill.getAlpha() * 2 + 60)).getRGB();

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0, 5 / 16f, 2.2f / 16f); // shoulder-blade height, on the back
        float scale = wings.scale.getValue();
        poseStack.scale(scale, scale, scale);

        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            poseStack.translate(side * HINGE_X, 0, 0);
            // Folded slightly back, flapping around the vertical hinge.
            poseStack.mulPose(Axis.YP.rotationDegrees(-side * (10f + swing)));
            final int s = side;
            collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> {
                for (float[] blade : BLADES) quad(pose, consumer, kite(s, blade), fillColor);
            });
            if (wings.outline.getValue()) {
                collector.submitCustomGeometry(poseStack, RenderTypes.linesTranslucent(), (pose, consumer) -> {
                    for (float[] blade : BLADES) outline(pose, consumer, kite(s, blade), lineColor);
                });
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /** Root, shoulder, tip, shoulder — the blade's four corners in body space (-Y is up). */
    private static float[][] kite(int side, float[] blade) {
        double angle = Math.toRadians(blade[0]);
        float length = blade[1], halfWidth = blade[2];
        float dx = (float) Math.cos(angle) * side, dy = (float) -Math.sin(angle);
        float px = -dy, py = dx; // perpendicular within the wing plane
        float shoulder = length * 0.45f;
        return new float[][] {
                {0f, 0f, 0f},
                {dx * shoulder + px * halfWidth, dy * shoulder + py * halfWidth, shoulder * SWEEP},
                {dx * length, dy * length, length * SWEEP},
                {dx * shoulder - px * halfWidth, dy * shoulder - py * halfWidth, shoulder * SWEEP},
        };
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, float[][] k, int color) {
        for (float[] v : k) consumer.addVertex(pose, v[0], v[1], v[2]).setColor(color);
    }

    private static void outline(PoseStack.Pose pose, VertexConsumer consumer, float[][] k, int color) {
        for (int i = 0; i < 4; i++) {
            float[] a = k[i], b = k[(i + 1) % 4];
            float nx = b[0] - a[0], ny = b[1] - a[1], nz = b[2] - a[2];
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-5f) continue;
            nx /= len; ny /= len; nz /= len;
            consumer.addVertex(pose, a[0], a[1], a[2]).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(2f);
            consumer.addVertex(pose, b[0], b[1], b[2]).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(2f);
        }
    }
}
