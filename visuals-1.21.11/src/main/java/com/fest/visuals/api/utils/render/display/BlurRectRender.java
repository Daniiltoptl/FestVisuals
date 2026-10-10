package com.fest.visuals.api.utils.render.display;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.render.KawaseBlurProgram;
import com.fest.visuals.api.utils.render.pipeline.FestPipelines;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;
import com.fest.visuals.api.utils.render.pipeline.FestTextures;
import com.fest.visuals.api.utils.render.pipeline.FestUniform;
import com.fest.visuals.api.utils.render.InterfaceConfig;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import net.minecraft.client.gui.render.TextureSetup;

public class BlurRectRender implements QuickImports {
    private static final float SMOOTHNESS = 0.8f;

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color, float mix) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, color, color, color, mix);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, radius, color, InterfaceConfig.getGlassy());
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        draw(matrixStack, x, y, width, height, radius, color, color, color, color, InterfaceConfig.getGlassy());
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight) {
        draw(matrixStack, x, y, width, height, radius, topLeft, topRight, bottomLeft, bottomRight, InterfaceConfig.getGlassy());
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight, float mix) {
        if (KawaseBlurProgram.fbos.isEmpty()) return;

        float[] tl = ColorUtil.normalize(topLeft);
        float[] bl = ColorUtil.normalize(bottomLeft);
        float[] br = ColorUtil.normalize(bottomRight);
        float[] tr = ColorUtil.normalize(topRight);
        float alpha = (tl[3] + bl[3] + br[3] + tr[3]) / 4f;

        Matrix4f matrix = matrixStack.last().pose();
        RenderTarget fbo = KawaseBlurProgram.fbos.getFirst();

        float horizontalPadding = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
        float verticalPadding = SMOOTHNESS / 2.0F + SMOOTHNESS;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        float uLeft, uRight, vTop, vBottom;

        if (mix != 1) {
            int texW = fbo.width;
            int texH = fbo.height;
            double scale = (double) texW / mc.getWindow().getGuiScaledWidth();
            float fx = (float) (x * scale);
            float fy = (float) (y * scale);
            float fw = (float) (width * scale);
            float fh = (float) (height * scale);
            uLeft = fx / texW;
            uRight = (fx + fw) / texW;
            vTop = 1f - (fy / texH);
            vBottom = 1f - ((fy + fh) / texH);
        } else {
            uLeft = uRight = vTop = vBottom = 0f;
        }

        FestRenderer renderer = FestRenderer.getInstance();
        BufferBuilder builder = renderer.begin(FestPipelines.BLURRED_RECT);

        builder.addVertex(matrix, adjustedX, adjustedY, 0f).setUv(uLeft, vTop).setColor(topLeft.getRGB());
        builder.addVertex(matrix, adjustedX, adjustedY + adjustedHeight, 0f).setUv(uLeft, vBottom).setColor(bottomLeft.getRGB());
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0f).setUv(uRight, vBottom).setColor(bottomRight.getRGB());
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY, 0f).setUv(uRight, vTop).setColor(topRight.getRGB());

        renderer.submit(builder, FestPipelines.BLURRED_RECT, TextureSetup.singleTexture(fbo.getColorTextureView(), FestTextures.sampler()), FestUniform.of(
                radius.x, radius.z, radius.w, radius.y,
                width, height, SMOOTHNESS, mix,
                tl[0], tl[1], tl[2], tl[3],
                bl[0], bl[1], bl[2], bl[3],
                br[0], br[1], br[2], br[3],
                tr[0], tr[1], tr[2], tr[3],
                alpha, 0f, 0f, 0f
        ));
    }
}
