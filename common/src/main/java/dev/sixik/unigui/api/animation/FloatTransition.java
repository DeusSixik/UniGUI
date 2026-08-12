package dev.sixik.unigui.api.animation;

/**
 * Deterministic scalar transition used by widget property animation.
 */
public final class FloatTransition {
    private static final int INFINITE_COMPACT_CYCLES = 4096;

    private final float start;
    private final float end;
    private final TransitionSpec spec;
    private float elapsedSeconds;

    public FloatTransition(float start, float end, TransitionSpec spec) {
        this.start = sanitize(start);
        this.end = sanitize(end);
        this.spec = spec == null ? TransitionSpec.DEFAULT : spec;
    }

    public float start() {
        return start;
    }

    public float end() {
        return end;
    }

    public float elapsedSeconds() {
        return elapsedSeconds;
    }

    public float durationSeconds() {
        return spec.durationSeconds();
    }

    public boolean finished() {
        return spec.durationSeconds() <= 0.0f
                || (!spec.infinite() && elapsedSeconds >= totalDurationSeconds());
    }

    public float value() {
        return valueAt(elapsedSeconds);
    }

    public float finalValue() {
        if (spec.durationSeconds() <= 0.0f || spec.infinite()) return end;
        int cycles = cycleCount();
        return spec.autoReverse() && cycles % 2 == 0 ? start : end;
    }

    public float tick(float deltaSeconds) {
        float duration = spec.durationSeconds();
        if (duration <= 0.0f) return end;

        elapsedSeconds += sanitizeDelta(deltaSeconds);
        if (spec.infinite()) {
            compactInfiniteElapsed(duration);
        } else {
            elapsedSeconds = Math.min(totalDurationSeconds(), elapsedSeconds);
        }
        return value();
    }

    private float valueAt(float elapsed) {
        float duration = spec.durationSeconds();
        if (duration <= 0.0f) return end;
        if (!spec.infinite() && elapsed >= totalDurationSeconds()) return finalValue();

        int cycleIndex = Math.max(0, (int) Math.floor(elapsed / duration));
        float cycleElapsed = elapsed - cycleIndex * duration;
        float t = AnimationEasing.clamp01(cycleElapsed / duration);
        if (spec.autoReverse() && cycleIndex % 2 == 1) {
            t = 1.0f - t;
        }

        float eased = spec.easing().apply(t);
        return start + (end - start) * eased;
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

    private static float sanitizeDelta(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
