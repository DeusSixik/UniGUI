package dev.sixik.unigui.api.animation;

/**
 * Типизированное правило смешивания двух значений.
 *
 * <p>Интерфейс позволяет {@link Tween} работать с любым пользовательским типом без знания его
 * устройства. Обычно интерполятор объявляется один раз как константа и переиспользуется:</p>
 *
 * <pre>{@code
 * Interpolator<Point> POINTS = (start, end, progress) -> new Point(
 *         start.x() + (end.x() - start.x()) * progress,
 *         start.y() + (end.y() - start.y()) * progress);
 * }</pre>
 *
 * <p>Для горячих числовых путей следует использовать primitive-специализации
 * {@link FloatInterpolator} и {@link IntInterpolator}, чтобы не платить за boxing.</p>
 */
@FunctionalInterface
public interface Interpolator<T> {
    /**
     * Интерполирует значение по progress 0..1.
     *
     * <p>Реализация не должна неожиданно изменять start или end: {@link Tween} хранит эти ссылки
     * для последующих кадров и retarget.</p>
     */
    T interpolate(T start, T end, float progress);
}
