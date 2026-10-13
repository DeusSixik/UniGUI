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
    COVER,

    /**
     * Nine-slice растяжение: углы без деформации, края вдоль одной оси, центр по обеим.
     *
     * <p>Рамки берутся из slice-настроек виджета в пикселях текстуры
     * (см. {@link dev.sixik.unigui.api.widget.visual.Surface#sliceBorders()}).
     * Работает через {@link DrawScope#nineSlice(NineSlice, float, float, float, float, Paint)}.</p>
     */
    NINE_SLICE
}