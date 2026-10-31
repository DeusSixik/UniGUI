package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GtnhClientTasksTest {
    @Test
    void tasksRunInOrderAndOnlyOnce() {
        List<Integer> order = new ArrayList<>();
        GtnhClientTasks.post(() -> order.add(1));
        GtnhClientTasks.post(() -> order.add(2));

        assertEquals(2, GtnhClientTasks.drain());
        assertEquals(List.of(1, 2), order);
        assertEquals(0, GtnhClientTasks.drain());
    }

    @Test
    void failingTaskDoesNotStopTheOthers() {
        List<Integer> order = new ArrayList<>();
        GtnhClientTasks.post(() -> {
            throw new IllegalStateException("expected by the test");
        });
        GtnhClientTasks.post(() -> order.add(7));

        assertEquals(2, GtnhClientTasks.drain());
        assertEquals(List.of(7), order);
    }

    @Test
    void taskQueuedByATaskWaitsForTheNextDrain() {
        List<Integer> order = new ArrayList<>();
        GtnhClientTasks.post(() -> GtnhClientTasks.post(() -> order.add(1)));

        assertEquals(1, GtnhClientTasks.drain());
        assertEquals(List.of(), order);
        assertEquals(1, GtnhClientTasks.drain());
        assertEquals(List.of(1), order);
    }
}