package com.fest.visuals.client.ui.clickgui.module.settings;

import java.awt.Color;
import java.time.Duration;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.ui.clickgui.module.SettingComponent;

/** Label on the left, switch on the right — the switch knob overshoots on its way across. */
public class BooleanComponent extends SettingComponent {
    private final BooleanSetting setting;

    private final AnimationUtil toggleAnimation = new AnimationUtil();
    private final AnimationUtil hoverAnimation = new AnimationUtil();

    public BooleanComponent(BooleanSetting setting) {
        this(setting, false);
    }

    public BooleanComponent(BooleanSetting setting, boolean inMenu) {
        super(setting);
        this.setting = setting;
        updateHeight(18f);
        toggleAnimation.setValue(setting.getValue() ? 1.0 : 0.0);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateHeight(18f);

        toggleAnimation.update();
        toggleAnimation.run(setting.getValue() ? 1.0 : 0.0, 380, Easing.BACK_OUT);

        boolean over = MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
        hoverAnimation.update();
        hoverAnimation.run(over ? 1.0 : 0.0, 220, Easing.EXPO_OUT);

        PoseStack matrices = RenderUtil.matrices();
        int full = (int) (getAlpha() * 255f);
        float hover = (float) hoverAnimation.getValue();
        float raw = (float) toggleAnimation.getValue();
        float value = Math.max(0f, Math.min(1f, raw));

        float trackW = scaled(17f);
        float trackH = scaled(9.5f);
        float trackX = getX() + getWidth() - trackW;
        float trackY = getY() + (getHeight() - trackH) / 2f;
        float round = trackH / 2f;

        float fontSize = scaled(7f);
        Color textColor = ColorUtil.interpolate(
                UIColors.textColor(full),
                UIColors.inactiveTextColor((int) (full * 0.82f)), Math.max(value, hover));

        Fonts.PS_MEDIUM.drawWrap(matrices, setting.getName(), getX(), getY() + (getHeight() - fontSize) / 2f,
                getWidth() - trackW - scaled(6f), fontSize, textColor, scaled(16f),
                Duration.ofMillis(3000), Duration.ofMillis(500));

        RenderUtil.RECT.draw(matrices, trackX, trackY, trackW, trackH, round,
                ColorUtil.setAlpha(UIColors.surfaceInner(), (int) ((0.8f + 0.2f * hover) * full)));

        if (value > 0.01f) {
            Color left = ColorUtil.setAlpha(UIColors.primary(), (int) (value * full));
            Color right = ColorUtil.setAlpha(UIColors.secondary(), (int) (value * full));
            RenderUtil.GRADIENT_RECT.draw(matrices, trackX, trackY, trackW, trackH, round, left, right, left, right);
        }

        float knob = trackH - scaled(3f);
        float travel = trackW - knob - scaled(3f);
        float knobX = trackX + scaled(1.5f) + travel * raw;
        knobX = Math.max(trackX + scaled(1.5f), Math.min(trackX + trackW - knob - scaled(1.5f), knobX));

        RenderUtil.RECT.draw(matrices, knobX, trackY + scaled(1.5f), knob, knob, knob / 2f,
                ColorUtil.setAlpha(Color.WHITE, full));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.toggle();
        }
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
