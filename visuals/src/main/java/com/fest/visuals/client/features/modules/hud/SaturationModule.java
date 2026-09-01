package com.fest.visuals.client.features.modules.hud;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;

/**
 * Draws saturation on the vanilla hunger bar.
 *
 * <p>Saturation is what actually decides when hunger starts dropping, and vanilla never shows it.
 * The bar sits in the gap between the food row and the hotbar, spanning exactly the ten food
 * icons, so it reads as part of the same gauge.
 */
@ModuleRegister(name = "Saturation", desc = "Показывает насыщение на шкале голода", category = Category.HUD)
public class SaturationModule extends Module {
    @Getter private static final SaturationModule instance = new SaturationModule();

    /** Vanilla food row: ten 9px icons ending at screenWidth / 2 + 91. */
    private static final int BAR_WIDTH = 81;

    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(240, 170, 60, 235));
    public final SliderSetting thickness = new SliderSetting("Толщина").value(2f).range(1f, 5f).step(0.5f);
    public final SliderSetting offset = new SliderSetting("Отступ").value(0f).range(-6f, 6f).step(1f);
    public final BooleanSetting background = new BooleanSetting("Подложка").value(true);
    public final BooleanSetting number = new BooleanSetting("Число").value(false);

    public SaturationModule() {
        addSettings(color, thickness, offset, background, number);
    }

    @Override
    public void onEvent() {
        addEvents(Render2DEvent.getInstance().subscribe(new Listener<>(event -> render(event.matrixStack()))));
    }

    private void render(PoseStack matrices) {
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() != null) return;

        // The same cases where vanilla hides the food row: no gauges in creative, and a mount
        // replaces the row with its health.
        if (mc.player.isSpectator() || mc.player.getAbilities().instabuild) return;
        if (mc.player.getVehicle() != null) return;

        float saturation = Math.min(20f, Math.max(0f, mc.player.getFoodData().getSaturationLevel()));

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
            // Grown from the right, the way the food icons empty.
            RenderUtil.RECT.draw(matrices, right - filled, y, filled, height, height / 2f, color.getValue());
        }

        if (number.getValue()) {
            String text = String.valueOf((int) Math.ceil(saturation));
            Fonts.PS_MEDIUM.drawText(matrices, text, right + 3f, y - 1.5f, 6.5f, color.getValue());
        }
    }
}
