package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.animation.AnimationClock;
import dev.sixik.unigui.api.utils.consumers.basic.FloatConsumer;

import java.util.Objects;

/**
 * Drives a scene sampler with a caller-supplied clock. All times are in seconds.
 */
public final class ScenePlaybackController {
    /**
     * Finite end time in seconds.
     */
    private final float durationSeconds;
    /**
     * Applies a frame at an absolute time in seconds.
     */
    private final FloatConsumer sample;
    /**
     * Current time in seconds, from zero to {@link #durationSeconds} inclusive.
     */
    private float timeSeconds;
    /**
     * Whether frame updates advance the current time.
     */
    private boolean playing;

    /**
     * Creates a controller paused at zero. Use {@link #seek(float)} or {@link #replay()} to sample the initial frame.
     *
     * @param durationSeconds finite nonnegative duration in seconds
     * @param sample          callback that applies a frame at the supplied time, such as {@code timeline::sample}
     * @throws IllegalArgumentException if the duration is negative or nonfinite
     * @throws NullPointerException     if the sampler is null
     */
    public ScenePlaybackController(float durationSeconds, FloatConsumer sample) {
        if (!Float.isFinite(durationSeconds) || durationSeconds < 0) {
            throw new IllegalArgumentException("Playback duration must be finite and nonnegative");
        }
        this.durationSeconds = durationSeconds;
        this.sample = Objects.requireNonNull(sample, "sample");
    }

    /**
     * Starts or resumes playback from the current time. Use {@link #replay()} to restart at the end.
     */
    public void play() {
        playing = timeSeconds < durationSeconds;
    }

    /**
     * Pauses playback at the current time.
     */
    public void pause() {
        playing = false;
    }

    /**
     * Samples the requested time clamped to the playback range. Keeps the play or pause state unless the
     * seek reaches the end. Seeking to the current time reapplies that frame.
     *
     * @param seconds finite target time in seconds
     * @throws IllegalArgumentException if the target time is nonfinite
     */
    public void seek(float seconds) {
        if (!Float.isFinite(seconds)) throw new IllegalArgumentException("Seek time must be finite");
        timeSeconds = Math.max(0, Math.min(durationSeconds, seconds));
        if (timeSeconds == durationSeconds) pause();
        sample.accept(timeSeconds);
    }

    /**
     * Samples the first frame and starts playback, or stays paused when the duration is zero.
     */
    public void replay() {
        playing = durationSeconds > 0;
        seek(0);
    }

    /**
     * Advances playback by the sanitized frame delta and pauses when the end is reached.
     *
     * @param deltaSeconds elapsed seconds since the previous update, sanitized by {@link AnimationClock#sanitizeDelta(float)}
     */
    public void update(float deltaSeconds) {
        if (!playing) return;
        float delta = AnimationClock.sanitizeDelta(deltaSeconds);
        if (delta > 0) seek(timeSeconds + delta);
    }

    /**
     * @return current time in seconds
     */
    public float timeSeconds() {
        return timeSeconds;
    }

    /**
     * @return whether playback is advancing
     */
    public boolean playing() {
        return playing;
    }
}
