package com.fest.visuals.client.ui.clickgui.module.settings;

import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.MultiBooleanSetting;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MouseUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.client.ui.clickgui.module.SettingComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class MultiBooleanComponent extends SettingComponent {
    private final MultiBooleanSetting setting;
    private final List<Bound> bounds = new ArrayList<>();
    private final Map<String, AnimationUtil> chipAnimations = new HashMap<>();

    public MultiBooleanComponent(MultiBooleanSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());

        for (BooleanSetting value : setting.getValue()) {
            AnimationUtil anim = new AnimationUtil();
            anim.setValue(value.getValue() ? 1.0 : 0.0);
            chipAnimations.put(value.getName(), anim);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PoseStack matrixStack = RenderUtil.matrices();

        float fontSize = scaled(7f);
        float header = fontSize + scaled(5f);
        int fullAlpha = (int) (getAlpha() * 255f);

        int enabled = 0;
        int total = 0;
        for (BooleanSetting value : setting.getValue()) {
            if (!value.isVisible()) continue;
            total++;
            if (value.getValue()) enabled++;
        }

        String countText = enabled + "/" + total;
        float countWidth = Fonts.PS_MEDIUM.getWidth(countText, fontSize);

        Fonts.PS_MEDIUM.drawWrap(matrixStack, setting.getName(), getX(), getY() + scaled(0.5f), getWidth() - countWidth - offset(), fontSize, UIColors.textColor(fullAlpha), scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));
        Fonts.PS_MEDIUM.drawText(matrixStack, countText, getX() + getWidth() - countWidth, getY() + scaled(0.5f), fontSize, UIColors.inactiveTextColor(fullAlpha));

        bounds.clear();
        float currentX = getX();
        float currentY = getY() + header;
        float tileSize = fontSize * 0.9f;
        float tileHeight = tileSize * 2.1f;
        float tilePadding = scaled(3f);

        for (BooleanSetting value : setting.getValue()) {
            if (!value.isVisible()) continue;

            AnimationUtil chipAnim = chipAnimations.computeIfAbsent(value.getName(), name -> {
                AnimationUtil anim = new AnimationUtil();
                anim.setValue(value.getValue() ? 1.0 : 0.0);
                return anim;
            });
            chipAnim.update();
            chipAnim.run(value.getValue() ? 1.0 : 0.0, 250, Easing.EXPO_OUT);

            float textWidth = Fonts.PS_MEDIUM.getWidth(value.getName(), tileSize);
            float tileWidth = textWidth + tileSize * 1.6f;

            if (currentX + tileWidth > getX() + getWidth() && currentX > getX()) {
                currentX = getX();
                currentY += tileHeight + tilePadding;
            }

            bounds.add(new Bound(currentX, currentY, tileWidth, tileHeight, value));

            float anim = (float) chipAnim.getValue();
            Color rectColor = ColorUtil.setAlpha(ColorUtil.interpolate(UIColors.primary(), UIColors.surfaceInner(), anim), fullAlpha);
            Color textColor = ColorUtil.interpolate(UIColors.textColor(fullAlpha), UIColors.inactiveTextColor(fullAlpha), anim);

            RenderUtil.RECT.draw(matrixStack, currentX, currentY, tileWidth, tileHeight, tileHeight / 2f, rectColor);
            Fonts.PS_MEDIUM.drawCenteredText(matrixStack, value.getName(), currentX + tileWidth / 2f, currentY + tileHeight / 2f - tileSize / 2f, tileSize, textColor);

            currentX += tileWidth + tilePadding;
        }

        setHeight(currentY - getY() + tileHeight + gap());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        for (Bound bound : bounds) {
            if (MouseUtil.isHovered(mouseX, mouseY, bound.x, bound.y, bound.width, bound.height)) {
                bound.setting.toggle();
                return;
            }
        }
    }

    private float getDefaultHeight() {
        return 12f;
    }

    private record Bound(float x, float y, float width, float height, BooleanSetting setting) {}

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
