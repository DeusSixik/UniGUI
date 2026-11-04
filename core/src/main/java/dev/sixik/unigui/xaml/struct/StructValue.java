package dev.sixik.unigui.xaml.struct;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Значение структуры, объявленной в документе (не Java-record): имя типа и значения полей.
 *
 * <p>Объект неизменяем. Изменение поля создаёт новое значение ({@link #with}), как у record.</p>
 */
public final class StructValue {
    private final StructType type;
    private final Map<String, Object> values;

    StructValue(StructType type, Map<String, Object> values) {
        this.type = type;
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** @return тип структуры */
    public StructType type() {
        return type;
    }

    /**
     * @param field имя поля
     * @return значение поля или {@code null}
     * @throws IllegalArgumentException если такого поля нет
     */
    public Object get(String field) {
        if (!values.containsKey(field)) {
            throw new IllegalArgumentException("Struct " + type.name() + " has no field '" + field + "'. Fields: "
                    + values.keySet());
        }
        return values.get(field);
    }

    /**
     * Возвращает копию с изменённым полем.
     *
     * @param field имя поля
     * @param value новое значение; приводится к типу поля
     * @return новое значение структуры
     */
    public StructValue with(String field, Object value) {
        get(field); // проверяет, что поле существует
        Map<String, Object> next = new LinkedHashMap<>(values);
        next.put(field, type.field(field).orElseThrow().type().coerce(value));
        return new StructValue(type, next);
    }

    /** @return значения полей по именам */
    public Map<String, Object> asMap() {
        return values;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StructValue value && type == value.type && values.equals(value.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(type), values);
    }

    @Override
    public String toString() {
        return type.name() + values;
    }
}
