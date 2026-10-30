package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomTooltipsTest {
    @AfterEach
    void reset() {
        CleanroomTooltips.discard();
    }

    @Test
    void passesThroughWhenNotDeferring() {
        assertFalse(CleanroomTooltips.defer(() -> {
        }));
    }

    @Test
    void queuesWhileDeferringAndReplaysInOrder() {
        List<Integer> order = new ArrayList<>();
        CleanroomTooltips.begin();
        assertTrue(CleanroomTooltips.defer(() -> order.add(1)));
        assertTrue(CleanroomTooltips.defer(() -> order.add(2)));
        assertTrue(order.isEmpty());
        CleanroomTooltips.end();
        CleanroomTooltips.flush();
        assertEquals(List.of(1, 2), order);
    }

    @Test
    void replayedDrawsGoStraightThrough() {
        List<String> drawn = new ArrayList<>();
        CleanroomTooltips.begin();
        CleanroomTooltips.defer(() -> {
            // The real hook calls GuiUtils again; with deferral off that call must not be queued a second time.
            assertFalse(CleanroomTooltips.defer(() -> drawn.add("again")));
            drawn.add("tooltip");
        });
        CleanroomTooltips.end();
        CleanroomTooltips.flush();
        assertEquals(List.of("tooltip"), drawn);
    }

    @Test
    void discardDropsQueueAndStopsDeferring() {
        List<Integer> order = new ArrayList<>();
        CleanroomTooltips.begin();
        CleanroomTooltips.defer(() -> order.add(1));
        CleanroomTooltips.discard();
        CleanroomTooltips.flush();
        assertTrue(order.isEmpty());
        assertFalse(CleanroomTooltips.deferring());
    }

    @Test
    void beginDropsLeftoversOfAnAbortedFrame() {
        List<Integer> order = new ArrayList<>();
        CleanroomTooltips.begin();
        CleanroomTooltips.defer(() -> order.add(1));
        CleanroomTooltips.begin();
        CleanroomTooltips.end();
        CleanroomTooltips.flush();
        assertTrue(order.isEmpty());
    }
}