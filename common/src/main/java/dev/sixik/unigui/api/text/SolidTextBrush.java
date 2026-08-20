package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;

/**
 * Одноцветная text brush-заливка.
 *
 * <p>Класс полезен там, где API принимает общий {@link TextBrush}. Для обычного текста чаще
 * проще оставить {@link TextRun#color()} или paint color.</p>
 *
 * @param color immutable snapshot цвета brush'а
 */
public record SolidTextBrush(ColorView color) implements TextBrush {
    public SolidTextBrush {
        color = TextBrush.snapshotOrWhite(color);
    }

    @Override
    public TextBrush copy() {
        return new SolidTextBrush(color);
    }
}