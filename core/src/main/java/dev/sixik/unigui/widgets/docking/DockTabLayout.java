package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Геометрия одной вкладки в полосе листа.
 *
 * <p>Нулевая ширина означает, что вкладка не помещается (прокручена в переполнении) и не рисуется и не
 * ловит курсор.</p>
 *
 * @param index индекс вкладки в листе
 * @param pane панель, которой принадлежит вкладка
 * @param x левая граница в root-координатах
 * @param y верхняя граница в root-координатах
 * @param width ширина вкладки
 * @param height высота вкладки (высота полосы)
 */
@AiDoc(summary = "Internal geometric layout record representing a single rendered tab in a docking tab strip."
)
record DockTabLayout(int index, DockPane pane, float x, float y, float width, float height) {
    /**
     * Проверяет, попадает ли точка во вкладку.
     *
     * @param rootX X точки
     * @param rootY Y точки
     * @return {@code true}, если вкладка видима и содержит точку
     */
    boolean contains(float rootX, float rootY) {
        return width > 0.0f && height > 0.0f
                && rootX >= x && rootX <= x + width
                && rootY >= y && rootY <= y + height;
    }
}
