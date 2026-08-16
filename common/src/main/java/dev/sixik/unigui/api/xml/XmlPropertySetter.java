package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;

/** Применяет распарсенное значение XML-атрибута к экземпляру виджета. */
@FunctionalInterface
public interface XmlPropertySetter<T extends Widget, V> {
    void set(T widget, V value);
}
