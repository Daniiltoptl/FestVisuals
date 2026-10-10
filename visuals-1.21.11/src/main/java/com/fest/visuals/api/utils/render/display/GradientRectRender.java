package com.fest.visuals.api.utils.render.display;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.render.pipeline.FestPipelines;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;
import com.fest.visuals.api.utils.render.pipeline.FestUniform;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;

public class GradientRectRender {
    private static final float SMOOTHNESS = 1f;

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius), topLeft, topRight, bottomLeft, bottomRight);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight) {
        Matrix4f matrix = matrixStack.last().pose();

        float horizontalPadding = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
        float verticalPadding = SMOOTHNESS / 2.0F + SMOOTHNESS;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        float[] tl = ColorUtil.normalize(topLeft);
        float[] bl = ColorUtil.normalize(bottomLeft);
        float[] br = ColorUtil.normalize(bottomRight);
        float[] tr = ColorUtil.normalize(topRight);

        FestRenderer renderer = FestRenderer.getInstance();
        BufferBuilder builder = renderer.begin(FestPipelines.GRADIENT_RECT);

        builder.addVertex(matrix, adjustedX, adjustedY, 0f).setColor(topLeft.getRGB());
        builder.addVertex(matrix, adjustedX, adjustedY + adjustedHeight, 0f).setColor(bottomLeft.getRGB());
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0f).setColor(bottomRight.getRGB());
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY, 0f).setColor(topRight.getRGB());

        renderer.submit(builder, FestPipelines.GRADIENT_RECT, FestUniform.of(
                radius.x, radius.z, radius.w, radius.y,
                width, height, SMOOTHNESS, 0f,
                tl[0], tl[1], tl[2], tl[3],
                bl[0], bl[1], bl[2], bl[3],
                br[0], br[1], br[2], br[3],
                tr[0], tr[1], tr[2], tr[3]
        ));
    }
}
