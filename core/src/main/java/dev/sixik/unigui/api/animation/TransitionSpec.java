package dev.sixik.unigui.api.animation;

/**
 * Неизменяемое описание тайминга одной transition-анимации.
 *
 * <p>{@code TransitionSpec} не хранит текущее время и не знает, какое свойство анимируется.
 * Он задаёт только правила движения: длительность, easing, количество повторов и auto-reverse
 * режим. Runtime состояние хранится в {@link FloatTransition} или более высокоуровневой обёртке
 * виджета. Для произвольных типов те же правила применяет {@link Tween}, а для целых значений без
 * boxing — {@link IntTransition}.</p>
 *
 * <p>{@link #repeatCount()} — это число дополнительных циклов после первого прохода. Например,
 * {@code repeat(2)} даст три прохода: первый + два повтора. Для бесконечной анимации используется
 * {@link #loop()} или {@link #repeatForever()}.</p>
 *
 * <pre>{@code
 * TransitionSpec click = TransitionSpec.of(0.12f, AnimationEasing.EASE_OUT).yoyo();
 * TransitionSpec pulse = TransitionSpec.of(0.70f, AnimationEasing.EASE_IN_OUT).loop().yoyo();
 * }</pre>
 *
 * @param durationSeconds длительность одного прохода в секундах
 * @param easing easing-функция одного прохода
 * @param repeatCount число дополнительных повторов или {@link #REPEAT_FOREVER}
 * @param autoReverse если {@code true}, каждый второй цикл идёт от end к start
 * @see FloatTransition
 * @see IntTransition
 * @see Tween
 */
public record TransitionSpec(float durationSeconds,
                             Easing easing,
                             int repeatCount,
                             boolean autoReverse) {
    /** Специальное значение для бесконечного количества повторов. */
    public static final int REPEAT_FOREVER = -1;
    /** Дефолтный короткий UI-переход для hover/click feedback. */
    public static final TransitionSpec DEFAULT = new TransitionSpec(0.16f, AnimationEasing.EASE_OUT);
    /** Мгновенное применение конечного значения без промежуточных кадров. */
    public static final TransitionSpec INSTANT = new TransitionSpec(0.0f, AnimationEasing.LINEAR);

    /**
     * Создаёт one-shot transition без повторов и auto-reverse.
     *
     * @param durationSeconds длительность в секундах
     * @param easing easing-функция
     */
    public TransitionSpec(float durationSeconds, Easing easing) {
        this(durationSeconds, easing, 0, false);
    }

    /** Нормализует длительность, easing и repeat count. */
    public TransitionSpec {
        durationSeconds = Float.isFinite(durationSeconds) ? Math.max(0.0f, durationSeconds) : 0.0f;
        easing = easing == null ? AnimationEasing.LINEAR : easing;
        repeatCount = repeatCount < 0 ? REPEAT_FOREVER : repeatCount;
    }

    /**
     * Создаёт transition с дефолтным {@link AnimationEasing#EASE_OUT}.
     *
     * @param durationSeconds длительность в секундах
     * @return новый {@code TransitionSpec}
     */
    public static TransitionSpec of(float durationSeconds) {
        return new TransitionSpec(durationSeconds, AnimationEasing.EASE_OUT);
    }

    /**
     * Создаёт transition с явным easing.
     *
     * @param durationSeconds длительность в секундах
     * @param easing easing-функция
     * @return новый {@code TransitionSpec}
     */
    public static TransitionSpec of(float durationSeconds, Easing easing) {
        return new TransitionSpec(durationSeconds, easing);
    }

    /**
     * Добавляет конечное число повторов после первого прохода.
     *
     * @param repeatCount число дополнительных проходов; отрицательные значения превращаются в 0
     * @return копия spec с новым числом повторов
     */
    public TransitionSpec repeat(int repeatCount) {
        return new TransitionSpec(durationSeconds, easing, Math.max(0, repeatCount), autoReverse);
    }

    /**
     * Включает бесконечное повторение transition'а.
     *
     * @return копия spec с {@link #REPEAT_FOREVER}
     */
    public TransitionSpec loop() {
        return repeatForever();
    }

    /**
     * Включает бесконечное повторение transition'а.
     *
     * @return копия spec с {@link #REPEAT_FOREVER}
     */
    public TransitionSpec repeatForever() {
        return new TransitionSpec(durationSeconds, easing, REPEAT_FOREVER, autoReverse);
    }

    /**
     * Включает auto-reverse: start -> end -> start -> ...
     *
     * @return копия spec с auto-reverse
     */
    public TransitionSpec yoyo() {
        return new TransitionSpec(durationSeconds, easing, repeatCount, true);
    }

    /**
     * Выключает auto-reverse.
     *
     * @return копия spec без auto-reverse
     */
    public TransitionSpec noYoyo() {
        return new TransitionSpec(durationSeconds, easing, repeatCount, false);
    }

    /**
     * Сбрасывает повторение, сохраняя длительность, easing и auto-reverse.
     *
     * @return one-shot копия spec
     */
    public TransitionSpec oneShot() {
        return new TransitionSpec(durationSeconds, easing, 0, autoReverse);
    }

    /**
     * Проверяет, бесконечный ли transition.
     *
     * @return {@code true}, если repeat count равен {@link #REPEAT_FOREVER}
     */
    public boolean infinite() {
        return repeatCount == REPEAT_FOREVER;
    }
}
