package dev.sixik.unigui.api.style;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MutableStyle implements Style {
    private final EnumMap<WidgetState, Map<StyleKey<?>, Object>> values = new EnumMap<>(WidgetState.class);
    private long version;

    public <T> MutableStyle put(StyleKey<T> key, T value) {
        return put(key, WidgetState.NORMAL, value);
    }

    public <T> MutableStyle put(StyleKey<T> key, WidgetState state, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(state, "state");
        if (value != null && !key.type().isInstance(value)) {
            throw new IllegalArgumentException("Style value for " + key.id() + " must be " + key.type().getName());
        }
        values.computeIfAbsent(state, ignored -> new HashMap<>()).put(key, value);
        version++;
        return this;
    }

    /**
     * Sets the default renderer override for widgets using this style.
     *
     * <p>The concrete widget casts this value to its renderer interface. Local
     * per-instance renderer setters still have priority over this style value.</p>
     */
    public MutableStyle renderer(Object renderer) {
        return put(StyleKeys.RENDERER, renderer);
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
        Objects.requireNonNull(key, "key");
        WidgetState normalizedState = state == null ? WidgetState.NORMAL : state;
        T value = lookup(key, normalizedState);
        if (value != null || normalizedState == WidgetState.NORMAL) {
            return value == null ? fallback : value;
        }
        value = lookup(key, WidgetState.NORMAL);
        return value == null ? fallback : value;
    }

    private <T> T lookup(StyleKey<T> key, WidgetState state) {
        Map<StyleKey<?>, Object> stateValues = values.get(state);
        if (stateValues == null) return null;
        Object value = stateValues.get(key);
        if (value == null) return null;
        return key.type().cast(value);
    }
}
