package dev.sixik.unigui.api.animation;

/**
 * Читает текущее float-значение кастомного свойства виджета для generic animation engine.
 *
 * <p>Интерфейс нужен, когда свойство не входит в {@link AnimatedProperty}, но его всё равно нужно
 * плавно менять через {@link FloatTransition}. Виджет передаёт reader вместе с
 * {@link FloatValueWriter}: reader берёт стартовое значение, writer применяет значения на кадрах.</p>
 *
 * <pre>{@code
 * widget.animateParameter(key, progress::value, progress::value, 86.0f, transition);
 * }</pre>
 *
 * @see FloatValueWriter
 */
@FunctionalInterface
public interface FloatValueReader {
    /**
     * Возвращает текущее значение свойства.
     *
     * @return float-значение, которое будет стартом transition'а
     */
    float get();
}