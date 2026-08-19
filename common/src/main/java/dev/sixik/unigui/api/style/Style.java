package dev.sixik.unigui.api.style;

import java.util.Map;

public interface Style {
    Style EMPTY = new Style() {
    };

    default long version() {
        return 0L;
    }

    default <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
        return fallback;
    }

    default Map<WidgetState, Map<StyleKey<?>, Object>> values() {
        return Map.of();
    }

    default Map<StyleKey<?>, Object> values(WidgetState state) {
        WidgetState normalized = state == null ? WidgetState.NORMAL : state;
        Map<StyleKey<?>, Object> values = values().get(normalized);
        return values == null ? Map.of() : values;
    }
}
