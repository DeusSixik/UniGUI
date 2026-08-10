package dev.sixik.unigui.api.layout;

public final class LayoutContext {
    /**
     * Хранит текстовое или идентификационное значение {@code availableWidth}.
     */
    private final float availableWidth;
    /**
     * Хранит числовой параметр {@code availableHeight}, используемый в расчётах, вводе или отрисовке.
     */
    private final float availableHeight;

    /**
     * Создаёт экземпляр {@code LayoutContext} и подготавливает его начальное состояние.
     */
    public LayoutContext(float availableWidth, float availableHeight) {
        this.availableWidth = sanitizeAvailable(availableWidth);
        this.availableHeight = sanitizeAvailable(availableHeight);
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code availableWidth}.
     */
    public float availableWidth() {
        return availableWidth;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code availableHeight}.
     */
    public float availableHeight() {
        return availableHeight;
    }

    /**
     * Приводит входное значение к безопасному или допустимому диапазону.
     */
    private static float sanitizeAvailable(float value) {
        if (Float.isNaN(value)) return 0.0f;
        return Float.isFinite(value) ? Math.max(0.0f, value) : Float.POSITIVE_INFINITY;
    }
}
