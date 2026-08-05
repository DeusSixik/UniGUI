package dev.sixik.unigui.api.style;

import java.util.Objects;

public final class StyleKey<T> {
    private final String id;
    private final Class<T> type;

    private StyleKey(String id, Class<T> type) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
    }

    public static <T> StyleKey<T> of(String id, Class<T> type) {
        return new StyleKey<>(id, type);
    }

    public String id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }
}
