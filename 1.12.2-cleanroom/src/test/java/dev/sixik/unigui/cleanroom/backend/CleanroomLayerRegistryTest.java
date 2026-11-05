package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.backend.UniGuiBackend;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomLayerRegistryTest {
    @Test
    void render_ordersByPriorityAndFiltersVisibility() {
        CleanroomLayerRegistry<String> registry = new CleanroomLayerRegistry<>();
        List<String> rendered = new ArrayList<>();
        registry.add(renderer("late", rendered), value -> true, 20);
        registry.add(renderer("hidden", rendered), value -> false, -10);
        registry.add(renderer("early", rendered), value -> true, -5);

        registry.render("screen", 320, 240, 0, 0, 0.0f);

        assertEquals(List.of("early", "late"), rendered);
    }

    @Test
    void handlePriorityChange_reordersNextRenderSnapshot() {
        CleanroomLayerRegistry<String> registry = new CleanroomLayerRegistry<>();
        List<String> rendered = new ArrayList<>();
        UniGuiBackend.LayerHandle first = registry.add(renderer("first", rendered), value -> true, 0);
        registry.add(renderer("second", rendered), value -> true, 1);

        first.priority(5);
        registry.render("screen", 320, 240, 0, 0, 0.0f);

        assertEquals(List.of("second", "first"), rendered);
    }

    @Test
    void close_disablesAndClosesRendererExactlyOnce() {
        CleanroomLayerRegistry<String> registry = new CleanroomLayerRegistry<>();
        List<String> calls = new ArrayList<>();
        UniGuiBackend.LayerHandle handle = registry.add(renderer("layer", calls), value -> true, 0);

        handle.close();
        handle.close();
        registry.render("screen", 320, 240, 0, 0, 0.0f);

        assertTrue(handle.closed());
        assertFalse(handle.enabled());
        assertEquals(List.of("close"), calls);
    }

    private static CleanroomLayerRegistry.LayerRenderer renderer(String name, List<String> calls) {
        return new CleanroomLayerRegistry.LayerRenderer() {
            @Override
            public void render(int width, int height, int mouseX, int mouseY, float partialTick) {
                calls.add(name);
            }

            @Override
            public void close() {
                calls.add("close");
            }
        };
    }
}
