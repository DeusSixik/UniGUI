package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.docs.AiDoc;

import dev.sixik.unigui.api.math.ColorView;

/**
 * Одноцветная text brush-заливка.
 *
 * <p>Класс полезен там, где API принимает общий {@link TextBrush}. Для обычного текста чаще
 * проще оставить {@link TextRun#color()} или paint color.</p>
 *
 * @param color immutable snapshot цвета brush'а
 */
@AiDoc(
        summary = "Solid single-color implementation of TextBrush.",
        purpose = "Fills text runs with a uniform color.",
        useWhen = {"Supplying a solid color brush to RichText."},
        related = {"TextBrush", "RichText"}
)
public record SolidTextBrush(ColorView color) implements TextBrush {
    public SolidTextBrush {
        color = TextBrush.snapshotOrWhite(color);
    }

    @Override
    public TextBrush copy() {
        return new SolidTextBrush(color);
    }
}