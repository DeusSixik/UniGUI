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
    /** Линейный прогресс без ускорения и замедления. */
    LINEAR {
        @Override
        public float apply(float t) { return Easing.clamp01(t); }
    },
    /** Совместимое имя для {@link #QUAD_IN}. */
    EASE_IN {
        @Override
        public float apply(float t) { return QUAD_IN.apply(t); }
    },
    /** Совместимое имя для {@link #QUAD_OUT}. */
    EASE_OUT {
        @Override
        public float apply(float t) { return QUAD_OUT.apply(t); }
    },
    /** Совместимое имя для {@link #QUAD_IN_OUT}. */
    EASE_IN_OUT {
        @Override
        public float apply(float t) { return QUAD_IN_OUT.apply(t); }
    },
    /** Квадратичная кривая: медленный старт, быстрый конец. */
    QUAD_IN {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t * t;
        }
    },
    /** Квадратичная кривая: быстрый старт, мягкий конец. */
    QUAD_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return 1.0f - (1.0f - t) * (1.0f - t);
        }
    },
    /** Квадратичная кривая: мягкий старт и мягкий конец. */
    QUAD_IN_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t < 0.5f ? 2.0f * t * t : 1.0f - square(-2.0f * t + 2.0f) * 0.5f;
        }
    },
    /** Кубическая кривая: медленный старт, быстрый конец. */
    CUBIC_IN {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t * t * t;
        }
    },
    /** Кубическая кривая: быстрый старт, мягкий конец. */
    CUBIC_OUT {
        @Override
        public float apply(float t) {
            t = 1.0f - Easing.clamp01(t);
            return 1.0f - t * t * t;
        }
    },
    /** Кубическая кривая: мягкий старт и мягкий конец. */
    CUBIC_IN_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t < 0.5f ? 4.0f * t * t * t : 1.0f - cube(-2.0f * t + 2.0f) * 0.5f;
        }
    },
    /** Экспоненциальная кривая: очень медленный старт, быстрый конец. */
    EXPO_IN {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t <= 0.0f ? 0.0f : (float) Math.pow(2.0, 10.0f * t - 10.0f);
        }
    },
    /** Экспоненциальная кривая: быстрый старт, очень мягкий конец. */
    EXPO_OUT {
        @Override
        public float apply(float t) {
            t = Easing.clamp01(t);
            return t >= 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0f * t);
        }
    },
    /** Экспоненциальная кривая: очень мягкий старт и конец. */
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

    private static float square(float value) { return value * value; }
    private static float cube(float value) { return value * value * value; }
}