package com.fest.visuals.client.features.modules.render;

import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.color.UIColors;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import java.awt.Color;

/**
 * Replaces the block outline. The outline glides from the previous block to the new one instead
 * of teleporting, fades in when the crosshair first lands on a block, and can carry a faint,
 * breathing fill.
 */
@ModuleRegister(name = "Block Highlight", desc = "Обводка блоков", category = Category.RENDER)
public class BlockHighlightModule extends Module {
    @Getter private static final BlockHighlightModule instance = new BlockHighlightModule();

    public final ModeSetting mode = new ModeSetting("Режим").value("Клиент").values("Клиент", "Кастомный");
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 120, 0, 200)).setVisible(() -> mode.getValue().equals("Кастомный"));
    public final SliderSetting lineWidth = new SliderSetting("Ширина").value(3f).range(1f, 8f).step(1f);
    public final BooleanSetting glide = new BooleanSetting("Плавное перемещение").value(true);
    public final SliderSetting glideSpeed = new SliderSetting("Скорость").value(18f).range(4f, 40f).step(1f).setVisible(glide::getValue);
    public final BooleanSetting fill = new BooleanSetting("Заливка").value(true);
    public final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки").value(0.12f).range(0.02f, 0.5f).step(0.01f)
            .setVisible(fill::getValue);

    private BlockPos lastPos;
    private double offsetX, offsetY, offsetZ;
    private float appear;
    private long lastDraw;

    public BlockHighlightModule() {
        addSettings(mode, color, lineWidth, glide, glideSpeed, fill, fillAlpha);
    }

    @Override
    public void onEvent() {}

    public Color outlineColor() {
        return mode.is("Кастомный") ? color.getValue() : UIColors.gradient(0, 210);
    }

    /**
     * Advances the animation for this frame and returns {offsetX, offsetY, offsetZ, alpha}: where to
     * draw the outline relative to the block, and how visible it is.
     */
    public double[] animate(BlockPos pos) {
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1f, Math.max(0.001f, (now - lastDraw) / 1000f));
        boolean fresh = now - lastDraw > 150 || lastPos == null;
        lastDraw = now;

        if (fresh) {
            appear = 0f;
            offsetX = offsetY = offsetZ = 0;
            lastPos = pos;
        } else if (!pos.equals(lastPos)) {
            // Start from wherever the outline currently is, so fast sweeps chain smoothly.
            double dx = lastPos.getX() - pos.getX();
            double dy = lastPos.getY() - pos.getY();
            double dz = lastPos.getZ() - pos.getZ();
            if (glide.getValue() && dx * dx + dy * dy + dz * dz < 36) {
                offsetX += dx;
                offsetY += dy;
                offsetZ += dz;
            } else {
                offsetX = offsetY = offsetZ = 0;
            }
            lastPos = pos;
        }

        double k = 1.0 - Math.exp(-glideSpeed.getValue() * dt);
        offsetX -= offsetX * k;
        offsetY -= offsetY * k;
        offsetZ -= offsetZ * k;
        appear += (1f - appear) * Math.min(1f, dt * 14f);

        return new double[]{offsetX, offsetY, offsetZ, appear};
    }
}
