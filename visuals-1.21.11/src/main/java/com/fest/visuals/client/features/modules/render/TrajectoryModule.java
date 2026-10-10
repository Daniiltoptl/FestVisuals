package com.fest.visuals.client.features.modules.render;

import java.awt.Color;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
import com.fest.visuals.api.module.setting.SliderSetting;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.combat.Trajectory;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.display.WorldShapes;

/**
 * Shows where an arrow, trident, pearl, snowball or potion will go before it is thrown.
 *
 * <p>The arc is drawn as a ribbon with a pattern that flows towards the landing point, and the
 * landing point gets a pulsing marker lying flat on the face it will hit. When the path ends in
 * an entity, the marker turns into that entity's box in the hit colour.
 */
@ModuleRegister(name = "Trajectory", desc = "Предсказание полёта стрел, пёрлов и зелий", category = Category.RENDER)
public class TrajectoryModule extends Module {
    @Getter private static final TrajectoryModule instance = new TrajectoryModule();

    public final SliderSetting width = new SliderSetting("Толщина").value(2f).range(0.5f, 6f).step(0.25f);
    public final BooleanSetting flow = new BooleanSetting("Бегущая линия").value(true);
    public final BooleanSetting marker = new BooleanSetting("Метка падения").value(true);
    public final BooleanSetting themeColor = new BooleanSetting("Цвет темы").value(true);
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(120, 200, 255, 230))
            .setVisible(() -> !themeColor.getValue());
    public final ColorSetting hitColor = new ColorSetting("Цвет попадания").value(new Color(255, 70, 70, 230));

    private float hitBlend;
    private long lastFrame;

    public TrajectoryModule() {
        addSettings(width, flow, marker, themeColor, color, hitColor);
    }

    @Override
    public void onEvent() {
        addEvents(Render3DEvent.getInstance().subscribe(new Listener<>(event -> render(event.matrixStack(), event.partialTicks()))));
    }

    private void render(PoseStack matrices, float partial) {
        if (mc.player == null || mc.level == null) return;

        ItemStack stack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        Trajectory.Launch launch = Trajectory.launchFor(mc.player, stack);
        if (launch == null) {
            stack = mc.player.getItemInHand(InteractionHand.OFF_HAND);
            launch = Trajectory.launchFor(mc.player, stack);
        }
        if (launch == null) return;

        Trajectory.Result result = Trajectory.simulate(mc.player, launch, partial, 300);
        List<Vec3> points = result.points();
        if (points.size() < 2) return;

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        hitBlend += ((result.hitEntity() != null ? 1f : 0f) - hitBlend) * Math.min(1f, dt * 12f);

        Color base = themeColor.getValue() ? UIColors.primary(230) : color.getValue();
        Color tint = ColorUtil.interpolate(hitColor.getValue(), base, hitBlend);

        RenderUtil.WORLD.startRender(matrices);
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer buffer = RenderUtil.WORLD.occludedQuads();

        double ribbon = width.getValue() * 0.012;
        double phase = (now % 1000L) / 1000.0;
        int count = points.size();

        float[] alphas = new float[count];
        for (int i = 0; i < count; i++) {
            float along = i / (float) count;
            float alpha = 0.35f + 0.65f * (float) Math.sin(Math.PI * Math.min(1.0, along * 1.4 + 0.1));
            if (flow.getValue()) {
                double wave = 0.5 + 0.5 * Math.sin((along * 18.0 - phase * 2.0) * Math.PI * 2.0);
                alpha *= (float) (0.55 + 0.45 * wave);
            }
            alphas[i] = alpha;
        }

        for (int i = 1; i < count; i++) {
            // Skip the first stretch in front of the camera, where the ribbon is all edge-on.
            if (points.get(i).distanceToSqr(points.get(0)) < 0.6) continue;

            WorldShapes.line(buffer, matrix, points.get(i - 1), points.get(i), ribbon,
                    WorldShapes.argb(tint, alphas[i - 1]), WorldShapes.argb(tint, alphas[i]));
        }

        if (marker.getValue() && result.landed()) {
            if (result.hitEntity() != null) {
                AABB box = result.hitEntity().getBoundingBox();
                WorldShapes.boxOutline(buffer, matrix, box.inflate(0.05), ribbon, tint.getRGB());
                WorldShapes.boxFill(buffer, matrix, box.inflate(0.05), WorldShapes.argb(tint, 0.2f));
            } else {
                drawLanding(buffer, matrix, result.end(), result.face(), tint, now);
            }
        }

        RenderUtil.WORLD.endRender(matrices);
    }

    private void drawLanding(VertexConsumer buffer, Matrix4f matrix, Vec3 at, Direction face, Color tint, long now) {
        double pulse = 0.5 + 0.5 * Math.sin(now / 160.0);
        double radius = 0.28 + pulse * 0.08;

        if (face == null || face.getAxis() == Direction.Axis.Y) {
            Vec3 centre = at.add(0, face == Direction.DOWN ? -0.01 : 0.01, 0);
            WorldShapes.disc(buffer, matrix, centre, radius, 32, WorldShapes.argb(tint, 0.35f), WorldShapes.argb(tint, 0.05f));
            WorldShapes.ring(buffer, matrix, centre, radius - 0.03, radius, now / 400.0, Math.PI * 1.5, 32,
                    tint.getRGB(), tint.getRGB());
        } else {
            // On a wall: a small square flush with the face.
            Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            Vec3 side = normal.cross(new Vec3(0, 1, 0)).normalize().scale(radius);
            Vec3 up = new Vec3(0, radius, 0);
            Vec3 centre = at.add(normal.scale(0.01));
            WorldShapes.quad(buffer, matrix, centre.subtract(side).subtract(up), centre.add(side).subtract(up),
                    centre.add(side).add(up), centre.subtract(side).add(up), WorldShapes.argb(tint, 0.3f));
        }
    }
}
