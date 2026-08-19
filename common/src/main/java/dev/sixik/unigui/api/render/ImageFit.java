package dev.sixik.unigui.api.render;

/**
 * Способ размещения текстуры внутри целевого прямоугольника.
 *
 * @see TexturePlacement#fit(TextureHandle, dev.sixik.unigui.api.math.RectView, ImageFit)
 */
public enum ImageFit {
    /** Растянуть source на весь destination без сохранения пропорций. */
    STRETCH,

    /** Сохранить пропорции и полностью уместить source внутри destination. */
    CONTAIN,

    /** Сохранить пропорции и полностью заполнить destination с возможным crop source. */
    COVER
}