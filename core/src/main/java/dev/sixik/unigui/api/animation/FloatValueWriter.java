package dev.sixik.unigui.api.animation;

/**
 * Применяет float-значение, рассчитанное generic animation engine.
 *
 * <p>Используется вместе с {@link FloatValueReader} для кастомных числовых свойств. Writer должен
 * быть лёгким и не создавать лишних объектов: он вызывается каждый кадр, пока transition активен.</p>
 *
 * @see FloatValueReader
 * @see FloatTransition
 */
@FunctionalInterface
public interface FloatValueWriter {
    /**
     * Записывает новое значение свойства.
     *
     * @param value значение, рассчитанное transition'ом на текущем кадре
     */
    void set(float value);
}