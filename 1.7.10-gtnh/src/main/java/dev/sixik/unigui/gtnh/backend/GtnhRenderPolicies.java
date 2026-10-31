package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.UiRenderPolicy;

import java.util.function.BooleanSupplier;

/**
 * Decides when a retained UI has to be redrawn into its cache texture (pure logic, no GL).
 *
 * <p>Port of the cache decision of the 1.20 screen: {@code CONTINUOUS} never uses the cache; the other modes redraw
 * the UI into a texture only when needed and draw that texture every frame.</p>
 */
public final class GtnhRenderPolicies {
    /**
     * What to do with the cache texture this frame.
     */
    public enum Action {
        /**
         * Draw the cached texture as is.
         */
        SKIP,
        /**
         * Redraw the UI into the cache first.
         */
        REFRESH
    }

    /**
     * The action and a short reason, used as a profiler scope name.
     */
    public record Decision(Action action, String reason) {
        private static final Decision STABLE = new Decision(Action.SKIP, "stable");

        static Decision refresh(String reason) {
            return new Decision(Action.REFRESH, reason);
        }
    }

    private GtnhRenderPolicies() {
    }

    /**
     * @param mode policy mode
     * @return {@code true} if the mode draws through a cache texture
     */
    public static boolean usesCache(UiRenderPolicy.Mode mode) {
        return mode != UiRenderPolicy.Mode.CONTINUOUS;
    }

    /**
     * Chooses between reusing and rebuilding the cache.
     *
     * @param mode              policy mode
     * @param intervalNanos     redraw interval for the timed modes (already resolved for {@code VSYNC})
     * @param hasTexture        {@code true} if a cache texture exists
     * @param sizeChanged       {@code true} if the window size differs from the cache size
     * @param layoutChanged     {@code true} if layout ran this frame
     * @param dirty             {@code true} if any widget in the tree is invalidated
     * @param animationsRunning evaluated lazily, only for {@code ON_DIRTY} when nothing else forces a redraw
     * @param nowNanos          current time
     * @param lastRenderNanos   time of the last cache redraw, {@code 0} if never
     * @param frameNanos        typical time between game frames, {@code 0} if unknown. A timed mode redraws when the next
     *                          frame would be farther from the target time than this one, which keeps the cadence regular
     *                          when the interval is not a multiple of the frame time; without it a frame arriving a
     *                          fraction of a millisecond early skips a whole refresh and the load becomes uneven
     * @return the decision
     */
    public static Decision decide(UiRenderPolicy.Mode mode, long intervalNanos, boolean hasTexture,
                                  boolean sizeChanged, boolean layoutChanged, boolean dirty,
                                  BooleanSupplier animationsRunning, long nowNanos, long lastRenderNanos,
                                  long frameNanos) {
        if (mode == UiRenderPolicy.Mode.CONTINUOUS) return Decision.STABLE;
        if (!hasTexture) return Decision.refresh("cold");
        if (sizeChanged) return Decision.refresh("resize");
        if (mode == UiRenderPolicy.Mode.ON_DIRTY) {
            if (layoutChanged) return Decision.refresh("layout");
            if (dirty) return Decision.refresh("dirty");
            if (animationsRunning.getAsBoolean()) return Decision.refresh("animations");
            return Decision.STABLE;
        }
        long half = Math.max(0L, Math.min(frameNanos, intervalNanos)) / 2L;
        return lastRenderNanos == 0L || nowNanos - lastRenderNanos + half >= intervalNanos
            ? Decision.refresh("interval") : Decision.STABLE;
    }

    /**
     * Resolves the redraw interval of a policy.
     *
     * @param policy        policy
     * @param refreshRateHz monitor refresh rate, used for {@code VSYNC}; non-positive means unknown (60)
     * @return interval in nanoseconds, at least 1
     */
    public static long intervalNanos(UiRenderPolicy policy, int refreshRateHz) {
        if (policy.mode() == UiRenderPolicy.Mode.VSYNC) {
            float rate = refreshRateHz > 0 ? refreshRateHz : 60.0f;
            return Math.max(1L, Math.round(1_000_000_000.0 / rate));
        }
        return Math.max(1L, policy.intervalNanos());
    }
}
