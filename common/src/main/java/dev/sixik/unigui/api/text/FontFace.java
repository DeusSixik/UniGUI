package dev.sixik.unigui.api.text;

/**
 * Backend-independent handle одного font face.
 *
 * <p>Face описывает данные шрифта и умеет отдавать метрики для requested pixel size. Владение
 * GPU atlas, glyph cache и backend texture ресурсами остаётся внутри renderer'а или text engine:
 * публичный API не должен протаскивать эти детали наружу.</p>
 *
 * @see FontRegistry
 * @see FontMetrics
 */
public interface FontFace {
    /**
     * Возвращает стабильный id шрифта.
     *
     * @return id, под которым face зарегистрирован в {@link FontRegistry}
     */
    String id();

    /**
     * Возвращает метрики шрифта для указанного размера.
     *
     * @param pixelSize размер текста в логических UI-пикселях
     * @return ascent/descent/line-height для данного размера
     */
    FontMetrics metrics(float pixelSize);

    /**
     * Измеряет advance одного Unicode code point.
     *
     * <p>Метод принимает code point, а не Java {@code char}, чтобы surrogate pairs и emoji-like
     * символы не ломали cursor/selection math.</p>
     *
     * @param codePoint Unicode code point
     * @param pixelSize размер текста в логических UI-пикселях
     * @return горизонтальный advance glyph'а
     */
    float advance(int codePoint, float pixelSize);
}