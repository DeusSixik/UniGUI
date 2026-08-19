package dev.sixik.unigui.api.style;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ordered style stack where later layers override earlier layers. */
public final class ResolvedStyle implements Style {
    private final List<Style> layers;

    public ResolvedStyle(List<Style> layers) {
        if (layers == null || layers.isEmpty()) {
            this.layers = List.of();
            return;
        }
        List<Style> normalized = new ArrayList<>(layers.size());
        for (Style layer : layers) {
            if (layer != null && layer != Style.EMPTY) {
                normalized.add(layer);
            }
        }
        this.layers = List.copyOf(normalized);
    }

    public static Style of(List<Style> layers) {
        ResolvedStyle resolved = new ResolvedStyle(layers);
        return resolved.layers.isEmpty() ? Style.EMPTY : resolved;
    }

    public List<Style> layers() {
        return layers;
    }

    @Override
    public long version() {
        long version = 0L;
        for (Style layer : layers) {
            version += layer.version();
        }
        return version;
    }

    @Override
    public <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
        T value = fallback;
        for (Style layer : layers) {
            value = layer.get(key, state, value);
        }
        return value;
    }

    @Override
    public Map<WidgetState, Map<StyleKey<?>, Object>> values() {
        if (layers.isEmpty()) return Map.of();
        EnumMap<WidgetState, Map<StyleKey<?>, Object>> merged = new EnumMap<>(WidgetState.class);
        for (Style layer : layers) {
            for (Map.Entry<WidgetState, Map<StyleKey<?>, Object>> stateEntry : layer.values().entrySet()) {
                merged.computeIfAbsent(stateEntry.getKey(), ignored -> new LinkedHashMap<>())
                        .putAll(stateEntry.getValue());
            }
        }
        EnumMap<WidgetState, Map<StyleKey<?>, Object>> snapshot = new EnumMap<>(WidgetState.class);
        for (Map.Entry<WidgetState, Map<StyleKey<?>, Object>> entry : merged.entrySet()) {
            snapshot.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(snapshot);
    }
}