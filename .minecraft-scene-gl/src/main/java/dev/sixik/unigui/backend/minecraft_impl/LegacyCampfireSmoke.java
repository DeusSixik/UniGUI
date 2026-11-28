package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.GlSceneSprites.Frame;

import java.util.Random;

/**
 * Seekable campfire-style smoke for compatibility-profile Furnace scenes.
 */
public class LegacyCampfireSmoke extends SceneNode {
    /**
     * Vanilla particle simulation rate in ticks per second.
     */
    private static final int TICKS_PER_SECOND = 20;
    /**
     * Maximum possible particle lifespan in seconds, rounded up to cover every path.
     */
    private static final double MAX_LIFETIME = 130.0 / TICKS_PER_SECOND;
    /**
     * Independent paths and mask shapes, reused in birth order.
     */
    private static final Frame[][] PATHS = bakePaths();
    /**
     * Emission schedule relative to the Furnace ignition.
     */
    private final SceneParticleSchedule schedule;
    /**
     * Current time in seconds relative to ignition.
     */
    private double seconds;

    /**
     * @param start inclusive time of the first birth in seconds
     * @param end   exclusive end of emission in seconds
     * @param rate  number of births per second
     * @throws IllegalArgumentException if the interval or rate is invalid
     */
    public LegacyCampfireSmoke(double start, double end, double rate) {
        schedule = new SceneParticleSchedule(start, end, rate);
        setCastShadow(false).setReceiveShadow(false);
    }

    /**
     * Sets the time used to sample the precomputed smoke paths.
     *
     * @param time absolute time in seconds on the ignition timeline
     * @throws IllegalArgumentException if the time is nonfinite
     */
    public void setTime(double time) {
        if (!Double.isFinite(time)) throw new IllegalArgumentException("Smoke time must be finite");
        if (seconds != time) {
            seconds = time;
            invalidateContent();
        }
    }

    /**
     * @return exclusive upper birth index at the current sample
     */
    int limit() {
        return schedule.limit(seconds);
    }

    /**
     * @param limit exclusive upper birth index @return first birth that may still be alive
     */
    int first(int limit) {
        return schedule.first(seconds, limit, MAX_LIFETIME);
    }

    /**
     * @param index birth index @return age of the particle in simulation ticks
     */
    double ageTicks(int index) {
        return (seconds - schedule.birth(index)) * TICKS_PER_SECOND;
    }

    /**
     * @param index birth index @return positive mask seed for the corresponding path
     */
    int seed(int index) {
        return index % PATHS.length + 1;
    }

    /**
     * @param seed one-based path and mask index @return borrowed read-only frames at whole ticks
     */
    Frame[] path(int seed) {
        return PATHS[seed - 1];
    }

    /**
     * Builds paths using the modern campfire smoke's size, lifespan, near-zero gravity, and
     * lateral drift. The horizontal velocity changes by a small signed amount per tick.
     * Each position advances by its current velocity in blocks per tick. At tick {@code t},
     * alpha is {@code max(0, 0.9 - 0.015 * max(0, t - (lifetime - 60)))}, where {@code lifetime}
     * is the length of the path in ticks. The closing sample is transparent.
     *
     * @return paths with positions in blocks and normalized UV and color components
     */
    private static Frame[][] bakePaths() {
        var random = new Random(0x5CE9E);
        Frame[][] paths = new Frame[12][];
        for (int i = 0; i < paths.length; i++) {
            double x = signed(random, 0.14), y = signed(random, 0.14), z = signed(random, 0.14);
            double vx = 0.003 + signed(random, 0.003);
            double vy = signed(random, 0.003) + random.nextFloat() / 500.0;
            double vz = 0.003 + signed(random, 0.003);
            int lifetime = 80 + random.nextInt(50);
            float size = (0.3f + random.nextFloat() * 0.3f) * 0.9f;
            Frame[] frames = new Frame[lifetime + 1];
            for (int tick = 0; tick <= lifetime; tick++) {
                float opacity = Math.max(0, 0.9f - 0.015f * Math.max(0, tick - (lifetime - 60)));
                frames[tick] = new Frame((float) x, (float) y, (float) z, size, 0, 0, 0, 1, 1,
                        0.52f, 0.50f, 0.48f, opacity);
                vx += signed(random, 0.0002);
                vy -= 0.000003;
                vz += signed(random, 0.0002);
                x += vx;
                y += vy;
                z += vz;
            }
            paths[i] = frames;
        }
        return paths;
    }

    /**
     * Maps {@code u} from [0, 1) to {@code (2 * u - 1) * range}.
     *
     * @param random source of uniform samples u
     * @param range  maximum absolute offset in blocks or blocks per tick
     * @return signed offset
     */
    private static double signed(Random random, double range) {
        return (2 * random.nextDouble() - 1) * range;
    }
}
