package dev.sixik.unigui.api.style;

import java.util.Objects;

/**
 * Метаданные декларативного style-свойства для XML и editor inspector'а.
 *
 * <p>Descriptor связывает типизированный {@link StyleKey}, человекочитаемые подписи и
 * {@link StyleValueCodec}. Благодаря этому редактор может показать свойство в нужной категории,
 * распарсить XML-значение и сохранить его обратно без знания конкретного Java-типа.</p>
 *
 * @param key типизированный ключ свойства
 * @param displayName имя свойства для UI инспектора
 * @param category группа свойства в инспекторе
 * @param defaultValue значение по умолчанию для новых стилей
 * @param codec codec для XML/string представления
 * @param description краткое описание свойства для tooltip/help UI
 * @param <T> Java-тип значения свойства
 */
public record StylePropertyDescriptor<T>(StyleKey<T> key,
                                         String displayName,
                                         String category,
                                         T defaultValue,
                                         StyleValueCodec<T> codec,
                                         String description) {
    /** Нормализует подписи и проверяет обязательные поля descriptor'а. */
    public StylePropertyDescriptor {
        key = Objects.requireNonNull(key, "key");
        displayName = normalize(displayName, key.id());
        category = normalize(category, "General");
        codec = Objects.requireNonNull(codec, "codec");
        description = normalize(description, "");
    }

    /**
     * Парсит строковое значение из ввод XML/editor.
     *
     * @param value строковое значение
     * @return типизированное значение свойства
     */
    public T parse(String value) {
        return codec.parse(value);
    }

    /**
     * Форматирует значение свойства для вывод XML/editor.
     *
     * @param value значение свойства
     * @return строковое представление или пустая строка для {@code null}
     */
    public String format(Object value) {
        if (value == null) return "";
        return codec.format(key.type().cast(value));
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}