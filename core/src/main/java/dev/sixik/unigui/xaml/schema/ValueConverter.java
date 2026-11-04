package dev.sixik.unigui.xaml.schema;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Превращает текст значения свойства в типизированное значение.
 *
 * <p>Конвертер сам знает свой {@link #kind()}, поэтому инспектору редактора не нужно угадывать тип поля по
 * имени атрибута, как это делал прежний {@code XmlAttributeDescriptor}.</p>
 *
 * @param <V> тип результата
 */
public final class ValueConverter<V> {
    private final Class<V> type;
    private final ValueKind kind;
    private final BiFunction<String, LoadContext, V> function;

    private ValueConverter(Class<V> type, ValueKind kind, BiFunction<String, LoadContext, V> function) {
        this.type = type;
        this.kind = kind;
        this.function = function;
    }

    /**
     * Создаёт конвертер, которому контекст загрузки не нужен.
     *
     * @param type     тип результата (в боксированном виде)
     * @param kind     редакторская категория
     * @param function разбор текста
     * @param <V>      тип результата
     * @return конвертер
     */
    public static <V> ValueConverter<V> of(Class<V> type, ValueKind kind, Function<String, V> function) {
        return new ValueConverter<>(type, kind, (text, context) -> function.apply(text));
    }

    /**
     * Создаёт конвертер, использующий контекст загрузки (например, резолвер текстур).
     *
     * @param type     тип результата
     * @param kind     редакторская категория
     * @param function разбор текста
     * @param <V>      тип результата
     * @return конвертер
     */
    public static <V> ValueConverter<V> withContext(Class<V> type, ValueKind kind,
                                                   BiFunction<String, LoadContext, V> function) {
        return new ValueConverter<>(type, kind, function);
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
        return function.apply(text, context == null ? LoadContext.DEFAULT : context);
    }

    /**
     * Возвращает тип результата.
     *
     * @return боксированный тип
     */
    public Class<V> type() {
        return type;
    }

    /**
     * Возвращает редакторскую категорию значений.
     *
     * @return категория
     */
    public ValueKind kind() {
        return kind;
    }
}
