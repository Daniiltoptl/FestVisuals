package com.fest.visuals.client.features.modules.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

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

import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;

@ModuleRegister(name = "Hitbox Customizer", desc = "Настраивает ванильные F3+B хитбоксы", category = Category.RENDER)
public class HitboxModule extends Module {
    @Getter private static final HitboxModule instance = new HitboxModule();

    public final BooleanSetting players = new BooleanSetting("Игроки").value(true);
    public final BooleanSetting mobs = new BooleanSetting("Мобы").value(true);
    public final SliderSetting range = new SliderSetting("Дистанция").value(24f).range(4f, 64f).step(1f);
    public final SliderSetting thickness = new SliderSetting("Толщина").value(1.5f).range(0.5f, 5f).step(0.25f);
    public final BooleanSetting fill = new BooleanSetting("Заливка").value(true);
    public final SliderSetting fillAlpha = new SliderSetting("Прозрачность").value(0.12f).range(0.02f, 0.5f).step(0.01f)
            .setVisible(fill::getValue);
    public final BooleanSetting eyeLine = new BooleanSetting("Линия взгляда").value(false);
    public final BooleanSetting themeColor = new BooleanSetting("Цвет темы").value(true);
    public final ColorSetting color = new ColorSetting("Цвет").value(new Color(255, 255, 255, 200))
            .setVisible(() -> !themeColor.getValue());
    public final ColorSetting targetColor = new ColorSetting("Цвет таргета").value(new Color(255, 80, 80, 230));
    public final ColorSetting friendColor = new ColorSetting("Цвет друга").value(new Color(90, 230, 120, 220));

    private final Map<Integer, Float> focus = new HashMap<>();
    private long lastFrame;

    public HitboxModule() {
        addSettings(players, mobs, range, thickness, fill, fillAlpha, eyeLine, themeColor, color, targetColor, friendColor);
    }

    @Override
    public void onEvent() { }

    @Override
    public void onDisable() {
        focus.clear();
    }

    private boolean wanted(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player || !living.isAlive()) return false;
        if (entity.distanceTo(mc.player) > range.getValue()) return false;
        return entity instanceof Player ? players.getValue() : mobs.getValue();
    }

    public void renderHitbox(Entity entity, float partial) {
        if (mc.player == null || mc.level == null) return;
        if (!wanted(entity)) return;

        long now = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1000f);
        if (entity == mc.level.entitiesForRendering().iterator().next()) { 
            lastFrame = now;
        }

        Entity aimed = mc.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
        boolean isTarget = entity == aimed || CombatTracker.getInstance().attackedRecently(entity);
        float blend = focus.getOrDefault(entity.getId(), 0f);
        blend += ((isTarget ? 1f : 0f) - blend) * Math.min(1f, dt * 10f);
        focus.put(entity.getId(), blend);

        LivingEntity living = (LivingEntity) entity;
        
        // Entity interpolation offset
        double d0 = Mth.lerp(partial, entity.xOld, entity.getX()) - entity.getX();
        double d1 = Mth.lerp(partial, entity.yOld, entity.getY()) - entity.getY();
        double d2 = Mth.lerp(partial, entity.zOld, entity.getZ()) - entity.getZ();
        AABB box = entity.getBoundingBox().move(d0, d1, d2);

        Color idle = entity instanceof Player player && FriendManager.getInstance().contains(player.getGameProfile().name())
                ? friendColor.getValue()
                : themeColor.getValue() ? UIColors.primary(200) : color.getValue();
        Color tint = ColorUtil.interpolate(targetColor.getValue(), idle, blend);

        float hurt = living.hurtTime / 10f;
        if (hurt > 0f) tint = ColorUtil.interpolate(new Color(255, 255, 255, tint.getAlpha()), tint, hurt * 0.7f);

        double grow = hurt * 0.04;
        AABB drawn = box.inflate(grow);
        float width = thickness.getValue() * (1.0f + blend * 0.5f);
        
        int strokeColor = argb(tint, 255);
        
        if (fill.getValue()) {
            float alpha = fillAlpha.getValue() * (1f + blend * 0.8f + hurt);
            int fillColor = argb(tint, (int) Math.min(255f, alpha * 255f));
            Gizmos.cuboid(drawn, GizmoStyle.strokeAndFill(strokeColor, width, fillColor));
        } else {
            Gizmos.cuboid(drawn, GizmoStyle.stroke(strokeColor, width));
        }

        if (eyeLine.getValue()) {
            Vec3 eye = new Vec3(0, entity.getEyeHeight(), 0).add(d0, d1, d2).add(entity.getX(), entity.getY(), entity.getZ());
            Vec3 look = entity.getViewVector(partial).scale(1.6);
            Gizmos.line(eye, eye.add(look), strokeColor, width * 1.2f);
        }
    }
    
    private int argb(Color c, int alpha) {
        return (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }
}
