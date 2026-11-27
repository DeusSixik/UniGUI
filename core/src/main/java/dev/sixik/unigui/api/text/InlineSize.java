package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.Locale;

/**
 * Размер inline-контента из строкового маркера.
 *
 * <p>Синтаксис части после {@code @} в маркере {@code {type:argument@size}}:</p>
 * <ul>
 *     <li>{@code 16} — квадрат 16×16 UI-пикселей;</li>
 *     <li>{@code 16x8} (или {@code 16×8}) — 16×8 UI-пикселей;</li>
 *     <li>{@code 1.5em} — квадрат 1.5 размера текста;</li>
 *     <li>{@code 2x1em} — 2×1 размера текста.</li>
 * </ul>
 *
 * @param width ширина в пикселях или em
 * @param height высота в пикселях или em
 * @param em {@code true}, если значения заданы в долях размера текста
 */
@AiDoc(
        summary = "Width/height of inline content parsed from a marker suffix, either in UI pixels or in em.",
        purpose = "Lets marker handlers create spans that are fixed-size or follow the surrounding text size.",
        useWhen = {"Implementing an InlineMarkerHandler that needs the marker's @size suffix."},
        related = {"InlineMarkers", "InlineMarkerHandler", "InlineContentSpan"}
)
public record InlineSize(float width, float height, boolean em) {
    /** Размер по умолчанию: квадрат в 1em, то есть равный размеру текста. */
    public static final InlineSize DEFAULT = new InlineSize(1.0f, 1.0f, true);

    public InlineSize {
        width = Float.isFinite(width) ? Math.max(0.0f, width) : 0.0f;
        height = Float.isFinite(height) ? Math.max(0.0f, height) : 0.0f;
    }

    /**
     * @param width ширина в UI-пикселях
     * @param height высота в UI-пикселях
     * @return абсолютный размер
     */
    public static InlineSize pixels(float width, float height) {
        return new InlineSize(width, height, false);
    }

    /**
     * @param width ширина в em
     * @param height высота в em
     * @return размер, следующий за размером текста
     */
    public static InlineSize ems(float width, float height) {
        return new InlineSize(width, height, true);
    }

    /**
     * Пиксельная ширина при заданном размере текста.
     *
     * @param pixelSize размер текста
     * @return ширина в UI-пикселях
     */
    public float pixelWidth(float pixelSize) {
        return em ? width * pixelSize : width;
    }

    /**
     * Пиксельная высота при заданном размере текста.
     *
     * @param pixelSize размер текста
     * @return высота в UI-пикселях
     */
    public float pixelHeight(float pixelSize) {
        return em ? height * pixelSize : height;
    }

    /**
     * Разбирает суффикс размера маркера.
     *
     * @param value строка вида {@code 16}, {@code 16x8}, {@code 1.5em}, {@code 2x1em}
     * @return размер или {@code null}, если строка не является корректным положительным размером
     */
    public static InlineSize parse(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('×', 'x');
        if (normalized.isEmpty()) return null;
        boolean em = normalized.endsWith("em");
        if (em) normalized = normalized.substring(0, normalized.length() - 2).trim();
        int separator = normalized.indexOf('x');
        try {
            float width;
            float height;
            if (separator < 0) {
                width = height = Float.parseFloat(normalized);
            } else {
                width = Float.parseFloat(normalized.substring(0, separator).trim());
                height = Float.parseFloat(normalized.substring(separator + 1).trim());
            }
            if (!(Float.isFinite(width) && width > 0.0f && Float.isFinite(height) && height > 0.0f)) return null;
            return new InlineSize(width, height, em);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public String toString() {
        String body = width == height ? format(width) : format(width) + "x" + format(height);
        return em ? body + "em" : body;
    }

    private static String format(float value) {
        return value == Math.rint(value) ? Integer.toString((int) value) : Float.toString(value);
    }
}
