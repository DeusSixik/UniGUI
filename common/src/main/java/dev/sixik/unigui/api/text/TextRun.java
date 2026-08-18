package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;
import java.util.Locale;

/**
 * Один текстовый run внутри {@link RichText}.
 *
 * <p>Run хранит только текст и style-параметры, которые нужны text engine: font face, размер,
 * цвет, tracking и transform. Цвет snapshot'ится в момент создания, чтобы immutable rich-text не
 * зависел от последующих изменений mutable color объекта.</p>
 *
 * @param text текст run'а
 * @param font font face или {@code null}, если backend должен выбрать default
 * @param pixelSize размер текста в UI-пикселях
 * @param color цвет run'а или {@code null}, если renderer должен использовать paint виджета
 * @param tracking дополнительный отступ между glyph'ами в UI-пикселях
 * @param transform transform текста перед сохранением run'а
 */
public record TextRun(String text, FontFace font, float pixelSize, ColorView color,
                      float tracking, TextTransform transform) implements RichTextSpan {
    /** Стандартный размер текста для plain widgets и fallback-измерений. */
    public static final float DEFAULT_PIXEL_SIZE = 10.0f;

    /**
     * Нормализует текстовый run: null text становится пустой строкой, размер не падает ниже 1px,
     * tracking не становится отрицательным, а цвет сохраняется immutable snapshot'ом.
     */
    public TextRun {
        text = text == null ? "" : text;
        transform = transform == null ? TextTransform.NONE : transform;
        if (transform == TextTransform.UPPERCASE) {
            text = text.toUpperCase(Locale.ROOT);
        }
        pixelSize = Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : DEFAULT_PIXEL_SIZE;
        tracking = Float.isFinite(tracking) ? Math.max(0.0f, tracking) : 0.0f;
        color = color == null ? null : new SnapshotColor(color.r(), color.g(), color.b(), color.a());
    }

    /**
     * Создаёт run без явного цвета, tracking и transform.
     */
    public TextRun(String text, FontFace font, float pixelSize) {
        this(text, font, pixelSize, null, 0.0f, TextTransform.NONE);
    }

    /**
     * Создаёт run с цветом, но без tracking и transform.
     */
    public TextRun(String text, FontFace font, float pixelSize, ColorView color) {
        this(text, font, pixelSize, color, 0.0f, TextTransform.NONE);
    }

    /**
     * Создаёт run с tracking, но без transform.
     */
    public TextRun(String text, FontFace font, float pixelSize, ColorView color, float tracking) {
        this(text, font, pixelSize, color, tracking, TextTransform.NONE);
    }

    /**
     * @return собственный текст run'а как plain fallback
     */
    @Override
    public String fallbackText() {
        return text;
    }

    /**
     * @return {@code true}, если run не содержит текста
     */
    @Override
    public boolean isEmpty() {
        return text.isEmpty();
    }

    private record SnapshotColor(float r, float g, float b, float a) implements ColorView {
    }
}
