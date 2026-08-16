package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.widget.Widget;

/** Добавляет дочерний виджет к родителю с учетом правил конкретного типа виджета. */
@FunctionalInterface
public interface XmlChildPolicy<T extends Widget> {
    void addChild(T parent, Widget child);
}
