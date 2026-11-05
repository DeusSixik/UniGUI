package dev.sixik.unigui.api.layout.v3;

import java.util.Objects;

/** Стабильный идентификатор, связывающий результат компоновки с виджетом или синтетическим узлом. */
public record LayoutNodeId(String value) {
    public LayoutNodeId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("LayoutNodeId cannot be empty");
        }
    }

    public static LayoutNodeId of(String value) {
        return new LayoutNodeId(value);
    }

    public static LayoutNodeId root() {
        return new LayoutNodeId("root");
    }

    @Override
    public String toString() {
        return value;
    }
}
