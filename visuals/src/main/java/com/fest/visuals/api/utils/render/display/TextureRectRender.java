package com.fest.visuals.api.utils.render.display;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.pipeline.FestPipelines;
import com.fest.visuals.api.utils.render.pipeline.FestRenderer;
import com.fest.visuals.api.utils.render.pipeline.FestTextures;
import com.fest.visuals.api.utils.render.pipeline.FestUniform;

import java.awt.*;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class TextureRectRender implements QuickImports {
    private static final float SMOOTHNESS = 0.8f;

    private static final int FACE_TEXELS = 8;

    public void drawHead(PoseStack matrixStack, Player player, float x, float y, float width, float height, float gap, float radius, Color color) {
        Identifier skin = ((AbstractClientPlayer) player).getSkin().body().texturePath();
        float u = 8f / 64f;
        float u2 = 40f / 64f;

        double scaleFactor = mc.getWindow().getGuiScale();
        float cell = Math.max(1f, Math.round(width * scaleFactor / FACE_TEXELS));
        float face = (float) (cell * FACE_TEXELS / scaleFactor);
        float inset = (float) (Math.max(1f, Math.round(gap * scaleFactor)) / scaleFactor);
        float hat = face + inset * 2f;

        float faceX = (float) (Math.round((x + (width - face) / 2f) * scaleFactor) / scaleFactor);
        float faceY = (float) (Math.round((y + (height - face) / 2f) * scaleFactor) / scaleFactor);

        GpuTextureView texture = FestTextures.view(skin);
        GpuSampler sampler = FestTextures.nearest();
        draw(matrixStack, faceX, faceY, face, face, new Vector4f(radius), color, u, u, u, u, texture, sampler, true);
        draw(matrixStack, faceX - inset, faceY - inset, hat, hat, new Vector4f(radius), color, u2, u, u, u, texture, sampler, true);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color, float u, float v, float texWidth, float texHeight, GpuTextureView texture) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, u, v, texWidth, texHeight, texture);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color, float u, float v, float texWidth, float texHeight, Identifier texture) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, u, v, texWidth, texHeight, FestTextures.view(texture));
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color, float u, float v, float texWidth, float texHeight, GpuTextureView texture) {
        boolean rounded = radius.x != 0f || radius.y != 0f || radius.z != 0f || radius.w != 0f;
        GpuSampler sampler = rounded ? FestTextures.sampler() : FestTextures.nearest();
        draw(matrixStack, x, y, width, height, radius, color, u, v, texWidth, texHeight, texture, sampler, false);
    }

    private void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color, float u, float v, float texWidth, float texHeight, GpuTextureView texture, GpuSampler sampler, boolean keepUvInsideRegion) {
        Matrix4f matrix = matrixStack.last().pose();

        boolean rounded = radius.x != 0f || radius.y != 0f || radius.z != 0f || radius.w != 0f;
        float pad = rounded ? SMOOTHNESS : 0f;

        float horizontalPadding = -pad / 2.0F + pad * 2.0F;
        float verticalPadding = pad / 2.0F + pad;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        float uScale = width == 0f ? 0f : texWidth / width;
        float vScale = height == 0f ? 0f : texHeight / height;
        float uMin = keepUvInsideRegion ? u : u - horizontalPadding / 2.0F * uScale;
        float vMin = keepUvInsideRegion ? v : v - verticalPadding / 2.0F * vScale;
        float uMax = keepUvInsideRegion ? u + texWidth : uMin + adjustedWidth * uScale;
        float vMax = keepUvInsideRegion ? v + texHeight : vMin + adjustedHeight * vScale;

        FestRenderer renderer = FestRenderer.getInstance();
        BufferBuilder builder = renderer.begin(FestPipelines.TEXTURE_RECT);
        int colorInt = color.getRGB();

        builder.addVertex(matrix, adjustedX, adjustedY, 0f).setUv(uMin, vMin).setColor(colorInt);
        builder.addVertex(matrix, adjustedX, adjustedY + adjustedHeight, 0f).setUv(uMin, vMax).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0f).setUv(uMax, vMax).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY, 0f).setUv(uMax, vMin).setColor(colorInt);

        renderer.submit(builder, FestPipelines.TEXTURE_RECT, TextureSetup.singleTexture(texture, sampler), FestUniform.of(
                radius.x, radius.z, radius.w, radius.y,
                width, height, pad, 0f
        ));
    }
}
