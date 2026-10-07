package com.fest.visuals.api.utils.render;

/**
 * A render-distance multiplier for FPS Boost that eases off when the game cannot hold its frame
 * target and recovers once it can.
 *
 * <p>It used to be sampled every frame and only recovered when FPS beat the target by 6% — which a
 * frame limiter never allows, so after one dip it stayed at its floor for good and chests, signs,
 * beds and dropped items vanished a few blocks away. Now it is sampled a few times a second, eases
 * down only when FPS is clearly short of the target, comes back as soon as the target is (nearly)
 * met, and never cuts the distance below 60%.
 */
public final class AdaptiveBudget {
    private static final AdaptiveBudget INSTANCE = new AdaptiveBudget();

    private static final double FLOOR = 0.6;
    private static final long SAMPLE_EVERY_MS = 250;

    private double factor = 1;
    private double average;
    private long lastSample;

    public static AdaptiveBudget getInstance() { return INSTANCE; }

    public double sample(double fps, double target) {
        long now = System.currentTimeMillis();
        if (now - lastSample < SAMPLE_EVERY_MS) return factor;
        lastSample = now;
        if (!Double.isFinite(fps) || fps <= 0 || target <= 0) return factor;

        average = average == 0 ? fps : average * 0.7 + fps * 0.3;
        if (average < target * 0.80) factor = Math.max(FLOOR, factor - 0.05);
        else if (average >= target * 0.95) factor = Math.min(1, factor + 0.05);
        return factor;
    }

    public double factor() { return factor; }

    public void reset() { factor = 1; average = 0; lastSample = 0; }
}
