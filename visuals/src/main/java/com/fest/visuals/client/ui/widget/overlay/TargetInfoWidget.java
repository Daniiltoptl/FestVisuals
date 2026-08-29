package com.fest.visuals.client.ui.widget.overlay;

import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.math.MathUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.client.features.modules.hud.TargetHudModule;
import com.fest.visuals.client.ui.widget.Widget;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.time.Duration;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TargetInfoWidget extends Widget {
    @Override
    public String getName() {
        return "Target info";
    }

    public TargetInfoWidget() {
        super(30f, 30f);
    }

    private final AnimationUtil showAnimation = new AnimationUtil();

    private float healthAnimation = 0f;

    private LivingEntity target;

    @Override
    public void render(PoseStack matrixStack) {
        update();
        LivingEntity pretendTarget = getTarget();

        if (pretendTarget != null) {
            target = pretendTarget;
        }

        if (showAnimation.getValue() <= 0.0 || target == null) return;

        healthAnimation = Mth.clamp(MathUtil.interpolate(healthAnimation, target.getHealth() / target.getMaxHealth(), 0.3f), 0f, 1f);
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float anim = (float) showAnimation.getValue();

        float[] headProperties = headProperties(x, y);
        float headX = headProperties[0];
        float headY = headProperties[1];
        float headSize = headProperties[2];

        float bigFontSize = headSize * 0.35f;
        float smallFontSize = (headSize * 0.4f) * 0.7f;

        String targetName = target.getName().getString();
        String healthText = String.format("%.1f", target.getHealth() + target.getAbsorptionAmount()) + "HP";
        float healthTextWidth = getMediumFont().getWidth(healthText, smallFontSize);

        float offset = getGap() * 3f;
        float margin = getGap() * 2f;
        float width = headSize * 3.7f + margin * 2f;
        float height = headSize + getGap() * 2f;
        float backgroundRound = offset * 0.7f;

        int fullAlpha = (int) (anim * 255f);

        float[] healthBarProperties = healthBarProperties();
        float healthBarHeight = healthBarProperties[0];
        float healthBarRound = healthBarProperties[1];
        float healthBarY = y + height - healthBarHeight - margin;
        float healthBarX = x + headSize + margin;
        float healthBarWidth = width - margin * 2.5f - headSize - healthTextWidth;

        float diffHealth = Math.abs(smallFontSize - healthBarHeight) / 2f;

        float nameDiffToHealthBar = Math.abs((y + margin) - (healthBarY - margin / 2f));
        float nameY = y + margin + nameDiffToHealthBar / 2f - bigFontSize / 2f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, backgroundRound, UIColors.widgetBlur(fullAlpha));

        Color textColor = UIColors.textColor(fullAlpha);
        getMediumFont().drawWrap(matrixStack, targetName, healthBarX, nameY, width - headSize - margin, bigFontSize, textColor, scaled(9f), Duration.ofMillis(2500), Duration.ofMillis(1700));
        getMediumFont().drawText(matrixStack, healthText, x + width - healthTextWidth - margin, healthBarY - diffHealth, smallFontSize, textColor);

        RenderUtil.RECT.draw(matrixStack, healthBarX, healthBarY, healthBarWidth, healthBarHeight, healthBarRound, UIColors.backgroundBlur(fullAlpha));

        Color color1 = UIColors.gradient(0, fullAlpha);
        Color color2 = UIColors.gradient(90, fullAlpha);
        RenderUtil.GRADIENT_RECT.draw(matrixStack, healthBarX, healthBarY, healthBarWidth * healthAnimation, healthBarHeight, healthBarRound, color1, color2, color1, color2);

        if (target instanceof Player player) {
            Color headColor = ColorUtil.setAlpha(Color.WHITE, fullAlpha);

            RenderUtil.TEXTURE_RECT.drawHead(matrixStack, player, headX, headY, headSize, headSize, getGap() / 2f, 0f, headColor);
        } else {
            float headFontSize = headSize * 0.8f;
            getSemiBoldFont().drawCenteredText(matrixStack, "?", headX + headSize / 2f, headY + headSize / 2f - headFontSize / 2f, headFontSize, UIColors.textColor(fullAlpha));
        }

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);
    }

    private float[] healthBarProperties() {
        float height = scaled(5f);
        float round = height * 0.3f;

        return new float[]{height, round};
    }

    private float[] headProperties(float xPos, float yPos) {
        float x = xPos + getGap();
        float y = yPos + getGap();
        float size = scaled(25f);
        return new float[]{x, y, size};
    }

    private void update() {
        showAnimation.update();
        showAnimation.run(getTarget() != null ? 1.0 : 0.0, getDuration(), getEasing());
    }

    private LivingEntity getTarget() {
        // Preview target: while chat is open, show self so the HUD is visible.
        if (mc.gui.screen() instanceof ChatScreen) return mc.player;
        return null;
    }

    @Override
    public float fontMul() { return TargetHudModule.getInstance().scale.getValue(); }
}
