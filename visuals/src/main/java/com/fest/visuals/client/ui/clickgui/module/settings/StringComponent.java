package com.fest.visuals.client.ui.clickgui.module.settings;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import com.fest.visuals.api.module.setting.StringSetting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.ScissorUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.ui.clickgui.module.SettingComponent;

/**
 * Editable text field: label above, box below.
 *
 * <p>Click to focus, type, Enter or Escape to let go. A secret setting is drawn as dots — the
 * value is still stored in plain text, this only keeps a password off the screen.
 */
public class StringComponent extends SettingComponent {
    private final StringSetting setting;
    private final AnimationUtil focusAnimation = new AnimationUtil();

    private final StringBuilder buffer = new StringBuilder();
    private boolean focused;

    public StringComponent(StringSetting setting) {
        super(setting);
        this.setting = setting;
        this.buffer.append(setting.getValue());
        updateHeight(30f);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateHeight(30f);

        // Another source (a config load) may have changed the value while the field was idle.
        if (!focused && !buffer.toString().equals(setting.getValue())) {
            buffer.setLength(0);
            buffer.append(setting.getValue());
        }

        focusAnimation.update();
        focusAnimation.run(focused ? 1.0 : 0.0, 260, Easing.EXPO_OUT);
        float focus = (float) focusAnimation.getValue();

        PoseStack matrices = RenderUtil.matrices();
        int full = (int) (getAlpha() * 255f);

        float labelSize = scaled(7f);
        Fonts.PS_MEDIUM.drawText(matrices, setting.getName(), getX(), getY(), labelSize,
                UIColors.inactiveTextColor((int) (full * 0.85f)));

        float boxY = getY() + labelSize + scaled(4f);
        float boxH = scaled(15f);
        float round = scaled(4f);
        boolean over = MouseUtil.isHovered(mouseX, mouseY, getX(), boxY, getWidth(), boxH);

        RenderUtil.RECT.draw(matrices, getX(), boxY, getWidth(), boxH, round,
                ColorUtil.setAlpha(UIColors.surfaceInner(), (int) ((0.75f + (over ? 0.25f : 0f)) * full)));

        if (focus > 0.01f) {
            float line = scaled(0.7f);
            RenderUtil.RECT.draw(matrices, getX(), boxY + boxH - line, getWidth() * focus, line, line,
                    ColorUtil.setAlpha(UIColors.primary(), (int) (focus * getAlpha() * 220f)));
        }

        float fontSize = scaled(6.6f);
        String shown = display();
        boolean empty = shown.isEmpty();
        if (empty) shown = setting.getPlaceholder();

        Color textColor = empty
                ? UIColors.inactiveTextColor((int) (full * 0.5f))
                : UIColors.textColor(full);

        float textX = getX() + scaled(5f);
        float textY = boxY + (boxH - fontSize) / 2f;
        float textWidth = Fonts.PS_MEDIUM.getWidth(shown, fontSize);
        float room = getWidth() - scaled(12f);
        // Long values scroll with the caret instead of spilling out of the box.
        float shift = focused && textWidth > room ? textWidth - room : 0f;

        if (setting.isSecret()) drawEye(matrices, boxY, boxH, mouseX, mouseY, full);

        ScissorUtil.start(matrices, getX(), boxY,
                getWidth() - scaled(3f) - (setting.isSecret() ? eyeSize() + scaled(3f) : 0f), boxH);
        Fonts.PS_MEDIUM.drawText(matrices, shown, textX - shift, textY, fontSize, textColor);

        if (focused) {
            float blink = (float) ((Math.sin(System.currentTimeMillis() / 300.0) + 1.0) / 2.0);
            float caretX = textX - shift + (empty ? 0f : textWidth) + scaled(1f);
            RenderUtil.RECT.draw(matrices, caretX, textY, scaled(0.6f), fontSize, 0f,
                    ColorUtil.setAlpha(UIColors.primary(), (int) (blink * getAlpha() * 255f)));
        }
        ScissorUtil.stop(matrices);
    }

    private String display() {
        if (!setting.isSecret() || setting.isRevealed()) return buffer.toString();
        return "•".repeat(buffer.length());
    }

    private float eyeSize() {
        return scaled(11f);
    }

    /** The reveal button, drawn inside the right edge of the field. */
    private boolean hoveredEye(double mouseX, double mouseY) {
        if (!setting.isSecret()) return false;

        float boxY = getY() + scaled(7f) + scaled(4f);
        float boxH = scaled(15f);
        float size = eyeSize();
        return MouseUtil.isHovered(mouseX, mouseY, getX() + getWidth() - size - scaled(3f),
                boxY + (boxH - size) / 2f, size, size);
    }

    private void drawEye(PoseStack matrices, float boxY, float boxH, int mouseX, int mouseY, int full) {
        float size = eyeSize();
        float x = getX() + getWidth() - size - scaled(3f);
        float y = boxY + (boxH - size) / 2f;

        boolean over = hoveredEye(mouseX, mouseY);
        Color tint = setting.isRevealed()
                ? ColorUtil.setAlpha(UIColors.primary(), full)
                : UIColors.inactiveTextColor((int) (full * (over ? 1f : 0.7f)));

        // An eye: a lens with a pupil, struck through while the value is hidden.
        float lensH = size * 0.52f;
        RenderUtil.RECT.draw(matrices, x, y + (size - lensH) / 2f, size, lensH, lensH / 2f, tint);

        float pupil = lensH * 0.5f;
        RenderUtil.RECT.draw(matrices, x + (size - pupil) / 2f, y + (size - pupil) / 2f, pupil, pupil, pupil / 2f,
                ColorUtil.setAlpha(UIColors.surfaceInner(), full));

        if (!setting.isRevealed()) {
            float thick = scaled(1f);
            RenderUtil.RECT.draw(matrices, x, y + size / 2f - thick / 2f, size, thick, thick / 2f, tint);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;

        if (hoveredEye(mouseX, mouseY)) {
            setting.setRevealed(!setting.isRevealed());
            return;
        }

        boolean hit = MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
        if (focused && !hit) commit();
        focused = hit;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return;

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (buffer.length() > 0) buffer.deleteCharAt(buffer.length() - 1);
                commit();
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> {
                commit();
                focused = false;
            }
            case GLFW.GLFW_KEY_V -> {
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    append(Minecraft.getInstance().keyboardHandler.getClipboard());
                }
            }
            default -> { }
        }
    }

    @Override
    public void charTyped(char chr) {
        if (!focused || Character.isISOControl(chr)) return;
        append(String.valueOf(chr));
    }

    private void append(String text) {
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c)) continue;
            if (buffer.length() >= setting.getMaxLength()) break;
            buffer.append(c);
        }
        commit();
    }

    private void commit() {
        setting.setValue(buffer.toString());
    }

    /** Escape reaches the screen first; let it close the field before it closes the GUI. */
    public boolean isFocused() {
        return focused;
    }

    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
