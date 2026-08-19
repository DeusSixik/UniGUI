package dev.sixik.unigui.api.animation;

/**
 * Небольшой набор easing-функций для retained UI-анимаций.
 *
 * <p>Easing получает линейный прогресс {@code t} от 0 до 1 и возвращает преобразованный прогресс.
 * Вход автоматически ограничивается диапазоном 0..1, поэтому случайные отрицательные значения,
 * {@code NaN} или значения выше 1 не пробрасываются дальше в расчёт transition'а.</p>
 *
 * <p>Пример: быстрый hover-переход.</p>
 *
 * <pre>{@code
 * TransitionSpec hover = TransitionSpec.of(0.16f, AnimationEasing.EASE_OUT);
 * button.animateOpacity(0.85f, hover);
 * }</pre>
 *
 * @see TransitionSpec
 * @see FloatTransition
 */
public enum AnimationEasing {
    /** Линейный прогресс без ускорения и замедления. */
    LINEAR {
        @Override
        public float apply(float t) {
            return clamp01(t);
        }
    },
    /** Медленный старт, быстрый конец. */
    EASE_IN {
        @Override
        public float apply(float t) {
            t = clamp01(t);
            return t * t;
        }
    },
    /** Быстрый старт, мягкое замедление к концу. */
    EASE_OUT {
        @Override
        public float apply(float t) {
            t = clamp01(t);
            return 1.0f - (1.0f - t) * (1.0f - t);
        }
    },
    /** Мягкий старт и мягкий конец. */
    EASE_IN_OUT {
        @Override
        public float apply(float t) {
            t = clamp01(t);
            return t < 0.5f
                    ? 2.0f * t * t
                    : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 2.0f) * 0.5f;
        }
    };

    /**
     * Применяет easing к линейному прогрессу.
     *
     * @param t прогресс 0..1; значения вне диапазона будут ограничены
     * @return easing-прогресс 0..1
     */
    public abstract float apply(float t);

    /**
     * Ограничивает значение диапазоном 0..1.
     *
     * @param value исходное значение
     * @return безопасное значение прогресса
     */
    static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}