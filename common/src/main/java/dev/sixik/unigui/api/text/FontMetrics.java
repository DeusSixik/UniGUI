package dev.sixik.unigui.api.text;

/**
 * Immutable метрики шрифта для одного requested pixel size.
 *
 * <p>Все значения выражены в логических UI-пикселях. Конструктор нормализует невалидные значения
 * в {@code 0.0f}, чтобы text layout не получал NaN/Infinity из backend font parser'а.</p>
 *
 * @param ascent расстояние от baseline до верхней части glyph area
 * @param descent расстояние от baseline до нижней части glyph area
 * @param lineGap дополнительный интервал между строками
 * @param lineHeight итоговая высота строки
 */
public record FontMetrics(float ascent, float descent, float lineGap, float lineHeight) {
    /** Нормализует метрики в конечные неотрицательные значения. */
    public FontMetrics {
        ascent = finiteNonNegative(ascent);
        descent = finiteNonNegative(descent);
        lineGap = finiteNonNegative(lineGap);
        lineHeight = finiteNonNegative(lineHeight);
    }

    private static float finiteNonNegative(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}