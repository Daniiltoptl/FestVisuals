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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.render.Render3DEvent;
import com.fest.visuals.api.module.Category;
import com.fest.visuals.api.module.Module;
import com.fest.visuals.api.module.ModuleRegister;
import com.fest.visuals.api.module.setting.BooleanSetting;
import com.fest.visuals.api.module.setting.ColorSetting;
import com.fest.visuals.api.module.setting.ModeSetting;
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.system.files.FileUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.combat.CombatTracker;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.display.WorldShapes;

/**
 * A bubble that bursts on the spot the hit landed, so a registered hit is obvious at a glance.
 *
 * <p>By default it waits for the server to actually take the health off — that is the whole
 * point of the indicator: a swing that missed or was blocked shows nothing. The bubble sticks to
 * the target's surface on the side facing the player and follows it while it plays.
 */
@ModuleRegister(name = "Hit Bubble", desc = "Пузырь в точке удара, когда урон прошёл", category = Category.RENDER)
public class HitBubbleModule extends Module {
    @Getter private static final HitBubbleModule instance = new HitBubbleModule();

    public final ModeSetting trigger = new ModeSetting("Когда").value("Урон прошёл").values("Урон прошёл", "При ударе");
    public final SliderSetting size = new SliderSetting("Размер").value(0.55f).range(0.2f, 1.5f).step(0.05f);
    public final SliderSetting duration = new SliderSetting("Длительность").value(450f).range(150f, 1200f).step(25f);
    public final BooleanSetting sparks = new BooleanSetting("Искры").value(true);
    public final BooleanSetting themeColor = new BooleanSetting("Цвет темы").value(true);
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(120, 200, 255))
            .setVisible(() -> !themeColor.getValue());
    public final ColorSetting critColor = new ColorSetting("Цвет крита").value(new Color(255, 70, 70));

    private final Identifier ring = FileUtil.getImage("circle/lean");
    private final Identifier glow = FileUtil.getImage("particles/glow");

    private final List<Bubble> bubbles = new ArrayList<>();
    private final Consumer<CombatTracker.Hit> hitListener = this::onHit;
    private final Consumer<CombatTracker.Swing> swingListener = this::onSwing;

    public HitBubbleModule() {
        addSettings(trigger, size, duration, sparks, themeColor, color, critColor);
    }

    @Override
    public void onEvent() {
        CombatTracker.getInstance().onHit(hitListener);
        CombatTracker.getInstance().onSwing(swingListener);
        addEvents(Render3DEvent.getInstance().subscribe(new Listener<>(event -> render(event.matrixStack()))));
    }

    @Override
    public void onDisable() {
        CombatTracker.getInstance().remove(hitListener);
        CombatTracker.getInstance().remove(swingListener);
        bubbles.clear();
    }

    private void onHit(CombatTracker.Hit hit) {
        if (hit.entity().isInvisible()) return;
        if (!trigger.is("Урон прошёл") || hit.heal() || !hit.ours()) return;
        spawn(hit.entity(), hit.crit());
    }

    private void onSwing(CombatTracker.Swing swing) {
        if (!trigger.is("При ударе")) return;
        spawn(swing.target(), swing.crit());
    }

    private void spawn(Entity target, boolean crit) {
        if (mc.player == null) return;

        // Where the player's eye line meets the target's box, nudged out so it sits on the surface.
        AABB box = target.getBoundingBox();
        Vec3 eye = mc.player.getEyePosition();
        Vec3 centre = box.getCenter();
        Vec3 onSurface = new Vec3(
                clamp(eye.x, box.minX, box.maxX),
                clamp(eye.y, box.minY + box.getYsize() * 0.2, box.maxY - box.getYsize() * 0.1),
                clamp(eye.z, box.minZ, box.maxZ));
        Vec3 outward = eye.subtract(centre).multiply(1, 0, 1);
        if (outward.lengthSqr() > 1.0E-6) onSurface = onSurface.add(outward.normalize().scale(0.08));

        bubbles.add(new Bubble(target, onSurface.subtract(target.position()), crit, System.currentTimeMillis()));
        if (bubbles.size() > 24) bubbles.remove(0);
    }

    private void render(PoseStack matrices) {
        if (bubbles.isEmpty()) return;

        long now = System.currentTimeMillis();
        long life = (long) duration.getValue().floatValue();
        bubbles.removeIf(bubble -> now - bubble.born > life || bubble.target.isRemoved());
        if (bubbles.isEmpty()) return;

        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        RenderUtil.WORLD.startRender(matrices);
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer ringBuffer = RenderUtil.WORLD.textured(ring);
        VertexConsumer glowBuffer = RenderUtil.WORLD.textured(glow);

        for (Bubble bubble : bubbles) {
            float t = (now - bubble.born) / (float) life;
            Vec3 at = bubble.target.getPosition(partial).add(bubble.offset);

            Color base = bubble.crit ? critColor.getValue() : themeColor.getValue() ? UIColors.primary() : color.getValue();
            float fade = (1f - t) * (1f - t);
            float grow = 1f - (float) Math.pow(1f - t, 4);
            double radius = size.getValue() * (bubble.crit ? 1.3 : 1.0);

            // The shell: a thin ring racing outwards.
            WorldShapes.sprite(ringBuffer, matrix, at, radius * (0.25 + 0.9 * grow), t * 1.5, WorldShapes.argb(base, fade));
            // The core: a soft glow that pops and shrinks away.
            float core = t < 0.2f ? t / 0.2f : 1f - (t - 0.2f) / 0.8f;
            WorldShapes.sprite(glowBuffer, matrix, at, radius * 0.6 * core, 0, WorldShapes.argb(base, core * 0.9f));

            if (sparks.getValue()) {
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3.0 + bubble.born % 628 / 100.0;
                    double reach = radius * (0.3 + 1.1 * grow);
                    Vec3[] axes = WorldShapes.cameraAxes();
                    Vec3 spark = at.add(axes[0].scale(Math.cos(angle) * reach)).add(axes[1].scale(Math.sin(angle) * reach));
                    WorldShapes.sprite(glowBuffer, matrix, spark, radius * 0.12 * (1f - t), 0, WorldShapes.argb(base, fade));
                }
            }
        }

        RenderUtil.WORLD.endRender(matrices);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Bubble(Entity target, Vec3 offset, boolean crit, long born) {}
}
