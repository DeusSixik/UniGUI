package dev.sixik.unigui.api.style;

public interface Style {
    Style EMPTY = new Style() {
    };

    default long version() {
        return 0L;
    }

    default <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
        return fallback;
    }
}
