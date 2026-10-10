package com.fest.visuals.client.ui.clickgui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import com.fest.visuals.api.system.configs.ConfigManager;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.ScissorUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.api.utils.render.fonts.Icons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ClickGuiConfigs {
    private final StringBuilder name = new StringBuilder();
    private boolean typing;
    private float scroll;
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    private List<String> configs = new ArrayList<>();

    public void refresh() {
        configs = new ArrayList<>(ConfigManager.getInstance().getConfigsNames());
        configs.sort(String.CASE_INSENSITIVE_ORDER);
    }

    public void close() {
        typing = false;
        name.setLength(0);
    }

    public boolean isTyping() {
        return typing;
    }

    public void render(GuiGraphics context, float windowX, float windowY, float alpha, int mouseX, int mouseY) {
        PoseStack matrices = RenderUtil.matrices();
        int full = (int) (alpha * 255f);
        float x = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(12f);
        float y = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(10f);
        float width = ClickGuiLayout.contentWidth() - ClickGuiLayout.scaled(24f);
        float fieldH = ClickGuiLayout.scaled(18f);

        RenderUtil.RECT.draw(matrices, x, y, width - ClickGuiLayout.scaled(64f), fieldH, ClickGuiLayout.scaled(5f), UIColors.surface(full));
        String placeholder = typing || !name.isEmpty() ? name.toString() : "Имя конфига...";
        Color fieldColor = name.isEmpty() && !typing ? UIColors.inactiveTextColor(full) : UIColors.textColor(full);
        Fonts.PS_MEDIUM.drawText(matrices, placeholder, x + ClickGuiLayout.scaled(6f), y + ClickGuiLayout.scaled(5.5f), ClickGuiLayout.scaled(7f), fieldColor);

        float saveX = x + width - ClickGuiLayout.scaled(58f);
        RenderUtil.RECT.draw(matrices, saveX, y, ClickGuiLayout.scaled(58f), fieldH, ClickGuiLayout.scaled(5f), UIColors.primary(full));
        Fonts.PS_BOLD.drawCenteredText(matrices, "Save", saveX + ClickGuiLayout.scaled(29f), y + ClickGuiLayout.scaled(5.5f), ClickGuiLayout.scaled(7f), ColorUtil.setAlpha(Color.WHITE, full));

        float listY = y + fieldH + ClickGuiLayout.scaled(8f);
        float listH = ClickGuiLayout.contentHeight() - fieldH - ClickGuiLayout.scaled(22f);
        scrollAnimation.update();
        scrollAnimation.run(scroll, 200, Easing.EXPO_OUT);

        ScissorUtil.start(matrices, x, listY, width, listH);
        float offset = (float) scrollAnimation.getValue();
        float row = listY + offset;
        for (String config : configs) {
            RenderUtil.RECT.draw(matrices, x, row, width, ClickGuiLayout.scaled(22f), ClickGuiLayout.scaled(5f), UIColors.surface(full));
            Fonts.PS_MEDIUM.drawText(matrices, config, x + ClickGuiLayout.scaled(8f), row + ClickGuiLayout.scaled(7f), ClickGuiLayout.scaled(7.5f), UIColors.textColor(full));

            float loadX = x + width - ClickGuiLayout.scaled(52f);
            float delX = x + width - ClickGuiLayout.scaled(26f);
            Fonts.ICONS.drawText(matrices, Icons.RIGHTR.getLetter(), loadX, row + ClickGuiLayout.scaled(6f), ClickGuiLayout.scaled(9f), UIColors.positiveColor(full));
            Fonts.ICONS.drawText(matrices, Icons.TRASH.getLetter(), delX, row + ClickGuiLayout.scaled(6f), ClickGuiLayout.scaled(9f), UIColors.negativeColor(full));
            row += ClickGuiLayout.scaled(26f);
        }
        ScissorUtil.stop(matrices);

        float content = configs.size() * ClickGuiLayout.scaled(26f);
        scroll = Mth.clamp(scroll, Math.min(listH - content, 0f), 0f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, float windowX, float windowY) {
        float x = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(12f);
        float y = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(10f);
        float width = ClickGuiLayout.contentWidth() - ClickGuiLayout.scaled(24f);
        float fieldH = ClickGuiLayout.scaled(18f);

        if (MouseUtil.isHovered(mouseX, mouseY, x, y, width - ClickGuiLayout.scaled(64f), fieldH)) {
            typing = true;
            return true;
        }

        float saveX = x + width - ClickGuiLayout.scaled(58f);
        if (MouseUtil.isHovered(mouseX, mouseY, saveX, y, ClickGuiLayout.scaled(58f), fieldH) && !name.isEmpty()) {
            ConfigManager.getInstance().save(name.toString());
            name.setLength(0);
            typing = false;
            refresh();
            return true;
        }

        float listY = y + fieldH + ClickGuiLayout.scaled(8f);
        float row = listY + (float) scrollAnimation.getValue();
        for (String config : configs) {
            if (MouseUtil.isHovered(mouseX, mouseY, x, row, width, ClickGuiLayout.scaled(22f))) {
                float loadX = x + width - ClickGuiLayout.scaled(52f);
                float delX = x + width - ClickGuiLayout.scaled(26f);
                if (MouseUtil.isHovered(mouseX, mouseY, loadX - ClickGuiLayout.scaled(4f), row, ClickGuiLayout.scaled(22f), ClickGuiLayout.scaled(22f))) {
                    ConfigManager.getInstance().load(config);
                    return true;
                }
                if (MouseUtil.isHovered(mouseX, mouseY, delX - ClickGuiLayout.scaled(4f), row, ClickGuiLayout.scaled(22f), ClickGuiLayout.scaled(22f))) {
                    ConfigManager.getInstance().remove(config);
                    refresh();
                    return true;
                }
            }
            row += ClickGuiLayout.scaled(26f);
        }
        typing = false;
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float windowX, float windowY) {
        if (!MouseUtil.isHovered(mouseX, mouseY, ClickGuiLayout.contentX(windowX), ClickGuiLayout.contentY(windowY), ClickGuiLayout.contentWidth(), ClickGuiLayout.contentHeight())) {
            return false;
        }
        scroll += (float) (amount * ClickGuiLayout.scaled(16f));
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!typing) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !name.isEmpty()) {
            name.deleteCharAt(name.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && !name.isEmpty()) {
            ConfigManager.getInstance().save(name.toString());
            name.setLength(0);
            typing = false;
            refresh();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            typing = false;
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (!typing) return false;
        if (name.length() < 24 && !Character.isISOControl(chr)) {
            name.append(chr);
            return true;
        }
        return false;
    }
}
