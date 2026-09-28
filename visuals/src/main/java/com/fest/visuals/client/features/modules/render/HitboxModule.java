package com.fest.visuals.client.features.modules.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
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
import com.fest.visuals.api.system.configs.FriendManager;
import com.fest.visuals.api.utils.color.ColorUtil;
import com.fest.visuals.api.utils.color.UIColors;
import com.fest.visuals.api.utils.combat.CombatTracker;
import com.fest.visuals.api.utils.render.RenderUtil;
import com.fest.visuals.api.utils.render.display.WorldShapes;

/**
 * Draws hitboxes the way you want them: colour, fill, thickness, and a highlight on whoever you
 * are fighting.
 *
 * <p>Depth-tested on purpose — a box is hidden behind a wall exactly like the entity it belongs
 * to, so this shows where to aim, not where people are hiding. Each box eases between its idle,
 * aimed-at and hit colours instead of snapping, and flashes on every registered hit.
 */
@ModuleRegister(name = "Hitbox Customizer", desc = "Настраиваемые хитбоксы с подсветкой цели", category = Category.RENDER)
public class HitboxModule extends Module {
    @Getter private static final HitboxModule instance = new HitboxModule();

    public final BooleanSetting players = new BooleanSetting("Игроки").value(true);
    public final BooleanSetting mobs = new BooleanSetting("Мобы").value(true);
    public final SliderSetting range = new SliderSetting("Дистанция").value(24f).range(4f, 64f).step(1f);
    public final SliderSetting thickness = new SliderSetting("Толщина").value(1.5f).range(0.5f, 5f).step(0.25f);
    public final BooleanSetting fill = new BooleanSetting("Заливка").value(true);
    public final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки").value(0.12f).range(0.02f, 0.5f).step(0.01f)
            .setVisible(fill::getValue);
    public final BooleanSetting eyeLine = new BooleanSetting("Линия взгляда").value(false);
    public final BooleanSetting themeColor = new BooleanSetting("Цвет темы").value(true);
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 255, 255, 200))
            .setVisible(() -> !themeColor.getValue());
    public final ColorSetting targetColor = new ColorSetting("Цвет цели").value(new Color(255, 80, 80, 230));
    public final ColorSetting friendColor = new ColorSetting("Цвет друзей").value(new Color(90, 230, 120, 220));

    /** Per-entity blend towards the target colour, eased every frame. */
    private final Map<Integer, Float> focus = new HashMap<>();
    private long lastFrame;

    public HitboxModule() {
        addSettings(players, mobs, range, thickness, fill, fillAlpha, eyeLine, themeColor, color, targetColor, friendColor);
    }

    @Override
    public void onEvent() {
        addEvents(Render3DEvent.getInstance().subscribe(new Listener<>(event -> render(event.matrixStack(), event.partialTicks()))));
    }

    @Override
    public void onDisable() {
        focus.clear();
    }

    private boolean wanted(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive()) return false;
        if (entity.distanceTo(mc.player) > range.getValue()) return false;
        return entity instanceof Player ? players.getValue() : mobs.getValue();
    }

    private void render(PoseStack matrices, float partial) {
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;

        Entity aimed = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;

        RenderUtil.WORLD.startRender(matrices);
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer buffer = RenderUtil.WORLD.occludedQuads();

        Map<Integer, Float> alive = new HashMap<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!wanted(entity)) continue;

            boolean isTarget = entity == aimed || CombatTracker.getInstance().attackedRecently(entity);
            float blend = focus.getOrDefault(entity.getId(), 0f);
            blend += ((isTarget ? 1f : 0f) - blend) * Math.min(1f, dt * 10f);
            alive.put(entity.getId(), blend);

            LivingEntity living = (LivingEntity) entity;
            Vec3 lerped = entity.getPosition(partial);
            AABB box = entity.getBoundingBox().move(lerped.subtract(entity.position()));

            Color idle = entity instanceof Player player && FriendManager.getInstance().contains(player.getGameProfile().name())
                    ? friendColor.getValue()
                    : themeColor.getValue() ? UIColors.primary(200) : color.getValue();
            Color tint = ColorUtil.interpolate(targetColor.getValue(), idle, blend);

            // hurtTime counts down from 10: flash white-hot, then settle back.
            float hurt = living.hurtTime / 10f;
            if (hurt > 0f) tint = ColorUtil.interpolate(new Color(255, 255, 255, tint.getAlpha()), tint, hurt * 0.7f);

            double grow = hurt * 0.04;
            AABB drawn = box.inflate(grow);

            double width = thickness.getValue() * 0.01 * (1.0 + blend * 0.5);
            WorldShapes.boxOutline(buffer, matrix, drawn, width, tint.getRGB());

            if (fill.getValue()) {
                float alpha = fillAlpha.getValue() * (1f + blend * 0.8f + hurt);
                WorldShapes.boxFill(buffer, matrix, drawn, WorldShapes.argb(tint, Math.min(1f, alpha * 255f / Math.max(1, tint.getAlpha()))));
            }

            if (eyeLine.getValue()) {
                Vec3 eye = entity.getEyePosition(partial);
                Vec3 look = entity.getViewVector(partial).scale(1.6);
                WorldShapes.line(buffer, matrix, eye, eye.add(look), width * 1.2, tint.getRGB(), WorldShapes.argb(tint, 0f));
            }
        }

        focus.clear();
        focus.putAll(alive);
        RenderUtil.WORLD.endRender(matrices);
    }
}
