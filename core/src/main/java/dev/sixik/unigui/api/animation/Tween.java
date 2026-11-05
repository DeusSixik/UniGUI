package dev.sixik.unigui.api.animation;

import java.util.Objects;

/**
 * Типизированный детерминированный переход между двумя значениями.
 *
 * <p>{@code Tween<T>} не знает устройство типа {@code T}: правило смешивания передаётся через
 * {@link Interpolator}. Поэтому он может анимировать векторы, цвета, прямоугольники и классы
 * приложения без изменений в ядре UniGUI.</p>
 *
 * <pre>{@code
 * record Point(float x, float y) {}
 *
 * Interpolator<Point> points = (a, b, t) -> new Point(
 *         a.x() + (b.x() - a.x()) * t,
 *         a.y() + (b.y() - a.y()) * t);
 *
 * Tween<Point> movement = new Tween<>(
 *         new Point(0.0f, 0.0f),
 *         new Point(100.0f, 40.0f),
 *         TransitionSpec.of(0.3f),
 *         points,
 *         model::position);
 * }</pre>
 *
 * <p>Класс хранит ссылки на start/end и не копирует пользовательские объекты. Интерполятор не
 * должен неожиданно изменять эти объекты. Для {@code float} и {@code int} в горячих путях следует
 * использовать {@link FloatTransition} и {@link IntTransition}: generic-вариант чисел требует
 * boxing результата на каждом кадре.</p>
 */
public final class Tween<T> implements PlayableAnimation {
    private static final int INFINITE_COMPACT_CYCLES = 4096;

    private T start;
    private T end;
    private T current;
    private final TransitionSpec spec;
    private final Interpolator<T> interpolator;
    private final ValueWriter<? super T> writer;
    private float elapsedSeconds;
    private boolean cancelled;

    /** Создаёт переход без автоматической записи результата во внешнее свойство. */
    public Tween(T start, T end, TransitionSpec spec, Interpolator<T> interpolator) {
        this(start, end, spec, interpolator, null);
    }

    /** Создаёт переход и writer, который будет получать значение на каждом обновлении. */
    public Tween(T start,
                 T end,
                 TransitionSpec spec,
                 Interpolator<T> interpolator,
                 ValueWriter<? super T> writer) {
        this.start = start;
        this.end = end;
        this.spec = spec == null ? TransitionSpec.DEFAULT : spec;
        this.interpolator = Objects.requireNonNull(interpolator, "interpolator");
        this.writer = writer;
        this.current = valueAt(0.0f);
    }

    /**
     * Создаёт переход, один раз прочитав начальное значение через reader.
     *
     * <p>Именованная фабрика используется вместо перегруженного конструктора, чтобы method
     * reference не создавал неоднозначность с параметром start типа {@code T}.</p>
     */
    public static <T> Tween<T> from(ValueReader<? extends T> reader,
                                    T end,
                                    TransitionSpec spec,
                                    Interpolator<T> interpolator,
                                    ValueWriter<? super T> writer) {
        T start = Objects.requireNonNull(reader, "reader").get();
        return new Tween<>(start, end, spec, interpolator, writer);
    }

    /** @return начальное значение перехода */
    public T start() { return start; }

    /** @return конечное значение перехода */
    public T end() { return end; }

    /** @return текущее значение без продвижения времени */
    public T value() { return current; }

    /** Синоним {@link #value()} для API анимационного контроллера. */
    public T currentValue() { return current; }

    /** @return прошедшее время в секундах */
    public float elapsedSeconds() { return elapsedSeconds; }

    /** Проверяет совместимость настроек для переиспользования через {@link #retarget(Object)}. */
    public boolean matches(TransitionSpec requestedSpec, Interpolator<T> requestedInterpolator) {
        TransitionSpec normalizedSpec = requestedSpec == null ? TransitionSpec.DEFAULT : requestedSpec;
        return spec.equals(normalizedSpec) && interpolator == requestedInterpolator;
    }

    /** @return {@code true}, если переход дошёл до конца всех циклов */
    public boolean finished() {
        return spec.durationSeconds() <= 0.0f
                || (!spec.infinite() && elapsedSeconds >= totalDurationSeconds());
    }

    /** Перестраивает переход от текущего значения без визуального скачка. */
    public Tween<T> retarget(T newEnd) {
        start = current;
        end = newEnd;
        elapsedSeconds = 0.0f;
        cancelled = false;
        current = valueAt(0.0f);
        return this;
    }

    /** @return значение, на котором закончится переход с учётом yoyo */
    public T finalValue() {
        if (spec.durationSeconds() <= 0.0f || spec.infinite()) return end;
        int cycles = cycleCount();
        return spec.autoReverse() && cycles % 2 == 0 ? start : end;
    }

    /** Продвигает переход и возвращает новое значение. */
    public T tick(float deltaSeconds) {
        if (cancelled) return current;

        float duration = spec.durationSeconds();
        if (duration <= 0.0f) {
            current = end;
        } else {
            elapsedSeconds += AnimationClock.sanitizeDelta(deltaSeconds);
            if (spec.infinite()) {
                compactInfiniteElapsed(duration);
            } else {
                elapsedSeconds = Math.min(totalDurationSeconds(), elapsedSeconds);
            }
            current = valueAt(elapsedSeconds);
        }
        if (writer != null) writer.set(current);
        return current;
    }

    @Override
    public void update(float deltaSeconds) { tick(deltaSeconds); }

    @Override
    public boolean isFinished() { return cancelled || finished(); }

    /** Отменяет переход, сохраняя последнее рассчитанное значение. */
    @Override
    public void cancel() { cancelled = true; }

    private T valueAt(float elapsed) {
        float duration = spec.durationSeconds();
        if (duration <= 0.0f) return end;
        if (!spec.infinite() && elapsed >= totalDurationSeconds()) return finalValue();

        int cycleIndex = Math.max(0, (int) Math.floor(elapsed / duration));
        float cycleElapsed = elapsed - cycleIndex * duration;
        float progress = Easing.clamp01(cycleElapsed / duration);
        if (spec.autoReverse() && cycleIndex % 2 == 1) progress = 1.0f - progress;
        return interpolator.interpolate(start, end, spec.easing().apply(progress));
    }

    private void compactInfiniteElapsed(float duration) {
        float compactAfter = duration * INFINITE_COMPACT_CYCLES;
        if (elapsedSeconds < compactAfter) return;

        int cycleIndex = Math.max(0, (int) Math.floor(elapsedSeconds / duration));
        float cycleElapsed = elapsedSeconds - cycleIndex * duration;
        int preservedCycle = spec.autoReverse() ? cycleIndex % 2 : 0;
        elapsedSeconds = preservedCycle * duration + cycleElapsed;
    }

    private float totalDurationSeconds() { return spec.durationSeconds() * cycleCount(); }

    private int cycleCount() { return spec.repeatCount() + 1; }
}
