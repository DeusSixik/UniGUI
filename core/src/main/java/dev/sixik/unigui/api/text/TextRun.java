package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

import dev.sixik.unigui.api.math.ColorView;

import java.util.Locale;

/**
 * Один текстовый run внутри {@link RichText}.
 *
 * <p>Run хранит только текст и style-параметры, которые нужны text engine: font face, размер,
 * цветовой tint, brush-заливку, tracking и transform. Цвет и brush snapshot'ятся в момент создания,
 * чтобы immutable rich-text не зависел от последующих изменений mutable объектов.</p>
 *
 * @param text      текст run'а
 * @param font      font face или {@code null}, если backend должен выбрать default
 * @param pixelSize размер текста в UI-пикселях
 * @param color     цветовой tint run'а или {@code null}, если renderer должен использовать paint виджета
 * @param brush     способ заливки текста или {@code null} для обычного solid paint/run color
 * @param tracking  дополнительный отступ между glyph'ами в UI-пикселях
 * @param transform transform текста перед сохранением run'а
 */
@AiDoc(
        summary = "Homogeneously styled contiguous substring within a RichText instance.",
        purpose = "Combines a text string with font face, pixel size, color, brush, tracking, and text transform.",
        useWhen = {"Inspecting individual styled segments of a RichText model."},
        related = {"RichText", "RichTextSpan", "FontFace", "TextBrush"}
)
public record TextRun(String text, FontFace font, float pixelSize, ColorView color, TextBrush brush,
                      float tracking, TextTransform transform) implements RichTextSpan {
    /**
     * Стандартный размер текста для plain widgets и fallback-измерений.
     */
    public static final float DEFAULT_PIXEL_SIZE = 10.0f;

    /**
     * Нормализует текстовый run: null text становится пустой строкой, размер не падает ниже 1px,
     * tracking не становится отрицательным, а цвет/brush сохраняются immutable snapshot'ами.
     */
    public TextRun {
        text = text == null ? "" : text;
        transform = transform == null ? TextTransform.NONE : transform;
        if (transform == TextTransform.UPPERCASE) {
            text = text.toUpperCase(Locale.ROOT);
        }
        pixelSize = Float.isFinite(pixelSize) ? Math.max(1.0f, pixelSize) : DEFAULT_PIXEL_SIZE;
        tracking = Float.isFinite(tracking) ? Math.max(0.0f, tracking) : 0.0f;
        color = TextBrush.snapshot(color);
        brush = brush == null ? null : brush.copy();
    }

    /**
     * Создаёт run без явного цвета, brush, tracking и transform.
     */
    public TextRun(String text, FontFace font, float pixelSize) {
        this(text, font, pixelSize, null, null, 0.0f, TextTransform.NONE);
    }

    /**
     * Создаёт run с цветом, но без brush, tracking и transform.
     */
    public TextRun(String text, FontFace font, float pixelSize, ColorView color) {
        this(text, font, pixelSize, color, null, 0.0f, TextTransform.NONE);
    }


    /**
     * Создаёт run с цветовым tint и brush-заливкой.
     */
    public TextRun(String text, FontFace font, float pixelSize, ColorView color, TextBrush brush) {
        this(text, font, pixelSize, color, brush, 0.0f, TextTransform.NONE);
    }

    /**
     * Создаёт run с tracking, но без transform.
     */
    public TextRun(String text, FontFace font, float pixelSize, ColorView color, float tracking) {
        this(text, font, pixelSize, color, null, tracking, TextTransform.NONE);
    }

    /**
     * Совместимый конструктор старого полного run API без brush.
     */
    public TextRun(String text, FontFace font, float pixelSize, ColorView color,
                   float tracking, TextTransform transform) {
        this(text, font, pixelSize, color, null, tracking, transform);
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

    /**
     * Сравнивает все параметры стиля, кроме текста.
     *
     * @param other другой run
     * @return {@code true}, если run'ы можно склеить в один без изменения вида
     */
    public boolean hasSameStyle(TextRun other) {
        return other != null
                && font == other.font
                && Float.compare(pixelSize, other.pixelSize) == 0
                && Float.compare(tracking, other.tracking) == 0
                && transform == other.transform
                && java.util.Objects.equals(color, other.color)
                && java.util.Objects.equals(brush, other.brush);
    }

    /**
     * @param text новый текст run'а
     * @return копия с тем же стилем и другим текстом или этот run, если текст не изменился
     */
    public TextRun withText(String text) {
        String normalized = text == null ? "" : text;
        if (normalized.equals(this.text)) return this;
        return new TextRun(normalized, font, pixelSize, color, brush, tracking, transform);
    }

    /**
     * @param font новый font face или {@code null}, чтобы backend выбрал default
     * @return копия с другим шрифтом или этот run, если шрифт тот же
     */
    public TextRun withFont(FontFace font) {
        if (font == this.font) return this;
        return new TextRun(text, font, pixelSize, color, brush, tracking, transform);
    }

    /**
     * @param pixelSize новый размер текста в UI-пикселях
     * @return копия с другим размером или этот run, если размер тот же
     */
    public TextRun withPixelSize(float pixelSize) {
        if (Float.compare(pixelSize, this.pixelSize) == 0) return this;
        return new TextRun(text, font, pixelSize, color, brush, tracking, transform);
    }
}