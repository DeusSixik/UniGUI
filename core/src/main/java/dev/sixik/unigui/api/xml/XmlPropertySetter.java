package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Применяет распарсенное значение XML-атрибута к экземпляру виджета.
 *
 * <p>Setter вызывается после успешного {@link XmlValueParser}. Обычно это ссылка
 * на fluent-setter виджета, например {@code Button::text}. Setter может выполнять
 * дополнительную glue-логику: привязку команды, пересборку texture handle или
 * обновление live color.</p>
 *
 * @param <T> тип виджета, к которому применяется атрибут
 * @param <V> тип распарсенного значения
 */
@FunctionalInterface
public interface XmlPropertySetter<T extends Widget, V> {
    /**
     * Применяет значение к виджету.
     *
     * @param widget экземпляр виджета, созданный XML-loader'ом
     * @param value значение после parser-а
     */
    void set(T widget, V value);
}
