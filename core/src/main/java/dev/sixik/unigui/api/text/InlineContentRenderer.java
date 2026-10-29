package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.render.DrawScope;

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
@FunctionalInterface
public interface InlineContentRenderer {
    /** Renderer-заглушка для безопасного отсутствия визуального вывода. */
    InlineContentRenderer NOOP = (draw, context) -> {
    };

    /**
     * Рисует inline-контент в уже рассчитанных bounds.
     *
     * @param draw текущий draw scope виджета
     * @param context финальная позиция, размер и paint inline-span'а
     */
    void render(DrawScope draw, InlineContentContext context);
}
