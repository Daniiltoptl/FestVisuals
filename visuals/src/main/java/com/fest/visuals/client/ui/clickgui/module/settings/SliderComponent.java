package com.fest.visuals.client.ui.clickgui.module.settings;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MathUtil;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.ui.clickgui.module.SettingComponent;

/**
 * Name on the left, value on the right, track underneath.
 *
 * <p>The fill chases the value rather than snapping to it, and the knob swells while dragging, so
 * a slider reads as grabbed even when the pointer has run off the row.
 */
public class SliderComponent extends SettingComponent {
    private final SliderSetting setting;

    private final AnimationUtil dragAnimation = new AnimationUtil();
    private boolean dragging;
    private float fill;
    private float preview;

    public SliderComponent(SliderSetting setting) {
        super(setting);
        this.setting = setting;
        this.preview = setting.getValue();
        updateHeight(22f);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateHeight(22f);

        dragAnimation.update();
        dragAnimation.run(dragging ? 1.0 : 0.0, 320, Easing.EXPO_OUT);
        float grab = (float) dragAnimation.getValue();

        PoseStack matrices = RenderUtil.matrices();
        int full = (int) (getAlpha() * 255f);

        float value = dragging ? preview : setting.getValue();
        float fontSize = scaled(7f);
        String text = label(value);
        float textWidth = Fonts.PS_MEDIUM.getWidth(text, fontSize);

        Fonts.PS_MEDIUM.drawText(matrices, setting.getName(), getX(), getY(), fontSize,
                UIColors.inactiveTextColor((int) (full * 0.85f)));
        Fonts.PS_BOLD.drawText(matrices, text, getX() + getWidth() - textWidth, getY(), fontSize,
                ColorUtil.interpolate(ColorUtil.setAlpha(UIColors.primary(), full), UIColors.textColor(full), grab));

        float trackH = scaled(3.2f);
        float trackY = getY() + fontSize + scaled(6f);
        float span = getWidth();
        float target = (value - setting.getMin()) / (setting.getMax() - setting.getMin()) * span;
        fill = MathUtil.interpolate(fill, target, 0.25f);

        RenderUtil.RECT.draw(matrices, getX(), trackY, span, trackH, trackH / 2f, UIColors.surfaceInner(full));

        Color left = ColorUtil.setAlpha(UIColors.primary(), full);
        Color right = ColorUtil.setAlpha(UIColors.secondary(), full);
        RenderUtil.GRADIENT_RECT.draw(matrices, getX(), trackY, Math.max(trackH, fill), trackH, trackH / 2f, left, right, left, right);

        float knob = scaled(6f) + scaled(1.6f) * grab;
        float knobX = Mth.clamp(getX() + fill - knob / 2f, getX(), getX() + span - knob);
        float knobY = trackY + trackH / 2f - knob / 2f;

        if (grab > 0.01f) {
            float halo = knob + scaled(5f) * grab;
            RenderUtil.RECT.draw(matrices, knobX - (halo - knob) / 2f, knobY - (halo - knob) / 2f, halo, halo, halo / 2f,
                    ColorUtil.setAlpha(UIColors.primary(), (int) (grab * getAlpha() * 70f)));
        }
        RenderUtil.RECT.draw(matrices, knobX, knobY, knob, knob, knob / 2f, ColorUtil.setAlpha(Color.WHITE, full));

        if (dragging) {
            float next = (float) ((mouseX - getX()) / span);
            next = setting.getMin() + next * (setting.getMax() - setting.getMin());
            next = Math.round(next / setting.getStep()) * setting.getStep();
            preview = MathUtil.round(Mth.clamp(next, setting.getMin(), setting.getMax()), setting.getStep());
            setting.setValue(preview);
        }
    }

    /** Whole steps read better without a trailing zero. */
    private String label(float value) {
        if (setting.getStep() >= 1f) return String.valueOf((int) value);
        return String.valueOf(MathUtil.round(value, setting.getStep()));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        // Anywhere on the row starts a drag: the track is three pixels tall, and hunting for it
        // with the pointer is not a fair ask.
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) setting.setValue(preview);
        dragging = false;
    }

    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
}
