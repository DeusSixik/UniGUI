package dev.sixik.unigui.api.animation;

/**
 * Применяет значение, рассчитанное типизированной анимацией.
 *
 * <p>Writer вызывается на каждом обновлении {@link Tween}, поэтому его реализация должна быть
 * лёгкой и не выполнять ненужных выделений памяти.</p>
 */
@FunctionalInterface
public interface ValueWriter<T> {
    /** Записывает новое значение свойства. */
    void set(T value);
}
