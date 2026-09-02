package dev.sixik.unigui.api.animation;

/**
 * Детерминированный transition одного {@code int}-значения без boxing на кадре.
 *
 * <p>По поведению совпадает с {@link FloatTransition}: поддерживает easing, repeat, yoyo,
 * retarget и бесконечные циклы. Для произвольных объектных типов используется {@link Tween}.</p>
 */
public final class IntTransition implements PlayableAnimation {
    private static final int INFINITE_COMPACT_CYCLES = 4096;

    private int start;
    private int end;
    private final TransitionSpec spec;
    private final IntInterpolator interpolator;
    private float elapsedSeconds;
    private boolean cancelled;

    /** Создаёт transition с линейной интерполяцией целых значений. */
    public IntTransition(int start, int end, TransitionSpec spec) {
        this(start, end, spec, IntInterpolator.LINEAR);
    }

    /** Создаёт transition с указанным primitive-интерполятором. */
    public IntTransition(int start, int end, TransitionSpec spec, IntInterpolator interpolator) {
        this.start = start;
        this.end = end;
        this.spec = spec == null ? TransitionSpec.DEFAULT : spec;
        this.interpolator = interpolator == null ? IntInterpolator.LINEAR : interpolator;
    }

    /** @return начальное значение transition'а */
    public int start() { return start; }

    /** @return конечное значение transition'а */
    public int end() { return end; }

    /** @return прошедшее время в секундах */
    public float elapsedSeconds() { return elapsedSeconds; }

    /** @return длительность одного прохода в секундах */
    public float durationSeconds() { return spec.durationSeconds(); }

    /** Проверяет совместимость настроек для переиспользования через {@link #retarget(int)}. */
    public boolean matches(TransitionSpec requestedSpec, IntInterpolator requestedInterpolator) {
        TransitionSpec normalizedSpec = requestedSpec == null ? TransitionSpec.DEFAULT : requestedSpec;
        IntInterpolator normalizedInterpolator = requestedInterpolator == null
                ? IntInterpolator.LINEAR : requestedInterpolator;
        return spec.equals(normalizedSpec) && interpolator == normalizedInterpolator;
    }

    /** @return {@code true}, если transition дошёл до конца всех циклов */
    public boolean finished() {
        return spec.durationSeconds() <= 0.0f
                || (!spec.infinite() && elapsedSeconds >= totalDurationSeconds());
    }

    /** @return текущее значение без продвижения времени */
    public int value() { return valueAt(elapsedSeconds); }

    /** Синоним {@link #value()} для унифицированного API transition'ов. */
    public int currentValue() { return value(); }

    /** Перестраивает transition от текущего значения без скачка. */
    public IntTransition retarget(int newEnd) {
        start = value();
        end = newEnd;
        elapsedSeconds = 0.0f;
        cancelled = false;
        return this;
    }

    /** @return значение, на котором завершится transition с учётом yoyo */
    public int finalValue() {
        if (spec.durationSeconds() <= 0.0f || spec.infinite()) return end;
        int cycles = cycleCount();
        return spec.autoReverse() && cycles % 2 == 0 ? start : end;
    }

    /** Продвигает transition и возвращает новое значение. */
    public int tick(float deltaSeconds) {
        if (cancelled) return value();
        float duration = spec.durationSeconds();
        if (duration <= 0.0f) return end;

        elapsedSeconds += AnimationClock.sanitizeDelta(deltaSeconds);
        if (spec.infinite()) {
            compactInfiniteElapsed(duration);
        } else {
            elapsedSeconds = Math.min(totalDurationSeconds(), elapsedSeconds);
        }
        return value();
    }

    @Override
    public void update(float deltaSeconds) { tick(deltaSeconds); }

    @Override
    public boolean isFinished() { return cancelled || finished(); }

    /** Отменяет transition. */
    @Override
    public void cancel() { cancelled = true; }

    private int valueAt(float elapsed) {
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
