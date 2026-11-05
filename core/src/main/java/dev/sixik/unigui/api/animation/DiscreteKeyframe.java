package dev.sixik.unigui.api.animation;

/**
 * Дискретная временная точка: предыдущее значение удерживается до {@link #timeSeconds()},
 * после чего свойство мгновенно получает {@link #value()}.
 *
 * @param timeSeconds время от начала storyboard в секундах
 * @param value значение свойства
 * @param <T> тип значения
 */
public record DiscreteKeyframe<T>(float timeSeconds, T value) implements Keyframe<T> {
    public DiscreteKeyframe {
        if (!Float.isFinite(timeSeconds) || timeSeconds < 0.0f) {
            throw new IllegalArgumentException("Время keyframe должно быть конечным и неотрицательным.");
        }
    }
}
