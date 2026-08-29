package com.fest.visuals.api.utils.render.display;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.render.pipeline.FestLayers;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BoxRender implements QuickImports {
    private final List<OutlinedBox> outlinedBoxes = new ArrayList<>();
    private final List<FilledBox> filledBoxes = new ArrayList<>();
    private final List<StripedBox> stripedBoxes = new ArrayList<>();

    public enum Render {
        FILL, OUTLINE, STRIPED
    }

    public void setup3DRender(PoseStack matrixStack) {
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cameraPos = camera.position();
        Frustum frustum = new Frustum(matrixStack.last().pose(), new org.joml.Matrix4f());
        frustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);

        filledBoxes.clear();
        stripedBoxes.clear();
        outlinedBoxes.clear();
    }

    private void renderFilledBoxes(List<FilledBox> boxes, Frustum frustum) { }

    private void renderStripedBoxes(List<StripedBox> boxes, Frustum frustum) { }

    private void renderOutlinedBoxes(List<OutlinedBox> boxes, Frustum frustum) { }

    private void renderDashedOutlinedBox(Vec3 pos, Vec3 params, PoseStack matrices, VertexConsumer buffer, Color color, float gapDistance, float lineWidth) {
        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        float x = (float)(pos.x - camPos.x);
        float y = (float)(pos.y - camPos.y);
        float z = (float)(pos.z - camPos.z);

        float w = (float)params.x;
        float h = (float)params.y;
        float d = (float)params.z;

        float x1 = x - w, y1 = y - h, z1 = z - d;
        float x2 = x + w, y2 = y + h, z2 = z + d;

        float totalSegment = gapDistance + gapDistance;

        renderDashedLine(matrices, buffer, color, x1, y1, z1, x2, y1, z1, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x2, y1, z1, x2, y1, z2, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x2, y1, z2, x1, y1, z2, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x1, y1, z2, x1, y1, z1, gapDistance, gapDistance, lineWidth);

        renderDashedLine(matrices, buffer, color, x1, y2, z1, x2, y2, z1, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x2, y2, z1, x2, y2, z2, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x2, y2, z2, x1, y2, z2, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x1, y2, z2, x1, y2, z1, gapDistance, gapDistance, lineWidth);

        renderDashedLine(matrices, buffer, color, x1, y1, z1, x1, y2, z1, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x2, y1, z1, x2, y2, z1, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x1, y1, z2, x1, y2, z2, gapDistance, gapDistance, lineWidth);
        renderDashedLine(matrices, buffer, color, x2, y1, z2, x2, y2, z2, gapDistance, gapDistance, lineWidth);
    }

    private void renderDashedLine(PoseStack matrices, VertexConsumer buffer, Color color, float x1, float y1, float z1, float x2, float y2, float z2, float dashLength, float gapDistance, float lineWidth) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float totalLength = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (Mth.equal(totalLength, 0.0f)) return;

        float t = 0.0f;
        while (t < 1.0f) {
            float tDashStart = t;
            float tDashEnd = Math.min(t + (dashLength / totalLength), 1.0f);
            if (tDashEnd > tDashStart) {
                float dashX1 = x1 + dx * tDashStart;
                float dashY1 = y1 + dy * tDashStart;
                float dashZ1 = z1 + dz * tDashStart;
                float dashX2 = x1 + dx * tDashEnd;
                float dashY2 = y1 + dy * tDashEnd;
                float dashZ2 = z1 + dz * tDashEnd;

                vertexLine(matrices, buffer, dashX1, dashY1, dashZ1, dashX2, dashY2, dashZ2, color, lineWidth);
            }
            t = tDashEnd;

            t = Math.min(t + (gapDistance / totalLength), 1.0f);
        }
    }

    private void renderFilledBox(Vec3 pos, Vec3 params, PoseStack matrices, VertexConsumer buffer, Color color) {
        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        float x = (float)(pos.x - camPos.x);
        float y = (float)(pos.y - camPos.y);
        float z = (float)(pos.z - camPos.z);

        float w = (float)params.x;
        float h = (float)params.y;
        float d = (float)params.z;

        float x1 = x - w, y1 = y - h, z1 = z - d;
        float x2 = x + w, y2 = y + h, z2 = z + d;

        vertexQuad(matrices, buffer, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, color);
        vertexQuad(matrices, buffer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, color);
        vertexQuad(matrices, buffer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, color);
        vertexQuad(matrices, buffer, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, color);
        vertexQuad(matrices, buffer, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        vertexQuad(matrices, buffer, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, color);
    }

    private void renderOutlinedBox(Vec3 pos, Vec3 params, PoseStack matrices, VertexConsumer buffer, Color color, float lineWidth) {
        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        float x = (float)(pos.x - camPos.x);
        float y = (float)(pos.y - camPos.y);
        float z = (float)(pos.z - camPos.z);

        float w = (float)params.x;
        float h = (float)params.y;
        float d = (float)params.z;

        float x1 = x - w, y1 = y - h, z1 = z - d;
        float x2 = x + w, y2 = y + h, z2 = z + d;

        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color, lineWidth);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color, lineWidth);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color, lineWidth);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color, lineWidth);

        vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color, lineWidth);
        vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color, lineWidth);
        vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color, lineWidth);
        vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color, lineWidth);

        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color, lineWidth);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color, lineWidth);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color, lineWidth);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color, lineWidth);
    }

    private void vertexLine(PoseStack matrices, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, Color lineColor, float lineWidth) {
        PoseStack.Pose entry = matrices.last();
        Matrix4f model = entry.pose();
        Vector3f normalVec = getNormalVec(x1, y1, z1, x2, y2, z2);

        buffer.addVertex(model, x1, y1, z1).setColor(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).setNormal(entry, normalVec.x, normalVec.y, normalVec.z).setLineWidth(lineWidth);
        buffer.addVertex(model, x2, y2, z2).setColor(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).setNormal(entry, normalVec.x, normalVec.y, normalVec.z).setLineWidth(lineWidth);
    }

    private Vector3f getNormalVec(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = Mth.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    private PoseStack matrixFrom(double x, double y, double z) {
        PoseStack matrices = new PoseStack();
        Camera camera = mc.gameRenderer.mainCamera();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0f));
        matrices.translate(x - camera.position().x, y - camera.position().y, z - camera.position().z);
        return matrices;
    }

    private void vertexQuad(PoseStack matrices, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, Color color) {
        PoseStack.Pose entry = matrices.last();
        Matrix4f model = entry.pose();
        buffer.addVertex(model, x1, y1, z1).setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.addVertex(model, x2, y2, z2).setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.addVertex(model, x3, y3, z3).setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        buffer.addVertex(model, x4, y4, z4).setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public void drawBox(float x1, float y1, float z1, float x2, float y2, float z2, float lineWidth, Color color, Render renderMode, float gapDistance) {
        Vec3 pos = new Vec3(x1, y1, z1);
        Vec3 params = new Vec3(x2 - x1, y2 - y1, z2 - z1);

        switch (renderMode) {
            case FILL -> filledBoxes.add(new FilledBox(pos, params, color));
            case OUTLINE -> outlinedBoxes.add(new OutlinedBox(pos, params, lineWidth, color));
            case STRIPED -> stripedBoxes.add(new StripedBox(pos, params, lineWidth, color, gapDistance));
        }
    }

    public record FilledBox(Vec3 pos, Vec3 params, Color color) {}
    public record OutlinedBox(Vec3 pos, Vec3 params, float lineWidth, Color color) {}
    public record StripedBox(Vec3 pos, Vec3 params, float lineWidth, Color color, float gapDistance) {}
}
