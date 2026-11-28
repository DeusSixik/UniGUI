package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.Objects;

/**
 * A reusable, timed pose sampler for a native entity render state.
 *
 * @param durationSeconds one cycle's duration at playback speed 1, finite and positive
 * @param stateType native render-state class accepted by the sampler
 * @param sampler writes the pose at the supplied normalized progress
 * @param <S> native render-state type
 */
@WikiPage(title = "EntityActionClip", slug = "entity-action-clip", category = "<key:scene.category>",
        summary = "<key:entity.action.clip.summary>")
@WikiSection(title = "<key:entity.action.clip.section.timing.title>", markdown = "<key:entity.action.clip.section.timing.body>")
@WikiExample(title = "<key:entity.action.clip.example.swing.title>", code = """
        timeline.track(zombie)
                .position(0, 0, 0, 0).position(1, 0, 0, 1)
                .walk(0, 0, 0.6f).walk(0.8f, 5, 0.6f).walk(1, 6, 0)
                .action(1.1f, MinecraftEntityActions.mainHandSwing(), 1, 0);
        """)
@WikiExample(title = "<key:entity.action.clip.example.golem.title>", code = """
        var attack = new EntityActionClip<>(0.5f, IronGolemRenderState.class,
                (state, progress) -> state.attackTicksRemaining = 10 * (1 - progress));
        timeline.track(golem).action(2, attack);
        """)
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@WikiLink(label = "EntityNode", target = "page:entity-node")
@AiDoc(summary = "A typed, reusable entity render-state sampler with a nominal duration in seconds.",
        criticalRules = {
                "Schedule via SceneTimeline.Track.action(startSeconds, clip[, speed, repeatCount]).",
                "The active interval includes its start and excludes its end. Repeat count counts additional cycles.",
                "Compute the entire action pose from normalized cycle progress. The selected clip can change when seeking."
        }, related = {"EntityNode", "SceneTimeline", "MinecraftEntityActions"})
public record EntityActionClip<S>(float durationSeconds, Class<S> stateType, PoseSampler<S> sampler) {
    /**
     * Validates the clip inputs.
     *
     * @throws IllegalArgumentException if durationSeconds is nonpositive or nonfinite
     * @throws NullPointerException if stateType or sampler is null
     */
    public EntityActionClip {
        if (!Float.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("Action duration must be finite and positive");
        }
        Objects.requireNonNull(stateType, "stateType");
        Objects.requireNonNull(sampler, "sampler");
    }

    /**
     * Samples the clip when the render state is compatible with it.
     *
     * @param state transient native entity render state
     * @param progress normalized progress in [0, 1)
     */
    void apply(Object state, float progress) {
        if (stateType.isInstance(state)) sampler.sample(stateType.cast(state), progress);
    }

    /**
     * Applies an action pose using only its sampled phase and the supplied frame state.
     *
     * @param <S> native render-state type
     */
    @FunctionalInterface
    public interface PoseSampler<S> {
        /**
         * Writes the current pose.
         *
         * @param state transient native state for this frame
         * @param progress normalized progress in [0, 1) through the current cycle
         */
        void sample(S state, float progress);
    }
}
