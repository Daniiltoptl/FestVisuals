package com.fest.visuals.api.utils.render.display;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.fest.visuals.api.utils.render.pipeline.FestPipelines;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;
import com.fest.visuals.api.utils.render.pipeline.FestUniform;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;

public class RectRender {
    private static final float SMOOTHNESS = 0.8f;

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        Matrix4f matrix = matrixStack.last().pose();

        float horizontalPadding = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
        float verticalPadding = SMOOTHNESS / 2.0F + SMOOTHNESS;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        FestRenderer renderer = FestRenderer.getInstance();
        BufferBuilder builder = renderer.begin(FestPipelines.RECT);
        int colorInt = color.getRGB();

        builder.addVertex(matrix, adjustedX, adjustedY, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX, adjustedY + adjustedHeight, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY, 0f).setColor(colorInt);

        renderer.submit(builder, FestPipelines.RECT, FestUniform.of(
                radius.x, radius.z, radius.w, radius.y,
                width, height, SMOOTHNESS, 0f
        ));
    }
}
