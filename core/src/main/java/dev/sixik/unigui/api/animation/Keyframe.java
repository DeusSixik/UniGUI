package dev.sixik.unigui.api.animation;

/**
 * Одна временная точка property-track'а.
 *
 * <p>Тип keyframe определяет поведение сегмента, который заканчивается в этой точке:
 * {@link DiscreteKeyframe} удерживает предыдущее значение до своего времени, а
 * {@link SplineKeyframe} интерполирует значение от предыдущей точки.</p>
 *
 * @param <T> тип анимируемого значения
 */
public sealed interface Keyframe<T> permits DiscreteKeyframe, SplineKeyframe {
    /** @return время точки от начала storyboard в секундах */
    float timeSeconds();

    /** @return значение свойства в этой временной точке */
    T value();

    /** @return {@code true}, если переход к этой точке должен быть мгновенным */
    default boolean discrete() {
        return this instanceof DiscreteKeyframe<?>;
    }
}
