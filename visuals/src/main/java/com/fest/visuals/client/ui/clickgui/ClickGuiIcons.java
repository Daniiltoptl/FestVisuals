package com.fest.visuals.client.ui.clickgui;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.fest.visuals.api.utils.render.RenderUtil;

/**
 * Vector glyphs for the click GUI, drawn from rounded rectangles so they follow the theme colour
 * and scale cleanly with the interface.
 *
 * <p>Shapes that need a hole — the eye, the marker — punch it with the surface colour behind them,
 * which the caller passes in; a pill and the panel body are different colours, so the icon cannot
 * guess it.
 */
public final class ClickGuiIcons {
    private ClickGuiIcons() {}

    public static void tab(PoseStack matrices, ClickGuiTab tab, float x, float y, float size, Color color, Color behind) {
        float cx = x + size / 2f;
        float cy = y + size / 2f;

        switch (tab) {
            case RENDER -> {
                RenderUtil.RECT.draw(matrices, x, cy - size * 0.3f, size, size * 0.6f, size * 0.3f, color);
                RenderUtil.RECT.draw(matrices, cx - size * 0.16f, cy - size * 0.16f, size * 0.32f, size * 0.32f, size * 0.16f, behind);
            }
            case HUD -> {
                RenderUtil.RECT.draw(matrices, x, y, size * 0.55f, size * 0.35f, size * 0.08f, color);
                RenderUtil.RECT.draw(matrices, x + size * 0.65f, y, size * 0.35f, size * 0.55f, size * 0.08f, color);
                RenderUtil.RECT.draw(matrices, x, y + size * 0.45f, size * 0.55f, size * 0.55f, size * 0.08f, color);
                RenderUtil.RECT.draw(matrices, x + size * 0.65f, y + size * 0.65f, size * 0.35f, size * 0.35f, size * 0.08f, color);
            }
            case PLAYER -> {
                RenderUtil.RECT.draw(matrices, cx - size * 0.2f, y, size * 0.4f, size * 0.4f, size * 0.2f, color);
                RenderUtil.RECT.draw(matrices, cx - size * 0.32f, y + size * 0.5f, size * 0.64f, size * 0.5f, size * 0.22f, color);
            }
            case OTHER -> {
                float dot = size * 0.22f;
                RenderUtil.RECT.draw(matrices, cx - dot / 2f, y + size * 0.08f, dot, dot, dot / 2f, color);
                RenderUtil.RECT.draw(matrices, cx - dot / 2f, cy - dot / 2f, dot, dot, dot / 2f, color);
                RenderUtil.RECT.draw(matrices, cx - dot / 2f, y + size - dot - size * 0.08f, dot, dot, dot / 2f, color);
            }
            case CONFIGS -> {
                RenderUtil.RECT.draw(matrices, x, y + size * 0.12f, size * 0.46f, size * 0.22f, size * 0.08f, color);
                RenderUtil.RECT.draw(matrices, x, y + size * 0.26f, size, size * 0.62f, size * 0.12f, color);
            }
            case WAYPOINTS -> {
                RenderUtil.RECT.draw(matrices, cx - size * 0.34f, y, size * 0.68f, size * 0.68f, size * 0.34f, color);
                RenderUtil.RECT.draw(matrices, cx - size * 0.1f, y + size * 0.55f, size * 0.2f, size * 0.45f, size * 0.06f, color);
                RenderUtil.RECT.draw(matrices, cx - size * 0.13f, y + size * 0.21f, size * 0.26f, size * 0.26f, size * 0.13f, behind);
            }
            case THEME -> {
                RenderUtil.RECT.draw(matrices, x, y, size, size, size * 0.42f, color);
                float dot = size * 0.17f;
                RenderUtil.RECT.draw(matrices, cx - dot * 1.5f, cy - dot * 1.4f, dot, dot, dot / 2f, behind);
                RenderUtil.RECT.draw(matrices, cx + dot * 0.5f, cy - dot * 0.6f, dot, dot, dot / 2f, behind);
                RenderUtil.RECT.draw(matrices, cx - dot * 0.7f, cy + dot * 0.6f, dot, dot, dot / 2f, behind);
            }
        }
    }

    /** Magnifier for the search field. */
    public static void search(PoseStack matrices, float x, float y, float size, Color color, Color behind) {
        float ring = size * 0.72f;
        RenderUtil.RECT.draw(matrices, x, y, ring, ring, ring / 2f, color);
        float inner = ring - size * 0.24f;
        RenderUtil.RECT.draw(matrices, x + (ring - inner) / 2f, y + (ring - inner) / 2f, inner, inner, inner / 2f, behind);

        float thick = size * 0.15f;
        matrices.pushPose();
        matrices.translate(x + ring * 0.82f, y + ring * 0.82f, 0f);
        matrices.mulPose(Axis.ZP.rotation((float) Math.toRadians(45f)));
        RenderUtil.RECT.draw(matrices, -thick / 2f, 0f, thick, size * 0.36f, thick / 2f, color);
        matrices.popPose();
    }

    /** Tick used by the mode list. */
    public static void check(PoseStack matrices, float cx, float cy, float size, Color color) {
        float thick = size * 0.17f;
        matrices.pushPose();
        matrices.translate(cx, cy, 0f);
        matrices.mulPose(Axis.ZP.rotation((float) Math.toRadians(45f)));
        RenderUtil.RECT.draw(matrices, -size * 0.28f, size * 0.14f - thick, size * 0.62f, thick, thick / 2f, color);
        RenderUtil.RECT.draw(matrices, -size * 0.28f, -size * 0.26f, thick, size * 0.4f, thick / 2f, color);
        matrices.popPose();
    }

    /** Close button of a settings card. */
    public static void cross(PoseStack matrices, float cx, float cy, float size, Color color) {
        float thick = size * 0.16f;
        matrices.pushPose();
        matrices.translate(cx, cy, 0f);
        matrices.mulPose(Axis.ZP.rotation((float) Math.toRadians(45f)));
        RenderUtil.RECT.draw(matrices, -size / 2f, -thick / 2f, size, thick, thick / 2f, color);
        RenderUtil.RECT.draw(matrices, -thick / 2f, -size / 2f, thick, size, thick / 2f, color);
        matrices.popPose();
    }
}
