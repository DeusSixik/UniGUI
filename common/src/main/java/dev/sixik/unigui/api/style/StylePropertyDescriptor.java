package dev.sixik.unigui.api.style;

import java.util.Objects;

/** Metadata for a declarative style property exposed to XML and editor inspectors. */
public record StylePropertyDescriptor<T>(StyleKey<T> key,
                                         String displayName,
                                         String category,
                                         T defaultValue,
                                         StyleValueCodec<T> codec,
                                         String description) {
    public StylePropertyDescriptor {
        key = Objects.requireNonNull(key, "key");
        displayName = normalize(displayName, key.id());
        category = normalize(category, "General");
        codec = Objects.requireNonNull(codec, "codec");
        description = normalize(description, "");
    }

    public T parse(String value) {
        return codec.parse(value);
    }

    public String format(Object value) {
        if (value == null) return "";
        return codec.format(key.type().cast(value));
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}