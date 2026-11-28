package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.animation.AnimationClock;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.utils.consumers.basic.FloatConsumer;

import java.util.Objects;

/**
 * Drives a scene sampler with a caller-supplied clock. All times are in seconds.
 */
@WikiPage(title = "ScenePlaybackController", slug = "scene-playback-controller", category = "<key:scene.category>", order = 177,
        summary = "<key:scene.playback.summary>")
@WikiSection(title = "<key:scene.playback.clock.title>", order = 10,
        markdown = "<key:scene.playback.clock.body>")
@WikiExample(title = "<key:scene.playback.example>", code = """
        var playback = new ScenePlaybackController(timeline.duration(), timeline::sample);
        playback.seek(0);
        playback.play();
        // In the frame loop:
        playback.update(deltaSeconds);
        """)
@WikiLink(label = "SceneSequence", target = "page:scene-sequence")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@AiDoc(summary = "Drives play, pause, seek and replay for a scene sampler.",
        criticalRules = {
                "All times are in seconds. Call update once per frame with elapsed seconds.",
                "The constructor starts paused at zero. Call seek(0) to apply the first frame immediately.",
                "Seek clamps to the playback range, reapplies the sampled frame and preserves the playback state until the end.",
                "Play resumes from the current time. Replay samples time zero and starts playback if the duration is positive.",
                "Frame deltas use AnimationClock.sanitizeDelta. Positive deltas are capped at 0.25 seconds."
        }, related = {"SceneSequence", "SceneTimeline", "SceneBuilder"})
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
    @WikiMethod(order = 10, summary = "<key:scene.playback.create>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.playback.invalid.duration>")
    @WikiThrows(type = NullPointerException.class, description = "<key:scene.playback.null.sampler>")
    @AiDoc(order = 10, summary = "Creates playback paused at zero with a fixed duration and a frame sampler.")
    public ScenePlaybackController(
            @WikiParameter(description = "<key:scene.playback.duration>") float durationSeconds,
            @WikiParameter(description = "<key:scene.playback.sampler>") FloatConsumer sample) {
        if (!Float.isFinite(durationSeconds) || durationSeconds < 0) {
            throw new IllegalArgumentException("Playback duration must be finite and nonnegative");
        }
        this.durationSeconds = durationSeconds;
        this.sample = Objects.requireNonNull(sample, "sample");
    }

    /**
     * Starts or resumes playback from the current time. Use {@link #replay()} to restart at the end.
     */
    @WikiMethod(order = 20, summary = "<key:scene.playback.play>")
    @AiDoc(order = 20, summary = "Resumes playback from the current time if it precedes the end.")
    public void play() {
        playing = timeSeconds < durationSeconds;
    }

    /**
     * Pauses playback at the current time.
     */
    @WikiMethod(order = 30, summary = "<key:scene.playback.pause>")
    @AiDoc(order = 30, summary = "Pauses playback at the current time.")
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
    @WikiMethod(order = 40, summary = "<key:scene.playback.seek>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.playback.invalid.seek>")
    @AiDoc(order = 40, summary = "Samples a clamped absolute time, preserving the play or pause state until the end.")
    public void seek(@WikiParameter(description = "<key:scene.playback.target>") float seconds) {
        if (!Float.isFinite(seconds)) throw new IllegalArgumentException("Seek time must be finite");
        timeSeconds = Math.max(0, Math.min(durationSeconds, seconds));
        if (timeSeconds == durationSeconds) pause();
        sample.accept(timeSeconds);
    }

    /**
     * Samples the first frame and starts playback, or stays paused when the duration is zero.
     */
    @WikiMethod(order = 50, summary = "<key:scene.playback.replay>")
    @AiDoc(order = 50, summary = "Samples the first frame and restarts playback.")
    public void replay() {
        playing = durationSeconds > 0;
        seek(0);
    }

    /**
     * Advances playback by the sanitized frame delta and pauses when the end is reached.
     *
     * @param deltaSeconds elapsed seconds since the previous update, sanitized by {@link AnimationClock#sanitizeDelta(float)}
     */
    @WikiMethod(order = 60, summary = "<key:scene.playback.update>")
    @AiDoc(order = 60, summary = "Advances playing scenes using a sanitized frame delta and pauses at the end.")
    public void update(@WikiParameter(description = "<key:scene.playback.delta>") float deltaSeconds) {
        if (!playing) return;
        float delta = AnimationClock.sanitizeDelta(deltaSeconds);
        if (delta > 0) seek(timeSeconds + delta);
    }

    /**
     * @return current time in seconds
     */
    @WikiMethod(order = 70, summary = "<key:scene.playback.time>")
    @WikiReturn("<key:scene.playback.time.return>")
    @AiDoc(order = 70, summary = "Returns the current playback time in seconds.")
    public float timeSeconds() {
        return timeSeconds;
    }

    /**
     * @return whether playback is advancing
     */
    @WikiMethod(order = 80, summary = "<key:scene.playback.playing>")
    @WikiReturn("<key:scene.playback.playing.return>")
    @AiDoc(order = 80, summary = "Reports whether frame updates currently advance playback.")
    public boolean playing() {
        return playing;
    }
}
