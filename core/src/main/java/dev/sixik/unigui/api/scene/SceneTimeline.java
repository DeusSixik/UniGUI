package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.Easing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.widgets.rendering.SceneAnchor3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Seekable keyframe animation for scene nodes.
 *
 * <p>{@link #sample(float)} evaluates the timeline at an absolute time in either direction. The same tracks
 * can be played by {@link #play(Scene3D)} or driven by a scrubber. Make custom samplers depend on the supplied
 * time or progress if they must behave the same during playback and seeking.</p>
 * <pre>{@code
 * SceneTimeline timeline = new SceneTimeline();
 * timeline.track(furnace)
 * .visible(0, false).visible(2, true)
 * .position(2, 0, 4, 0)
 * .position(2.8f, 0, 1, 0, AnimationEasing.QUAD_IN)
 * .state(4, "minecraft:furnace[facing=south,lit=true]");
 * timeline.assemble(house, 0, 3);
 * timeline.play(viewport);
 * }</pre>
 * <p>Keys can be added in any order. Between keys, interpolation uses the destination key's easing.
 * Values hold at the first and last keys outside their interval. Visibility and block states change at the
 * key time, inclusive. Properties without keys retain their existing values.</p>
 */
@WikiPage(
        title = "SceneTimeline", slug = "scene-timeline",
        category = "API/3D Scene", order = 170,
        summary = "Seekable keyframe tracks and samplers for scene tutorials.")
@WikiSection(title = "<key:scene.timeline.section.tracks.title>", order = 10, markdown = "<key:scene.timeline.section.tracks.body>")
@WikiSection(title = "<key:scene.timeline.section.methods.title>", order = 15, markdown = "<key:scene.timeline.section.methods.body>")
@WikiSection(title = "<key:scene.timeline.section.stages.title>", order = 25, markdown = "<key:scene.timeline.section.stages.body>")
@WikiSection(title = "<key:scene.timeline.section.effects.title>", order = 30, markdown = "<key:scene.timeline.section.effects.body>")
@WikiSection(title = "<key:scene.timeline.section.playback.title>", order = 20,
        markdown = "<key:scene.timeline.section.playback.body>")
@WikiSection(title = "<key:scene.timeline.section.builder.title>", order = 35,
        markdown = "<key:scene.timeline.section.builder.body>")
@WikiExample(title = "Play and seek a furnace tutorial", code = """
        SceneTimeline tutorial = new SceneTimeline().assemble(floor, 0, 2).duration(5);
        tutorial.track(furnace)
                .visible(0, false).visible(2, true)
                .position(2, 0, 4, 0)
                .position(3, 0, 0, 0, AnimationEasing.QUAD_IN)
                .state(4, "minecraft:furnace[facing=south,lit=true]");
        tutorial.play(viewport);
        // When the user chooses a checkpoint:
        tutorial.stop(viewport);
        tutorial.sample(3.5f);
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "SceneBuilder", target = "page:scene-builder")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "SceneAnchor3D", target = "page:scene-anchor-3d")
@WikiLink(label = "BlockNode", target = "page:block-node")
@WikiLink(label = "EntityNode", target = "page:entity-node")
@WikiLink(label = "EntityActionClip", target = "page:entity-action-clip")
@WikiLink(label = "SceneMaterialEffect", target = "page:scene-material-effect")
@WikiLink(label = "StructureNode", target = "page:structure-node")
@AiDoc(
        summary = "Seekable keyframe tracks and samplers for scene tutorials.",
        purpose = "Drive playback, replay, checkpoint navigation and timeline sliders from one scene definition.",
        criticalRules = {
                "Times are in seconds. `sample` applies the state at absolute time and can seek in either direction.",
                "Easing comes from the destination key. Discrete visibility and block state keys apply from their timestamp inclusive.",
                "Track methods include position, yaw, rotation, scale, head, walk, walkTo, action, visible, visibleBetween and state.",
                "Use `timeline.stage(startSeconds)` to group actions on a local clock. Callbacks receive parent time minus the start, including negative time before the stage. A stage's duration contributes to the parent duration unless the parent has an explicit duration.",
                "On an EntityNode, `track.walkTo(start, end, x, y, z[, easing, phasePerUnit, strideStrength])` adds movement, facing and gait keys. Append segments in chronological order. Each segment comes to a stop and uses distance in the parent XZ plane to advance the gait. The default is linear movement with 8 phase units per scene unit and a peak stride strength of 0.6. Add head and action keys independently.",
                "Use action for a seekable entity clip. The active clip with the latest start time wins. At equal start times, the last added clip wins.",
                "Use `timeline.materialEffect(start, end, preset, endBehavior, nodes...)` for a seekable surface effect. This supplies its own shader time in local timeline seconds. Use `track(node).materialEffect(seconds, effect)` for discrete changes to the base effect.",
                "EffectEnd.RESTORE uses the sampled base effect at the end, HOLD freezes the final shader time, and HIDE suppresses the target subtree. Use the returned clip's `setEffect(preset, endBehavior)` to switch presets for all its targets. Null disables the clip. Sample again to apply the selection.",
                "Use head and walk on EntityNode tracks. Walk drives native locomotion on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.x. Use state on BlockNode tracks.",
                "Make onSample and during callbacks derive state from the supplied absolute time or progress.",
                "For custom playback controls, create `new ScenePlaybackController(timeline.duration(), timeline::sample)` and pass frame deltas to `update`. Use `play`, `pause`, `seek`, and `replay`. Seek preserves playback state until the end.",
                "Use `SceneBuilder` to populate a scene and compile timed commands into a SceneTimeline. Use `sequence.timeline()` to add tracks that need the low-level API.",
                "Use `timeline.trackAnchor(anchor)` to keyframe a UI anchor's local or world position, screen offset in logical UI units, and visibility. `visibleBetween(from, to)` uses an inclusive start and an exclusive end.",
                "Use `stop(viewport)` before scrubbing manually. `play(viewport)` restarts from time zero."
        },
        related = {"Scene3D", "SceneBuilder", "SceneNode", "BlockNode", "EntityNode", "StructureNode"},
        examples = @AiDoc.Example(title = "Play and seek a furnace tutorial", code = """
                SceneTimeline tutorial = new SceneTimeline().assemble(floor, 0, 2).duration(5);
                tutorial.track(furnace)
                        .visible(0, false).visible(2, true)
                        .position(2, 0, 4, 0)
                        .position(3, 0, 0, 0, AnimationEasing.QUAD_IN)
                        .state(4, "minecraft:furnace[facing=south,lit=true]");
                tutorial.play(viewport);
                // When the user chooses a checkpoint:
                tutorial.stop(viewport);
                tutorial.sample(3.5f);
                """))
public final class SceneTimeline {
    /**
     * Tracks indexed by node.
     */
    private final Map<SceneNode, Track> tracks = new IdentityHashMap<>();
    /** UI anchor tracks indexed by their target, or null before the first track is added. */
    private Map<SceneAnchor3D, AnchorTrack> anchorTracks;
    /**
     * Order in which tracks are applied.
     */
    private final List<Track> order = new ArrayList<>();
    /**
     * Custom samplers and stages invoked after the node tracks on each frame.
     */
    private final List<Sampler> samplers = new ArrayList<>();
    /**
     * Explicit duration in seconds, or a negative value to infer it.
     */
    private float explicitDuration = -1;
    /**
     * Latest key time or interval end in seconds.
     */
    private float lastKey;

    /**
     * Receives absolute time or normalized progress.
     */
    @FunctionalInterface
    public interface Sampler {
        /**
         * Samples the current value.
         *
         * @param value time in seconds or progress in [0, 1].
         */
        void sample(float value);
    }

    /**
     * Gets or creates a track for the node.
     *
     * @param node animated node.
     * @return the node's track.
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 10,
            summary = "Gets or creates a track for the node.")
    @WikiReturn("the node's track")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 10,
            summary = "Gets or creates a track for the node.")
    public Track track(
            @WikiParameter(description = "animated node.") SceneNode node) {
        Objects.requireNonNull(node, "node");
        return tracks.computeIfAbsent(node, key -> {
            Track track = new Track(key);
            order.add(track);
            return track;
        });
    }

    /**
     * Gets or creates the track for a projected UI anchor. Its keys are sampled after the node tracks,
     * in registration order with stages and callbacks.
     *
     * @param anchor anchor returned by {@link Scene3D#addAnchor}
     * @return the anchor's track on this timeline
     * @throws NullPointerException if the anchor is null
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 15,
            summary = "<key:scene.timeline.anchor.summary>")
    @WikiReturn("<key:scene.timeline.anchor.return>")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 15,
            summary = "Gets or creates a track for the position, screen offset and visibility of an anchored widget.")
    public AnchorTrack trackAnchor(@WikiParameter(description = "<key:scene.timeline.anchor.param>") SceneAnchor3D anchor) {
        Objects.requireNonNull(anchor, "anchor");
        if (anchorTracks == null) anchorTracks = new IdentityHashMap<>();
        return anchorTracks.computeIfAbsent(anchor, key -> {
            AnchorTrack track = new AnchorTrack(key);
            samplers.add(track::sample);
            return track;
        });
    }

    /**
     * Animates custom progress during an interval. Progress is zero before the start and one after the end.
     *
     * @param start    start time in seconds.
     * @param end      end time in seconds, at or after start.
     * @param easing   easing function, or null for linear progress.
     * @param progress receives eased progress when sampled.
     * @return this timeline.
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 20,
            summary = "Animates custom progress during an interval. Progress is zero before the start and one after the end.")
    @WikiReturn("this timeline")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 20,
            summary = "Animates custom progress during an interval. Progress is zero before the start and one after the end.")
    public SceneTimeline during(
            @WikiParameter(description = "start time in seconds.") float start,
            @WikiParameter(description = "end time in seconds, at or after start.") float end,
            @WikiParameter(description = "easing function, or null for linear progress.") Easing easing,
            @WikiParameter(description = "receives eased progress when sampled.") Sampler progress) {
        requireTime(start);
        requireTime(end);
        if (end < start) throw new IllegalArgumentException("Interval ends before it starts: " + start + " > " + end);
        Objects.requireNonNull(progress, "progress");
        Easing curve = easing == null ? AnimationEasing.LINEAR : easing;
        float length = end - start;
        samplers.add(seconds -> progress.sample(curve.apply(length == 0
                ? (seconds >= end ? 1 : 0) : Easing.clamp01((seconds - start) / length))));
        lastKey = Math.max(lastKey, end);
        return this;
    }

    /**
     * Invokes a sampler with the absolute time, for example to control the visibility of an anchored label.
     *
     * @param sampler receives time in seconds.
     * @return this timeline.
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 30,
            summary = "Invokes a sampler with the absolute time, for example to control the visibility of an anchored label.")
    @WikiReturn("this timeline")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 30,
            summary = "Invokes a sampler with the absolute time, for example to control the visibility of an anchored label.")
    public SceneTimeline onSample(
            @WikiParameter(description = "receives time in seconds.") Sampler sampler) {
        samplers.add(Objects.requireNonNull(sampler, "sampler"));
        return this;
    }

    /**
     * Creates a stage that runs on time relative to its start. The parent samples stages and callbacks in
     * registration order after its own node tracks. Local time is {@code parentTime - startSeconds}; it is
     * negative before the start so samplers can restore inactive state. The stage continues to be sampled
     * after its duration, like any timeline driven by {@link #sample(float)}.
     *
     * @param startSeconds finite nonnegative start time on this timeline, in seconds
     * @return the new child timeline to configure in local seconds
     * @throws IllegalArgumentException if the start time is negative or nonfinite
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 35,
            summary = "<key:scene.timeline.stage.summary>")
    @WikiReturn("<key:scene.timeline.stage.return>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.timeline.stage.invalid>")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 35,
            summary = "Creates a child timeline sampled in seconds relative to its start, including negative time before it starts.")
    public SceneTimeline stage(@WikiParameter(description = "<key:scene.timeline.stage.start>") float startSeconds) {
        requireTime(startSeconds);
        SceneTimeline child = new SceneTimeline();
        addStage(startSeconds, child);
        return child;
    }

    /**
     * Attaches a newly created local timeline.
     *
     * @param startSeconds validated start in parent seconds
     * @param child        unattached local timeline
     */
    void addStage(float startSeconds, SceneTimeline child) {
        samplers.add(new Stage(startSeconds, child));
    }

    /**
     * Schedules a surface effect across one or more nodes. The shader uses this timeline's local seconds,
     * even inside a stage. The interval includes its start and excludes its end. From the end onward,
     * {@code endBehavior} determines whether the clip still applies. Among applicable clips on this timeline,
     * the latest start wins, with the last added clip winning ties.
     *
     * @param start       inclusive start in local seconds, finite and nonnegative
     * @param end         exclusive end in local seconds, finite and strictly greater than start
     * @param effect      surface effect to schedule, or null to disable the clip
     * @param endBehavior behavior at and after the end
     * @param nodes       one or more nodes whose base visibility and effect are preserved beneath the clip
     * @return clip whose effect and end behavior can be changed before another sample
     * @throws IllegalArgumentException if the interval is invalid or there are no targets
     * @throws NullPointerException     if the target array, any target or end behavior is null
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 45, summary = "<key:scene.timeline.effect.summary>")
    @WikiReturn("<key:scene.timeline.effect.return>")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 45,
            summary = "Schedules a replaceable, seekable surface effect across one or more scene nodes.")
    public MaterialEffectClip materialEffect(
            @WikiParameter(description = "<key:scene.timeline.effect.start>") float start,
            @WikiParameter(description = "<key:scene.timeline.effect.end>") float end,
            @WikiParameter(description = "<key:scene.timeline.effect.preset>") SceneMaterialEffect effect,
            @WikiParameter(description = "<key:scene.timeline.effect.finish>") EffectEnd endBehavior,
            @WikiParameter(description = "<key:scene.timeline.effect.nodes>") SceneNode... nodes) {
        requireTime(start);
        requireTime(end);
        if (end <= start) throw new IllegalArgumentException("Material effect end must be greater than its start");
        Objects.requireNonNull(nodes, "nodes");
        if (nodes.length == 0) throw new IllegalArgumentException("Material effect requires at least one node");
        for (SceneNode node : nodes) Objects.requireNonNull(node, "node");

        var clip = new MaterialEffectClip(start, end).setEffect(effect, endBehavior);
        for (SceneNode node : nodes) {
            Track track = track(node);
            if (track.materialClips == null) track.materialClips = new ArrayList<>();
            if (track.materialClips.contains(clip)) continue;
            track.materialClips.add(clip);
            track.materialClips.sort(Comparator.comparingDouble(key -> key.start));
        }
        lastKey = Math.max(lastKey, end);
        return clip;
    }

    /**
     * Reveals the structure from bottom to top, starting with no visible layers.
     *
     * @param structure structure to reveal.
     * @param start     start time in seconds.
     * @param end       end time in seconds.
     * @return this timeline.
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 40,
            summary = "Reveals the structure from bottom to top, starting with no visible layers.")
    @WikiReturn("this timeline")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 40,
            summary = "Reveals the structure from bottom to top, starting with no visible layers.")
    public SceneTimeline assemble(
            @WikiParameter(description = "structure to reveal.") StructureNode structure,
            @WikiParameter(description = "start time in seconds.") float start,
            @WikiParameter(description = "end time in seconds.") float end) {
        Objects.requireNonNull(structure, "structure");
        return during(start, end, AnimationEasing.LINEAR, progress -> {
            int[] range = structure.layerRange(Integer.MAX_VALUE);
            if (range == null) structure.setTopVisibleY(Integer.MAX_VALUE);
            else if (progress >= 1) structure.setTopVisibleY(Integer.MAX_VALUE);
            else structure.setTopVisibleY(range[0] - 1 + (int) (progress * (range[1] - range[0] + 1)));
        });
    }

    /**
     * Sets the duration explicitly, for example to hold the last frame past the last key.
     *
     * @param seconds duration in seconds.
     * @return this timeline.
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 50,
            summary = "Sets the duration explicitly, for example to hold the last frame past the last key.")
    @WikiReturn("this timeline")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 50,
            summary = "Sets the duration explicitly, for example to hold the last frame past the last key.")
    public SceneTimeline duration(
            @WikiParameter(description = "duration in seconds.") float seconds) {
        requireTime(seconds);
        explicitDuration = seconds;
        return this;
    }

    /**
     * Reads the playback duration.
     *
     * @return the explicit duration or the latest key, interval or stage end, in seconds, capped at Float.MAX_VALUE.
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 50,
            summary = "Reads the playback duration.")
    @WikiReturn("the explicit duration or the latest key, interval or stage end, in seconds")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 50,
            summary = "Reads the playback duration.")
    public float duration() {
        if (explicitDuration >= 0) return explicitDuration;
        double end = lastKey;
        for (Sampler sampler : samplers) {
            if (sampler instanceof Stage stage) {
                end = Math.max(end, (double) stage.start() + stage.timeline().duration());
            }
        }
        return (float) Math.min(end, Float.MAX_VALUE);
    }

    /**
     * Applies the timeline state at the supplied time.
     *
     * @param seconds time since the start, in seconds.
     */
    @WikiMethod(section = "Playback", sectionOrder = 20, order = 10,
            summary = "Applies the timeline state at the supplied time.")
    @AiDoc(section = "Playback", sectionOrder = 20, order = 10,
            summary = "Applies the timeline state at the supplied time.")
    public void sample(
            @WikiParameter(description = "time since the start, in seconds.") float seconds) {
        float time = Float.isFinite(seconds) ? seconds : 0;
        for (int i = 0; i < order.size(); i++) order.get(i).sample(time);
        for (int i = 0; i < samplers.size(); i++) samplers.get(i).sample(time);
    }

    /**
     * Plays the timeline once from the beginning. Calling again restarts playback.
     *
     * @param viewport viewport that drives animation ticks.
     * @return this timeline.
     */
    @WikiMethod(section = "Playback", sectionOrder = 20, order = 20,
            summary = "Plays the timeline once from the beginning. Calling again restarts playback.")
    @WikiReturn("this timeline")
    @AiDoc(section = "Playback", sectionOrder = 20, order = 20,
            summary = "Plays the timeline once from the beginning. Calling again restarts playback.")
    public SceneTimeline play(
            @WikiParameter(description = "viewport that drives animation ticks.") Scene3D viewport) {
        viewport.stopParameterAnimation(this);
        sample(0);
        float length = duration();
        if (length > 0) {
            viewport.animateParameterFrom(this, 0, this::sample, length,
                    TransitionSpec.of(length, AnimationEasing.LINEAR));
        }
        return this;
    }

    /**
     * Stops playback and retains the current frame.
     *
     * @param viewport viewport passed to {@link #play(Scene3D)}.
     */
    @WikiMethod(section = "Playback", sectionOrder = 20, order = 30,
            summary = "Stops playback and retains the current frame.")
    @AiDoc(section = "Playback", sectionOrder = 20, order = 30,
            summary = "Stops playback and retains the current frame.")
    public void stop(
            @WikiParameter(description = "viewport passed to `play(Scene3D)`.") Scene3D viewport) {
        viewport.stopParameterAnimation(this);
    }

    /**
     * Validates a time.
     *
     * @param seconds time in seconds.
     * @throws IllegalArgumentException if time is negative or nonfinite.
     */
    static void requireTime(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Time must be finite and non-negative: " + seconds);
        }
    }

    /**
     * A local timeline placed on its parent's clock.
     *
     * @param start    offset in parent seconds
     * @param timeline child that owns the local tracks and callbacks
     */
    private record Stage(float start, SceneTimeline timeline) implements Sampler {
        /**
         * Samples the child at the parent time minus the stage offset, saturated to the finite float range.
         *
         * @param seconds time in parent seconds
         */
        @Override
        public void sample(float seconds) {
            timeline.sample(Math.max(-Float.MAX_VALUE, seconds - start));
        }
    }

    /**
     * Determines what a scheduled material effect does once its interval ends.
     */
    public enum EffectEnd {
        /**
         * Releases the clip and displays the sampled base surface or another applicable clip.
         */
        RESTORE,
        /**
         * Keeps the effect with shader time frozen at the end of its interval.
         */
        HOLD,
        /**
         * Hides the node and its subtree while this clip continues to apply.
         */
        HIDE
    }

    /**
     * A scheduled surface effect shared by all its targets. Changes take effect on the next timeline sample.
     */
    public static final class MaterialEffectClip {
        /**
         * Inclusive start in local seconds.
         */
        private final float start;
        /**
         * Exclusive end in local seconds.
         */
        private final float end;
        /**
         * Selected surface effect with local timing attached, or null when disabled.
         */
        private SceneMaterialEffect effect;
        /**
         * Behavior from the end onward.
         */
        private EffectEnd endBehavior;

        /**
         * Stores a validated interval.
         *
         * @param start inclusive start in local seconds
         * @param end   exclusive end in local seconds
         */
        private MaterialEffectClip(float start, float end) {
            this.start = start;
            this.end = end;
        }

        /**
         * Replaces the effect for all targets, using the clip's existing interval as shader timing.
         *
         * @param effect      surface effect, or null to disable the clip including its end behavior
         * @param endBehavior behavior at and after the end
         * @return this clip
         * @throws NullPointerException if end behavior is null
         */
        public MaterialEffectClip setEffect(SceneMaterialEffect effect, EffectEnd endBehavior) {
            Objects.requireNonNull(endBehavior, "endBehavior");
            this.effect = effect == null ? null : effect.withTiming(start, end - start);
            this.endBehavior = endBehavior;
            return this;
        }
    }

    /** Seekable keys for a projected UI anchor's position, screen offset and visibility. */
    public final class AnchorTrack {
        /** Anchor receiving the sampled keys. */
        private final SceneAnchor3D anchor;
        /** Position keys in scene or node-local units. */
        private final Channel position = new Channel(3);
        /** Horizontal and vertical offset keys in logical UI units. */
        private final Channel screenOffset = new Channel(2);
        /** Visibility keys encoded as zero or one. */
        private final Channel visible = new Channel(1);
        /** Reused buffer for applying key values. */
        private final float[] values = new float[3];

        /** @param anchor anchor to animate */
        private AnchorTrack(SceneAnchor3D anchor) {
            this.anchor = anchor;
        }

        /**
         * Adds a position key with linear interpolation from the preceding key.
         *
         * @param seconds finite nonnegative key time in seconds
         * @param x finite X in the anchor's coordinate space
         * @param y finite Y in the anchor's coordinate space
         * @param z finite Z in the anchor's coordinate space
         * @return this anchor track
         * @throws IllegalArgumentException if the time or coordinates are invalid
         */
        public AnchorTrack position(float seconds, float x, float y, float z) {
            return position(seconds, x, y, z, null);
        }

        /**
         * Adds a position key in world or node-local coordinates, according to the anchor's binding.
         *
         * @param seconds finite nonnegative key time in seconds
         * @param x finite X in the anchor's coordinate space
         * @param y finite Y in the anchor's coordinate space
         * @param z finite Z in the anchor's coordinate space
         * @param easing interpolation into this key, or null for linear
         * @return this anchor track
         * @throws IllegalArgumentException if the time or coordinates are invalid
         */
        public AnchorTrack position(float seconds, float x, float y, float z, Easing easing) {
            position.add(seconds, easing, x, y, z);
            return this;
        }

        /**
         * Adds a screen-space offset key applied after the point is projected.
         *
         * @param seconds finite nonnegative key time in seconds
         * @param x finite horizontal offset in logical UI units, positive to the right
         * @param y finite vertical offset in logical UI units, positive down
         * @param easing interpolation into this key, or null for linear
         * @return this anchor track
         * @throws IllegalArgumentException if the time or offsets are invalid
         */
        public AnchorTrack screenOffset(float seconds, float x, float y, Easing easing) {
            screenOffset.add(seconds, easing, x, y, 0);
            return this;
        }

        /**
         * Adds a discrete visibility key that applies from the specified time, inclusive.
         *
         * @param seconds finite nonnegative key time in seconds
         * @param shown true to show the widget when the point is projectable
         * @return this anchor track
         * @throws IllegalArgumentException if the time is invalid
         */
        public AnchorTrack visible(float seconds, boolean shown) {
            visible.add(seconds, STEP, shown ? 1 : 0, 0, 0);
            return this;
        }

        /**
         * Shows the widget only during the interval {@code [from, to)}, subject to projection.
         *
         * @param from inclusive start time in seconds, finite and nonnegative
         * @param to exclusive end time in seconds, finite and at or after the start
         * @return this anchor track
         * @throws IllegalArgumentException if the interval is invalid
         */
        public AnchorTrack visibleBetween(float from, float to) {
            requireTime(from);
            requireTime(to);
            if (to < from) throw new IllegalArgumentException("Anchor visibility interval ends before it starts");
            return visible(0, false).visible(from, true).visible(to, false);
        }

        /**
         * Applies the sampled anchor properties.
         * @param seconds local timeline time in seconds
         */
        private void sample(float seconds) {
            if (position.sample(seconds, values)) anchor.position(values[0], values[1], values[2]);
            if (screenOffset.sample(seconds, values)) anchor.screenOffset(values[0], values[1]);
            if (visible.sample(seconds, values)) anchor.visible(values[0] > 0.5f);
        }
    }

    /** Keyframes for one node's transform, visibility, entity pose and block state. */
    public final class Track {
        /**
         * Animated node.
         */
        private final SceneNode node;
        /**
         * Position XYZ in scene units.
         */
        private final Channel position = new Channel(3);
        /**
         * Yaw and pitch in radians.
         */
        private final Channel rotation = new Channel(2);
        /**
         * Dimensionless XYZ scale.
         */
        private final Channel scale = new Channel(3);
        /**
         * Visibility stored as one or zero.
         */
        private final Channel visible = new Channel(1);
        /**
         * Entity head yaw and pitch in radians.
         */
        private final Channel head = new Channel(2);
        /**
         * Native accumulated gait phase, allocated on the first walk key.
         */
        private Channel walkPhase;
        /**
         * Normalized stride strength in [0, 1], allocated alongside {@link #walkPhase}.
         */
        private Channel walkStrength;
        /**
         * Scheduled actions in ascending start time, or null for tracks without actions.
         */
        private List<ActionKey> actions;
        /**
         * Base surface keys sorted by time, or null if this track leaves the base surface alone.
         */
        private List<MaterialKey> materials;
        /**
         * Scheduled surface overrides sorted by start, or null when none were registered.
         */
        private List<MaterialEffectClip> materialClips;
        /**
         * Block state key times in seconds.
         */
        private float[] stateTimes = new float[0];
        /**
         * Block states in the order of {@link #stateTimes}.
         */
        private String[] states = new String[0];
        /**
         * Last applied state, used to avoid resetting the block cache.
         */
        private String appliedState;
        /**
         * Reusable rotation quaternion.
         */
        private final MutableQuat quaternion = new MutableQuat();
        /**
         * Reusable buffer for sampled values.
         */
        private final float[] values = new float[3];

        /**
         * Creates an empty track.
         *
         * @param node animated node.
         */
        private Track(SceneNode node) {
            this.node = node;
        }

        /**
         * Adds a position key with linear interpolation from the preceding key.
         *
         * @param seconds key time in seconds.
         * @param x       parent-space X in scene units.
         * @param y       parent-space Y in scene units.
         * @param z       parent-space Z in scene units.
         * @return this track.
         */
        public Track position(float seconds, float x, float y, float z) {
            return position(seconds, x, y, z, null);
        }

        /**
         * Adds a position key.
         *
         * @param seconds key time in seconds.
         * @param x       parent-space X in scene units.
         * @param y       parent-space Y in scene units.
         * @param z       parent-space Z in scene units.
         * @param easing  interpolation from the preceding key, or null for linear.
         * @return this track.
         */
        public Track position(float seconds, float x, float y, float z, Easing easing) {
            position.add(seconds, easing, x, y, z);
            return this;
        }

        /**
         * Records a discrete position change and captures the unanimated position as the initial key.
         * A relative position adds the supplied delta to the position sampled at {@code seconds}.
         *
         * @param seconds  key time in seconds
         * @param x        parent-space X coordinate or delta in scene units
         * @param y        parent-space Y coordinate or delta in scene units
         * @param z        parent-space Z coordinate or delta in scene units
         * @param relative true to add the coordinates to the sampled position
         * @throws IllegalArgumentException if the time or resulting coordinates are invalid
         */
        void stepPosition(float seconds, float x, float y, float z, boolean relative) {
            requirePositionTime(seconds);
            seedPosition();
            if (relative) {
                position.sample(seconds, values);
                x += values[0];
                y += values[1];
                z += values[2];
            }
            position(seconds, x, y, z, STEP);
        }

        /**
         * Adds a builder key with interpolation from the previous position key.
         *
         * @param seconds key time in seconds
         * @param x       finite parent-space X in scene units
         * @param y       finite parent-space Y in scene units
         * @param z       finite parent-space Z in scene units
         * @param easing  interpolation into this key, or null for linear
         * @throws IllegalArgumentException if the key time overlaps a timed move
         */
        void stepPositionKey(float seconds, float x, float y, float z, Easing easing) {
            requirePositionTime(seconds);
            position(seconds, x, y, z, easing);
        }

        /**
         * Holds the sampled position until {@code start}, then interpolates each component as
         * {@code from + (destination - from) * easing((t - start) / (end - start))} for sample time {@code t}.
         *
         * @param start  inclusive move start in seconds
         * @param end    arrival time in seconds, strictly after start
         * @param x      finite destination X in parent-space scene units
         * @param y      finite destination Y in parent-space scene units
         * @param z      finite destination Z in parent-space scene units
         * @param easing movement curve, or null for linear
         * @throws IllegalArgumentException if the interval overlaps another position command or displacement is nonfinite
         */
        void movePosition(float start, float end, float x, float y, float z, Easing easing) {
            requirePositionTime(start);
            seedPosition();
            position.sample(start, values);
            if (!Float.isFinite(x - values[0]) || !Float.isFinite(y - values[1]) || !Float.isFinite(z - values[2])) {
                throw new IllegalArgumentException("Move displacement must be finite");
            }
            position(start, values[0], values[1], values[2], STEP);
            position(end, x, y, z, easing == null ? null : progress -> progress == 0 ? 0 : easing.apply(progress));
        }

        /**
         * Captures the node's current position as the initial key if a builder command needs it.
         */
        private void seedPosition() {
            if (position.times.length == 0) {
                var initial = node.position(new MutableVec3());
                position(0, initial.x(), initial.y(), initial.z());
            }
        }

        /**
         * Checks that a builder command can follow the position keys already recorded in time order.
         *
         * @param seconds command time in seconds
         * @throws IllegalArgumentException if a previously scheduled move still owns the position at this time
         */
        private void requirePositionTime(float seconds) {
            int count = position.times.length;
            if (count != 0 && position.times[count - 1] > seconds) {
                throw new IllegalArgumentException("Position command at " + seconds
                        + " overlaps a move ending at " + position.times[count - 1]);
            }
        }

        /**
         * Adds a yaw key with a linear transition.
         *
         * @param seconds key time in seconds.
         * @param yaw     yaw in radians.
         * @return this track.
         */
        public Track yaw(float seconds, float yaw) {
            return rotation(seconds, yaw, 0, null);
        }

        /**
         * Adds a rotation key. Angles interpolate as scalars, so a rotation greater than π can be specified directly.
         *
         * @param seconds key time in seconds.
         * @param yaw     yaw in radians.
         * @param pitch   pitch in radians.
         * @param easing  interpolation from the preceding key, or null for linear.
         * @return this track.
         */
        public Track rotation(float seconds, float yaw, float pitch, Easing easing) {
            rotation.add(seconds, easing, yaw, pitch, 0);
            return this;
        }

        /**
         * Adds a uniform scale key with a linear transition.
         *
         * @param seconds key time in seconds.
         * @param scale   dimensionless factor along every axis.
         * @return this track.
         */
        public Track scale(float seconds, float scale) {
            return scale(seconds, scale, scale, scale, null);
        }

        /**
         * Adds a scale key.
         *
         * @param seconds key time in seconds.
         * @param x       local X scale factor.
         * @param y       local Y scale factor.
         * @param z       local Z scale factor.
         * @param easing  interpolation from the preceding key, or null for linear.
         * @return this track.
         */
        public Track scale(float seconds, float x, float y, float z, Easing easing) {
            scale.add(seconds, easing, x, y, z);
            return this;
        }

        /**
         * Adds an {@link EntityNode} head rotation key relative to the body.
         *
         * @param seconds key time in seconds.
         * @param yaw     yaw in radians.
         * @param pitch   pitch in radians.
         * @param easing  interpolation from the preceding key, or null for linear.
         * @return this track.
         * @throws IllegalArgumentException if the node is not an {@link EntityNode}.
         */
        public Track head(float seconds, float yaw, float pitch, Easing easing) {
            if (!(node instanceof EntityNode)) throw new IllegalArgumentException("Only an EntityNode has a head");
            head.add(seconds, easing, yaw, pitch, 0);
            return this;
        }

        /**
         * Adds a vanilla locomotion key with linear interpolation. The model's idle clock uses the sampled
         * timeline time. Pair these keys with position keys to move the entity through the scene.
         *
         * @param seconds  key time in seconds
         * @param phase    accumulated gait phase in native model units
         * @param strength normalized stride strength in [0, 1]
         * @return this track
         * @throws IllegalArgumentException if the key values are invalid or the node is not an entity
         */
        public Track walk(float seconds, float phase, float strength) {
            return walk(seconds, phase, strength, null);
        }

        /**
         * Adds a vanilla locomotion key with an easing curve.
         *
         * @param seconds  key time in seconds
         * @param phase    accumulated gait phase in native model units
         * @param strength normalized stride strength in [0, 1]
         * @param easing   interpolation from the preceding key, or null for linear
         * @return this track
         * @throws IllegalArgumentException if the key values are invalid or the node is not an entity
         */
        public Track walk(float seconds, float phase, float strength, Easing easing) {
            if (!(node instanceof EntityNode))
                throw new IllegalArgumentException("Only an EntityNode has a walk animation");
            if (!(strength >= 0 && strength <= 1))
                throw new IllegalArgumentException("Walk strength must be in [0, 1]");
            addWalkKey(seconds, phase, strength, easing);
            return this;
        }

        /**
         * Adds a straight walk using linear movement, 8 native phase units per scene unit and peak strength 0.6.
         *
         * @param start start time in seconds
         * @param end   arrival time in seconds, strictly after start
         * @param x     destination X relative to the parent, in scene units
         * @param y     destination Y relative to the parent, in scene units
         * @param z     destination Z relative to the parent, in scene units
         * @return this track
         * @throws IllegalArgumentException if the node is not an entity or the time or coordinates are invalid
         * @see #walkTo(float, float, float, float, float, Easing, float, float)
         */
        public Track walkTo(float start, float end, float x, float y, float z) {
            return walkTo(start, end, x, y, z, null, 8, 0.6f);
        }

        /**
         * Adds position, facing and gait keys for a straight walk. The origin and phase come from existing keys
         * sampled at {@code start}, or from the node's current position and phase zero if there are no keys.
         * Append segments in chronological order and add manual facing keys afterward where needed.
         *
         * <p>For parent-space horizontal displacement {@code (dx, dz)}, the yaw of the local +Z axis is
         * {@code atan2(dx, dz)}. The accumulated gait phase is {@code basePhase + hypot(dx, dz) * phasePerUnit * p},
         * where {@code p} is the same eased progress used for position. Stride strength ramps in and out over
         * the first and last fifths of the segment. A zero horizontal displacement keeps the existing facing
         * and adds no gait phase or stride strength.</p>
         *
         * @param start          start time in seconds
         * @param end            arrival time in seconds, strictly after start
         * @param x              destination X relative to the parent, in scene units
         * @param y              destination Y relative to the parent, in scene units
         * @param z              destination Z relative to the parent, in scene units
         * @param easing         deterministic, nondecreasing movement curve, or null for linear; output is clamped to [0, 1]
         * @param phasePerUnit   finite positive gait phase increment per horizontal scene unit
         * @param strideStrength peak stride strength in [0, 1]
         * @return this track
         * @throws IllegalArgumentException if the node is not an entity, the interval or parameters are invalid,
         *                                  or the displacement or resulting phase exceeds the float range
         */
        public Track walkTo(float start, float end, float x, float y, float z, Easing easing,
                            float phasePerUnit, float strideStrength) {
            if (!(node instanceof EntityNode)) throw new IllegalArgumentException("Only an EntityNode can walk");
            requireTime(start);
            requireTime(end);
            if (end <= start || !Float.isFinite(phasePerUnit) || phasePerUnit <= 0
                    || !(strideStrength >= 0 && strideStrength <= 1)) {
                throw new IllegalArgumentException("Walk requires a positive interval and phase rate and strength in [0, 1]");
            }

            MutableVec3 origin = position.sample(start, values)
                    ? new MutableVec3(values[0], values[1], values[2]) : node.position(new MutableVec3());
            float dx = x - origin.x();
            float dy = y - origin.y();
            float dz = z - origin.z();
            double distance = Math.hypot(dx, dz);
            float firstPhase = walkPhase != null && walkPhase.sample(start, values) ? values[0] : 0;
            float lastPhase = (float) (firstPhase + distance * phasePerUnit);
            if (!Float.isFinite(dx) || !Float.isFinite(dy) || !Float.isFinite(dz)
                    || !Float.isFinite(lastPhase - firstPhase)) {
                throw new IllegalArgumentException("Walk displacement and gait phase must be finite");
            }

            Easing curve = easing == null ? null : p -> p <= 0 ? 0 : Easing.clamp01(easing.apply(p));
            position(start, origin.x(), origin.y(), origin.z()).position(end, x, y, z, curve);
            addWalkKey(start, firstPhase, 0, null);
            walkPhase.add(end, curve, lastPhase, 0, 0);
            walkStrength.add(end, null, 0, 0, 0);
            if (distance > 0) {
                float facing = (float) Math.atan2(dx, dz);
                rotation(start, facing, 0, STEP).yaw(end, facing);
                float ramp = (end - start) * 0.2f;
                float rise = start + ramp;
                float fall = end - ramp;
                if (start < rise && rise <= fall && fall < end) {
                    walkStrength.add(rise, null, strideStrength, 0, 0);
                    walkStrength.add(fall, null, strideStrength, 0, 0);
                }
            }
            return this;
        }

        /**
         * Adds phase and strength at the same time to the independent gait channels.
         *
         * @param seconds  key time in seconds
         * @param phase    accumulated gait phase in native model units
         * @param strength stride strength in [0, 1]
         * @param easing   interpolation into both values, or null for linear
         * @throws IllegalArgumentException if a key time or value is invalid
         */
        private void addWalkKey(float seconds, float phase, float strength, Easing easing) {
            if (walkPhase == null) {
                walkPhase = new Channel(1);
                walkStrength = new Channel(1);
            }
            walkPhase.add(seconds, easing, phase, 0, 0);
            walkStrength.add(seconds, easing, strength, 0, 0);
        }

        /**
         * Plays one action cycle at its default speed.
         *
         * @param start inclusive start time in seconds
         * @param clip  a reusable entity action
         * @return this track
         * @throws IllegalArgumentException if the interval is invalid or the node is not an entity
         * @throws NullPointerException     if clip is null
         */
        public Track action(float start, EntityActionClip<?> clip) {
            return action(start, clip, 1, 0);
        }

        /**
         * Schedules one or more cycles of an entity action. The action ends at
         * {@code start + (1 + repeatCount) * duration / speed}, where duration is the clip's nominal cycle
         * duration in seconds. The end is exclusive. When intervals overlap, the latest start wins, with the
         * last added clip winning ties.
         *
         * @param start       inclusive start time in seconds
         * @param clip        a reusable entity action
         * @param speed       finite positive speed multiplier
         * @param repeatCount number of additional cycles, at least zero
         * @return this track
         * @throws IllegalArgumentException if the interval is invalid or the node is not an entity
         * @throws NullPointerException     if clip is null
         */
        public Track action(float start, EntityActionClip<?> clip, float speed, int repeatCount) {
            if (!(node instanceof EntityNode)) throw new IllegalArgumentException("Only an EntityNode has actions");
            requireTime(start);
            Objects.requireNonNull(clip, "clip");
            if (!Float.isFinite(speed) || speed <= 0 || repeatCount < 0) {
                throw new IllegalArgumentException("Action speed must be positive and repeat count nonnegative");
            }
            double cycleSeconds = clip.durationSeconds() / (double) speed;
            float end = (float) (start + (repeatCount + 1.0) * cycleSeconds);
            if (!Float.isFinite(end) || end <= start) {
                throw new IllegalArgumentException("Action interval exceeds timeline precision or range");
            }
            if (actions == null) actions = new ArrayList<>();
            actions.add(new ActionKey(start, end, cycleSeconds, clip));
            actions.sort(Comparator.comparingDouble(ActionKey::start));
            lastKey = Math.max(lastKey, end);
            return this;
        }

        /**
         * Adds a visibility key that takes effect at the specified time, inclusive.
         *
         * @param seconds key time in seconds.
         * @param visible visibility after the key.
         * @return this track.
         */
        public Track visible(float seconds, boolean visible) {
            this.visible.add(seconds, STEP, visible ? 1 : 0, 0, 0);
            return this;
        }

        /**
         * Records a visibility step, preserving the original flag before the first key.
         *
         * @param seconds key time in seconds
         * @param shown   visibility at and after the key
         */
        void stepVisibility(float seconds, boolean shown) {
            if (visible.times.length == 0) visible(0, node.visible());
            visible(seconds, shown);
        }

        /**
         * Shows the node only during {@code [from, to)}.
         *
         * @param from inclusive start time in seconds.
         * @param to   exclusive end time in seconds.
         * @return this track.
         */
        public Track visibleBetween(float from, float to) {
            return visible(0, false).visible(from, true).visible(to, false);
        }

        /**
         * Adds a discrete base surface key. The first value holds before its time and the last key wins
         * at equal times. Scheduled effects can temporarily overlay the base surface.
         *
         * @param seconds finite nonnegative key time in seconds
         * @param effect  base effect, or null for the default surface
         * @return this track
         * @throws IllegalArgumentException if the time is negative or nonfinite
         */
        public Track materialEffect(float seconds, SceneMaterialEffect effect) {
            requireTime(seconds);
            if (materials == null) materials = new ArrayList<>();
            materials.add(new MaterialKey(seconds, effect));
            materials.sort(Comparator.comparingDouble(MaterialKey::time));
            lastKey = Math.max(lastKey, seconds);
            return this;
        }

        /**
         * Adds a block state key that takes effect at the specified time, inclusive.
         *
         * @param seconds key time in seconds.
         * @param state   block specification such as {@code minecraft:furnace[lit=true]}.
         * @return this track.
         * @throws IllegalArgumentException if the node is not a {@link BlockNode}.
         */
        public Track state(float seconds, String state) {
            if (!(node instanceof BlockNode)) throw new IllegalArgumentException("Only a BlockNode has a block state");
            requireTime(seconds);
            BlockSpec.parse(state);
            int at = insertionPoint(stateTimes, stateTimes.length, seconds);
            stateTimes = insert(stateTimes, at, seconds);
            String[] grown = Arrays.copyOf(states, states.length + 1);
            System.arraycopy(grown, at, grown, at + 1, states.length - at);
            grown[at] = state;
            states = grown;
            lastKey = Math.max(lastKey, seconds);
            return this;
        }

        /**
         * Gets another node's track from the same timeline.
         *
         * @param other next node to animate.
         * @return the other node's track.
         */
        public Track track(SceneNode other) {
            return SceneTimeline.this.track(other);
        }

        /**
         * Reads the enclosing timeline.
         *
         * @return this track's timeline.
         */
        public SceneTimeline timeline() {
            return SceneTimeline.this;
        }

        /**
         * Applies the sampled state to the node.
         *
         * @param seconds time in seconds.
         */
        private void sample(float seconds) {
            if (position.sample(seconds, values)) node.setPosition(values[0], values[1], values[2]);
            if (rotation.sample(seconds, values)) node.setRotation(quaternion.setYawPitch(values[0], values[1]));
            if (scale.sample(seconds, values)) node.setScale(values[0], values[1], values[2]);
            if (visible.sample(seconds, values)) node.setVisible(values[0] > 0.5f);
            if (head.sample(seconds, values)) ((EntityNode) node).setHead(values[0], values[1]);
            if (walkPhase != null && walkPhase.sample(seconds, values)) {
                float phase = values[0];
                walkStrength.sample(seconds, values);
                ((EntityNode) node).setAnimation(Math.max(0, seconds), phase, Math.max(0, Math.min(1, values[0])));
            }
            if (actions != null) sampleAction((EntityNode) node, seconds);
            if (materials != null) {
                int index = materials.size() - 1;
                while (index > 0 && materials.get(index).time() > seconds) index--;
                node.setMaterialEffect(materials.get(index).effect());
            }
            if (materialClips != null) sampleMaterialClip(seconds);
            if (states.length != 0) {
                int index = Math.max(0, insertionPoint(stateTimes, stateTimes.length, seconds) - 1);
                String state = states[index];
                if (!state.equals(appliedState)) {
                    appliedState = state;
                    ((BlockNode) node).setState(state);
                }
            }
        }

        /**
         * Evaluates the latest active action or restores the node's base pose.
         *
         * @param entity  node receiving the sampled action
         * @param seconds absolute scene sample time in seconds
         */
        private void sampleAction(EntityNode entity, float seconds) {
            for (int i = actions.size() - 1; i >= 0; i--) {
                ActionKey key = actions.get(i);
                if (seconds >= key.start() && seconds < key.end()) {
                    entity.setTimelineAction(key.clip(), key.progress(seconds));
                    return;
                }
            }
            entity.setTimelineAction(null, 0);
        }

        /**
         * Applies the latest applicable surface clip or releases the timeline's override.
         *
         * @param seconds sample time in local seconds
         */
        private void sampleMaterialClip(float seconds) {
            for (int i = materialClips.size() - 1; i >= 0; i--) {
                MaterialEffectClip clip = materialClips.get(i);
                if (clip.effect == null || seconds < clip.start) continue;
                if (seconds < clip.end) {
                    node.setTimelineMaterialEffect(clip.effect, seconds, false);
                    return;
                }
                switch (clip.endBehavior) {
                    case HOLD -> {
                        node.setTimelineMaterialEffect(clip.effect, clip.end, false);
                        return;
                    }
                    case HIDE -> {
                        node.setTimelineMaterialEffect(null, 0, true);
                        return;
                    }
                    case RESTORE -> {
                    }
                }
            }
            node.setTimelineMaterialEffect(null, 0, false);
        }
    }

    /**
     * A discrete base material effect key.
     *
     * @param time   inclusive key time in seconds
     * @param effect base effect, or null for the default surface
     */
    private record MaterialKey(float time, SceneMaterialEffect effect) {
    }

    /**
     * Scheduled playback of one clip.
     *
     * @param start        inclusive scene start time in seconds
     * @param end          exclusive scene end time in seconds
     * @param cycleSeconds duration of a single cycle at the scheduled speed
     * @param clip         pose sampled over each cycle
     */
    private record ActionKey(float start, float end, double cycleSeconds, EntityActionClip<?> clip) {
        /**
         * Evaluates the current cycle index as {@code floor((seconds - start) / cycleSeconds)}.
         * Progress is {@code (seconds - cycleStart) / cycleSeconds}, where each cycle boundary rounds to
         * the same float precision as the public timeline clock.
         *
         * @param seconds scene sample within the action interval, in seconds
         * @return normalized cycle progress in [0, 1)
         */
        float progress(float seconds) {
            double elapsed = (double) seconds - start;
            double cycle = Math.floor(elapsed / cycleSeconds);
            if (seconds >= (float) (start + (cycle + 1) * cycleSeconds)) return 0;
            float cycleStart = (float) (start + cycle * cycleSeconds);
            return Math.min(Math.nextDown(1f), (float) (((double) seconds - cycleStart) / cycleSeconds));
        }
    }

    /**
     * Step easing that holds the preceding value until the destination key.
     */
    private static final Easing STEP = progress -> progress >= 1 ? 1 : 0;

    /**
     * Time-sorted keys for a property with one, two or three numeric components.
     */
    private final class Channel {
        /**
         * Number of components per key.
         */
        private final int width;
        /**
         * Key times in seconds.
         */
        private float[] times = new float[0];
        /**
         * Consecutive key values with {@link #width} components per key.
         */
        private float[] values = new float[0];
        /**
         * Easing into each destination key.
         */
        private Easing[] easings = new Easing[0];

        /**
         * Creates an empty key sequence.
         *
         * @param width number of components per key.
         */
        private Channel(int width) {
            this.width = width;
        }

        /**
         * Inserts a key after any existing keys at the same time.
         *
         * @param seconds key time in seconds.
         * @param easing  interpolation into the key, or null for linear.
         * @param a       first component.
         * @param b       second component, if used.
         * @param c       third component, if used.
         */
        private void add(float seconds, Easing easing, float a, float b, float c) {
            requireTime(seconds);
            if (!Float.isFinite(a) || !Float.isFinite(b) || !Float.isFinite(c)) {
                throw new IllegalArgumentException("Key values must be finite");
            }
            int count = times.length;
            int at = insertionPoint(times, count, seconds);
            times = insert(times, at, seconds);
            float[] grown = new float[(count + 1) * width];
            System.arraycopy(values, 0, grown, 0, at * width);
            System.arraycopy(values, at * width, grown, (at + 1) * width, (count - at) * width);
            float[] key = {a, b, c};
            System.arraycopy(key, 0, grown, at * width, width);
            values = grown;
            Easing[] curves = Arrays.copyOf(easings, count + 1);
            System.arraycopy(curves, at, curves, at + 1, count - at);
            curves[at] = easing == null ? AnimationEasing.LINEAR : easing;
            easings = curves;
            lastKey = Math.max(lastKey, seconds);
        }

        /**
         * Interpolates {@code v = a + (b - a) * easing((t - ta) / (tb - ta))}, where a and b are adjacent
         * key values at times ta and tb, and t is the sample time.
         *
         * @param seconds sample time t in seconds.
         * @param out     buffer for the interpolated components.
         * @return false when no keys exist, true otherwise.
         */
        private boolean sample(float seconds, float[] out) {
            int count = times.length;
            if (count == 0) return false;
            int next = insertionPoint(times, count, seconds);
            if (next == 0 || next == count || easings[next] == STEP) {
                int key = Math.max(0, next - 1);
                System.arraycopy(values, key * width, out, 0, width);
                return true;
            }
            int previous = next - 1;
            float span = times[next] - times[previous];
            float progress = easings[next].apply(span <= 0 ? 1 : (seconds - times[previous]) / span);
            for (int i = 0; i < width; i++) {
                float from = values[previous * width + i];
                out[i] = from + (values[next * width + i] - from) * progress;
            }
            return true;
        }
    }

    /**
     * Finds the first key strictly after the given time.
     *
     * @param times sorted key times in seconds.
     * @param count number of valid keys.
     * @param value time to find in seconds.
     * @return insertion index from zero through count.
     */
    private static int insertionPoint(float[] times, int count, float value) {
        int low = 0, high = count;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (times[middle] <= value) low = middle + 1;
            else high = middle;
        }
        return low;
    }

    /**
     * Inserts a value into an array copy.
     *
     * @param array original array.
     * @param at    insertion index.
     * @param value value to insert.
     * @return the new array.
     */
    private static float[] insert(float[] array, int at, float value) {
        float[] grown = new float[array.length + 1];
        System.arraycopy(array, 0, grown, 0, at);
        grown[at] = value;
        System.arraycopy(array, at, grown, at + 1, array.length - at);
        return grown;
    }
}
