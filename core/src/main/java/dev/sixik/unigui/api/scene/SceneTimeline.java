package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.Easing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.ArrayList;
import java.util.Arrays;
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
@WikiSection(title = "Tracks and interpolation", order = 10, markdown = """
        `track(node)` returns a `SceneTimeline.Track`. Add position, rotation and scale keys with `position`, `yaw`,
        `rotation` and `scale`. Positions are parent-relative, rotations use radians and scale is unitless. Use
        `head` for entity head angles, `state` for block states, and `visible` or `visibleBetween` for timed
        visibility. The first and last keys hold outside their range.
        """)
@WikiSection(title = "Track methods", order = 15, markdown = """
        | Method | Property and units |
        | --- | --- |
        | `position(seconds, x, y, z[, easing])` | Parent-relative position in scene units. |
        | `yaw(seconds, yaw)` | Yaw in radians with a linear transition and zero pitch. |
        | `rotation(seconds, yaw, pitch, easing)` | Yaw and pitch in radians, interpolated as scalars. |
        | `scale(seconds, scale)` | Uniform dimensionless scale with a linear transition. |
        | `scale(seconds, x, y, z, easing)` | Dimensionless scale factors along the local axes. |
        | `head(seconds, yaw, pitch, easing)` | Entity head angles in radians relative to the body. |
        | `visible(seconds, visible)` | Discrete visibility from the key time onward. |
        | `visibleBetween(from, to)` | Visibility from the inclusive start to the exclusive end. |
        | `state(seconds, state)` | Discrete block state from the key time onward. |

        Use `track(otherNode)` on a track to move to another node in the same timeline.
        `timeline()` returns the timeline that owns the track.
        """)
@WikiSection(title = "Playback and checkpoints", order = 20, markdown = """
        `play(viewport)` runs the timeline once from the beginning. Connect replay to `play` and connect sliders or
        checkpoint buttons to `stop` followed by `sample(seconds)`. Use `duration(seconds)` to hold the final state
        longer. The `during` callback receives normalized eased progress. `onSample` receives time in seconds after
        node tracks have been applied.
        """)
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
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "BlockNode", target = "page:block-node")
@WikiLink(label = "EntityNode", target = "page:entity-node")
@WikiLink(label = "StructureNode", target = "page:structure-node")
@AiDoc(
        summary = "Seekable keyframe tracks and samplers for scene tutorials.",
        purpose = "Drive playback, replay, checkpoint navigation and timeline sliders from one scene definition.",
        criticalRules = {
                "Times are in seconds. `sample` applies the state at absolute time and can seek in either direction.",
                "Easing comes from the destination key. Discrete visibility and block state keys apply from their timestamp inclusive.",
                "Track methods include position, yaw, rotation, scale, head, visible, visibleBetween and state.",
                "Use head only on EntityNode tracks and state only on BlockNode tracks.",
                "Make onSample and during callbacks derive state from the supplied absolute time or progress.",
                "Use `stop(viewport)` before scrubbing manually. `play(viewport)` restarts from time zero."
        },
        related = {"Scene3D", "SceneNode", "BlockNode", "EntityNode", "StructureNode"},
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
    /**
     * Order in which tracks are applied.
     */
    private final List<Track> order = new ArrayList<>();
    /**
     * Custom samplers invoked on each frame.
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
     * Animates custom progress during an interval. Progress is zero before the start and one after the end.
     *
     * @param start start time in seconds.
     * @param end end time in seconds, at or after start.
     * @param easing easing function, or null for linear progress.
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
     * Reveals the structure from bottom to top, starting with no visible layers.
     *
     * @param structure structure to reveal.
     * @param start start time in seconds.
     * @param end end time in seconds.
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
     * @return the explicit duration or the latest key or interval end, in seconds.
     */
    @WikiMethod(section = "Authoring", sectionOrder = 10, order = 50,
            summary = "Reads the playback duration.")
    @WikiReturn("the explicit duration or the latest key or interval end, in seconds")
    @AiDoc(section = "Authoring", sectionOrder = 10, order = 50,
            summary = "Reads the playback duration.")
    public float duration() {
        return explicitDuration >= 0 ? explicitDuration : lastKey;
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
    private static void requireTime(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Time must be finite and non-negative: " + seconds);
        }
    }

    /**
     * Keyframes for one node's transform, visibility, entity head rotation and block state.
     */
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
         * @param x parent-space X in scene units.
         * @param y parent-space Y in scene units.
         * @param z parent-space Z in scene units.
         * @return this track.
         */
        public Track position(float seconds, float x, float y, float z) {
            return position(seconds, x, y, z, null);
        }

        /**
         * Adds a position key.
         *
         * @param seconds key time in seconds.
         * @param x parent-space X in scene units.
         * @param y parent-space Y in scene units.
         * @param z parent-space Z in scene units.
         * @param easing interpolation from the preceding key, or null for linear.
         * @return this track.
         */
        public Track position(float seconds, float x, float y, float z, Easing easing) {
            position.add(seconds, easing, x, y, z);
            return this;
        }

        /**
         * Adds a yaw key with a linear transition.
         *
         * @param seconds key time in seconds.
         * @param yaw yaw in radians.
         * @return this track.
         */
        public Track yaw(float seconds, float yaw) {
            return rotation(seconds, yaw, 0, null);
        }

        /**
         * Adds a rotation key. Angles interpolate as scalars, so a rotation greater than π can be specified directly.
         *
         * @param seconds key time in seconds.
         * @param yaw yaw in radians.
         * @param pitch pitch in radians.
         * @param easing interpolation from the preceding key, or null for linear.
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
         * @param scale dimensionless factor along every axis.
         * @return this track.
         */
        public Track scale(float seconds, float scale) {
            return scale(seconds, scale, scale, scale, null);
        }

        /**
         * Adds a scale key.
         *
         * @param seconds key time in seconds.
         * @param x local X scale factor.
         * @param y local Y scale factor.
         * @param z local Z scale factor.
         * @param easing interpolation from the preceding key, or null for linear.
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
         * @param yaw yaw in radians.
         * @param pitch pitch in radians.
         * @param easing interpolation from the preceding key, or null for linear.
         * @return this track.
         * @throws IllegalArgumentException if the node is not an {@link EntityNode}.
         */
        public Track head(float seconds, float yaw, float pitch, Easing easing) {
            if (!(node instanceof EntityNode)) throw new IllegalArgumentException("Only an EntityNode has a head");
            head.add(seconds, easing, yaw, pitch, 0);
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
         * Shows the node only during {@code [from, to)}.
         *
         * @param from inclusive start time in seconds.
         * @param to exclusive end time in seconds.
         * @return this track.
         */
        public Track visibleBetween(float from, float to) {
            return visible(0, from == 0).visible(from, true).visible(to, false);
        }

        /**
         * Adds a block state key that takes effect at the specified time, inclusive.
         *
         * @param seconds key time in seconds.
         * @param state block specification such as {@code minecraft:furnace[lit=true]}.
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
            if (states.length != 0) {
                int index = Math.max(0, insertionPoint(stateTimes, stateTimes.length, seconds) - 1);
                String state = states[index];
                if (!state.equals(appliedState)) {
                    appliedState = state;
                    ((BlockNode) node).setState(state);
                }
            }
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
         * @param easing interpolation into the key, or null for linear.
         * @param a first component.
         * @param b second component, if used.
         * @param c third component, if used.
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
         * @param out buffer for the interpolated components.
         * @return false when no keys exist, true otherwise.
         */
        private boolean sample(float seconds, float[] out) {
            int count = times.length;
            if (count == 0) return false;
            int next = insertionPoint(times, count, seconds);
            if (next == 0 || next == count) {
                int key = next == 0 ? 0 : count - 1;
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
     * @param at insertion index.
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
