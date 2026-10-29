package dev.sixik.unigui.api.style;

import java.util.Objects;

/**
 * Типизированный ключ одного style-свойства.
 *
 * <p>Ключ состоит из строкового id, который используется в XML/редакторе, и Java-типа,
 * который защищает runtime от записи значения неподходящего класса. Два ключа считаются
 * одинаковыми только если совпадают и id, и тип.</p>
 *
 * <pre>{@code
 * StyleKey<Float> radius = StyleKey.of("radius", Float.class);
 * }</pre>
 *
 * @param <T> Java-тип значения свойства
 */
public final class StyleKey<T> {
    private final String id;
    private final Class<T> type;

    private StyleKey(String id, Class<T> type) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Создаёт новый ключ свойства.
     *
     * @param id стабильный строковый id свойства
     * @param type Java-тип значения
     * @return типизированный ключ
     * @param <T> Java-тип значения
     */
    public static <T> StyleKey<T> of(String id, Class<T> type) {
        return new StyleKey<>(id, type);
    }

    /** @return строковый id свойства для XML, StylePack и инспектора */
    public String id() {
        return id;
    }

    /** @return Java-тип значения свойства */
    public Class<T> type() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StyleKey<?> styleKey)) return false;
        return id.equals(styleKey.id) && type.equals(styleKey.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "StyleKey[" + id + "]";
    }
}