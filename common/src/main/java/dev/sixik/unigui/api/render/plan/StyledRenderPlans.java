package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.WidgetState;

/** Shared helpers for applying resolved Style values while building RenderPlans. */
public final class StyledRenderPlans {
    private StyledRenderPlans() {
    }

    public static WidgetState state(WidgetState state) {
        return state == null ? WidgetState.NORMAL : state;
    }

    public static <T> T value(Style style, StyleKey<T> key, WidgetState state, T fallback) {
        if (style == null || key == null) return fallback;
        return style.get(key, state(state), fallback);
    }
}