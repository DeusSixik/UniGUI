package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.render.UiRenderPolicy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomRenderPoliciesTest {
    private static CleanroomRenderPolicies.Decision decide(UiRenderPolicy.Mode mode, boolean hasTexture,
                                                           boolean size, boolean layout, boolean dirty,
                                                           boolean animating, long now, long last) {
        return CleanroomRenderPolicies.decide(mode, 100L, hasTexture, size, layout, dirty, () -> animating, now, last);
    }

    @Test
    void continuousNeverUsesTheCache() {
        assertFalse(CleanroomRenderPolicies.usesCache(UiRenderPolicy.Mode.CONTINUOUS));
        assertTrue(CleanroomRenderPolicies.usesCache(UiRenderPolicy.Mode.ON_DIRTY));
        assertEquals(CleanroomRenderPolicies.Action.SKIP,
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
        assertEquals(CleanroomRenderPolicies.Action.SKIP,
                decide(UiRenderPolicy.Mode.ON_DIRTY, true, false, false, false, false, 10, 5).action());
    }

    @Test
    void animationsAreOnlyCheckedWhenNothingElseForcesARedraw() {
        AtomicInteger checks = new AtomicInteger();
        CleanroomRenderPolicies.decide(UiRenderPolicy.Mode.ON_DIRTY, 100L, true, false, false, true,
                () -> {
                    checks.incrementAndGet();
                    return true;
                }, 10, 5);
        assertEquals(0, checks.get(), "the tree walk is skipped when the dirty flag already decides");
        CleanroomRenderPolicies.decide(UiRenderPolicy.Mode.ON_DIRTY, 100L, true, false, false, false,
                () -> {
                    checks.incrementAndGet();
                    return false;
                }, 10, 5);
        assertEquals(1, checks.get());
    }

    @Test
    void timedModesRefreshOnceTheIntervalHasPassedAndIgnoreDirty() {
        assertEquals("interval", decide(UiRenderPolicy.Mode.FIXED_FPS, true, false, false, false, false, 10, 0).reason());
        assertEquals(CleanroomRenderPolicies.Action.SKIP,
                decide(UiRenderPolicy.Mode.FIXED_FPS, true, false, true, true, true, 150, 100).action());
        assertEquals("interval", decide(UiRenderPolicy.Mode.VSYNC, true, false, false, false, false, 200, 100).reason());
    }

    @Test
    void intervalFollowsFpsAndRefreshRate() {
        assertEquals(20_000_000L, CleanroomRenderPolicies.intervalNanos(UiRenderPolicy.fixedFps(50.0f), 144));
        assertEquals(Math.round(1_000_000_000.0 / 144), CleanroomRenderPolicies.intervalNanos(UiRenderPolicy.vsync(), 144));
        assertEquals(Math.round(1_000_000_000.0 / 60), CleanroomRenderPolicies.intervalNanos(UiRenderPolicy.vsync(), 0));
    }
}