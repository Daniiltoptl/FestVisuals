package com.fest.visuals.api.utils.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.fest.visuals.api.event.Listener;
import com.fest.visuals.api.event.events.client.TickEvent;
import com.fest.visuals.api.event.events.player.world.AttackEvent;
import com.fest.visuals.api.event.events.player.world.CritEvent;
import com.fest.visuals.api.system.interfaces.QuickImports;

/**
 * Turns health changes into hits.
 *
 * <p>The client is never told "X took N damage"; it only sees each entity's synced health change.
 * Diffing health once a tick recovers the amount for every living entity in range, whoever dealt
 * it. Swings and crits of the local player are remembered for a short window so a later health
 * drop can be attributed to them — the server applies the damage a round trip after the click.
 */
public class CombatTracker implements QuickImports {
    @Getter private static final CombatTracker instance = new CombatTracker();

    /** How long after our swing a health drop still counts as ours. */
    private static final long OURS_WINDOW_MS = 1000L;
    /** How long after a crit swing the resulting drop is shown as a crit. */
    private static final long CRIT_WINDOW_MS = 700L;
    private static final double RANGE = 48.0;

    public record Hit(LivingEntity entity, float amount, boolean heal, boolean crit, boolean ours, Vec3 position) {}

    /** A swing of the local player, reported immediately: before the server confirms anything. */
    public record Swing(Entity target, boolean crit) {}

    private final Map<Integer, Float> health = new HashMap<>();
    private final Map<Integer, Long> attackedAt = new HashMap<>();
    private final Map<Integer, Long> critAt = new HashMap<>();

    private final List<Consumer<Hit>> hitListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Swing>> swingListeners = new CopyOnWriteArrayList<>();

    private boolean running;
    private Entity pendingTarget;
    private Entity lastTarget;
    private long lastTargetAt;

    public void onHit(Consumer<Hit> listener) {
        start();
        if (!hitListeners.contains(listener)) hitListeners.add(listener);
    }

    public void onSwing(Consumer<Swing> listener) {
        start();
        if (!swingListeners.contains(listener)) swingListeners.add(listener);
    }

    public void remove(Object listener) {
        hitListeners.remove(listener);
        swingListeners.remove(listener);
    }

    public boolean attackedRecently(Entity entity) {
        Long at = attackedAt.get(entity.getId());
        return at != null && System.currentTimeMillis() - at <= OURS_WINDOW_MS;
    }

    /** The entity the local player last swung at, if that was within {@code windowMs}. */
    public Entity recentTarget(long windowMs) {
        start();
        if (lastTarget == null || System.currentTimeMillis() - lastTargetAt > windowMs) return null;
        return lastTarget;
    }

    /** Subscribed once and kept: the bookkeeping is cheap, and consumers come and go. */
    public void start() {
        if (running) return;
        running = true;

        TickEvent.getInstance().subscribe(new Listener<>(event -> tick()));

        // The attack event fires before the crit decision is made, so the swing is reported one
        // step later: either by the crit event, or at the next tick as a plain hit.
        AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            flushPending();
            pendingTarget = event.entity();
            lastTarget = event.entity();
            lastTargetAt = System.currentTimeMillis();
            attackedAt.put(event.entity().getId(), lastTargetAt);
        }));

        CritEvent.getInstance().subscribe(new Listener<>(event -> {
            critAt.put(event.target().getId(), System.currentTimeMillis());
            if (pendingTarget == event.target()) {
                pendingTarget = null;
                fireSwing(new Swing(event.target(), true));
            }
        }));
    }

    private void flushPending() {
        if (pendingTarget == null) return;
        Entity target = pendingTarget;
        pendingTarget = null;
        fireSwing(new Swing(target, false));
    }

    private void fireSwing(Swing swing) {
        for (Consumer<Swing> listener : swingListeners) listener.accept(swing);
    }

    private void tick() {
        flushPending();

        if (mc.player == null || mc.level == null) {
            health.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Map<Integer, Float> seen = new HashMap<>();
        List<Hit> hits = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player) continue;
            if (entity.distanceTo(mc.player) > RANGE) continue;

            float current = living.getHealth() + living.getAbsorptionAmount();
            Float previous = health.get(entity.getId());
            seen.put(entity.getId(), current);

            if (previous == null) continue;

            float delta = current - previous;
            if (Math.abs(delta) < 0.05f) continue;

            // A dead entity snaps to zero; that last drop is still damage worth showing.
            boolean heal = delta > 0f;
            Long attacked = attackedAt.get(entity.getId());
            Long crit = critAt.get(entity.getId());

            boolean ours = attacked != null && now - attacked <= OURS_WINDOW_MS;
            boolean wasCrit = !heal && crit != null && now - crit <= CRIT_WINDOW_MS;
            if (wasCrit) critAt.remove(entity.getId());

            Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.75, 0);
            hits.add(new Hit(living, Math.abs(delta), heal, wasCrit, ours, at));
        }

        health.clear();
        health.putAll(seen);
        attackedAt.values().removeIf(at -> now - at > OURS_WINDOW_MS * 4);
        critAt.values().removeIf(at -> now - at > CRIT_WINDOW_MS * 4);

        for (Hit hit : hits) {
            for (Consumer<Hit> listener : hitListeners) listener.accept(hit);
        }
    }
}
