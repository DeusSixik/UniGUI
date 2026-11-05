package dev.sixik.unigui.api.animation;

/**
 * Преобразует линейный прогресс анимации в прогресс с заданным характером движения.
 *
 * <p>Easing отвечает только за распределение времени и не зависит от типа анимируемого значения.
 * Один easing можно использовать с {@link FloatTransition}, {@link IntTransition} и любым
 * {@link Tween}. Интерполяцию самих значений выполняют {@link FloatInterpolator},
 * {@link IntInterpolator} или {@link Interpolator}.</p>
 *
 * <p>Стандартный именованный набор кривых находится в {@link AnimationEasing}. Для кривой,
 * заданной контрольными точками, используйте {@link #cubicBezier(float, float, float, float)}.
 * Результат намеренно не ограничивается: elastic, back и другие пользовательские функции могут
 * выходить за диапазон 0..1 для создания overshoot-эффекта.</p>
 *
 * <pre>{@code
 * Easing cubicIn = progress -> progress * progress * progress;
 * Easing material = Easing.cubicBezier(0.4f, 0.0f, 0.2f, 1.0f);
 * TransitionSpec transition = TransitionSpec.of(0.35f, material);
 * }</pre>
 */
@FunctionalInterface
public interface Easing {
    /** Число итераций Newton-Raphson при поиске параметра cubic-bezier. */
    int BEZIER_NEWTON_ITERATIONS = 8;
    /** Число итераций bisection fallback для устойчивого решения cubic-bezier. */
    int BEZIER_BISECTION_ITERATIONS = 12;
    /** Порог, при котором производная считается слишком малой для Newton-Raphson. */
    float BEZIER_DERIVATIVE_EPSILON = 0.000001f;

    /**
     * Преобразует линейный прогресс.
     *
     * @param progress линейный прогресс в диапазоне 0..1
     * @return преобразованный прогресс; может выходить за диапазон 0..1
     */
    float apply(float progress);

    /**
     * Создаёт cubic-bezier easing по двум внутренним контрольным точкам.
     *
     * <p>Начальная и конечная точки всегда равны {@code (0, 0)} и {@code (1, 1)}. Координаты
     * {@code x1}/{@code x2} ограничены диапазоном 0..1, поэтому X-координата кривой монотонна и
     * для каждого progress существует однозначный параметр. Поиск параметра выполняется сначала
     * методом Newton-Raphson, затем устойчивым bisection fallback.</p>
     *
     * <p>Y-координаты могут выходить за диапазон 0..1: это позволяет описывать overshoot-кривые.
     * Невалидные значения приводят к {@link IllegalArgumentException} при создании easing, а не во
     * время рендеринга.</p>
     *
     * @param x1 X первой внутренней контрольной точки в диапазоне 0..1
     * @param y1 Y первой внутренней контрольной точки
     * @param x2 X второй внутренней контрольной точки в диапазоне 0..1
     * @param y2 Y второй внутренней контрольной точки
     * @return неизменяемая easing-функция
     * @throws IllegalArgumentException если координаты не являются конечными или X вне диапазона
     */
    static Easing cubicBezier(float x1, float y1, float x2, float y2) {
        validateBezierCoordinate("x1", x1, true);
        validateBezierCoordinate("y1", y1, false);
        validateBezierCoordinate("x2", x2, true);
        validateBezierCoordinate("y2", y2, false);
        return new CubicBezierEasing(x1, y1, x2, y2);
    }

    /**
     * Ограничивает значение диапазоном 0..1.
     *
     * @param value исходное значение
     * @return конечный прогресс; невалидные значения превращаются в 0
     */
    static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static void validateBezierCoordinate(String name, float value, boolean horizontal) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Cubic-bezier coordinate '" + name + "' must be finite.");
        }
        if (horizontal && (value < 0.0f || value > 1.0f)) {
            throw new IllegalArgumentException("Cubic-bezier coordinate '" + name + "' must be in range 0..1.");
        }
    }

    /** Реализация cubic-bezier, которая не создаёт временные объекты при {@link #apply(float)}. */
    final class CubicBezierEasing implements Easing {
        private final float ax;
        private final float bx;
        private final float cx;
        private final float ay;
        private final float by;
        private final float cy;

        private CubicBezierEasing(float x1, float y1, float x2, float y2) {
            cx = 3.0f * x1;
            bx = 3.0f * (x2 - x1) - cx;
            ax = 1.0f - cx - bx;
            cy = 3.0f * y1;
            by = 3.0f * (y2 - y1) - cy;
            ay = 1.0f - cy - by;
        }

        @Override
        public float apply(float progress) {
            float x = clamp01(progress);
            if (x <= 0.0f || x >= 1.0f) return x;

            float parameter = x;
            boolean converged = false;
            for (int iteration = 0; iteration < BEZIER_NEWTON_ITERATIONS; iteration++) {
                float error = sampleX(parameter) - x;
                if (Math.abs(error) <= 0.00001f) {
                    converged = true;
                    break;
                }
                float derivative = sampleXDerivative(parameter);
                if (Math.abs(derivative) < BEZIER_DERIVATIVE_EPSILON) break;

                float next = parameter - error / derivative;
                if (next < 0.0f || next > 1.0f) break;
                parameter = next;
            }
            if (!converged && Math.abs(sampleX(parameter) - x) > 0.00001f) {
                parameter = solveByBisection(x);
            }
            return sampleY(parameter);
        }

        private float solveByBisection(float x) {
            float lower = 0.0f;
            float upper = 1.0f;
            for (int iteration = 0; iteration < BEZIER_BISECTION_ITERATIONS; iteration++) {
                float middle = (lower + upper) * 0.5f;
                if (sampleX(middle) < x) lower = middle;
                else upper = middle;
            }
            return (lower + upper) * 0.5f;
        }

        private float sampleX(float t) { return ((ax * t + bx) * t + cx) * t; }
        private float sampleY(float t) { return ((ay * t + by) * t + cy) * t; }
        private float sampleXDerivative(float t) { return (3.0f * ax * t + 2.0f * bx) * t + cx; }
    }
}