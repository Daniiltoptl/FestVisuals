package com.fest.visuals.client.ui.clickgui.module.settings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.features.modules.utility.SoundsModule;
import com.fest.visuals.client.ui.clickgui.ClickGuiIcons;
import com.fest.visuals.client.ui.clickgui.module.SettingComponent;

/**
 * Vertical list of choices with a tick on the selected one.
 *
 * <p>A single highlight slides between rows instead of every row fading on its own, which reads as
 * one object moving rather than two things blinking.
 */
public class ModeComponent extends SettingComponent {
    private final ModeSetting setting;
    private final List<Bound> bounds = new ArrayList<>();

    private final AnimationUtil selectAnimation = new AnimationUtil();
    private final AnimationUtil hoverAnimation = new AnimationUtil();
    private int hoveredIndex = -1;

    public ModeComponent(ModeSetting setting) {
        super(setting);
        this.setting = setting;
        selectAnimation.setValue(indexOf(setting.getValue()));
        updateHeight(14f);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PoseStack matrices = RenderUtil.matrices();
        int full = (int) (getAlpha() * 255f);

        float titleSize = scaled(7f);
        float rowHeight = scaled(14f);
        float optionSize = scaled(6.8f);

        Fonts.PS_MEDIUM.drawText(matrices, setting.getName(), getX(), getY(), titleSize,
                UIColors.inactiveTextColor((int) (full * 0.85f)));

        float listY = getY() + titleSize + scaled(5f);
        float indent = scaled(6f);

        selectAnimation.update();
        selectAnimation.run(indexOf(setting.getValue()), 420, Easing.EXPO_OUT);

        hoverAnimation.update();
        hoverAnimation.run(hoveredIndex >= 0 ? 1.0 : 0.0, 200, Easing.EXPO_OUT);

        // Selection highlight, positioned by the animated index so it glides between rows.
        float slideY = listY + (float) selectAnimation.getValue() * rowHeight;
        RenderUtil.RECT.draw(matrices, getX(), slideY, getWidth(), rowHeight, scaled(5f),
                ColorUtil.setAlpha(UIColors.primary(), (int) (getAlpha() * 30f)));
        RenderUtil.RECT.draw(matrices, getX(), slideY + rowHeight * 0.25f, scaled(1.6f), rowHeight * 0.5f, scaled(0.8f),
                ColorUtil.setAlpha(UIColors.primary(), full));

        bounds.clear();
        hoveredIndex = -1;

        for (int i = 0; i < setting.getModes().size(); i++) {
            String mode = setting.getModes().get(i);
            float rowY = listY + i * rowHeight;
            boolean selected = setting.is(mode);
            boolean over = MouseUtil.isHovered(mouseX, mouseY, getX(), rowY, getWidth(), rowHeight);
            if (over) hoveredIndex = i;

            bounds.add(new Bound(getX(), rowY, getWidth(), rowHeight, mode));

            if (over && !selected) {
                RenderUtil.RECT.draw(matrices, getX(), rowY, getWidth(), rowHeight, scaled(5f),
                        ColorUtil.setAlpha(UIColors.surfaceInner(), (int) (getAlpha() * 120f)));
            }

            Color color = selected
                    ? ColorUtil.setAlpha(Color.WHITE, full)
                    : UIColors.inactiveTextColor((int) (full * (over ? 0.9f : 0.65f)));

            Fonts.PS_MEDIUM.drawText(matrices, mode, getX() + indent, rowY + (rowHeight - optionSize) / 2f, optionSize, color);

            if (selected) {
                ClickGuiIcons.check(matrices, getX() + getWidth() - scaled(7f), rowY + rowHeight / 2f, scaled(6.5f),
                        ColorUtil.setAlpha(UIColors.primary(), full));
            }
        }

        setHeight(titleSize + scaled(5f) + setting.getModes().size() * rowHeight);
    }

    private float indexOf(String mode) {
        int index = setting.getModes().indexOf(mode);
        return Math.max(0, index);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        for (Bound bound : bounds) {
            if (MouseUtil.isHovered(mouseX, mouseY, bound.x, bound.y, bound.width, bound.height)) {
                setting.setValue(bound.value);
                SoundsModule.getInstance().playClickSound(true);
                return;
            }
        }
    }

    private record Bound(float x, float y, float width, float height, String value) {}

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
