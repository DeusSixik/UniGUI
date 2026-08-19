package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Converts a declarative style value between XML text and a typed Java value. */
public interface StyleValueCodec<T> {
    T parse(String value);

    String format(T value);

    static <T> StyleValueCodec<T> of(Function<String, T> parser, Function<T, String> formatter) {
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(formatter, "formatter");
        return new StyleValueCodec<>() {
            @Override
            public T parse(String value) {
                return parser.apply(value == null ? "" : value.trim());
            }

            @Override
            public String format(T value) {
                return value == null ? "" : formatter.apply(value);
            }
        };
    }

    static StyleValueCodec<String> string() {
        return of(value -> value, value -> value);
    }

    static StyleValueCodec<Float> floatingPoint() {
        return of(value -> {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Expected floating point value.");
            }
            return Float.parseFloat(value.trim());
        }, value -> {
            if (value == null) return "";
            if (!Float.isFinite(value)) return "0";
            String text = Float.toString(value);
            return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
        });
    }

    static StyleValueCodec<ColorView> color() {
        return of(MutableColor::fromHex, value -> {
            if (value == null) return "";
            return MutableColor.rgba(value.r(), value.g(), value.b(), value.a()).toHexString(true);
        });
    }

    static <E extends Enum<E>> StyleValueCodec<E> enumCodec(Class<E> type) {
        Objects.requireNonNull(type, "type");
        return of(value -> {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Expected " + type.getSimpleName() + " value.");
            }
            String normalized = value.trim()
                    .replace('-', '_')
                    .replace('.', '_')
                    .toUpperCase(Locale.ROOT);
            return Enum.valueOf(type, normalized);
        }, value -> value == null ? "" : value.name());
    }
}