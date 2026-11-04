package dev.sixik.unigui.xaml.schema;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Двусторонний конвертер значения свойства: текст в значение и значение в текст.
 *
 * <p>Конвертер сам знает свой {@link #kind()}, поэтому инспектору редактора не нужно угадывать тип поля по
 * имени атрибута. Обратное преобразование ({@link #format}) нужно, чтобы сохранять живой UI обратно в
 * документ; для него действует инвариант {@code parse(format(v)) == v}.</p>
 *
 * @param <V> тип результата
 */
public final class ValueConverter<V> {
    private final Class<V> type;
    private final ValueKind kind;
    private final BiFunction<String, LoadContext, V> parser;
    private final Function<Object, String> formatter;

    private ValueConverter(Class<V> type, ValueKind kind, BiFunction<String, LoadContext, V> parser,
                           Function<Object, String> formatter) {
        this.type = type;
        this.kind = kind;
        this.parser = parser;
        this.formatter = formatter;
    }

    /**
     * Создаёт конвертер только для чтения документа.
     *
     * @param type   тип результата (в боксированном виде)
     * @param kind   редакторская категория
     * @param parser разбор текста
     * @param <V>    тип результата
     * @return конвертер, у которого {@link #canFormat()} ложно
     */
    public static <V> ValueConverter<V> of(Class<V> type, ValueKind kind, Function<String, V> parser) {
        return new ValueConverter<>(type, kind, (text, context) -> parser.apply(text), null);
    }

    /**
     * Создаёт двусторонний конвертер.
     *
     * @param type      тип результата
     * @param kind      редакторская категория
     * @param parser    разбор текста
     * @param formatter запись значения в текст; принимает любую реализацию типа (например, любой
     *                  {@code ColorView}), поэтому получает {@code Object}
     * @param <V>       тип результата
     * @return конвертер
     */
    public static <V> ValueConverter<V> of(Class<V> type, ValueKind kind, Function<String, V> parser,
                                          Function<Object, String> formatter) {
        return new ValueConverter<>(type, kind, (text, context) -> parser.apply(text), formatter);
    }

    /**
     * Создаёт конвертер, использующий контекст загрузки при разборе (например, резолвер текстур).
     *
     * @param type      тип результата
     * @param kind      редакторская категория
     * @param parser    разбор текста
     * @param formatter запись значения в текст
     * @param <V>       тип результата
     * @return конвертер
     */
    public static <V> ValueConverter<V> withContext(Class<V> type, ValueKind kind,
                                                   BiFunction<String, LoadContext, V> parser,
                                                   Function<Object, String> formatter) {
        return new ValueConverter<>(type, kind, parser, formatter);
    }

    /**
     * Разбирает текст.
     *
     * @param text    значение свойства
     * @param context контекст загрузки
     * @return значение
     * @throws IllegalArgumentException если текст не соответствует типу
     */
    public V convert(String text, LoadContext context) {
        return parser.apply(text, context == null ? LoadContext.DEFAULT : context);
    }

    /**
     * Записывает значение в текст, который {@link #convert} разберёт обратно в то же значение.
     *
     * @param value значение; {@code null} не допускается
     * @return текст
     * @throws UnsupportedOperationException если конвертер не умеет записывать значения
     */
    public String format(Object value) {
        if (formatter == null) {
            throw new UnsupportedOperationException("Converter for " + type.getSimpleName() + " cannot format values");
        }
        return formatter.apply(value);
    }

    /** @return {@code true}, если значения можно записать обратно в текст */
    public boolean canFormat() {
        return formatter != null;
    }

    /** @return тип результата (боксированный) */
    public Class<V> type() {
        return type;
    }

    /** @return редакторская категория значений */
    public ValueKind kind() {
        return kind;
    }
}
