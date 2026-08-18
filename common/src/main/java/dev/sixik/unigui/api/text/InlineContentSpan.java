package dev.sixik.unigui.api.text;

/**
 * Атомарный non-text span внутри {@link RichText}.
 *
 * <p>Span занимает фиксированную ширину и высоту в строке, имеет plain fallback и рисуется
 * через {@link InlineContentRenderer}. Он не участвует в дереве widgets, не получает события
 * ввода и не хранит layout-состояние. Для text engine это один неделимый layout-атом, похожий
 * на один code point.</p>
 *
 * <p>{@code fallbackText} нужен для clipboard, search, debug text и сериализации. По умолчанию
 * используется Unicode object replacement character.</p>
 *
 * @param id стабильный id для debug/diagnostics
 * @param fallbackText plain-text fallback span'а
 * @param width ширина в UI-пикселях
 * @param height высота в UI-пикселях
 * @param alignment вертикальное выравнивание внутри строки
 * @param renderer renderer, который получит финальные bounds после layout
 */
public record InlineContentSpan(
        String id,
        String fallbackText,
        float width,
        float height,
        InlineContentAlignment alignment,
        InlineContentRenderer renderer
) implements RichTextSpan {
    /** Стандартный fallback для atomic inline object. */
    public static final String DEFAULT_FALLBACK_TEXT = "\uFFFC";

    /**
     * Нормализует входные значения span'а.
     *
     * <p>Размеры приводятся к неотрицательным значениям, пустой fallback заменяется на
     * {@link #DEFAULT_FALLBACK_TEXT}, а отсутствующий renderer превращается в безопасный NOOP.</p>
     */
    public InlineContentSpan {
        id = id == null ? "" : id;
        fallbackText = normalizeFallback(fallbackText);
        width = sanitizeSize(width);
        height = sanitizeSize(height);
        alignment = alignment == null ? InlineContentAlignment.CENTER : alignment;
        renderer = renderer == null ? InlineContentRenderer.NOOP : renderer;
    }

    /**
     * Создаёт square inline-span.
     *
     * @param id стабильный id span'а
     * @param fallbackText plain fallback
     * @param size ширина и высота в UI-пикселях
     * @param renderer renderer inline-контента
     */
    public InlineContentSpan(String id, String fallbackText, float size, InlineContentRenderer renderer) {
        this(id, fallbackText, size, size, InlineContentAlignment.CENTER, renderer);
    }

    /**
     * @return {@code true}, если span не имеет полезной площади для layout
     */
    @Override
    public boolean isEmpty() {
        return width <= 0.0f && height <= 0.0f;
    }

    private static String normalizeFallback(String fallbackText) {
        return fallbackText == null || fallbackText.isEmpty() ? DEFAULT_FALLBACK_TEXT : fallbackText;
    }

    private static float sanitizeSize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
