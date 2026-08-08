package dev.sixik.unigui.api.layout.v3;

import java.util.Objects;

/** Stable identifier used to connect a layout result back to its widget or synthetic node. */
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
