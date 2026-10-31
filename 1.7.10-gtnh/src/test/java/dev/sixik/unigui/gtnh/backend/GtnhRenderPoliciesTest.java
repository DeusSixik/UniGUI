package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.UiRenderPolicy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhRenderPoliciesTest {
    private static GtnhRenderPolicies.Decision decide(UiRenderPolicy.Mode mode, boolean hasTexture,
                                                           boolean size, boolean layout, boolean dirty,
                                                           boolean animating, long now, long last) {
        return GtnhRenderPolicies.decide(mode, 100L, hasTexture, size, layout, dirty, () -> animating, now, last, 0L);
    }

    @Test
    void continuousNeverUsesTheCache() {
        assertFalse(GtnhRenderPolicies.usesCache(UiRenderPolicy.Mode.CONTINUOUS));
        assertTrue(GtnhRenderPolicies.usesCache(UiRenderPolicy.Mode.ON_DIRTY));
        assertEquals(GtnhRenderPolicies.Action.SKIP,
                decide(UiRenderPolicy.Mode.CONTINUOUS, false, true, true, true, true, 10, 0).action());
    }

    @Test
    void coldCacheAndResizeAlwaysRefresh() {
        assertEquals("cold", decide(UiRenderPolicy.Mode.ON_DIRTY, false, false, false, false, false, 10, 5).reason());
        assertEquals("resize", decide(UiRenderPolicy.Mode.ON_DIRTY, true, true, false, false, false, 10, 5).reason());
        assertEquals("resize", decide(UiRenderPolicy.Mode.FIXED_FPS, true, true, false, false, false, 10, 5).reason());
    }

    @Test
    void onDirtyRefreshesOnLayoutThenDirtyThenAnimationsOtherwiseSkips() {
        assertEquals("layout", decide(UiRenderPolicy.Mode.ON_DIRTY, true, false, true, true, true, 10, 5).reason());
        assertEquals("dirty", decide(UiRenderPolicy.Mode.ON_DIRTY, true, false, false, true, true, 10, 5).reason());
        assertEquals("animations", decide(UiRenderPolicy.Mode.ON_DIRTY, true, false, false, false, true, 10, 5).reason());
        assertEquals(GtnhRenderPolicies.Action.SKIP,
                decide(UiRenderPolicy.Mode.ON_DIRTY, true, false, false, false, false, 10, 5).action());
    }

    @Test
    void animationsAreOnlyCheckedWhenNothingElseForcesARedraw() {
        AtomicInteger checks = new AtomicInteger();
        GtnhRenderPolicies.decide(UiRenderPolicy.Mode.ON_DIRTY, 100L, true, false, false, true,
                () -> {
                    checks.incrementAndGet();
                    return true;
                }, 10, 5, 0L);
        assertEquals(0, checks.get(), "the tree walk is skipped when the dirty flag already decides");
        GtnhRenderPolicies.decide(UiRenderPolicy.Mode.ON_DIRTY, 100L, true, false, false, false,
                () -> {
                    checks.incrementAndGet();
                    return false;
                }, 10, 5, 0L);
        assertEquals(1, checks.get());
    }

    @Test
    void timedModesRefreshOnceTheIntervalHasPassedAndIgnoreDirty() {
        assertEquals("interval", decide(UiRenderPolicy.Mode.FIXED_FPS, true, false, false, false, false, 10, 0).reason());
        assertEquals(GtnhRenderPolicies.Action.SKIP,
                decide(UiRenderPolicy.Mode.FIXED_FPS, true, false, true, true, true, 150, 100).action());
        assertEquals("interval", decide(UiRenderPolicy.Mode.VSYNC, true, false, false, false, false, 200, 100).reason());
    }

    /**
     * Frames come every 8.0 / 8.4 ms (about 120 fps) and the interval is 1/60 s. The sum of two frames is 16.4 ms,
     * just under the interval: a plain "elapsed >= interval" test skips the second frame and refreshes on the third,
     * which makes the load uneven. With the frame time known the cadence is a regular one refresh in two frames.
     */
    @Test
    void timedModeKeepsARegularCadenceWhenFramesArriveJustEarly() {
        long interval = 16_666_667L;
        long[] deltas = {8_000_000L, 8_400_000L};
        for (long frameNanos : new long[] {0L, 8_200_000L}) {
            long now = 1_000_000_000L;
            long last = 0L;
            int framesSince = 0;
            java.util.List<Integer> gaps = new java.util.ArrayList<>();
            for (int frame = 0; frame < 400; frame++) {
                now += deltas[frame % 2];
                framesSince++;
                var decision = GtnhRenderPolicies.decide(UiRenderPolicy.Mode.VSYNC, interval, true, false,
                        false, false, () -> false, now, last, frameNanos);
                if (decision.action() == GtnhRenderPolicies.Action.REFRESH) {
                    if (last != 0L) gaps.add(framesSince);
                    last = now;
                    framesSince = 0;
                }
            }
            boolean regular = gaps.stream().allMatch(gap -> gap == 2);
            if (frameNanos == 0L) {
                assertFalse(regular, "without the frame time the cadence is uneven (the bug this guards against)");
            } else {
                assertTrue(regular, "gaps were " + gaps.subList(0, Math.min(12, gaps.size())));
            }
        }
    }

    @Test
    void intervalFollowsFpsAndRefreshRate() {
        assertEquals(20_000_000L, GtnhRenderPolicies.intervalNanos(UiRenderPolicy.fixedFps(50.0f), 144));
        assertEquals(Math.round(1_000_000_000.0 / 144), GtnhRenderPolicies.intervalNanos(UiRenderPolicy.vsync(), 144));
        assertEquals(Math.round(1_000_000_000.0 / 60), GtnhRenderPolicies.intervalNanos(UiRenderPolicy.vsync(), 0));
    }
}