package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Добавляет дочерний виджет к родителю с учётом правил конкретного XML-типа.
 *
 * <p>Policy используется для обычных child-элементов и property-child слотов.
 * Контейнеры обычно вызывают {@code addChild}, а специальные виджеты могут
 * проверять ограничения: например, что {@code ScrollView} получил только один
 * {@code Content} child.</p>
 *
 * @param <T> тип родительского виджета
 */
@FunctionalInterface
public interface XmlChildPolicy<T extends Widget> {
    /**
     * Добавляет или назначает дочерний виджет.
     *
     * @param parent родитель, уже созданный XML-loader'ом
     * @param child дочерний виджет, созданный из вложенного XML-элемента
     */
    void addChild(T parent, Widget child);
}
