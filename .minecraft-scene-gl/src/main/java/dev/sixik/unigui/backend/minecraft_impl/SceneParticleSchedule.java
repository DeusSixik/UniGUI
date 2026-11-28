package dev.sixik.unigui.backend.minecraft_impl;

/**
 * Uniform particle births sampled by the Minecraft scene adapters.
 *
 * @param startSeconds inclusive time of the first birth, finite and nonnegative
 * @param endSeconds   exclusive end of emission, or positive infinity
 * @param rate         finite births per second in (0, 128]
 */
public record SceneParticleSchedule(double startSeconds, double endSeconds, double rate) {
    /**
     * The maximum number of recent births retained per emitter during a draw.
     */
    static final int MAX_PARTICLES = 128;

    /**
     * @throws IllegalArgumentException if the interval or rate is outside its range
     */
    public SceneParticleSchedule {
        if (!Double.isFinite(startSeconds) || startSeconds < 0 || !(endSeconds > startSeconds)
                || !(rate > 0 && rate <= MAX_PARTICLES)) {
            throw new IllegalArgumentException("Invalid scene particle interval or rate");
        }
    }

    /**
     * Evaluates {@code startSeconds + index / rate}, with index zero marking the first birth.
     *
     * @param index zero-based emission index
     * @return birth time in seconds
     */
    public double birth(int index) {
        return startSeconds + index / rate;
    }

    /**
     * Caps a sample at the last representable value before the end and corrects floating-point rounding
     * by comparing actual birth times near {@code floor((sample - startSeconds) * rate)}.
     *
     * @param time sample time in seconds
     * @return exclusive upper index of births at that time
     */
    public int limit(double time) {
        double sample = Math.min(time, Math.nextDown(endSeconds));
        if (sample < startSeconds) return 0;
        int index = (int) Math.floor((sample - startSeconds) * rate);
        if (index == Integer.MAX_VALUE) return index;
        if (index > 0 && birth(index) > sample) return index;
        if (index < Integer.MAX_VALUE - 1 && birth(index + 1) <= sample) return index + 2;
        return index + 1;
    }

    /**
     * @param time     current sample in seconds
     * @param limit    exclusive upper birth index at the sample
     * @param lifetime longest native lifespan in seconds
     * @return first recent birth whose particle may still be alive
     */
    public int first(double time, int limit, double lifetime) {
        return Math.max(limit(time - lifetime), limit - MAX_PARTICLES);
    }
}
