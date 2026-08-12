package dev.sixik.unigui.api.animation;

/**
 * Immutable transition timing contract.
 *
 * <p>{@code repeatCount} is the number of extra cycles after the first pass.
 * Use {@link #loop()} or {@link #repeatForever()} for an infinite animation.</p>
 */
public record TransitionSpec(float durationSeconds,
                             AnimationEasing easing,
                             int repeatCount,
                             boolean autoReverse) {
    public static final int REPEAT_FOREVER = -1;
    public static final TransitionSpec DEFAULT = new TransitionSpec(0.16f, AnimationEasing.EASE_OUT);
    public static final TransitionSpec INSTANT = new TransitionSpec(0.0f, AnimationEasing.LINEAR);

    public TransitionSpec(float durationSeconds, AnimationEasing easing) {
        this(durationSeconds, easing, 0, false);
    }

    public TransitionSpec {
        durationSeconds = Float.isFinite(durationSeconds) ? Math.max(0.0f, durationSeconds) : 0.0f;
        easing = easing == null ? AnimationEasing.LINEAR : easing;
        repeatCount = repeatCount < 0 ? REPEAT_FOREVER : repeatCount;
    }

    public static TransitionSpec of(float durationSeconds) {
        return new TransitionSpec(durationSeconds, AnimationEasing.EASE_OUT);
    }

    public static TransitionSpec of(float durationSeconds, AnimationEasing easing) {
        return new TransitionSpec(durationSeconds, easing);
    }

    /**
     * Replays the transition {@code repeatCount} extra times after the first pass.
     */
    public TransitionSpec repeat(int repeatCount) {
        return new TransitionSpec(durationSeconds, easing, Math.max(0, repeatCount), autoReverse);
    }

    /**
     * Repeats the transition forever.
     */
    public TransitionSpec loop() {
        return repeatForever();
    }

    /**
     * Repeats the transition forever.
     */
    public TransitionSpec repeatForever() {
        return new TransitionSpec(durationSeconds, easing, REPEAT_FOREVER, autoReverse);
    }

    /**
     * Alternates direction every cycle: start -> end -> start -> ...
     */
    public TransitionSpec yoyo() {
        return new TransitionSpec(durationSeconds, easing, repeatCount, true);
    }

    public TransitionSpec noYoyo() {
        return new TransitionSpec(durationSeconds, easing, repeatCount, false);
    }

    public TransitionSpec oneShot() {
        return new TransitionSpec(durationSeconds, easing, 0, autoReverse);
    }

    public boolean infinite() {
        return repeatCount == REPEAT_FOREVER;
    }
}
