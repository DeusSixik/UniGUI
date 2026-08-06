package dev.sixik.unigui.api.animation;

/**
 * Immutable transition timing contract.
 */
public record TransitionSpec(float durationSeconds, AnimationEasing easing) {
    public static final TransitionSpec DEFAULT = new TransitionSpec(0.16f, AnimationEasing.EASE_OUT);
    public static final TransitionSpec INSTANT = new TransitionSpec(0.0f, AnimationEasing.LINEAR);

    public TransitionSpec {
        durationSeconds = Float.isFinite(durationSeconds) ? Math.max(0.0f, durationSeconds) : 0.0f;
        easing = easing == null ? AnimationEasing.LINEAR : easing;
    }

    public static TransitionSpec of(float durationSeconds) {
        return new TransitionSpec(durationSeconds, AnimationEasing.EASE_OUT);
    }

    public static TransitionSpec of(float durationSeconds, AnimationEasing easing) {
        return new TransitionSpec(durationSeconds, easing);
    }
}
