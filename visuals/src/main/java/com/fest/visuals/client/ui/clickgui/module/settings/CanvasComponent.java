package com.fest.visuals.client.ui.clickgui.module.settings;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.fest.visuals.api.module.setting.CanvasSetting;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.ui.clickgui.module.SettingComponent;

/**
 * The paintable grid: left button draws, right button erases, and both keep painting while the
 * button is held so a line can be drawn in one stroke.
 *
 * <p>Two buttons underneath clear the grid and put the default cross back, since a grid painted
 * into a corner is otherwise tedious to undo.
 */
public class CanvasComponent extends SettingComponent {
    private final CanvasSetting setting;

    private boolean painting;
    private boolean erasing;

    public CanvasComponent(CanvasSetting setting) {
        super(setting);
        this.setting = setting;
    }

    private float cell() {
        return getWidth() / CanvasSetting.SIZE;
    }

    private float gridY() {
        return getY() + scaled(9f);
    }

    private float buttonsY() {
        return gridY() + getWidth() + scaled(4f);
    }

    private float buttonHeight() {
        return scaled(12f);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        setHeight(scaled(9f) + getWidth() + scaled(4f) + buttonHeight());

        PoseStack matrices = RenderUtil.matrices();
        int full = (int) (getAlpha() * 255f);

        Fonts.PS_MEDIUM.drawText(matrices, setting.getName(), getX(), getY(), scaled(7f),
                UIColors.inactiveTextColor((int) (full * 0.85f)));

        float cell = cell();
        float top = gridY();

        RenderUtil.RECT.draw(matrices, getX(), top, getWidth(), getWidth(), scaled(3f),
                ColorUtil.setAlpha(Color.BLACK, (int) (full * 0.45f)));

        if (painting || erasing) paint(mouseX, mouseY);

        int centre = CanvasSetting.SIZE / 2;
        for (int row = 0; row < CanvasSetting.SIZE; row++) {
            for (int column = 0; column < CanvasSetting.SIZE; column++) {
                float cx = getX() + column * cell;
                float cy = top + row * cell;

                if (setting.get(column, row)) {
                    RenderUtil.RECT.draw(matrices, cx, cy, cell, cell, 0f, UIColors.primary(full));
                } else if (column == centre || row == centre) {
                    // Faint cross-hairs mark the centre so a shape can be aimed at it.
                    RenderUtil.RECT.draw(matrices, cx, cy, cell, cell, 0f,
                            ColorUtil.setAlpha(UIColors.inactiveTextColor(), (int) (full * 0.12f)));
                }
            }
        }

        float half = (getWidth() - scaled(4f)) / 2f;
        drawButton(matrices, getX(), half, "Очистить", mouseX, mouseY, full);
        drawButton(matrices, getX() + half + scaled(4f), half, "Сброс", mouseX, mouseY, full);
    }

    private void drawButton(PoseStack matrices, float x, float width, String label, int mouseX, int mouseY, int full) {
        float y = buttonsY();
        float height = buttonHeight();
        boolean over = MouseUtil.isHovered(mouseX, mouseY, x, y, width, height);

        RenderUtil.RECT.draw(matrices, x, y, width, height, scaled(4f),
                ColorUtil.setAlpha(UIColors.surfaceInner(), (int) ((over ? 1f : 0.75f) * full)));

        float fontSize = scaled(6.5f);
        Fonts.PS_MEDIUM.drawCenteredText(matrices, label, x + width / 2f, y + (height - fontSize) / 2f,
                fontSize, over ? UIColors.textColor(full) : UIColors.inactiveTextColor(full));
    }

    private void paint(double mouseX, double mouseY) {
        float cell = cell();
        float top = gridY();
        if (!MouseUtil.isHovered(mouseX, mouseY, getX(), top, getWidth(), getWidth())) return;

        int column = (int) ((mouseX - getX()) / cell);
        int row = (int) ((mouseY - top) / cell);
        setting.set(column, row, painting);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        float half = (getWidth() - scaled(4f)) / 2f;
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), buttonsY(), half, buttonHeight())) {
            setting.clear();
            return;
        }
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX() + half + scaled(4f), buttonsY(), half, buttonHeight())) {
            setting.reset();
            return;
        }

        if (!MouseUtil.isHovered(mouseX, mouseY, getX(), gridY(), getWidth(), getWidth())) return;

        painting = button == 0;
        erasing = button == 1;
        paint(mouseX, mouseY);
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        painting = false;
        erasing = false;
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
