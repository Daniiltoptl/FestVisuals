package com.fest.visuals.client.features.modules.hud;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;

/**
 * Shows saturation on the vanilla hunger bar.
 *
 * <p>The default look follows AppleSkin: each food icon gets a golden outline for the saturation
 * it holds, two points per icon, filling from the right the way the icons themselves empty. The
 * outline is traced from the icon's own silhouette and drawn straight after vanilla draws the
 * row, so it sits exactly on the icons. The older thin bar under the row is kept as a mode.
 */
@ModuleRegister(name = "Saturation", desc = "Показывает насыщение на шкале голода", category = Category.HUD)
public class SaturationModule extends Module {
    @Getter private static final SaturationModule instance = new SaturationModule();

    /** Vanilla food row: ten 9px icons ending at screenWidth / 2 + 91. */
    private static final int BAR_WIDTH = 81;

    /** Border of the 9x9 drumstick, the difference between the empty and full food sprites. */
    private static final String[] OUTLINE = {
            "..XX.....",
            ".X..X....",
            "X....X...",
            "X.....X..",
            ".X....X..",
            "..X...X..",
            "...XXX.XX",
            "......X.X",
            "......XX."
    };

    public final ModeSetting mode = new ModeSetting("Вид").value("Обводка (AppleSkin)").values("Обводка (AppleSkin)", "Полоска");
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 200, 40, 255));
    public final SliderSetting thickness = new SliderSetting("Толщина").value(2f).range(1f, 5f).step(0.5f)
            .setVisible(() -> mode.is("Полоска"));
    public final SliderSetting offset = new SliderSetting("Отступ").value(0f).range(-6f, 6f).step(1f)
            .setVisible(() -> mode.is("Полоска"));
    public final BooleanSetting background = new BooleanSetting("Подложка").value(true)
            .setVisible(() -> mode.is("Полоска"));
    public final BooleanSetting number = new BooleanSetting("Число").value(false);

    public SaturationModule() {
        addSettings(mode, color, thickness, offset, background, number);
    }

    @Override
    public void onEvent() {
        addEvents(Render2DEvent.getInstance().subscribe(new Listener<>(event -> renderBar(event.matrixStack()))));
    }

    private float saturation(Player player) {
        return Math.min(20f, Math.max(0f, player.getFoodData().getSaturationLevel()));
    }

    /** Called by the HUD mixin right after vanilla has drawn the food row. */
    public void drawOutline(GuiGraphics context, Player player, int top, int right) {
        if (!isEnabled() || !mode.is("Обводка (AppleSkin)") || player == null) return;

        float saturation = saturation(player);
        int argb = color.getValue().getRGB();

        for (int icon = 0; icon < 10; icon++) {
            float points = Math.min(2f, saturation - icon * 2f);
            if (points <= 0f) break;

            int x = right - icon * 8 - 9;
            // A partly saturated icon shows only the right-hand part of its outline.
            int fromColumn = Math.round(9f * (1f - points / 2f));
            for (int row = 0; row < 9; row++) {
                String line = OUTLINE[row];
                for (int column = fromColumn; column < 9; column++) {
                    if (line.charAt(column) != 'X') continue;
                    context.fill(x + column, top + row, x + column + 1, top + row + 1, argb);
                }
            }
        }

        if (number.getValue()) {
            context.drawString(mc.font, String.valueOf((int) Math.ceil(saturation)), right + 3, top, argb, true);
        }
    }

    private void renderBar(PoseStack matrices) {
        if (!mode.is("Полоска") || mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        if (mc.player.isSpectator() || mc.player.getAbilities().instabuild) return;
        if (mc.player.getVehicle() != null) return;

        float saturation = saturation(mc.player);

        float height = thickness.getValue();
        float right = mc.getWindow().getGuiScaledWidth() / 2f + 91f;
        float left = right - BAR_WIDTH;
        float y = mc.getWindow().getGuiScaledHeight() - 30f + offset.getValue();

        if (background.getValue()) {
            RenderUtil.RECT.draw(matrices, left, y, BAR_WIDTH, height, height / 2f,
                    ColorUtil.setAlpha(Color.BLACK, 130));
        }

        float filled = BAR_WIDTH * (saturation / 20f);
        if (filled > 0.5f) {
            RenderUtil.RECT.draw(matrices, right - filled, y, filled, height, height / 2f, color.getValue());
        }

        if (number.getValue()) {
            String text = String.valueOf((int) Math.ceil(saturation));
            Fonts.PS_MEDIUM.drawText(matrices, text, right + 3f, y - 1.5f, 6.5f, color.getValue());
        }
    }
}
