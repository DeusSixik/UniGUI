package dev.sixik.unigui.api.animation;

/** Стандартные объектные интерполяторы для {@link Tween}. */
public final class Interpolators {
    /**
     * Линейный интерполятор {@link Float}.
     *
     * <p>Для горячего UI-пути предпочтителен {@link FloatInterpolator#LINEAR}, потому что этот
     * вариант создаёт boxed-результат.</p>
     */
    public static final Interpolator<Float> FLOAT = FloatInterpolator.LINEAR::interpolate;

    /**
     * Линейный интерполятор {@link Integer} с округлением до ближайшего целого.
     *
     * <p>Для часто обновляемого значения предпочтителен {@link IntInterpolator#LINEAR} вместе с
     * {@link IntTransition}, чтобы не создавать {@link Integer} на каждом кадре.</p>
     */
    public static final Interpolator<Integer> INTEGER = IntInterpolator.LINEAR::interpolate;

    private static final Interpolator<Object> DISCRETE =
            (start, end, progress) -> progress < 1.0f ? start : end;

    private Interpolators() {
    }

    /**
     * Возвращает дискретный интерполятор: start сохраняется до конца перехода, затем выбирается end.
     * Подходит для enum, строк, текстур и других неплавных состояний.
     */
    @SuppressWarnings("unchecked")
    public static <T> Interpolator<T> discrete() {
        return (Interpolator<T>) DISCRETE;
    }

    /**
     * Создаёт ступенчатый интерполятор с заданным порогом переключения.
     *
     * @param threshold прогресс переключения; ограничивается диапазоном 0..1
     */
    public static <T> Interpolator<T> step(float threshold) {
        float normalized = Easing.clamp01(threshold);
        return (start, end, progress) -> progress < normalized ? start : end;
    }
}
