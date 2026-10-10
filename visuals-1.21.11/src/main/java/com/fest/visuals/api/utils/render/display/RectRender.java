package com.fest.visuals.api.utils.render.display;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.fest.visuals.api.utils.render.pipeline.FestPipelines;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;
import com.fest.visuals.api.utils.render.pipeline.FestUniform;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import com.fest.visuals.api.utils.color.ColorUtil;

public class RectRender {
    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        Matrix4f matrix = matrixStack.last().pose();

        FestRenderer renderer = FestRenderer.getInstance();
        BufferBuilder builder = renderer.begin(FestPipelines.RECT);
        
        // Ensure radii are not larger than half the width/height
        float maxR = Math.min(width, height) * 0.5f;
        float rTopLeft = Math.max(0, Math.min(radius.x, maxR));
        float rTopRight = Math.max(0, Math.min(radius.y, maxR));
        float rBottomRight = Math.max(0, Math.min(radius.z, maxR));
        float rBottomLeft = Math.max(0, Math.min(radius.w, maxR));

        if (rTopLeft <= 0.75f && rTopRight <= 0.75f && rBottomRight <= 0.75f && rBottomLeft <= 0.75f) {
            // Fast path for non-rounded rects
            int colorInt = color.getRGB();
            builder.addVertex(matrix, x, y, 0f).setColor(colorInt);
            builder.addVertex(matrix, x, y + height, 0f).setColor(colorInt);
            builder.addVertex(matrix, x + width, y + height, 0f).setColor(colorInt);
            builder.addVertex(matrix, x + width, y, 0f).setColor(colorInt);
            renderer.submit(builder, FestPipelines.RECT, FestUniform.of(0f, 0f, 0f, 0f, width, height, 0f, 0f));
            return;
        }

        int alpha = color.getAlpha();
        if (alpha <= 0) return;

        // Draw the inner rectangles
        float maxTop = Math.max(rTopLeft, rTopRight);
        float maxBottom = Math.max(rBottomLeft, rBottomRight);
        float maxLeft = Math.max(rTopLeft, rBottomLeft);
        float maxRight = Math.max(rTopRight, rBottomRight);
        
        int colorInt = color.getRGB();
        
        // Center block
        fill(builder, matrix, x + maxLeft, y + maxTop, x + width - maxRight, y + height - maxBottom, colorInt);
        
        // Top block
        if (rTopLeft < width - rTopRight) {
            fill(builder, matrix, x + rTopLeft, y, x + width - rTopRight, y + maxTop, colorInt);
        }
        // Bottom block
        if (rBottomLeft < width - rBottomRight) {
            fill(builder, matrix, x + rBottomLeft, y + height - maxBottom, x + width - rBottomRight, y + height, colorInt);
        }
        // Left block
        if (rTopLeft < height - rBottomLeft) {
            fill(builder, matrix, x, y + rTopLeft, x + maxLeft, y + height - rBottomLeft, colorInt);
        }
        // Right block
        if (rTopRight < height - rBottomRight) {
            fill(builder, matrix, x + width - maxRight, y + rTopRight, x + width, y + height - rBottomRight, colorInt);
        }
        
        // Corners CPU SDF
        drawCorner(builder, matrix, x, y, rTopLeft, alpha, color, true, true); // TL
        drawCorner(builder, matrix, x + width - rTopRight, y, rTopRight, alpha, color, false, true); // TR
        drawCorner(builder, matrix, x + width - rBottomRight, y + height - rBottomRight, rBottomRight, alpha, color, false, false); // BR
        drawCorner(builder, matrix, x, y + height - rBottomLeft, rBottomLeft, alpha, color, true, false); // BL
        
        // Pass radius=0 so the shader acts as a simple color passthrough
        renderer.submit(builder, FestPipelines.RECT, FestUniform.of(0f, 0f, 0f, 0f, width, height, 0f, 0f));
    }
    
    private void drawCorner(BufferBuilder builder, Matrix4f matrix, float cx, float cy, float r, int alpha, Color baseColor, boolean left, boolean top) {
        if (r <= 0) return;
        int ir = (int) Math.ceil(r);
        for (int v = 0; v < ir; v++) {
            float dy = top ? (r - (v + 0.5f)) : (v + 0.5f - (ir - r));
            for (int u = 0; u < ir; u++) {
                float dx = left ? (r - (u + 0.5f)) : (u + 0.5f - (ir - r));
                float dist = (float) Math.hypot(dx, dy);
                float delta = r - dist;
                float cov = Math.max(0.0f, Math.min(1.0f, delta + 0.5f));
                if (cov <= 0.01f) continue;
                
                int c = (cov >= 0.99f) ? baseColor.getRGB() : ColorUtil.setAlpha(baseColor, (int) Math.round(alpha * cov)).getRGB();
                float px = cx + u;
                float py = cy + v;
                fill(builder, matrix, px, py, px + 1, py + 1, c);
            }
        }
    }
    
    private void fill(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
        if (x1 >= x2 || y1 >= y2) return;
        builder.addVertex(matrix, x1, y1, 0f).setColor(color);
        builder.addVertex(matrix, x1, y2, 0f).setColor(color);
        builder.addVertex(matrix, x2, y2, 0f).setColor(color);
        builder.addVertex(matrix, x2, y1, 0f).setColor(color);
    }
}
