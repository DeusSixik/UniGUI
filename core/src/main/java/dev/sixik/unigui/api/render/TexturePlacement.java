package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;

import java.util.Objects;

/**
 * Результат размещения source-текстуры в destination bounds.
 *
 * <p>Запись хранит финальные экранные координаты и UV-область. {@link #fit(TextureHandle, RectView, ImageFit)}
 * применяет {@link ImageFit#STRETCH}, {@link ImageFit#CONTAIN} или {@link ImageFit#COVER}, чтобы виджет
 * мог передать backend'у уже готовую geometry/uv пару.</p>
 *
 * @param x X-координата назначения
 * @param y Y-координата назначения
 * @param width ширина назначения
 * @param height высота назначения
 * @param u начальная U-координата source
 * @param v начальная V-координата source
 * @param uWidth ширина source UV области
 * @param vHeight высота source UV области
 */
@AiDoc(
        summary = "Immutable computed destination coordinates and normalized source UV boundaries resulting from an ImageFit calculation.",
        purpose = "Pre-calculates final screen bounds and UV texture coordinates for immediate draw command dispatch.",
        useWhen = {
                "Position an image within widget bounds while honoring aspect ratio via `fit(TextureHandle, RectView, ImageFit)`.",
                "Extract fitted screen coordinates and UV rect to pass directly into `DrawScope#texture`."
        },
        criticalRules = {
                "Screen coordinates (`x`, `y`, `width`, `height`) are in UI points, while UV parameters (`u`, `v`, `uWidth`, `vHeight`) are normalized `0.0..1.0`.",
                "`ImageFit.CONTAIN` centers the image inside destination bounds, letterboxing or pillarboxing empty space.",
                "`ImageFit.COVER` centers and crops the image UVs to completely fill destination bounds without distortion."
        },
        related = {"ImageFit", "TextureHandle", "RectView", "DrawScope"},
        examples = {
                @AiDoc.Example(
                        title = "Compute letterboxed texture placement",
                        order = 10,
                        code = """
                                TexturePlacement p = TexturePlacement.fit(icon, widget.layoutBounds(), ImageFit.CONTAIN);
                                scope.texture(icon, p.x(), p.y(), p.width(), p.height(), p.u(), p.v(), p.uWidth(), p.vHeight(), Paint.NONE);
                                """)
        })
public record TexturePlacement(float x, float y, float width, float height,
                               float u, float v, float uWidth, float vHeight) {
    /**
     * Размещает всю текстуру внутри destination.
     *
     * @param texture текстура
     * @param destination целевые bounds
     * @param fit режим размещения
     * @return рассчитанное размещение
     */
    public static TexturePlacement fit(TextureHandle texture, RectView destination, ImageFit fit) {
        return fit(texture, new MutableRect(0.0f, 0.0f, 1.0f, 1.0f), destination, fit);
    }

    /**
     * Размещает часть текстуры внутри destination.
     *
     * @param texture текстура
     * @param source source UV область в диапазоне {@code 0..1}
     * @param destination целевые bounds
     * @param fit режим размещения
     * @return рассчитанное размещение
     */
    public static TexturePlacement fit(TextureHandle texture, RectView source,
                                       RectView destination, ImageFit fit) {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        ImageFit effectiveFit = fit == null ? ImageFit.STRETCH : fit;

        float x = destination.x();
        float y = destination.y();
        float width = Math.max(0.0f, destination.width());
        float height = Math.max(0.0f, destination.height());
        float u = source.x();
        float v = source.y();
        float uWidth = Math.max(0.0f, source.width());
        float vHeight = Math.max(0.0f, source.height());
        if (width <= 0.0f || height <= 0.0f || uWidth <= 0.0f || vHeight <= 0.0f) {
            return new TexturePlacement(x, y, 0.0f, 0.0f, u, v, 0.0f, 0.0f);
        }

        float sourceWidth = Math.max(1.0f, texture.width()) * uWidth;
        float sourceHeight = Math.max(1.0f, texture.height()) * vHeight;
        float sourceAspect = sourceWidth / sourceHeight;
        float destinationAspect = width / height;

        if (effectiveFit == ImageFit.CONTAIN) {
            if (sourceAspect > destinationAspect) {
                float fittedHeight = width / sourceAspect;
                y += (height - fittedHeight) * 0.5f;
                height = fittedHeight;
            } else {
                float fittedWidth = height * sourceAspect;
                x += (width - fittedWidth) * 0.5f;
                width = fittedWidth;
            }
        } else if (effectiveFit == ImageFit.COVER) {
            if (sourceAspect > destinationAspect) {
                float visibleWidth = uWidth * destinationAspect / sourceAspect;
                u += (uWidth - visibleWidth) * 0.5f;
                uWidth = visibleWidth;
            } else {
                float visibleHeight = vHeight * sourceAspect / destinationAspect;
                v += (vHeight - visibleHeight) * 0.5f;
                vHeight = visibleHeight;
            }
        }

        return new TexturePlacement(x, y, width, height, u, v, uWidth, vHeight);
    }
}
