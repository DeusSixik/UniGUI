package dev.sixik.unigui.api.style;

/**
 * Describes how a named style should be rendered.
 *
 * <p>Declarative styles are editable data. Custom renderer backends are kept as
 * an explicit escape hatch for effects that cannot be expressed as properties.</p>
 */
public sealed interface StyleBackend permits StyleBackend.Declarative, StyleBackend.Custom {
    Style style();

    static StyleBackend declarative(Style style) {
        return new Declarative(style);
    }

    static StyleBackend custom(String rendererId, Style style) {
        return new Custom(rendererId, style);
    }

    default boolean customRenderer() {
        return false;
    }

    default String rendererId() {
        return "";
    }

    record Declarative(Style style) implements StyleBackend {
        public Declarative {
            style = style == null ? Style.EMPTY : style;
        }
    }

    record Custom(String rendererId, Style style) implements StyleBackend {
        public Custom {
            rendererId = normalize(rendererId);
            style = style == null ? Style.EMPTY : style;
        }

        @Override
        public boolean customRenderer() {
            return !rendererId.isEmpty();
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
