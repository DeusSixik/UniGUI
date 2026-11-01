package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;

/**
 * Контекст рисования декора модуля. Рисование идёт в экранных координатах; методы {@link #x(float)},
 * {@link #y(float)} и {@link #scale(float)} переводят node-local world-единицы в экран с учётом зума.
 */
public interface NodePaintContext {
    /**
     * @return draw scope графа
     */
    DrawScope draw();

    /**
     * @return рисуемая нода
     */
    NodeGraphItem item();

    /**
     * @return стиль ноды
     */
    NodeStyle style();

    /**
     * @return текущий зум графа
     */
    float zoom();

    /**
     * @return {@code true}, если нода под курсором
     */
    boolean hovered();

    /**
     * @return {@code true}, если нода выделена
     */
    boolean selected();

    /**
     * @param localX X относительно ноды в world-единицах
     * @return X в координатах root
     */
    float x(float localX);

    /**
     * @param localY Y относительно ноды в world-единицах
     * @return Y в координатах root
     */
    float y(float localY);

    /**
     * @param length длина в world-единицах
     * @return длина в пикселях экрана
     */
    float scale(float length);
}
