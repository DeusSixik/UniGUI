package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhTooltipsTest {
    @AfterEach
    void reset() {
        GtnhTooltips.discard();
    }

    @Test
    void passesThroughWhenNotDeferring() {
        assertFalse(GtnhTooltips.defer(() -> {
        }));
    }

    @Test
    void queuesWhileDeferringAndReplaysInOrder() {
        List<Integer> order = new ArrayList<>();
        GtnhTooltips.begin();
        assertTrue(GtnhTooltips.defer(() -> order.add(1)));
        assertTrue(GtnhTooltips.defer(() -> order.add(2)));
        assertTrue(order.isEmpty());
        GtnhTooltips.end();
        GtnhTooltips.flush();
        assertEquals(List.of(1, 2), order);
    }

    @Test
    void replayedDrawsGoStraightThrough() {
        List<String> drawn = new ArrayList<>();
        GtnhTooltips.begin();
        GtnhTooltips.defer(() -> {
            // The real hook calls GuiUtils again; with deferral off that call must not be queued a second time.
            assertFalse(GtnhTooltips.defer(() -> drawn.add("again")));
            drawn.add("tooltip");
        });
        GtnhTooltips.end();
        GtnhTooltips.flush();
        assertEquals(List.of("tooltip"), drawn);
    }

    @Test
    void discardDropsQueueAndStopsDeferring() {
        List<Integer> order = new ArrayList<>();
        GtnhTooltips.begin();
        GtnhTooltips.defer(() -> order.add(1));
        GtnhTooltips.discard();
        GtnhTooltips.flush();
        assertTrue(order.isEmpty());
        assertFalse(GtnhTooltips.deferring());
    }

    @Test
    void beginDropsLeftoversOfAnAbortedFrame() {
        List<Integer> order = new ArrayList<>();
        GtnhTooltips.begin();
        GtnhTooltips.defer(() -> order.add(1));
        GtnhTooltips.begin();
        GtnhTooltips.end();
        GtnhTooltips.flush();
        assertTrue(order.isEmpty());
    }
}