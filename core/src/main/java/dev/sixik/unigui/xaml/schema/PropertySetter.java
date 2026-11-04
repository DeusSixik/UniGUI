package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Применяет уже разобранное значение к виджету.
 *
 * @param <T> тип виджета
 * @param <V> тип значения
 */
@FunctionalInterface
public interface PropertySetter<T extends Widget, V> {
    /**
     * Устанавливает значение.
     *
     * @param target  виджет
     * @param value   значение, полученное из {@link ValueConverter}
     * @param context контекст загрузки
     */
    void set(T target, V value, LoadContext context);
}
