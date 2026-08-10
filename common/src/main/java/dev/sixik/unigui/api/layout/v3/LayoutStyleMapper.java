package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutStyle;

/**
 * Преобразует текущий изменяемый {@code LayoutStyle} и устаревшие {@code LayoutConstraints} в снимки V3.
 */
public final class LayoutStyleMapper {
    /**
     * Создаёт экземпляр {@code LayoutStyleMapper} и подготавливает его начальное состояние.
     */
    private LayoutStyleMapper() {
    }

    /**
     * Создаёт или восстанавливает объект через операцию {@code from}.
     */
    public static LayoutStyleSnapshot from(LayoutStyle style) {
        return LayoutStyleSnapshot.from(style);
    }

    /**
     * Создаёт или восстанавливает объект через операцию {@code from}.
     */
    public static LayoutStyleSnapshot from(LayoutConstraints constraints) {
        return LayoutStyleSnapshot.from(constraints);
    }

    /**
     * Создаёт или восстанавливает объект через операцию {@code from}.
     */
    public static LayoutStyleSnapshot from(LayoutStyle style, LayoutConstraints fallbackConstraints) {
        if (style != null) {
            return LayoutStyleSnapshot.from(style);
        }
        return LayoutStyleSnapshot.from(fallbackConstraints);
    }
}
