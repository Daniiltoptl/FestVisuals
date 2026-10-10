package com.fest.visuals.client.features.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.system.files.FileUtil;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.combat.CombatTracker;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.display.WorldShapes;

/**
 * A separate, louder effect for critical hits.
 *
 * <p>Fires the instant the swing qualifies as a crit — the same check the server runs — with a
 * star burst thrown off the target, a shockwave ring and an optional flash at the screen edges.
 */
@ModuleRegister(name = "Critical Effect", desc = "Яркий эффект на критический удар", category = Category.RENDER)
public class CriticalEffectModule extends Module {
    @Getter private static final CriticalEffectModule instance = new CriticalEffectModule();

    public final ModeSetting shape = new ModeSetting("Частицы").value("Звёзды").values("Звёзды", "Искры", "Сердца");
    public final SliderSetting count = new SliderSetting("Количество").value(14f).range(4f, 40f).step(1f);
    public final SliderSetting power = new SliderSetting("Сила разлёта").value(1f).range(0.3f, 2.5f).step(0.05f);
    public final SliderSetting size = new SliderSetting("Размер").value(0.18f).range(0.05f, 0.5f).step(0.01f);
    public final BooleanSetting shockwave = new BooleanSetting("Ударная волна").value(true);
    public final BooleanSetting screenFlash = new BooleanSetting("Вспышка экрана").value(false);
    public final BooleanSetting rainbow = new BooleanSetting("Переливание").value(false);
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 70, 70));

    private final Identifier ringTexture = FileUtil.getImage("circle/lean");

    private final List<Spark> sparks = new ArrayList<>();
    private final List<Wave> waves = new ArrayList<>();
    private final Consumer<CombatTracker.Swing> swingListener = this::onSwing;

    private long flashAt;
    private long lastFrame;

    public CriticalEffectModule() {
        addSettings(shape, count, power, size, shockwave, screenFlash, rainbow, color);
    }

    @Override
    public void onEvent() {
        CombatTracker.getInstance().onSwing(swingListener);
        addEvents(
                Render3DEvent.getInstance().subscribe(new Listener<>(event -> renderWorld(event.matrixStack()))),
                Render2DEvent.getInstance().subscribe(new Listener<>(event -> renderFlash(event.matrixStack())))
        );
    }

    @Override
    public void onDisable() {
        CombatTracker.getInstance().remove(swingListener);
        sparks.clear();
        waves.clear();
    }

    private Identifier sparkTexture() {
        return FileUtil.getImage(switch (shape.getValue()) {
            case "Искры" -> "particles/spark_2";
            case "Сердца" -> "particles/heart";
            default -> "particles/star";
        });
    }

    private void onSwing(CombatTracker.Swing swing) {
        if (!swing.crit()) return;

        Entity target = swing.target();
        Vec3 centre = target.position().add(0, target.getBbHeight() * 0.6, 0);
        long now = System.currentTimeMillis();

        int amount = count.getValue().intValue();
        for (int i = 0; i < amount; i++) {
            // Spread over a sphere, biased upwards so the burst reads as a pop, not a spill.
            double yaw = Math.random() * Math.PI * 2.0;
            double pitch = Math.random() * Math.PI * 0.8 - Math.PI * 0.25;
            double speed = (0.06 + Math.random() * 0.08) * power.getValue();
            Vec3 velocity = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch) + 0.35, Math.sin(yaw) * Math.cos(pitch))
                    .scale(speed);
            sparks.add(new Spark(centre, velocity, Math.random() * Math.PI * 2, (Math.random() - 0.5) * 0.4,
                    now, 550 + (long) (Math.random() * 350), i * 25));
        }

        if (shockwave.getValue()) waves.add(new Wave(centre, now));
        if (screenFlash.getValue()) flashAt = now;
    }

    private int tint(int index, float alpha) {
        Color base = rainbow.getValue()
                ? Color.getHSBColor(((System.currentTimeMillis() / 8 + index * 18) % 360) / 360f, 0.75f, 1f)
                : color.getValue();
        return WorldShapes.argb(base, alpha);
    }

    private void renderWorld(PoseStack matrices) {
        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;

        sparks.removeIf(spark -> now - spark.born > spark.life);
        waves.removeIf(wave -> now - wave.born > 420);
        if (sparks.isEmpty() && waves.isEmpty()) return;

        RenderUtil.WORLD.startRender(matrices);
        Matrix4f matrix = matrices.last().pose();

        if (!sparks.isEmpty()) {
            VertexConsumer buffer = RenderUtil.WORLD.textured(sparkTexture());
            float ticks = dt * 20f;

            for (Spark spark : sparks) {
                // Simple ballistic step with air drag, scaled to real time so it is frame-rate proof.
                spark.velocity = spark.velocity.add(0, -0.012 * ticks, 0).scale(Math.pow(0.9, ticks));
                spark.position = spark.position.add(spark.velocity.scale(ticks));
                spark.roll += spark.spin * ticks;

                float t = (now - spark.born) / (float) spark.life;
                float grow = Math.min(1f, t * 6f);
                float fade = t < 0.6f ? 1f : 1f - (t - 0.6f) / 0.4f;
                WorldShapes.sprite(buffer, matrix, spark.position, size.getValue() * grow * (1f - t * 0.5f),
                        spark.roll, tint(spark.hue, fade));
            }
        }

        if (!waves.isEmpty()) {
            VertexConsumer buffer = RenderUtil.WORLD.textured(ringTexture);
            for (Wave wave : waves) {
                float t = (now - wave.born) / 420f;
                float grow = 1f - (1f - t) * (1f - t) * (1f - t);
                WorldShapes.sprite(buffer, matrix, wave.centre, 0.2 + 1.3 * grow, 0, tint(0, (1f - t) * 0.9f));
            }
        }

        RenderUtil.WORLD.endRender(matrices);
    }

    private void renderFlash(PoseStack matrices) {
        if (flashAt == 0) return;
        float t = (System.currentTimeMillis() - flashAt) / 260f;
        if (t >= 1f) {
            flashAt = 0;
            return;
        }

        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();
        float edge = Math.min(width, height) * 0.18f;
        int alpha = (int) (110 * (1f - t) * (1f - t));
        Color base = ColorUtil.setAlpha(color.getValue(), alpha);
        Color clear = ColorUtil.setAlpha(color.getValue(), 0);

        // Four gradient bands from the edges inwards: a vignette in the crit colour.
        RenderUtil.GRADIENT_RECT.draw(matrices, 0, 0, width, edge, 0f, base, base, clear, clear);
        RenderUtil.GRADIENT_RECT.draw(matrices, 0, height - edge, width, edge, 0f, clear, clear, base, base);
        RenderUtil.GRADIENT_RECT.draw(matrices, 0, 0, edge, height, 0f, base, clear, base, clear);
        RenderUtil.GRADIENT_RECT.draw(matrices, width - edge, 0, edge, height, 0f, clear, base, clear, base);
    }

    private static final class Spark {
        private Vec3 position;
        private Vec3 velocity;
        private double roll;
        private final double spin;
        private final long born;
        private final long life;
        private final int hue;

        private Spark(Vec3 position, Vec3 velocity, double roll, double spin, long born, long life, int hue) {
            this.position = position;
            this.velocity = velocity;
            this.roll = roll;
            this.spin = spin;
            this.born = born;
            this.life = life;
            this.hue = hue;
        }
    }

    private record Wave(Vec3 centre, long born) {}
}
