package dev.sixik.unigui.api.style;

/**
 * Неизменяемая группа геометрических визуальных токенов темы.
 */
public final class ThemeShapes {
    private final float radiusSm;
    private final float radiusMd;
    private final float radiusLg;
    private final float borderWidth;

    private ThemeShapes(Builder builder) {
        radiusSm = nonNegative(builder.radiusSm, "radiusSm");
        radiusMd = nonNegative(builder.radiusMd, "radiusMd");
        radiusLg = nonNegative(builder.radiusLg, "radiusLg");
        borderWidth = nonNegative(builder.borderWidth, "borderWidth");
    }

    /**
     * @return builder с базовыми значениями формы
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return builder, инициализированный значениями группы
     */
    public Builder toBuilder() {
        return new Builder().radiusSm(radiusSm).radiusMd(radiusMd).radiusLg(radiusLg).borderWidth(borderWidth);
    }

    /**
     * @return малый радиус
     */
    public float radiusSm() {
        return radiusSm;
    }

    /**
     * @return основной радиус
     */
    public float radiusMd() {
        return radiusMd;
    }

    /**
     * @return большой радиус
     */
    public float radiusLg() {
        return radiusLg;
    }

    /**
     * @return стандартная толщина границы
     */
    public float borderWidth() {
        return borderWidth;
    }

    /**
     * Изменяемый сборщик геометрических токенов до публикации снимка.
     */
    public static final class Builder {
        private float radiusSm = 3.0f;
        private float radiusMd = 5.0f;
        private float radiusLg = 10.0f;
        private float borderWidth = 1.0f;

        /**
         * Задаёт малый радиус.
         */
        public Builder radiusSm(float value) {
            radiusSm = value;
            return this;
        }

        /**
         * Задаёт основной радиус.
         */
        public Builder radiusMd(float value) {
            radiusMd = value;
            return this;
        }

        /**
         * Задаёт большой радиус.
         */
        public Builder radiusLg(float value) {
            radiusLg = value;
            return this;
        }

        /**
         * Задаёт толщину границы.
         */
        public Builder borderWidth(float value) {
            borderWidth = value;
            return this;
        }

        /**
         * @return неизменяемая группа форм
         */
        public ThemeShapes build() {
            return new ThemeShapes(this);
        }
    }

    private static float nonNegative(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
        return value;
    }
}
