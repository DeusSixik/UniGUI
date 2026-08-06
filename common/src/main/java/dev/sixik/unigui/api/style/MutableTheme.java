package dev.sixik.unigui.api.style;

import java.util.HashMap;
import java.util.Map;

public final class MutableTheme implements Theme {
    private final Map<String, Style> styles = new HashMap<>();
    private Style fallback = Style.EMPTY;
    private long version;

    public MutableTheme fallback(Style fallback) {
        this.fallback = fallback == null ? Style.EMPTY : fallback;
        version++;
        return this;
    }

    public MutableTheme put(String widgetType, Style style) {
        if (widgetType == null || widgetType.isEmpty()) {
            return fallback(style);
        }
        styles.put(widgetType, style == null ? Style.EMPTY : style);
        version++;
        return this;
    }

    @Override
    public long version() {
        long result = version + fallback.version();
        for (Style style : styles.values()) {
            result += style.version();
        }
        return result;
    }

    @Override
    public Style styleFor(String widgetType) {
        if (widgetType == null || widgetType.isEmpty()) return fallback;
        return styles.getOrDefault(widgetType, fallback);
    }
}
