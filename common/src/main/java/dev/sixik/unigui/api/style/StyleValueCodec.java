package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Преобразует декларативное style-значение между XML-текстом и типизированным Java-значением.
 *
 * <p>Codec подключается к {@link StylePropertyDescriptor}. Это позволяет одному editor/XML pipeline
 * одинаково работать с числами, цветами, enum'ами и будущими пользовательскими типами.</p>
 *
 * @param <T> Java-тип значения свойства
 */
public interface StyleValueCodec<T> {
    /**
     * Парсит строку в типизированное значение.
     *
     * @param value строка из ввод XML/editor
     * @return типизированное значение
     */
    T parse(String value);

    /**
     * Форматирует значение обратно в XML/editor строку.
     *
     * @param value значение свойства
     * @return строковое представление
     */
    String format(T value);

    /**
     * Создаёт codec из двух функций.
     *
     * @param parser функция парсинга
     * @param formatter функция форматирования
     * @return новый codec
     * @param <T> Java-тип значения
     */
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

    /**
     * @return codec для обычных строк
     */
    static StyleValueCodec<String> string() {
        return of(value -> value, value -> value);
    }

    /**
     * @return codec для {@link Float} значений
     */
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

    /**
     * @return codec для RGBA/HEX цветов
     */
    static StyleValueCodec<ColorView> color() {
        return of(MutableColor::fromHex, value -> {
            if (value == null) return "";
            return MutableColor.rgba(value.r(), value.g(), value.b(), value.a()).toHexString(true);
        });
    }

    /**
     * Создаёт codec для enum-значений.
     *
     * @param type enum-класс
     * @return codec, принимающий {@code kebab-case}, {@code dot.case} и {@code UPPER_CASE}
     * @param <E> enum-тип
     */
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