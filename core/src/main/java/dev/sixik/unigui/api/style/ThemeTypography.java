package dev.sixik.unigui.api.style;

/**
 * Неизменяемая группа типографических визуальных токенов темы.
 */
public final class ThemeTypography {
    private final float bodySize;
    private final float smallSize;
    private final float titleSize;
    private final int bodyWeight;

    private ThemeTypography(Builder builder) {
        bodySize = positive(builder.bodySize, "bodySize");
        smallSize = positive(builder.smallSize, "smallSize");
        titleSize = positive(builder.titleSize, "titleSize");
        bodyWeight = builder.bodyWeight;
        if (bodyWeight < 100 || bodyWeight > 900) {
            throw new IllegalArgumentException("bodyWeight must be between 100 and 900");
        }
    }

    /**
     * @return builder с базовыми значениями типографики
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return размер основного текста
     */
    public float bodySize() {
        return bodySize;
    }

    /**
     * @return размер вторичного текста
     */
    public float smallSize() {
        return smallSize;
    }

    /**
     * @return размер заголовка
     */
    public float titleSize() {
        return titleSize;
    }

    /**
     * @return насыщенность основного текста
     */
    public int bodyWeight() {
        return bodyWeight;
    }

    /**
     * @return builder, инициализированный значениями группы
     */
    public Builder toBuilder() {
        return new Builder().bodySize(bodySize).smallSize(smallSize).titleSize(titleSize).bodyWeight(bodyWeight);
    }

    /**
     * Изменяемый сборщик типографических токенов до публикации снимка.
     */
    public static final class Builder {
        private float bodySize = 14.0f;
        private float smallSize = 12.0f;
        private float titleSize = 18.0f;
        private int bodyWeight = 400;

        /**
         * Задаёт размер основного текста.
         */
        public Builder bodySize(float value) {
            bodySize = value;
            return this;
        }

        /**
         * Задаёт размер вторичного текста.
         */
        public Builder smallSize(float value) {
            smallSize = value;
            return this;
        }

        /**
         * Задаёт размер заголовка.
         */
        public Builder titleSize(float value) {
            titleSize = value;
            return this;
        }

        /**
         * Задаёт насыщенность основного текста.
         */
        public Builder bodyWeight(int value) {
            bodyWeight = value;
            return this;
        }

        /**
         * @return неизменяемая группа типографики
         */
        public ThemeTypography build() {
            return new ThemeTypography(this);
        }
    }

    private static float positive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
        return value;
    }
}
