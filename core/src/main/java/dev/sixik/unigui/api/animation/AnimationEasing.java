package dev.sixik.unigui.api.animation;

/**
 * Стандартные именованные easing-функции для retained UI-анимаций.
 *
 * <p>Easing получает линейный прогресс {@code t} от 0 до 1 и возвращает преобразованный прогресс.
 * Вход автоматически ограничивается диапазоном 0..1, поэтому случайные отрицательные значения,
 * {@code NaN} или значения выше 1 не пробрасываются дальше в расчёт transition'а.</p>
 *
 * <p>Константы {@code EASE_*} сохранены для обратной совместимости и соответствуют quad-кривым.
 * Для новых стилей и анимаций предпочтительнее явно выбирать семейство: {@code QUAD_*},
 * {@code CUBIC_*} или {@code EXPO_*}.</p>
 *
 * <pre>{@code
 * TransitionSpec hover = TransitionSpec.of(0.16f, AnimationEasing.CUBIC_OUT);
 * button.animateOpacity(0.85f, hover);
 * }</pre>
 *
 * @see Easing#cubicBezier(float, float, float, float)
 * @see TransitionSpec
 * @see FloatTransition
 */
public enum AnimationEasing implements Easing {
    /**
     * Linear progress without acceleration or deceleration.
     */
    LINEAR {
        @Override
        public float apply(float t) {
            return Easing.clamp01(t);
        }
    },
    /**
     * Compatibility alias for {@link #QUAD_IN}.
     */
    EASE_IN {
        @Override
        public float apply(float t) {
            return QUAD_IN.apply(t);
        }
    },
    /**
     * Compatibility alias for {@link #QUAD_OUT}.
     */
    EASE_OUT {
        @Override
        public float apply(float t) {
            return QUAD_OUT.apply(t);
        }
    },
    /**
     * Compatibility alias for {@link #QUAD_IN_OUT}.
     */
    EASE_IN_OUT {
        @Override
        public float apply(float t) {
            return QUAD_IN_OUT.apply(t);
        }
    },
    /**
     * Quadratic curve: slow start, fast end.
     */
    QUAD_IN {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t * t;
        }
    },
    /**
     * Quadratic curve: fast start, gentle end.
     */
    QUAD_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return 1.0f - (1.0f - t) * (1.0f - t);
        }
    },
    /**
     * Quadratic curve: gentle start and gentle end.
     */
    QUAD_IN_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t < 0.5f ? 2.0f * t * t : 1.0f - square(-2.0f * t + 2.0f) * 0.5f;
        }
    },
    /**
     * Cubic curve: slow start, fast end.
     */
    CUBIC_IN {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t * t * t;
        }
    },
    /**
     * Cubic curve: fast start, gentle end.
     */
    CUBIC_OUT {
        @Override
        public float apply(float t) {
            t = 1.0f - Easing.clamp01(t);
            return 1.0f - t * t * t;
        }
    },
    /**
     * Cubic curve: gentle start and gentle end.
     */
    CUBIC_IN_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t < 0.5f ? 4.0f * t * t * t : 1.0f - cube(-2.0f * t + 2.0f) * 0.5f;
        }
    },
    /**
     * Exponential curve: very slow start, fast end.
     */
    EXPO_IN {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t <= 0.0f ? 0.0f : (float) Math.pow(2.0, 10.0f * t - 10.0f);
        }
    },
    /**
     * Exponential curve: fast start, very gentle end.
     */
    EXPO_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t >= 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0f * t);
        }
    },
    /**
     * Exponential curve: very gentle start and end.
     */
    EXPO_IN_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            if (t <= 0.0f || t >= 1.0f) return t;
            return t < 0.5f
                    ? (float) Math.pow(2.0, 20.0f * t - 10.0f) * 0.5f
                    : (2.0f - (float) Math.pow(2.0, -20.0f * t + 10.0f)) * 0.5f;
        }
    };

    private static float square(float value) {
        return value * value;
    }

    private static float cube(float value) {
        return value * value * value;
    }
}