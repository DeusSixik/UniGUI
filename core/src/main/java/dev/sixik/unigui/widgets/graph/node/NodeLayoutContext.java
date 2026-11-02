package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphPort;

/**
 * Сервисы раскладки, которые граф даёт модулю. Все координаты — world-единицы относительно левого
 * верхнего угла ноды, то есть не зависят от зума и панорамирования.
 */
@WikiPage(title = "NodeLayoutContext", slug = "node-layout-context", category = "<key:graph.category>", summary = "<key:node.layout.context.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.layout.context.overview>", order = 10)
public interface NodeLayoutContext {
    /**
     * @return нода, которую раскладывают
     */
    NodeGraphItem item();

    /**
     * @return стиль ноды
     */
    NodeStyle style();

    /**
     * @param widget уже измеренный виджет модуля
     * @return его предпочитаемая ширина вместе с margin
     */
    float preferredWidth(Widget widget);

    /**
     * @param widget уже измеренный виджет модуля
     * @return его предпочитаемая высота вместе с margin
     */
    float preferredHeight(Widget widget);

    /**
     * Размещает виджет модуля.
     *
     * @param widget виджет
     * @param x      левая граница относительно ноды
     * @param y      верхняя граница относительно ноды
     * @param width  ширина
     * @param height высота
     */
    void place(Widget widget, float x, float y, float width, float height);

    /**
     * Привязывает порт к точке ноды: именно сюда приходит провод.
     *
     * @param port порт модуля
     * @param x    X относительно ноды
     * @param y    Y относительно ноды
     */
    void anchor(NodeGraphPort port, float x, float y);
}
