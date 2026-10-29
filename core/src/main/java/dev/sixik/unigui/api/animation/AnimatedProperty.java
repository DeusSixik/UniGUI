package dev.sixik.unigui.api.animation;

/**
 * Встроенные числовые свойства виджета, которые умеет анимировать базовый transition engine.
 *
 * <p>Эти значения используются как стабильные ключи для transform/opacity-анимаций в
 * {@code WidgetBase}. Для нестандартных числовых параметров, которых нет в этом enum, используется
 * пара {@link FloatValueReader}/{@link FloatValueWriter}: виджет сам отдаёт getter и setter, а
 * engine ведёт обычный {@link FloatTransition}.</p>
 *
 * <p>Пример: встроенный scale tween.</p>
 *
 * <pre>{@code
 * widget.animateScale(1.08f, 1.08f, TransitionSpec.of(0.18f, AnimationEasing.EASE_OUT));
 * }</pre>
 *
 * @see FloatTransition
 * @see TransitionSpec
 */
public enum AnimatedProperty {
    /** Общая прозрачность виджета в диапазоне 0..1. */
    OPACITY,
    /** Смещение виджета по X поверх layout-позиции. */
    POSITION_X,
    /** Смещение виджета по Y поверх layout-позиции. */
    POSITION_Y,
    /** Масштаб по горизонтали. */
    SCALE_X,
    /** Масштаб по вертикали. */
    SCALE_Y,
    /** Поворот в градусах вокруг текущего pivot. */
    ROTATION_DEGREES,
    /** X-координата custom pivot в UI-пикселях. */
    PIVOT_X,
    /** Y-координата custom pivot в UI-пикселях. */
    PIVOT_Y
}