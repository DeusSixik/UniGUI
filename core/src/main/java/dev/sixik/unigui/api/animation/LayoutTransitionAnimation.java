package dev.sixik.unigui.api.animation;

/**
 * Аддитивный visual offset для FLIP-анимации изменения layout-позиции.
 *
 * <p>Layout сразу сохраняет конечные bounds виджета, а эта анимация временно
 * возвращает его визуал в предыдущую позицию и затем сводит смещение к нулю.
 * Размеры и hit-test продолжают использовать актуальный layout.</p>
 *
 * <p>Объект не создаёт временных значений в {@link #update(float)} и может
 * безопасно складываться с другими transform-эффектами, например shake.</p>
 */
public final class LayoutTransitionAnimation implements PlayableAnimation {
    private final float startOffsetX;
    private final float startOffsetY;
    private final float durationSeconds;
    private final Easing easing;
    private float elapsedSeconds;
    private float offsetX;
    private float offsetY;
    private boolean finished;

    public LayoutTransitionAnimation(float offsetX,
                                     float offsetY,
                                     float durationSeconds,
                                     Easing easing) {
        this.startOffsetX = finite(offsetX);
        this.startOffsetY = finite(offsetY);
        this.durationSeconds = Float.isFinite(durationSeconds)
                ? Math.max(0.0f, durationSeconds)
                : 0.0f;
        this.easing = easing == null ? AnimationEasing.LINEAR : easing;
        this.offsetX = this.startOffsetX;
        this.offsetY = this.startOffsetY;
        if (this.durationSeconds <= 0.0f
                || (this.startOffsetX == 0.0f && this.startOffsetY == 0.0f)) {
            finish();
        }
    }

    /** @return текущее горизонтальное смещение относительно новых bounds */
    public float offsetX() {
        return offsetX;
    }

    /** @return текущее вертикальное смещение относительно новых bounds */
    public float offsetY() {
        return offsetY;
    }

    /** @return длительность перехода в секундах */
    public float durationSeconds() {
        return durationSeconds;
    }

    /** @return нормализованный прогресс от {@code 0} до {@code 1} */
    public float progress() {
        return durationSeconds <= 0.0f
                ? 1.0f
                : Easing.clamp01(elapsedSeconds / durationSeconds);
    }

    @Override
    public void update(float deltaSeconds) {
        if (finished) return;
        float delta = AnimationClock.sanitizeDelta(deltaSeconds);
        if (delta <= 0.0f) return;

        elapsedSeconds = Math.min(durationSeconds, elapsedSeconds + delta);
        float remaining = 1.0f - easing.apply(progress());
        offsetX = startOffsetX * remaining;
        offsetY = startOffsetY * remaining;
        if (elapsedSeconds >= durationSeconds) finish();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void cancel() {
        finish();
    }

    private void finish() {
        elapsedSeconds = durationSeconds;
        offsetX = 0.0f;
        offsetY = 0.0f;
        finished = true;
    }

    private static float finite(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }
}
