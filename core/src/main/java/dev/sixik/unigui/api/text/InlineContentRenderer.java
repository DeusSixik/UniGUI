package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.TextureHandle;

/**
 * Renderer для одного атомарного inline-span'а внутри {@link RichText}.
 *
 * <p>Renderer вызывается после раскладки текста и получает {@link InlineContentContext} с
 * финальными bounds. Через него можно рисовать texture, icon, shape или любую короткую
 * последовательность draw-команд, не превращая inline-контент в отдельный widget.</p>
 *
 * <pre>{@code
 * RichText text = RichText.builder()
 *         .append("Status ")
 *         .inline("status:ok", "[ok]", 10.0f, 10.0f, (draw, context) ->
 *                 draw.circle(context.x(), context.y(), context.width(), context.height(),
 *                         Paint.fill(context.color())))
 *         .build();
 * }</pre>
 *
 * @see InlineContentSpan
 * @see InlineContentContext
 */
@AiDoc(
        summary = "Procedural rendering callback for drawing InlineContentSpan instances.",
        purpose = "Receives calculated bounding coordinates during layout and executes drawing via DrawScope.",
        useWhen = {"Implementing custom drawing for inline items (e.g. animated icons or item stacks)."},
        related = {"InlineContentSpan", "DrawScope", "InlineContentContext"}
)
@FunctionalInterface
public interface InlineContentRenderer {
    /**
     * Renderer-заглушка для безопасного отсутствия визуального вывода.
     */
    InlineContentRenderer NOOP = (draw, context) -> {
    };

    /**
     * Рисует inline-контент в уже рассчитанных bounds.
     *
     * @param draw    текущий draw scope виджета
     * @param context финальная позиция, размер и paint inline-span'а
     */
    void render(DrawScope draw, InlineContentContext context);

    /**
     * Renderer, рисующий texture без окраски цветом текста.
     *
     * <p>Прозрачность paint'а сохраняется (fade-анимации продолжают работать), а RGB заменяется белым,
     * поэтому цветная иконка внутри цветного текста выглядит как в исходной текстуре.</p>
     *
     * @param texture texture-handle; {@code null} — span занимает место, но ничего не рисует
     * @return renderer texture
     */
    static InlineContentRenderer texture(TextureHandle texture) {
        return texture(texture, false);
    }

    /**
     * Renderer, рисующий texture.
     *
     * @param texture texture-handle; {@code null} — span занимает место, но ничего не рисует
     * @param tint    {@code true} — умножать texture на цвет текста (подходит для одноцветных glyph-иконок),
     *                {@code false} — сохранить цвета texture, учитывая только прозрачность
     * @return renderer texture
     */
    static InlineContentRenderer texture(TextureHandle texture, boolean tint) {
        if (texture == null) return NOOP;
        return (draw, context) -> draw.texture(texture, context.x(), context.y(), context.width(), context.height(),
                tint ? context.tintPaint() : context.untintedPaint());
    }
}
