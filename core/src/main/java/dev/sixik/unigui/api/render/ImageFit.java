package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;

/**
 * Способ размещения текстуры внутри целевого прямоугольника.
 *
 * @see TexturePlacement#fit(TextureHandle, dev.sixik.unigui.api.math.RectView, ImageFit)
 */
@AiDoc(
        summary = "Aspect-ratio and sizing strategies for fitting textures inside arbitrary target rectangle bounds.",
        purpose = "Governs how source image content conforms to widget layout dimensions.",
        useWhen = {
                "`STRETCH`: Scale image to completely fill destination bounds without preserving aspect ratio.",
                "`CONTAIN`: Scale image preserving aspect ratio until it is completely visible inside destination bounds (letterboxing).",
                "`COVER`: Scale image preserving aspect ratio until destination bounds are completely filled (cropping overflow).",
                "`NINE_SLICE`: Deconstruct texture into 4 fixed corners, 4 stretching edges, and 1 center area using slice borders."
        },
        criticalRules = {
                "`NINE_SLICE` requires valid `SliceBorders` defined on the widget visual surface.",
                "`CONTAIN` and `COVER` calculate aspect ratios using `TextureHandle#width()` and `height()`."
        },
        related = {"TexturePlacement", "TextureHandle", "SliceBorders", "NineSlice"},
        examples = {
                @AiDoc.Example(
                        title = "Compute contained placement",
                        order = 10,
                        code = """
                                TexturePlacement p = TexturePlacement.fit(icon, widgetBounds, ImageFit.CONTAIN);
                                scope.texture(icon, p.x(), p.y(), p.width(), p.height(), Paint.NONE);
                                """)
        })
public enum ImageFit {
    /**
     * Растянуть source на весь destination без сохранения пропорций.
     */
    STRETCH,

    /**
     * Сохранить пропорции и полностью уместить source внутри destination.
     */
    CONTAIN,

    /**
     * Сохранить пропорции и полностью заполнить destination с возможным crop source.
     */
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
