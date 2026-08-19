package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.WidgetState;

/**
 * Общие helper-методы для применения {@link Style} при построении {@link RenderPlan}.
 */
public final class StyledRenderPlans {
    private StyledRenderPlans() {
    }

    /**
     * Нормализует {@code null} состояние в {@link WidgetState#NORMAL}.
     *
     * @param state состояние виджета
     * @return нормализованное состояние
     */
    public static WidgetState state(WidgetState state) {
        return state == null ? WidgetState.NORMAL : state;
    }

    /**
     * Безопасно читает style-значение.
     *
     * @param style style или {@code null}
     * @param key ключ свойства
     * @param state состояние виджета
     * @param fallback fallback-значение
     * @return значение из style или fallback
     * @param <T> Java-тип значения
     */
    public static <T> T value(Style style, StyleKey<T> key, WidgetState state, T fallback) {
        if (style == null || key == null) return fallback;
        return style.get(key, state(state), fallback);
    }
}