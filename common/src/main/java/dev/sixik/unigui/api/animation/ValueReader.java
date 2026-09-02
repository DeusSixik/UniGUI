package dev.sixik.unigui.api.animation;

/** Читает текущее значение свойства перед запуском типизированной анимации. */
@FunctionalInterface
public interface ValueReader<T> {
    /** Возвращает значение, которое станет началом перехода. */
    T get();
}
