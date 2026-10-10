package com.fest.visuals.client.features.modules.render;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.world.phys.HitResult;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.CanvasSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.render.RenderUtil;

/**
 * Replaces the vanilla crosshair.
 *
 * <p>Three built-in shapes plus a painted one: the canvas setting is a small pixel grid drawn in
 * the click GUI and blown up on screen, so any shape can be made without adding a texture.
 *
 * <p>The colour eases towards the target tint rather than snapping, which is what makes the
 * entity highlight readable at a glance instead of flickering as the aim crosses a hitbox.
 */
@ModuleRegister(name = "Crosshair", desc = "Свой прицел с рисовалкой", category = Category.RENDER)
public class CrosshairModule extends Module {
    @Getter private static final CrosshairModule instance = new CrosshairModule();

    public final ModeSetting style = new ModeSetting("Форма")
            .value("Свой рисунок").values("Крест", "Точка", "Круг", "Свой рисунок");

    public final SliderSetting gap = new SliderSetting("Отступ").value(3f).range(0f, 12f).step(1f)
            .setVisible(() -> style.is("Крест"));
    public final SliderSetting length = new SliderSetting("Длина").value(5f).range(1f, 16f).step(1f)
            .setVisible(() -> style.is("Крест"));
    public final SliderSetting thickness = new SliderSetting("Толщина").value(1f).range(1f, 5f).step(1f)
            .setVisible(() -> !style.is("Свой рисунок"));
    public final SliderSetting dotSize = new SliderSetting("Размер").value(2f).range(1f, 10f).step(1f)
            .setVisible(() -> style.is("Точка") || style.is("Круг"));
    public final SliderSetting pixelSize = new SliderSetting("Размер пикселя").value(2f).range(1f, 6f).step(1f)
            .setVisible(() -> style.is("Свой рисунок"));

    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 255, 255, 230));
    public final BooleanSetting outline = new BooleanSetting("Обводка").value(true);
    public final BooleanSetting centreDot = new BooleanSetting("Точка в центре").value(false)
            .setVisible(() -> style.is("Крест"));

    public final BooleanSetting entityHighlight = new BooleanSetting("Красный на существах").value(true);
    public final ColorSetting entityColor = new ColorSetting("Цвет на существах")
            .value(new Color(255, 60, 60, 240)).setVisible(entityHighlight::getValue);

    /** Always visible, and first in the card: it doubles as the preview of what is on screen. */
    public final CanvasSetting canvas = new CanvasSetting("Рисунок").tint(() -> color.getValue());

    private final AnimationUtil highlightAnimation = new AnimationUtil();

    public CrosshairModule() {
        addSettings(canvas, style, gap, length, thickness, dotSize, pixelSize,
                color, outline, centreDot, entityHighlight, entityColor);
    }

    @Override
    public void onEvent() {
        addEvents(Render2DEvent.getInstance().subscribe(new Listener<>(event -> render(event.context(), event.matrixStack()))));
    }

    /**
     * Hidden in third person and while a screen is open, the way the vanilla one is. F1 needs no
     * check here: the render event fires from the HUD, which Minecraft skips entirely when the
     * interface is hidden.
     */
    private boolean shouldRender() {
        return mc.player != null && mc.level != null
                && mc.screen == null
                && mc.options.getCameraType().isFirstPerson();
    }

    /** Rectangles of the current frame, drawn in two passes so outlines never cover pixels. */
    private final java.util.List<float[]> rects = new java.util.ArrayList<>();

    private void render(net.minecraft.client.gui.GuiGraphics context, PoseStack matrices) {
        if (!shouldRender()) return;
        rects.clear();

        boolean onEntity = entityHighlight.getValue()
                && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY;

        highlightAnimation.update();
        highlightAnimation.run(onEntity ? 1.0 : 0.0, 160, Easing.SINE_OUT);

        Color tint = ColorUtil.interpolate(entityColor.getValue(), color.getValue(),
                (float) highlightAnimation.getValue());

        // Whole pixels, so every square of a painted crosshair lands on the pixel grid.
        float centreX = mc.getWindow().getGuiScaledWidth() / 2;
        float centreY = mc.getWindow().getGuiScaledHeight() / 2;

        switch (style.getValue()) {
            case "Точка" -> dot(matrices, centreX, centreY, dotSize.getValue(), tint);
            case "Круг" -> circle(matrices, centreX, centreY, tint);
            case "Свой рисунок" -> painted(matrices, centreX, centreY, tint);
            default -> cross(matrices, centreX, centreY, tint);
        }
        flush(context, tint);
    }

    /**
     * Crisp, pixel-aligned rectangles through the vanilla GUI fill. The client's rounded-rect
     * renderer feathers every edge, which turned a painted crosshair into a field of soft dots.
     */
    private void flush(net.minecraft.client.gui.GuiGraphics context, Color tint) {
        if (outline.getValue()) {
            int shadow = ColorUtil.setAlpha(Color.BLACK, (int) (tint.getAlpha() * 0.55f)).getRGB();
            for (float[] r : rects) {
                context.fill(Math.round(r[0]) - 1, Math.round(r[1]) - 1, Math.round(r[0] + r[2]) + 1, Math.round(r[1] + r[3]) + 1, shadow);
            }
        }
        int color = tint.getRGB();
        for (float[] r : rects) {
            context.fill(Math.round(r[0]), Math.round(r[1]), Math.round(r[0] + r[2]), Math.round(r[1] + r[3]), color);
        }
    }

    private void bar(PoseStack matrices, float x, float y, float width, float height, Color tint) {
        rects.add(new float[]{x, y, width, height});
    }

    private void cross(PoseStack matrices, float centreX, float centreY, Color tint) {
        float t = thickness.getValue();
        float g = gap.getValue();
        float l = length.getValue();

        bar(matrices, centreX - t / 2f, centreY - g - l, t, l, tint);
        bar(matrices, centreX - t / 2f, centreY + g, t, l, tint);
        bar(matrices, centreX - g - l, centreY - t / 2f, l, t, tint);
        bar(matrices, centreX + g, centreY - t / 2f, l, t, tint);

        if (centreDot.getValue()) dot(matrices, centreX, centreY, t, tint);
    }

    private void dot(PoseStack matrices, float centreX, float centreY, float size, Color tint) {
        bar(matrices, centreX - size / 2f, centreY - size / 2f, size, size, tint);
    }

    /**
     * Rasterised pixel by pixel: every pixel whose centre lies within half the thickness of the
     * radius is filled. Placing small squares along the circumference and rounding them to the
     * pixel grid made a lopsided, almost-round ring.
     */
    private void circle(PoseStack matrices, float centreX, float centreY, Color tint) {
        float radius = dotSize.getValue() + 2f;
        float half = Math.max(0.5f, thickness.getValue() / 2f);
        int reach = (int) Math.ceil(radius + half) + 1;

        for (int dy = -reach; dy < reach; dy++) {
            for (int dx = -reach; dx < reach; dx++) {
                double distance = Math.hypot(dx + 0.5, dy + 0.5);
                if (Math.abs(distance - radius) <= half) {
                    bar(matrices, centreX + dx, centreY + dy, 1f, 1f, tint);
                }
            }
        }
    }

    private void painted(PoseStack matrices, float centreX, float centreY, Color tint) {
        float pixel = pixelSize.getValue();
        float origin = CanvasSetting.SIZE / 2f * pixel;

        for (int row = 0; row < CanvasSetting.SIZE; row++) {
            for (int column = 0; column < CanvasSetting.SIZE; column++) {
                if (!canvas.get(column, row)) continue;

                float x = centreX - origin + column * pixel;
                float y = centreY - origin + row * pixel;
                bar(matrices, x, y, pixel, pixel, tint);
            }
        }
    }
}
