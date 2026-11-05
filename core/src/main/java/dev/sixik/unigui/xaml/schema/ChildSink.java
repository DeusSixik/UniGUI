package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Правило добавления дочернего виджета к родителю конкретного типа.
 *
 * <p>Именно здесь живут особенности вроде «ScrollView принимает ровно одного ребёнка» или «после добавления
 * ребёнка в TabControl нужно применить отложенные мутации».</p>
 *
 * @param <T> тип родителя
 */
@FunctionalInterface
public interface ChildSink<T extends Widget> {
    /**
     * Добавляет ребёнка.
     *
     * @param parent родитель
     * @param child  уже созданный и настроенный дочерний виджет
     * @throws IllegalArgumentException если родитель не принимает такого ребёнка
     */
    void add(T parent, Widget child);
}
