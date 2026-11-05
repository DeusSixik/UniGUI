package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.math.ColorView;

/**
 * Значения стиля, разрешённые один раз за проход отрисовки.
 *
 * <p>Виджеты данных читают десятки style-свойств. Чтобы не обходить цепочку темы и локальных стилей на каждый
 * штрих, виджет заполняет этот объект в начале {@code render()} и дальше работает только с его полями. Объект
 * принадлежит одному виджету и переиспользуется между кадрами, поэтому заполнение не выделяет память.</p>
 *
 * <p>Единицы: размеры и толщины — UI-пиксели, цвета — компоненты в диапазоне [0, 1].</p>
 */
final class PlotPalette {
    /** Цвет фона виджета; используется, только если {@link #backgroundVisible} равно {@code true}. */
    ColorView background;
    /** Цвет рамки виджета; используется, только если {@link #borderVisible} равно {@code true}. */
    ColorView border;
    /** Рисуется ли фон. */
    boolean backgroundVisible;
    /** Рисуется ли рамка. */
    boolean borderVisible;
    /** Толщина рамки в UI-пикселях. */
    float borderWidth = 1.0f;
    /** Радиус скругления фона и рамки в UI-пикселях. */
    float radius;

    /** Цвет подписей: значений у точек и столбцов, делений осей, названий узлов. */
    ColorView text;
    /** Размер шрифта подписей в UI-пикселях. */
    float textSize = 9.0f;

    /** Цвет линии ряда. */
    ColorView line;
    /** Толщина линии ряда в UI-пикселях. */
    float lineWidth = 1.5f;
    /** Цвет заливки под линией ряда. */
    ColorView fill;
    /** Рисуется ли заливка под линией. */
    boolean fillVisible = true;
    /** Цвет маркера точки. */
    ColorView point;
    /** Цвет маркера наведённой точки и направляющей. */
    ColorView pointHover;
    /** Радиус маркера точки в UI-пикселях. */
    float pointRadius = 3.0f;

    /** Цвет осей и засечек. */
    ColorView axis;
    /** Цвет линий сетки. */
    ColorView grid;
    /** Рисуется ли сетка и подписи оси значений. */
    boolean gridVisible = true;
    /** Цвет столбца. */
    ColorView bar;
    /** Цвет наведённого столбца. */
    ColorView barHover;
    /** Зазор между соседними столбцами в UI-пикселях. */
    float barGap = 4.0f;
    /** Радиус скругления свободного конца столбца в UI-пикселях. */
    float barRadius = 2.0f;

    /** Цвет фона подсказки. */
    ColorView tooltipBackground;
    /** Цвет рамки подсказки. */
    ColorView tooltipBorder;
    /** Цвет текста подсказки. */
    ColorView tooltipText;
}
