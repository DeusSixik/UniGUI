package dev.sixik.unigui.api.layout;

public final class LayoutContext {
    /**
     * Доступная ширина родителя для раскладки; бесконечность означает отсутствие предела.
     */
    private final float availableWidth;
    /**
     * Доступная высота родителя для раскладки; бесконечность означает отсутствие предела.
     */
    private final float availableHeight;

    /**
     * Создаёт контекст с доступными шириной и высотой родителя.
     */
    public LayoutContext(float availableWidth, float availableHeight) {
        this.availableWidth = sanitizeAvailable(availableWidth);
        this.availableHeight = sanitizeAvailable(availableHeight);
    }

    /**
     * Возвращает доступную ширину.
     */
    public float availableWidth() {
        return availableWidth;
    }

    /**
     * Возвращает доступную высоту.
     */
    public float availableHeight() {
        return availableHeight;
    }

    /**
     * Нормализует доступный размер: отрицательные в ноль, бесконечность означает отсутствие предела.
     */
    private static float sanitizeAvailable(float value) {
        if (Float.isNaN(value)) return 0.0f;
        return Float.isFinite(value) ? Math.max(0.0f, value) : Float.POSITIVE_INFINITY;
    }
}
