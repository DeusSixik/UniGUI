package dev.sixik.unigui.xaml.component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Именованные значения: аргументы создания компонента и аргументы его событий.
 *
 * <p>Объект неизменяем. Имена и типы значений проверяет контракт компонента, а не этот класс, поэтому один и тот же
 * тип годится и для параметров, и для событий.</p>
 */
public final class Args {
    private static final Args EMPTY = new Args(Map.of());

    private final Map<String, Object> values;

    private Args(Map<String, Object> values) {
        this.values = values;
    }

    /**
     * @return пустой набор
     */
    public static Args empty() {
        return EMPTY;
    }

    /**
     * Создаёт набор из пар «имя, значение».
     *
     * @param pairs чередующиеся имя (строка) и значение
     * @return набор
     * @throws IllegalArgumentException если пар нечётное число, имя не строка или имя повторяется
     */
    public static Args of(Object... pairs) {
        if (pairs.length == 0) return EMPTY;
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Args.of expects name/value pairs");
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (!(pairs[i] instanceof String name) || name.isBlank()) {
                throw new IllegalArgumentException("Args name at position " + i + " must be a non-blank string");
            }
            if (map.containsKey(name)) throw new IllegalArgumentException("Duplicate argument '" + name + "'");
            map.put(name, pairs[i + 1]);
        }
        return new Args(Collections.unmodifiableMap(map));
    }

    /**
     * Создаёт набор из отображения.
     *
     * @param values имена и значения
     * @return набор
     */
    public static Args of(Map<String, ?> values) {
        return values.isEmpty() ? EMPTY : new Args(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
    }

    /**
     * @return {@code true}, если набор пуст
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * @param name имя
     * @return {@code true}, если значение задано (даже {@code null})
     */
    public boolean has(String name) {
        return values.containsKey(name);
    }

    /**
     * @param name имя
     * @return значение или {@code null}
     */
    public Object get(String name) {
        return values.get(name);
    }

    /**
     * Возвращает значение нужного класса.
     *
     * @param name имя
     * @param type ожидаемый класс
     * @param <T>  тип значения
     * @return значение или {@code null}
     * @throws ClassCastException если значение другого класса
     */
    public <T> T get(String name, Class<T> type) {
        return type.cast(values.get(name));
    }

    /**
     * @return неизменяемое отображение
     */
    public Map<String, Object> asMap() {
        return values;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Args args && values.equals(args.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
