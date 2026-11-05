package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutStyle;

/**
 * Преобразует текущий изменяемый {@code LayoutStyle} и устаревшие {@code LayoutConstraints} в снимки V3.
 */
public final class LayoutStyleMapper {
    /**
     * Запрещает создание экземпляров; только статические преобразования.
     */
    private LayoutStyleMapper() {
    }

    /**
     * Преобразует изменяемый стиль в неизменяемый снимок V3.
     */
    public static LayoutStyleSnapshot from(LayoutStyle style) {
        return LayoutStyleSnapshot.from(style);
    }

    /**
     * Преобразует ограничения старого формата в неизменяемый снимок V3.
     */
    public static LayoutStyleSnapshot from(LayoutConstraints constraints) {
        return LayoutStyleSnapshot.from(constraints);
    }

    /**
     * Преобразует стиль в снимок V3; при {@code null}-стиле использует запасные ограничения.
     */
    public static LayoutStyleSnapshot from(LayoutStyle style, LayoutConstraints fallbackConstraints) {
        if (style != null) {
            return LayoutStyleSnapshot.from(style);
        }
        return LayoutStyleSnapshot.from(fallbackConstraints);
    }
}
