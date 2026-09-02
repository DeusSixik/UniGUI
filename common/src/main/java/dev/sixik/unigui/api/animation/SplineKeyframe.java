package dev.sixik.unigui.api.animation;

/**
 * Интерполируемая временная точка.
 *
 * <p>{@link #easing()} применяется к сегменту от предыдущего keyframe до текущего.
 * Для cubic-bezier используй {@link #cubicBezier(float, Object, float, float, float, float)}.</p>
 *
 * @param timeSeconds время от начала storyboard в секундах
 * @param value значение свойства
 * @param easing easing-функция входящего сегмента
 * @param <T> тип значения
 */
public record SplineKeyframe<T>(float timeSeconds, T value, Easing easing) implements Keyframe<T> {
    public SplineKeyframe {
        if (!Float.isFinite(timeSeconds) || timeSeconds < 0.0f) {
            throw new IllegalArgumentException("Время keyframe должно быть конечным и неотрицательным.");
        }
        easing = easing == null ? AnimationEasing.LINEAR : easing;
    }

    /** Создаёт линейно интерполируемую временную точку. */
    public static <T> SplineKeyframe<T> linear(float timeSeconds, T value) {
        return new SplineKeyframe<>(timeSeconds, value, AnimationEasing.LINEAR);
    }

    /** Создаёт временную точку с cubic-bezier easing входящего сегмента. */
    public static <T> SplineKeyframe<T> cubicBezier(float timeSeconds,
                                                    T value,
                                                    float x1,
                                                    float y1,
                                                    float x2,
                                                    float y2) {
        return new SplineKeyframe<>(timeSeconds, value, Easing.cubicBezier(x1, y1, x2, y2));
    }
}
