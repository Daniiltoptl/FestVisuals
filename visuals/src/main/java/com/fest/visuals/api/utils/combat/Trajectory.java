package com.fest.visuals.api.utils.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.fest.visuals.api.system.interfaces.QuickImports;

/**
 * Forward simulation of whatever the player is about to throw or shoot.
 *
 * <p>Mirrors the vanilla launch ({@code shootFromRotation}: same angle offsets, same speeds, the
 * shooter's own motion added) and the vanilla flight step (move, drag, gravity) with the gravity
 * values of 26.2 — 0.05 for arrows and potions, 0.03 for thrown items, 0.07 for XP bottles.
 */
public final class Trajectory implements QuickImports {
    public record Launch(double speed, double gravity, double pitchOffset, double drag) {}

    public record Result(List<Vec3> points, Vec3 end, Entity hitEntity, Direction face, boolean landed) {}

    private Trajectory() {
    }

    /** What the held stack would fire right now, or null if it is not a projectile. */
    public static Launch launchFor(Player player, ItemStack stack) {
        if (stack.isEmpty()) return null;

        if (stack.is(Items.BOW)) {
            if (!player.isUsingItem()) return null;
            float power = BowItem.getPowerForTime(player.getTicksUsingItem());
            return new Launch(Math.max(0.1f, power) * 3.0, 0.05, 0, 0.99);
        }
        if (stack.is(Items.CROSSBOW)) {
            return CrossbowItem.isCharged(stack) ? new Launch(3.15, 0.05, 0, 0.99) : null;
        }
        if (stack.is(Items.TRIDENT)) {
            return player.isUsingItem() ? new Launch(2.5, 0.05, 0, 0.99) : null;
        }
        if (stack.is(Items.SNOWBALL) || stack.is(Items.EGG) || stack.is(Items.BLUE_EGG)
                || stack.is(Items.BROWN_EGG) || stack.is(Items.ENDER_PEARL)) {
            return new Launch(1.5, 0.03, 0, 0.99);
        }
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            return new Launch(0.5, 0.05, -20, 0.99);
        }
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return new Launch(0.7, 0.07, -20, 0.99);
        }
        return null;
    }

    public static Result simulate(Player player, Launch launch, float partial, int maxSteps) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double offsetRad = Math.toRadians(pitch + launch.pitchOffset());

        Vec3 direction = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(offsetRad),
                Math.cos(yawRad) * Math.cos(pitchRad)).normalize();

        Vec3 own = player.getDeltaMovement();
        Vec3 velocity = direction.scale(launch.speed()).add(own.x, player.onGround() ? 0.0 : own.y, own.z);

        Vec3 position = player.getEyePosition(partial).subtract(0, 0.1, 0);
        List<Vec3> points = new ArrayList<>();
        points.add(position);

        for (int step = 0; step < maxSteps; step++) {
            Vec3 next = position.add(velocity);

            BlockHitResult block = mc.level.clip(new ClipContext(position, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 reach = block.getType() != HitResult.Type.MISS ? block.getLocation() : next;

            Entity hitEntity = null;
            Vec3 entityHit = null;
            double best = Double.MAX_VALUE;
            AABB sweep = new AABB(position, reach).inflate(1.0);
            for (Entity entity : mc.level.getEntities(player, sweep, e -> e.isPickable() && !e.isSpectator())) {
                Optional<Vec3> clip = entity.getBoundingBox().inflate(0.3).clip(position, reach);
                if (clip.isEmpty()) continue;
                double distance = position.distanceToSqr(clip.get());
                if (distance < best) {
                    best = distance;
                    hitEntity = entity;
                    entityHit = clip.get();
                }
            }

            if (hitEntity != null) {
                points.add(entityHit);
                return new Result(points, entityHit, hitEntity, null, true);
            }
            if (block.getType() != HitResult.Type.MISS) {
                points.add(block.getLocation());
                return new Result(points, block.getLocation(), null, block.getDirection(), true);
            }

            position = next;
            points.add(position);
            velocity = velocity.scale(launch.drag()).subtract(0, launch.gravity(), 0);

            if (position.y < mc.level.getMinY() - 16) break;
        }

        return new Result(points, position, null, null, false);
    }
}
