package com.fest.visuals.client.features.modules.render;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render2DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.combat.CombatTracker;
import com.fest.visuals.api.utils.math.ProjectionUtil;
import com.fest.visuals.api.utils.render.fonts.Fonts;

/**
 * Floating numbers over whatever took damage or healed.
 *
 * <p>Each number pops in on a springy overshoot, drifts up along a small arc so a burst of hits
 * fans out instead of stacking, and dissolves in the last third of its life. Crits come in
 * larger, redder and with a short shake so they read at a glance in a fight.
 */
@ModuleRegister(name = "Damage Numbers", desc = "Всплывающие цифры урона и лечения", category = Category.RENDER)
public class DamageNumbersModule extends Module {
    @Getter private static final DamageNumbersModule instance = new DamageNumbersModule();

    public final ModeSetting animation = new ModeSetting("Анимация").value("Пружина").values("Пружина", "Выстрел", "Плавно");
    public final ModeSetting source = new ModeSetting("Показывать").value("Все").values("Все", "Только свои удары");
    public final SliderSetting scale = new SliderSetting("Размер").value(1f).range(0.5f, 2.5f).step(0.05f);
    public final SliderSetting duration = new SliderSetting("Длительность").value(1100f).range(400f, 3000f).step(50f);
    public final BooleanSetting heals = new BooleanSetting("Лечение").value(true);
    public final BooleanSetting outline = new BooleanSetting("Обводка").value(true);
    public final ColorSetting normalColor = new ColorSetting("Цвет урона").value(new Color(255, 196, 64));
    public final ColorSetting critColor = new ColorSetting("Цвет крита").value(new Color(255, 58, 58));
    public final ColorSetting healColor = new ColorSetting("Цвет лечения").value(new Color(92, 235, 110));

    private final List<Popup> numbers = new ArrayList<>();
    private final Consumer<CombatTracker.Hit> hitListener = this::onHit;

    public DamageNumbersModule() {
        addSettings(animation, source, scale, duration, heals, outline, normalColor, critColor, healColor);
    }

    @Override
    public void onEvent() {
        CombatTracker.getInstance().onHit(hitListener);
        addEvents(Render2DEvent.getInstance().subscribe(new Listener<>(event -> render(event.matrixStack()))));
    }

    @Override
    public void onDisable() {
        CombatTracker.getInstance().remove(hitListener);
        numbers.clear();
    }

    private void onHit(CombatTracker.Hit hit) {
        if (hit.entity().isInvisible()) return;
        if (hit.heal() && !heals.getValue()) return;
        if (source.is("Только свои удары") && !hit.ours()) return;

        // Spread out sideways so rapid hits on one target fan out rather than overlap.
        double angle = Math.random() * Math.PI * 2.0;
        double spread = 0.25 + Math.random() * 0.2;
        Vec3 drift = new Vec3(Math.cos(angle) * spread, 0, Math.sin(angle) * spread);

        numbers.add(new Popup(hit.position(), drift, hit.amount(), hit.heal(), hit.crit(), System.currentTimeMillis()));
        if (numbers.size() > 64) numbers.remove(0);
    }

    private void render(PoseStack matrices) {
        if (numbers.isEmpty()) return;

        long now = System.currentTimeMillis();
        long life = (long) duration.getValue().floatValue();
        numbers.removeIf(number -> now - number.born > life);

        for (Popup number : numbers) {
            float t = (now - number.born) / (float) life;
            draw(matrices, number, t);
        }
    }

    private void draw(PoseStack matrices, Popup number, float t) {
        // Rise and arc outwards; "Выстрел" throws the number up hard and lets it fall back.
        double rise;
        double outward;
        switch (animation.getValue()) {
            case "Выстрел" -> {
                rise = 1.6 * t - 1.3 * t * t;
                outward = t * 1.2;
            }
            case "Плавно" -> {
                rise = 0.7 * easeOutCubic(t);
                outward = t * 0.4;
            }
            default -> {
                rise = 0.9 * easeOutCubic(t);
                outward = easeOutCubic(t) * 0.7;
            }
        }

        Vec3 world = number.origin.add(number.drift.scale(outward)).add(0, rise, 0);
        Vector2f screen = ProjectionUtil.projectExact(world);
        if (screen == null) return;

        float pop = switch (animation.getValue()) {
            case "Выстрел" -> t < 0.08f ? t / 0.08f : 1f;
            case "Плавно" -> Math.min(1f, t / 0.15f);
            default -> spring(Math.min(1f, t / 0.35f));
        };

        float fade = t < 0.65f ? 1f : 1f - (t - 0.65f) / 0.35f;
        float size = 9f * scale.getValue() * pop * (number.crit ? 1.35f : 1f);
        if (size <= 0.2f || fade <= 0.01f) return;

        Color base = number.heal ? healColor.getValue() : number.crit ? critColor.getValue() : normalColor.getValue();
        int alpha = Math.round(base.getAlpha() * fade);
        int color = (alpha << 24) | (base.getRed() << 16) | (base.getGreen() << 8) | base.getBlue();

        String text = (number.heal ? "+" : "") + format(number.amount) + (number.crit ? "!" : "");

        float x = screen.x;
        float y = screen.y;
        if (number.crit && t < 0.25f) {
            // A short, decaying shake that sells the crit without making it hard to read.
            float shake = (1f - t / 0.25f) * 1.6f;
            x += (float) Math.sin(t * 90.0) * shake;
            y += (float) Math.cos(t * 70.0) * shake;
        }

        float width = Fonts.PS_BOLD.getWidth(text, size);
        int outlineColor = (Math.round(170 * fade) << 24);

        Fonts.PS_BOLD.drawText(matrices, text, x - width / 2f, y - size / 2f, size, 0.05f,
                color, -1, -1f, 0.5f, 0f, outline.getValue() ? outlineColor : -1, outline.getValue() ? 0.25f : 0f);
    }

    private static String format(float amount) {
        if (amount >= 10f || Math.abs(amount - Math.round(amount)) < 0.05f) {
            return String.valueOf(Math.round(amount));
        }
        return String.format(Locale.ROOT, "%.1f", amount);
    }

    private static float easeOutCubic(float t) {
        float inv = 1f - Math.min(1f, Math.max(0f, t));
        return 1f - inv * inv * inv;
    }

    /** Overshoots past one and settles back: the "pop". */
    private static float spring(float t) {
        if (t >= 1f) return 1f;
        return (float) (1.0 - Math.exp(-6.0 * t) * Math.cos(t * 9.0));
    }

    private record Popup(Vec3 origin, Vec3 drift, float amount, boolean heal, boolean crit, long born) {}
}
