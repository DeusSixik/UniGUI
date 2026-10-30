package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

/**
 * Transition для RGBA-цвета.
 *
 * <p>{@code ColorTransition} раскладывает цвет на четыре независимых {@link FloatTransition}:
 * red, green, blue и alpha. Это позволяет использовать тот же {@link TransitionSpec}, easing,
 * repeat и yoyo-логику, что и для обычных числовых свойств.</p>
 *
 * <p>Класс пишет результат в переданный {@link MutableColor}, чтобы не создавать новый цвет на
 * каждом кадре. Это важно для UI-анимаций, которые могут тикаться сотни раз в секунду на большом
 * дереве виджетов.</p>
 *
 * <pre>{@code
 * ColorTransition transition = new ColorTransition(oldColor, newColor,
 *         TransitionSpec.of(0.25f, AnimationEasing.EASE_OUT));
 * transition.tick(deltaSeconds, widget.background());
 * }</pre>
 *
 * @see FloatTransition
 * @see TransitionSpec
 */
public final class ColorTransition {
    private final FloatTransition red;
    private final FloatTransition green;
    private final FloatTransition blue;
    private final FloatTransition alpha;

    /**
     * Создаёт transition между двумя цветами.
     *
     * @param start начальный цвет; {@code null} заменяется прозрачным чёрным
     * @param end конечный цвет; {@code null} заменяется начальным цветом
     * @param spec тайминг transition'а; {@code null} заменяется на {@link TransitionSpec#DEFAULT}
     */
    public ColorTransition(ColorView start, ColorView end, TransitionSpec spec) {
        ColorView safeStart = start == null ? new MutableColor() : start;
        ColorView safeEnd = end == null ? safeStart : end;
        TransitionSpec normalized = spec == null ? TransitionSpec.DEFAULT : spec;
        this.red = new FloatTransition(safeStart.r(), safeEnd.r(), normalized);
        this.green = new FloatTransition(safeStart.g(), safeEnd.g(), normalized);
        this.blue = new FloatTransition(safeStart.b(), safeEnd.b(), normalized);
        this.alpha = new FloatTransition(safeStart.a(), safeEnd.a(), normalized);
    }

    /**
     * Проверяет, завершились ли все четыре цветовых канала.
     *
     * @return {@code true}, если red/green/blue/alpha transition'ы завершены
     */
    public boolean finished() {
        return red.finished() && green.finished() && blue.finished() && alpha.finished();
    }

    /**
     * Продвигает transition и записывает цвет в target.
     *
     * @param deltaSeconds время кадра в секундах
     * @param target изменяемый цвет, в который будет записан результат; {@code null} игнорируется
     */
    public void tick(float deltaSeconds, MutableColor target) {
        if (target == null) return;
        target.set(
                red.tick(deltaSeconds),
                green.tick(deltaSeconds),
                blue.tick(deltaSeconds),
                alpha.tick(deltaSeconds));
    }

    /**
     * Принудительно записывает финальный цвет в target.
     *
     * <p>Метод полезен, когда владелец останавливает transition и хочет гарантированно оставить
     * свойство в корректном конечном состоянии.</p>
     *
     * @param target изменяемый цвет, в который будет записан финал; {@code null} игнорируется
     */
    public void finish(MutableColor target) {
        if (target == null) return;
        target.set(red.finalValue(), green.finalValue(), blue.finalValue(), alpha.finalValue());
    }
}