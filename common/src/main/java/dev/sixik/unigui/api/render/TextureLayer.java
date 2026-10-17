package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;

import java.util.Objects;

/**
 * Один текстурный слой поверхности: текстура + её размещение и тонировка.
 *
 * <p>Слой — изменяемый value-объект: handle, fit, UV source, nine-slice рамки
 * и tint живут вместе, чтобы пачка текстур рисовалась одним проходом без
 * размазывания настроек по параллельным массивам. {@code null}-текстура —
 * пустой слой, рендер его пропускает.</p>
 */
public final class TextureLayer {
    private TextureHandle texture;
    private final MutableRect source = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private ImageFit fit = ImageFit.STRETCH;
    private SliceBorders sliceBorders = SliceBorders.ZERO;
    private boolean visible = true;

    /**
     * Создаёт слой с текстурой и дефолтным размещением (весь UV, stretch, белый tint).
     *
     * @param texture handle текстуры или {@code null} для пустого слоя
     */
    public TextureLayer(TextureHandle texture) {
        this.texture = texture;
    }

    /** @return handle текстуры или {@code null} */
    public TextureHandle texture() {
        return texture;
    }

    /**
     * Задаёт handle текстуры.
     *
     * @param texture handle или {@code null}, чтобы опустошить слой
     * @return этот слой для chained-настройки
     */
    public TextureLayer texture(TextureHandle texture) {
        this.texture = texture;
        return this;
    }

    /** @return live UV source-rect */
    public MutableRect source() {
        return source;
    }

    /**
     * Задаёт UV source-rect.
     *
     * @param u начальная U-координата
     * @param v начальная V-координата
     * @param width ширина UV-области
     * @param height высота UV-области
     * @return этот слой для chained-настройки
     */
    public TextureLayer source(float u, float v, float width, float height) {
        source.set(u, v, width, height);
        return this;
    }

    /** @return live tint-цвет слоя */
    public MutableColor tint() {
        return tint;
    }

    /**
     * Задаёт tint-цвет слоя.
     *
     * @param color новый tint; {@code null} игнорируется
     * @return этот слой для chained-настройки
     */
    public TextureLayer tint(dev.sixik.unigui.api.math.ColorView color) {
        if (color != null) tint.set(color);
        return this;
    }

    /** @return fit-режим слоя */
    public ImageFit fit() {
        return fit;
    }

    /**
     * Задаёт fit-режим слоя.
     *
     * @param fit режим; {@code null} трактуется как {@link ImageFit#STRETCH}
     * @return этот слой для chained-настройки
     */
    public TextureLayer fit(ImageFit fit) {
        this.fit = fit == null ? ImageFit.STRETCH : fit;
        return this;
    }

    /** @return рамки nine-slice в пикселях текстуры */
    public SliceBorders sliceBorders() {
        return sliceBorders;
    }

    /**
     * Задаёт рамки nine-slice.
     *
     * @param borders рамки; {@code null} сбрасывает к нулевым
     * @return этот слой для chained-настройки
     */
    public TextureLayer sliceBorders(SliceBorders borders) {
        this.sliceBorders = borders == null ? SliceBorders.ZERO : borders;
        return this;
    }

    /**
     * Задаёт одинаковые рамки nine-slice со всех сторон.
     *
     * @param border ширина рамки в пикселях текстуры
     * @return этот слой для chained-настройки
     */
    public TextureLayer sliceBorders(float border) {
        return sliceBorders(SliceBorders.all(border));
    }

    /** @return {@code true}, если слой участвует в рендере */
    public boolean visible() {
        return visible;
    }

    /**
     * Показывает или прячет слой без удаления из пачки.
     *
     * @param visible {@code true}, чтобы рисовать слой
     * @return этот слой для chained-настройки
     */
    public TextureLayer visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /** @return {@code true}, если слой нечего рисовать */
    public boolean isEmpty() {
        return texture == null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TextureLayer other)) return false;
        return Objects.equals(texture, other.texture)
                && sameRect(source, other.source)
                && sameColor(tint, other.tint)
                && fit == other.fit
                && Objects.equals(sliceBorders, other.sliceBorders)
                && visible == other.visible;
    }

    @Override
    public int hashCode() {
        return Objects.hash(texture,
                Float.floatToIntBits(source.x()), Float.floatToIntBits(source.y()),
                Float.floatToIntBits(source.width()), Float.floatToIntBits(source.height()),
                Float.floatToIntBits(tint.r()), Float.floatToIntBits(tint.g()),
                Float.floatToIntBits(tint.b()), Float.floatToIntBits(tint.a()),
                fit, sliceBorders, visible);
    }

    private static boolean sameRect(MutableRect left, MutableRect right) {
        return Float.compare(left.x(), right.x()) == 0
                && Float.compare(left.y(), right.y()) == 0
                && Float.compare(left.width(), right.width()) == 0
                && Float.compare(left.height(), right.height()) == 0;
    }

    private static boolean sameColor(MutableColor left, MutableColor right) {
        return Float.compare(left.r(), right.r()) == 0
                && Float.compare(left.g(), right.g()) == 0
                && Float.compare(left.b(), right.b()) == 0
                && Float.compare(left.a(), right.a()) == 0;
    }

    @Override
    public String toString() {
        return "TextureLayer(" + (texture == null ? "empty" : texture.id()) + " " + fit + ")";
    }
}
