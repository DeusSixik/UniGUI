package dev.sixik.unigui.api.animation;

/**
 * Процедурная затухающая осцилляция для короткого UI-фидбэка.
 *
 * <p>Shake хранит только смещение эффекта и не заменяет базовую позицию владельца. Владелец должен
 * прибавлять {@link #offsetX()} и {@link #offsetY()} поверх своей текущей позиции, а после завершения
 * вернуть эти смещения обратно. Несколько экземпляров можно проигрывать параллельно и складывать их
 * смещения.</p>
 *
 * <p>Класс не создаёт объекты во время {@link #update(float)}. Он реализует общий
 * {@link PlayableAnimation} и может использоваться напрямую в {@link AnimationController}.</p>
 */
public final class ShakeAnimation implements PlayableAnimation {
    private final float amplitudeX;
    private final float amplitudeY;
    private final float durationSeconds;
    private final int cycles;
    private float elapsedSeconds;
    private boolean cancelled;

    /**
     * Создаёт shake по горизонтали.
     *
     * @param amplitude максимальная амплитуда по X
     * @param durationSeconds длительность эффекта в секундах
     * @param cycles количество колебаний
     */
    public ShakeAnimation(float amplitude, float durationSeconds, int cycles) {
        this(amplitude, 0.0f, durationSeconds, cycles);
    }

    /**
     * Создаёт shake по двум осям.
     *
     * @param amplitudeX максимальная амплитуда по X
     * @param amplitudeY максимальная амплитуда по Y
     * @param durationSeconds длительность эффекта в секундах
     * @param cycles количество колебаний
     */
    public ShakeAnimation(float amplitudeX, float amplitudeY, float durationSeconds, int cycles) {
        this.amplitudeX = finiteOrZero(amplitudeX);
        this.amplitudeY = finiteOrZero(amplitudeY);
        this.durationSeconds = finiteOrZero(durationSeconds) > 0.0f
                ? finiteOrZero(durationSeconds) : 0.0f;
        this.cycles = Math.max(1, cycles);
    }

    /** @return амплитуда по X */
    public float amplitudeX() { return amplitudeX; }

    /** @return амплитуда по Y */
    public float amplitudeY() { return amplitudeY; }

    /** @return длительность эффекта в секундах */
    public float durationSeconds() { return durationSeconds; }

    /** @return количество колебаний */
    public int cycles() { return cycles; }

    /** @return прошедшее время в секундах */
    public float elapsedSeconds() { return elapsedSeconds; }

    /** @return текущее смещение по X */
    public float offsetX() { return offset(amplitudeX); }

    /** @return текущее смещение по Y */
    public float offsetY() { return offset(amplitudeY); }

    @Override
    public void update(float deltaSeconds) {
        if (cancelled || isFinished()) return;
        elapsedSeconds = Math.min(durationSeconds, elapsedSeconds + AnimationClock.sanitizeDelta(deltaSeconds));
    }

    @Override
    public boolean isFinished() {
        return cancelled || durationSeconds <= 0.0f || elapsedSeconds >= durationSeconds;
    }

    /** Завершает shake и обнуляет его смещение. */
    @Override
    public void cancel() {
        cancelled = true;
        elapsedSeconds = durationSeconds;
    }

    private float offset(float amplitude) {
        if (amplitude == 0.0f || durationSeconds <= 0.0f || isFinished()) return 0.0f;
        float progress = Easing.clamp01(elapsedSeconds / durationSeconds);
        float decay = 1.0f - progress;
        return (float) Math.sin(progress * cycles * Math.PI * 2.0f) * amplitude * decay;
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }
}