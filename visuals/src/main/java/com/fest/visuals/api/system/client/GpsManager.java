package com.fest.visuals.api.system.client;

import com.mojang.math.Axis;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2i;
import com.fest.visuals.api.system.interfaces.QuickImports;
import com.fest.visuals.api.utils.animation.AnimationUtil;
import com.fest.visuals.api.utils.animation.Easing;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;
import com.fest.visuals.api.system.files.FileUtil;
import com.fest.visuals.client.services.RenderService;

@Getter
@Setter
public class GpsManager implements QuickImports {
    @Getter private static final GpsManager instance = new GpsManager();

    private Vector2i gpsPosition = null;
    private Vector2i lastGpsPosition = null;

    private final AnimationUtil distanceAnimation = new AnimationUtil();
    private final AnimationUtil switchAnimation = new AnimationUtil();

    public void update(GuiGraphicsExtractor context) {
        if (gpsPosition != null) {
            lastGpsPosition = gpsPosition;
        }

        if (context == null) return;

        float scale = RenderService.getInstance().getScale();

        boolean noGps = gpsPosition == null;
        boolean noLastGps = lastGpsPosition == null;
        float distance = !noLastGps ? getDistance(lastGpsPosition) : getDistance(new Vector2i(0, 0));
        float maxDistance = 10f;
        float minDistance = 3f;

        distanceAnimation.update();
        switchAnimation.update();

        distanceAnimation.run(distance <= minDistance ? 0.0 : distance >= maxDistance ? 1.0 : (distance - minDistance) / (maxDistance - minDistance), 500, Easing.EXPO_OUT);
        switchAnimation.run(noGps ? 0.0 : 1.0, 500, Easing.EXPO_OUT);

        if (distanceAnimation.getValue() < 0.1 || switchAnimation.getValue() < 0.1) return;

        float switchAnim = (float) switchAnimation.getValue();

        double combinedAnim = distanceAnimation.getValue() * switchAnim;

        float x = mc.getWindow().getGuiScaledWidth() / 2f;
        float y = mc.getWindow().getGuiScaledHeight() / 6f;

        float targetX = noGps ? 0f : (float) lastGpsPosition.x;
        float targetY = noGps ? 0f : (float) lastGpsPosition.y;
        float rotation = getRotations(new Vec2(targetX, targetY)) - mc.player.getYRot();

        float arrowHeight = 12f * scale;

        RenderUtil.matrices().pushPose();
        RenderUtil.matrices().translate(x, y, 0.0f);
        RenderUtil.matrices().mulPose(Axis.ZP.rotationDegrees(rotation));
        RenderUtil.matrices().translate(-x, -y, 0.0f);
        drawPointerIcon(context, x, y - arrowHeight * 1.75f, 30f * scale, ColorUtil.setAlpha(UIColors.gradient((int) combinedAnim), (int) (255 * combinedAnim)));
        RenderUtil.OTHER.scaleStop(RenderUtil.matrices());

        float textY = (y + arrowHeight * (2f - switchAnim));
        Fonts.PS_BOLD.drawCenteredText(RenderUtil.matrices(), String.format("%.1f", distance) + "m", x, textY, 8f * scale, UIColors.textColor((int) (255 * combinedAnim)));
    }

    private void drawPointerIcon(GuiGraphicsExtractor context, float x, float y, float size, java.awt.Color color) {
        float scaledSize = size + 8;

        RenderUtil.TEXTURE_RECT.draw(RenderUtil.matrices(), x - scaledSize / 2f, y, scaledSize, scaledSize, 0f, color,
                0f, 0f, 1f, 1f, FileUtil.getImage("pointers/arrow_gps"));
    }

    private float getDistance(Vector2i targetVec) {
        double x = mc.player.position().x - targetVec.x;
        double z = mc.player.position().z - targetVec.y;
        return Mth.sqrt((float) (x * x + z * z));
    }

    private float getRotations(Vec2 vec) {
        if (mc.player == null) return 0.0f;
        double x = vec.x - mc.player.position().x;
        double z = vec.y - mc.player.position().z;
        return (float) -(Math.toDegrees(Math.atan2(x, z)));
    }
}

