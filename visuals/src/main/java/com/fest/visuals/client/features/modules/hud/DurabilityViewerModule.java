package com.fest.visuals.client.features.modules.hud;

import java.awt.Color;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.other.SoundUtil;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;

/**
 * Durability of what you are holding, right above its hotbar slot.
 *
 * <p>A small gauge that slides in when a damageable item is picked up, eases as it wears, goes
 * from green through yellow to red, and starts to pulse — optionally with a warning sound —
 * once it drops below the threshold, so a sword never breaks mid-fight by surprise.
 */
@ModuleRegister(name = "Durability Viewer", desc = "Прочность предмета в руке с предупреждением", category = Category.HUD)
public class DurabilityViewerModule extends Module {
    @Getter private static final DurabilityViewerModule instance = new DurabilityViewerModule();

    public final SliderSetting warnAt = new SliderSetting("Предупреждать при, %").value(15f).range(1f, 60f).step(1f);
    public final BooleanSetting percent = new BooleanSetting("Проценты").value(true);
    public final BooleanSetting offhand = new BooleanSetting("Вторая рука").value(true);
    public final BooleanSetting warnSound = new BooleanSetting("Звук при износе").value(false);

    private final Gauge main = new Gauge();
    private final Gauge off = new Gauge();

    public DurabilityViewerModule() {
        addSettings(warnAt, percent, offhand, warnSound);
    }

    @Override
    public void onEvent() {
        addEvents(Render2DEvent.getInstance().subscribe(new Listener<>(event -> render(event.matrixStack()))));
    }

    private void render(PoseStack matrices) {
        if (mc.player == null || mc.gui.screen() != null) return;

        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();
        int selected = mc.player.getInventory().getSelectedSlot();

        float slotX = width / 2f - 91f + selected * 20f + 2f;
        main.draw(matrices, mc.player.getItemInHand(InteractionHand.MAIN_HAND), slotX, height - 22f - 9f);

        if (offhand.getValue()) {
            off.draw(matrices, mc.player.getItemInHand(InteractionHand.OFF_HAND), width / 2f - 91f - 27f, height - 22f - 9f);
        }
    }

    private final class Gauge {
        private float shown = -1f;
        private float visible;
        private boolean warned;
        private long lastFrame;

        void draw(PoseStack matrices, ItemStack stack, float x, float y) {
            long now = System.currentTimeMillis();
            float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
            lastFrame = now;

            boolean has = !stack.isEmpty() && stack.isDamageableItem() && stack.getMaxDamage() > 0;
            float ratio = has ? 1f - stack.getDamageValue() / (float) stack.getMaxDamage() : 0f;

            visible += ((has ? 1f : 0f) - visible) * Math.min(1f, dt * 12f);
            if (visible < 0.01f) {
                shown = -1f;
                return;
            }
            if (!has) ratio = Math.max(0f, shown);
            if (shown < 0f) shown = ratio;
            shown += (ratio - shown) * Math.min(1f, dt * 8f);

            boolean low = ratio * 100f <= warnAt.getValue();
            if (low && has && !warned && warnSound.getValue()) {
                SoundUtil.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.7f, 0.6f);
            }
            warned = low && has;

            float pulse = low ? (float) (0.6 + 0.4 * Math.sin(now / 110.0)) : 1f;
            int alpha = Math.round(255 * visible);
            Color gauge = gaugeColor(shown);

            // Slides up out of the slot as it appears.
            float slide = (1f - visible) * 6f;
            float barWidth = 16f;
            float barY = y + slide;

            RenderUtil.RECT.draw(matrices, x - 1f, barY - 1f, barWidth + 2f, 4f, 2f,
                    ColorUtil.setAlpha(Color.BLACK, Math.round(150 * visible)));
            RenderUtil.RECT.draw(matrices, x, barY, Math.max(1f, barWidth * shown), 2f, 1f,
                    ColorUtil.setAlpha(gauge, Math.round(alpha * pulse)));

            if (percent.getValue()) {
                String text = Math.round(ratio * 100f) + "%";
                float size = 5.5f;
                float textWidth = Fonts.PS_BOLD.getWidth(text, size);
                Fonts.PS_BOLD.drawText(matrices, text, x + barWidth / 2f - textWidth / 2f, barY - size - 1.5f, size, 0.05f,
                        ColorUtil.setAlpha(gauge, Math.round(alpha * pulse)).getRGB(), -1, -1f, 0.5f, 0f,
                        (Math.round(150 * visible) << 24), 0.2f);
            }
        }
    }

    /** Green when fresh, yellow halfway, red when nearly gone. */
    private static Color gaugeColor(float ratio) {
        Color red = new Color(255, 70, 60);
        Color yellow = new Color(255, 205, 60);
        Color green = new Color(90, 230, 110);
        return ratio > 0.5f
                ? ColorUtil.interpolate(green, yellow, (ratio - 0.5f) * 2f)
                : ColorUtil.interpolate(yellow, red, ratio * 2f);
    }
}
