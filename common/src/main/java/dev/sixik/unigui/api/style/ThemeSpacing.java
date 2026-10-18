package dev.sixik.unigui.api.style;

/**
 * Неизменяемая группа визуальных отступов и размеров хрома темы.
 */
public final class ThemeSpacing {
    private final float controlHeight;
    private final float controlPaddingX;
    private final float controlGap;
    private final float popupGap;

    private ThemeSpacing(Builder builder) {
        controlHeight = nonNegative(builder.controlHeight, "controlHeight");
        controlPaddingX = nonNegative(builder.controlPaddingX, "controlPaddingX");
        controlGap = nonNegative(builder.controlGap, "controlGap");
        popupGap = nonNegative(builder.popupGap, "popupGap");
    }

    /**
     * @return builder с базовыми значениями отступов
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return стандартная высота контрола
     */
    public float controlHeight() {
        return controlHeight;
    }

    /**
     * @return горизонтальный внутренний отступ контрола
     */
    public float controlPaddingX() {
        return controlPaddingX;
    }

    /**
     * @return расстояние между визуальными частями контрола
     */
    public float controlGap() {
        return controlGap;
    }

    /**
     * @return расстояние между контролом и popup
     */
    public float popupGap() {
        return popupGap;
    }

    /**
     * @return builder, инициализированный значениями группы
     */
    public Builder toBuilder() {
        return new Builder().controlHeight(controlHeight).controlPaddingX(controlPaddingX)
                .controlGap(controlGap).popupGap(popupGap);
    }

    /**
     * Изменяемый сборщик токенов отступов до публикации снимка.
     */
    public static final class Builder {
        private float controlHeight = 36.0f;
        private float controlPaddingX = 12.0f;
        private float controlGap = 8.0f;
        private float popupGap = 6.0f;

        /**
         * Задаёт стандартную высоту контрола.
         */
        public Builder controlHeight(float value) {
            controlHeight = value;
            return this;
        }

        /**
         * Задаёт горизонтальный внутренний отступ контрола.
         */
        public Builder controlPaddingX(float value) {
            controlPaddingX = value;
            return this;
        }

        /**
         * Задаёт расстояние между визуальными частями контрола.
         */
        public Builder controlGap(float value) {
            controlGap = value;
            return this;
        }

        /**
         * Задаёт расстояние между контролом и popup.
         */
        public Builder popupGap(float value) {
            popupGap = value;
            return this;
        }

        /**
         * @return неизменяемая группа отступов
         */
        public ThemeSpacing build() {
            return new ThemeSpacing(this);
        }
    }

    private static float nonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return value;
    }
}
