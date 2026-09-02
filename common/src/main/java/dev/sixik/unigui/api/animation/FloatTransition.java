package dev.sixik.unigui.api.animation;

/**
 * Детерминированный transition одного float-значения.
 *
 * <p>{@code FloatTransition} хранит runtime-состояние анимации: начальное значение, конечное
 * значение, {@link TransitionSpec} и прошедшее время. Виджеты используют его для opacity,
 * позиции, scale, rotation и любых кастомных числовых параметров.</p>
 *
 * <p>Класс не знает, куда применять результат. Он только возвращает текущее значение через
 * {@link #value()} или {@link #tick(float)}. Привязка к конкретному свойству делается снаружи:
 * встроенные свойства идут через {@link AnimatedProperty}, а кастомные — через
 * {@link FloatValueReader}/{@link FloatValueWriter}.</p>
 *
 * <pre>{@code
 * FloatTransition transition = new FloatTransition(0.0f, 1.0f,
 *         TransitionSpec.of(0.25f, AnimationEasing.EASE_OUT));
 *
 * float opacity = transition.tick(frame.deltaSeconds());
 * }</pre>
 *
 * <p>Для бесконечных transition'ов накопленное время периодически compact'ится, чтобы long-running
 * UI не терял точность float-вычислений.</p>
 *
 * @see TransitionSpec
 * @see ColorTransition
 */
public final class FloatTransition implements PlayableAnimation {
    private static final int INFINITE_COMPACT_CYCLES = 4096;

    private float start;
    private float end;
    private final TransitionSpec spec;
    private final FloatInterpolator interpolator;
    private float elapsedSeconds;
    private boolean cancelled;

    /**
     * Создаёт transition между двумя float-значениями.
     *
     * @param start начальное значение; невалидные числа заменяются на 0
     * @param end конечное значение; невалидные числа заменяются на 0
     * @param spec тайминг transition'а; {@code null} заменяется на {@link TransitionSpec#DEFAULT}
     */
    public FloatTransition(float start, float end, TransitionSpec spec) {
        this(start, end, spec, FloatInterpolator.LINEAR);
    }

    /** Создаёт transition с primitive-интерполятором. */
    public FloatTransition(float start, float end, TransitionSpec spec, FloatInterpolator interpolator) {
        this.start = sanitize(start);
        this.end = sanitize(end);
        this.spec = spec == null ? TransitionSpec.DEFAULT : spec;
        this.interpolator = interpolator == null ? FloatInterpolator.LINEAR : interpolator;
    }

    /** @return начальное значение transition'а */
    public float start() {
        return start;
    }

    /** @return конечное значение transition'а */
    public float end() {
        return end;
    }

    /** @return прошедшее время в секундах */
    public float elapsedSeconds() {
        return elapsedSeconds;
    }

    /** @return длительность одного прохода в секундах */
    public float durationSeconds() {
        return spec.durationSeconds();
    }

    /**
     * Проверяет, можно ли переиспользовать transition с указанными настройками.
     *
     * @param requestedSpec требуемый spec
     * @param requestedInterpolator требуемый интерполятор
     * @return {@code true}, если transition можно retarget'ить без создания объекта
     */
    public boolean matches(TransitionSpec requestedSpec, FloatInterpolator requestedInterpolator) {
        TransitionSpec normalizedSpec = requestedSpec == null ? TransitionSpec.DEFAULT : requestedSpec;
        FloatInterpolator normalizedInterpolator = requestedInterpolator == null
                ? FloatInterpolator.LINEAR : requestedInterpolator;
        return spec.equals(normalizedSpec) && interpolator == normalizedInterpolator;
    }

    /**
     * Проверяет, завершился ли transition.
     *
     * <p>Бесконечный transition никогда не считается завершённым, пока его явно не остановит владелец.</p>
     *
     * @return {@code true}, если transition дошёл до конца всех циклов
     */
    public boolean finished() {
        return spec.durationSeconds() <= 0.0f
                || (!spec.infinite() && elapsedSeconds >= totalDurationSeconds());
    }

    /**
     * Возвращает текущее значение без продвижения времени.
     *
     * @return значение на текущем {@link #elapsedSeconds()}
     */
    public float value() {
        return valueAt(elapsedSeconds);
    }

    /** Синоним {@link #value()} для унифицированного API transition'ов. */
    public float currentValue() {
        return value();
    }

    /** Перестраивает transition от текущего значения без скачка. */
    public FloatTransition retarget(float newEnd) {
        this.start = value();
        this.end = sanitize(newEnd);
        this.elapsedSeconds = 0.0f;
        this.cancelled = false;
        return this;
    }

    /**
     * Возвращает значение, на котором transition должен остановиться.
     *
     * <p>Для yoyo-transition'а финальное значение зависит от количества циклов: если последний
     * цикл вернулся назад, финалом будет {@link #start()}.</p>
     *
     * @return конечное runtime-значение
     */
    public float finalValue() {
        if (spec.durationSeconds() <= 0.0f || spec.infinite()) return end;
        int cycles = cycleCount();
        return spec.autoReverse() && cycles % 2 == 0 ? start : end;
    }

    /**
     * Продвигает transition и возвращает новое значение.
     *
     * @param deltaSeconds время кадра в секундах; отрицательные и невалидные значения игнорируются
     * @return значение после продвижения времени
     */
    public float tick(float deltaSeconds) {
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

    private float valueAt(float elapsed) {
        float duration = spec.durationSeconds();
        if (duration <= 0.0f) return end;
        if (!spec.infinite() && elapsed >= totalDurationSeconds()) return finalValue();

        int cycleIndex = Math.max(0, (int) Math.floor(elapsed / duration));
        float cycleElapsed = elapsed - cycleIndex * duration;
        float t = Easing.clamp01(cycleElapsed / duration);
        if (spec.autoReverse() && cycleIndex % 2 == 1) {
            t = 1.0f - t;
        }

        float eased = spec.easing().apply(t);
        return interpolator.interpolate(start, end, eased);
    }

    private void compactInfiniteElapsed(float duration) {
        float compactAfter = duration * INFINITE_COMPACT_CYCLES;
        if (elapsedSeconds < compactAfter) return;

        int cycleIndex = Math.max(0, (int) Math.floor(elapsedSeconds / duration));
        float cycleElapsed = elapsedSeconds - cycleIndex * duration;
        int preservedCycle = spec.autoReverse() ? cycleIndex % 2 : 0;
        elapsedSeconds = preservedCycle * duration + cycleElapsed;
    }

    private float totalDurationSeconds() {
        return spec.durationSeconds() * cycleCount();
    }

    private int cycleCount() {
        return spec.repeatCount() + 1;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

}
