package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

/**
 * Вершина произвольного {@link DrawMesh}.
 *
 * <p>Цвет snapshot'ится в конструкторе, чтобы последующие изменения mutable color-объекта не меняли
 * уже записанную draw-команду.</p>
 *
 * @param x X-координата вершины
 * @param y Y-координата вершины
 * @param u U-координата текстуры
 * @param v V-координата текстуры
 * @param color цвет вершины
 */
public record DrawVertex(float x, float y, float u, float v, ColorView color) {
    /** Нормализует цвет вершины в immutable snapshot. */
    public DrawVertex {
        color = color == null ? new MutableColor() : new MutableColor(color.r(), color.g(), color.b(), color.a());
    }

    /**
     * Создаёт вершину без явных texture coordinates.
     *
     * @param x X-координата
     * @param y Y-координата
     * @param color цвет вершины
     */
    public DrawVertex(float x, float y, ColorView color) {
        this(x, y, 0.0f, 0.0f, color);
    }

    /**
     * Создаёт независимую копию вершины.
     *
     * @return новая вершина с теми же значениями
     */
    public DrawVertex copy() {
        return new DrawVertex(x, y, u, v, color);
    }
}