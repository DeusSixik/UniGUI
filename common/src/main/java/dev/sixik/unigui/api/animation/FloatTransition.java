package dev.sixik.unigui.api.animation;

/**
 * Deterministic scalar transition used by widget property animation.
 */
public final class FloatTransition {
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
        return spec.durationSeconds() <= 0.0f || elapsedSeconds >= spec.durationSeconds();
    }

    public float value() {
        float duration = spec.durationSeconds();
        if (duration <= 0.0f) return end;
        float t = AnimationEasing.clamp01(elapsedSeconds / duration);
        float eased = spec.easing().apply(t);
        return start + (end - start) * eased;
    }

    public float tick(float deltaSeconds) {
        elapsedSeconds = Math.min(spec.durationSeconds(), elapsedSeconds + sanitizeDelta(deltaSeconds));
        return value();
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeDelta(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }
}
