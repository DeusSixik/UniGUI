package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;

/**
 * Контекст рендера одного inline-span'а после текстовой раскладки.
 *
 * <p>{@link dev.sixik.unigui.impl.text.TextEngine} сначала измеряет строку и определяет
 * финальную позицию inline-контента, а затем передаёт сюда готовые bounds. Renderer не должен
 * самостоятельно пересчитывать переносы, baseline или alignment: он просто рисует в выданном
 * прямоугольнике через {@link dev.sixik.unigui.api.render.DrawScope}.</p>
 *
 * @param span исходный inline-span
 * @param bounds финальные root/screen bounds для рисования
 * @param lineHeight высота строки, в которую помещён span
 * @param paint базовый paint текущего текста; renderer может использовать его цвет как tint
 */
public record InlineContentContext(
        InlineContentSpan span,
        RectView bounds,
        float lineHeight,
        Paint paint
) {
    /**
     * Возвращает стабильный id inline-контента.
     *
     * @return id span'а или пустая строка, если span отсутствует
     */
    public String id() {
        return span == null ? "" : span.id();
    }

    /**
     * @return X-координата финальных bounds
     */
    public float x() {
        return bounds == null ? 0.0f : bounds.x();
    }

    /**
     * @return Y-координата финальных bounds
     */
    public float y() {
        return bounds == null ? 0.0f : bounds.y();
    }

    /**
     * @return ширина inline-контента в UI-пикселях
     */
    public float width() {
        return bounds == null ? 0.0f : bounds.width();
    }

    /**
     * @return высота inline-контента в UI-пикселях
     */
    public float height() {
        return bounds == null ? 0.0f : bounds.height();
    }

    /**
     * Удобный доступ к цвету базового paint'а.
     *
     * @return цвет текста или {@code null}, если paint отсутствует
     */
    public ColorView color() {
        return paint == null ? null : paint.color();
    }
}
