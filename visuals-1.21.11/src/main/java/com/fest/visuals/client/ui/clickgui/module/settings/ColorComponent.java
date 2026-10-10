package com.fest.visuals.client.ui.clickgui.module.settings;

import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.system.files.FileUtil;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.ui.clickgui.module.ExpandableComponent;
import com.fest.visuals.client.ui.theme.Theme;

import java.awt.*;
import net.minecraft.client.gui.GuiGraphics;

import static com.fest.visuals.api.system.interfaces.QuickImports.mc;

import com.mojang.blaze3d.vertex.PoseStack;

public class ColorComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final Theme.ElementColor elementColor;
    private final ColorSetting setting;

    private boolean draggingWheel = false;
    private boolean draggingBright = false;
    private boolean draggingAlpha = false;

    private float hueCache = 0f;
    private float satCache = 0f;
    private boolean inited;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.setting = setting;
        this.elementColor = null;
        updateHeight(getDefaultHeight());
        initHueCache();
    }

    public ColorComponent(Theme.ElementColor elementColor) {
        super(null);
        this.elementColor = elementColor;
        this.setting = null;
        updateHeight(getDefaultHeight());
    }

    private void initHueCache() {
        if (inited) return;
        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hueCache = hsb[0];
        satCache = hsb[1];
        inited = true;
    }

    private Color getCurrentColor() {
        return setting != null ? setting.getValue() : elementColor.getColor();
    }

    private void setCurrentColor(Color color) {
        if (setting != null) setting.setValue(color);
        else elementColor.setColor(color);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        PoseStack ms = RenderUtil.matrices();
        updateOpen();
        initHueCache();

        if (draggingWheel) updateWheel(mouseX, mouseY);
        if (draggingBright) updateBright(mouseX);
        if (draggingAlpha) updateAlpha(mouseX);

        float baseHeight = scaled(getDefaultHeight());
        float fontSize = baseHeight * 0.45f;
        int fullAlpha = (int) (getAlpha() * 255f);

        if (setting != null) {
            Fonts.PS_MEDIUM.drawText(ms, setting.getName(), getX(), getY() + baseHeight / 2f - fontSize / 2f, fontSize, UIColors.textColor(fullAlpha));

            float previewSize = baseHeight * 0.7f;
            float previewX = getX() + getWidth() - previewSize;
            float previewY = getY() + baseHeight / 2f - previewSize / 2f;
            float previewRound = previewSize * 0.2f;
            RenderUtil.RECT.draw(ms, previewX, previewY, previewSize, previewSize, previewRound, ColorUtil.setAlpha(getCurrentColor(), (int) (getCurrentColor().getAlpha() / 255f * fullAlpha)));
            updateHeight(getDefaultHeight());
        }

        float animValue = getAnimValue();
        if (animValue > 0.0) {
            drawWheel(ms, animValue);
            drawBrightBar(ms, animValue);
            drawAlphaBar(ms, animValue);
            drawSelectors(ms);

            float extraHeight = (getWheelSize() + getBarHeight() * 2 + gap() * 2) * animValue;
            float baseHeightFinal = setting != null ? baseHeight : 0f;
            setHeight(baseHeightFinal + extraHeight);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (setting != null && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()))) {
            toggleOpen();
            return;
        }

        if (isNotOver()) return;

        float wheelSize = getWheelSize();
        float wheelX = getPickerX() + getWidth() / 2f - wheelSize / 2f;
        if (MouseUtil.isHovered(mouseX, mouseY, wheelX, getWheelY(), wheelSize, wheelSize)) {
            draggingWheel = true;
            updateWheel(mouseX, mouseY);
        } else if (MouseUtil.isHovered(mouseX, mouseY, getPickerX(), getBrightY(), getPickerWidth(), getBarHeight())) {
            draggingBright = true;
            updateBright(mouseX);
        } else if (MouseUtil.isHovered(mouseX, mouseY, getPickerX(), getAlphaY(), getPickerWidth(), getBarHeight())) {
            draggingAlpha = true;
            updateAlpha(mouseX);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        draggingWheel = false;
        draggingBright = false;
        draggingAlpha = false;
    }

    private void updateWheel(double mouseX, double mouseY) {
        float wheelSize = getWheelSize();
        float wheelX = getPickerX() + getWidth() / 2f - wheelSize / 2f;
        float wheelY = getWheelY() + getAnimY();
        float cx = wheelX + wheelSize / 2f;
        float cy = wheelY + wheelSize / 2f;
        float dx = (float) (mouseX - cx);
        float dy = (float) (mouseY - cy);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float radius = wheelSize / 2f;

        float hue = (float) ((Math.atan2(dy, dx) + Math.PI) / (2 * Math.PI));
        float sat = Math.min(1f, dist / radius);
        
        hueCache = hue;
        satCache = sat;

        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        setCurrentColor(new Color(Color.HSBtoRGB(hue, sat, hsb[2])));
    }

    private void updateBright(double mouseX) {
        float rel = (float) ((mouseX - getPickerX()) / getPickerWidth());
        rel = Math.max(0f, Math.min(1f, rel));

        Color color = getCurrentColor();
        setCurrentColor(new Color(Color.HSBtoRGB(hueCache, satCache, rel)));
    }

    private void updateAlpha(double mouseX) {
        float rel = (float) ((mouseX - getPickerX()) / getPickerWidth());
        rel = Math.max(0f, Math.min(1f, rel));
        int alpha = (int) (rel * 255);

        Color c = getCurrentColor();
        setCurrentColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
    }

    private void drawWheel(PoseStack ms, float animValue) {
        float wheelSize = getWheelSize();
        float wheelX = getPickerX() + getWidth() / 2f - wheelSize / 2f;
        float y = getWheelY() + getAnimY();

        int globalAlpha = (int) (getAnimValue() * getAlpha() * 255f);
        Color color = getCurrentColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        
        RenderUtil.TEXTURE_RECT.draw(
                ms, wheelX, y, wheelSize, wheelSize, wheelSize / 2f,
                new Color(255, 255, 255, globalAlpha),
                0f, 0f, 1f, 1f,
                FileUtil.getImage("interface/color_wheel")
        );

        float darkness = 1f - hsb[2];
        if (darkness > 0) {
            RenderUtil.RECT.draw(ms, wheelX, y, wheelSize, wheelSize, wheelSize / 2f, new Color(0, 0, 0, (int) (darkness * globalAlpha)));
        }
    }

    private void drawBrightBar(PoseStack ms, float animValue) {
        float y = getBrightY() + getAnimY();
        float h = getBarHeight();
        Color left = new Color(0, 0, 0, (int) (getAnimValue() * getAlpha() * 255f));
        Color right = new Color(Color.HSBtoRGB(hueCache, satCache, 1f));
        right = ColorUtil.setAlpha(right, (int) (getAnimValue() * getAlpha() * 255f));

        RenderUtil.GRADIENT_RECT.draw(ms, getPickerX(), y, getPickerWidth(), h, h * 0.3f, left, right, left, right);
    }

    private void drawAlphaBar(PoseStack ms, float animValue) {
        float y = getAlphaY() + getAnimY();
        float h = getBarHeight();
        Color c = getCurrentColor();
        Color left = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
        Color right = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (getAnimValue() * getAlpha() * 255f));

        RenderUtil.GRADIENT_RECT.draw(ms, getPickerX(), y, getPickerWidth(), h, h * 0.3f, left, right, left, right);
    }

    private void drawSelectors(PoseStack ms) {
        int alpha = (int) (getAnimValue() * getAlpha() * 255f);
        Color currentColor = getCurrentColor();
        Color cursorColor = ColorUtil.setAlpha(Color.WHITE, alpha);
        float[] hsb = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        float lineOffset = scaled(4f);
        float lineWidth = lineOffset;
        float lineHeight = lineWidth;
        float lineRound = lineOffset * 0.5f;
        float lineYOffset = getBarHeight() / 2f - lineHeight / 2f;

        float circleOffset = scaled(2f);
        float circleSize = circleOffset * 2f;

        float wheelSize = getWheelSize();
        float wheelX = getPickerX() + getWidth() / 2f - wheelSize / 2f;
        float angle = (hueCache * 2f * (float) Math.PI) - (float) Math.PI;
        float dist = satCache * (wheelSize / 2f);
        float selX = wheelX + wheelSize / 2f + (float) Math.cos(angle) * dist;
        float selY = getWheelY() + getAnimY() + wheelSize / 2f + (float) Math.sin(angle) * dist;
        RenderUtil.RECT.draw(ms, selX - circleOffset, selY - circleOffset, circleSize, circleSize, circleSize * 0.5f, cursorColor);

        float brightX = getPickerX() + hsb[2] * getPickerWidth();
        RenderUtil.RECT.draw(ms, brightX - lineOffset, getBrightY() + getAnimY() + lineYOffset, lineWidth, lineHeight, lineRound, cursorColor);

        float alphaRel = currentColor.getAlpha() / 255f;
        float alphaX = getPickerX() + alphaRel * getPickerWidth();
        RenderUtil.RECT.draw(ms, alphaX - lineOffset, getAlphaY() + getAnimY() + lineYOffset, lineWidth, lineHeight, lineRound, cursorColor);
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) { }

    private float getAnimY() { return (-gap() * (1f - getAnimValue())); }
    private float getWheelY() { return getY() + (setting != null ? scaled(getDefaultHeight()) : 0f); }
    private float getWheelSize() { return getWidth() * getAnimValue() * 0.55f; }
    private float getBrightY() { return getWheelY() + getWheelSize() + gap(); }
    private float getBarHeight() { return scaled(5f) * getAnimValue(); }
    private float getAlphaY() { return getBrightY() + getBarHeight() + gap(); }
    private float getPickerX() { return getX(); }
    private float getPickerWidth() { return getWidth(); }
    private float getDefaultHeight() { return 15f; }
    private float getAnimValue() { return getValue(); }
}
