package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.animation.Easing;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Builds a seekable scene sequence from timed commands and local stages.
 */
@WikiPage(title = "SceneBuilder", slug = "scene-builder", category = "<key:scene.category>", order = 175,
        summary = "<key:scene.builder.summary>")
@WikiSection(title = "<key:scene.builder.section.timing.title>", order = 10,
        markdown = "<key:scene.builder.section.timing.body>")
@WikiSection(title = "<key:scene.builder.section.step.title>", order = 20,
        markdown = "<key:scene.builder.section.step.body>")
@WikiSection(title = "<key:scene.builder.section.playback.title>", order = 30,
        markdown = "<key:scene.builder.section.playback.body>")
@WikiExample(title = "<key:scene.builder.example.furnace>", code = """
        var furnace = new BlockNode("minecraft:furnace[facing=south,lit=false]");
        var sequence = SceneBuilder.create()
                .addElements(furnace)
                .when(0, step -> step.moveTo(furnace, 0.5f, 4, 0.5f).visible(false, furnace))
                .when(2, step -> step.visible(true, furnace)
                        .moveTo(furnace, 0.5f, 1, 0.5f, 0.8f, AnimationEasing.QUAD_IN))
                .duration(4)
                .build();
        
        var viewport = new Scene3D(sequence.scene());
        var playback = sequence.newPlaybackController();
        playback.seek(2.4f);
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@WikiLink(label = "SceneAnchor3D", target = "page:scene-anchor-3d")
@AiDoc(summary = "Builds a seekable 3D scene sequence from timed commands and local stages.",
        purpose = "Author the scene graph and its timeline alongside each other for replayable demonstrations.",
        criticalRules = {
                "`when(seconds, step -> ...)` calls the consumer once during authoring. Record changes through `step`. Build compiles commands in time order and breaks ties by declaration order.",
                "Step commands are `moveTo(node, x, y, z)`, `moveBy(x, y, z, nodes...)`, and `visible(shown, nodes...)` for discrete changes. `position(node, x, y, z, easing)` and `rotation(node, yaw, pitch, easing)` interpolate from the previous key and hold the first key before its time.",
                "`step.moveTo(node, x, y, z, durationSeconds, easing)` starts from the sampled parent-space position at the step time. A timed move can be followed by another position command at or after arrival. Positions use scene units, angles use radians, and times use seconds.",
                "`stage(seconds)` returns a child SceneTimeline whose zero is at that time in the sequence. Configure it before build. Playback duration defaults to the latest key or stage end unless `duration(seconds)` overrides it.",
                "`build()` attaches the supplied nodes and applies frame zero. Repeated calls return the same SceneSequence. Create a Scene3D for `sequence.scene()` and a controller with `sequence.newPlaybackController()`. Feed it frame deltas through `update`.",
                "To animate an anchored widget, add it to the viewport, set up `sequence.timeline().trackAnchor(anchor)`, then create the playback controller so its duration includes the anchor keys."
        },
        related = {"Scene", "SceneTimeline", "SceneSequence", "ScenePlaybackController", "Scene3D", "SceneAnchor3D"},
        examples = @AiDoc.Example(title = "Build a sequence with a local stage", code = """
                var furnace = new BlockNode("minecraft:furnace[facing=south,lit=false]");
                furnace.setPosition(0.5f, 4, 0.5f);
                var builder = SceneBuilder.create().addElements(furnace)
                        .when(2, step -> step.moveTo(furnace, 0.5f, 1, 0.5f, 0.8f, AnimationEasing.QUAD_IN));
                var ignition = builder.stage(3);
                ignition.track(furnace)
                        .state(0, "minecraft:furnace[facing=south,lit=false]")
                        .state(0.5f, "minecraft:furnace[facing=south,lit=true]");
                var sequence = builder.build();
                var viewport = new Scene3D(sequence.scene());
                var playback = sequence.newPlaybackController();
                playback.replay();
                """))
public final class SceneBuilder {
    /**
     * Root nodes to attach when building the scene.
     */
    private final List<SceneNode> roots = new ObjectArrayList<>();
    /**
     * Commands collected in declaration order.
     */
    private final List<ScheduledStep> steps = new ObjectArrayList<>();
    /**
     * Explicit duration in seconds, or negative to infer it from the commands.
     */
    private float duration = -1;
    /**
     * Completed sequence, or null while the builder can still be edited.
     */
    private SceneSequence sequence;

    /**
     * Creates an empty builder.
     */
    private SceneBuilder() {
    }

    /**
     * @return a builder for a new scene
     */
    @WikiMethod(order = 10, summary = "<key:scene.builder.create.summary>")
    @WikiReturn("<key:scene.builder.create.return>")
    @AiDoc(order = 10, summary = "Creates an empty scene builder.")
    public static SceneBuilder create() {
        return new SceneBuilder();
    }

    /**
     * Adds root nodes to the new scene. Building moves these nodes into the scene with their children.
     *
     * @param nodes root nodes to add, in scene order
     * @return this builder
     * @throws NullPointerException  if the array or any element is null
     * @throws IllegalStateException if this builder has already built a sequence
     */
    @WikiMethod(order = 20, summary = "<key:scene.builder.add.elements.summary>")
    @WikiReturn("<key:scene.builder.return.this>")
    @WikiThrows(type = NullPointerException.class, description = "<key:scene.builder.nodes.null>")
    @WikiThrows(type = IllegalStateException.class, description = "<key:scene.builder.already.built>")
    @AiDoc(order = 20, summary = "Registers root nodes in scene order. Build attaches the supplied nodes and their children.")
    public SceneBuilder addElements(@WikiParameter(description = "<key:scene.builder.nodes>") SceneNode... nodes) {
        requireOpen();
        roots.addAll(List.of(nodes));
        return this;
    }

    /**
     * Records commands for the given time. The callback configures the step once during this call.
     * Steps run in time order, with declarations at equal times applied in insertion order.
     *
     * @param seconds   finite nonnegative command time in seconds
     * @param configure callback that records commands through the supplied step
     * @return this builder
     * @throws IllegalArgumentException if the time is negative or nonfinite
     * @throws NullPointerException     if the callback is null
     * @throws IllegalStateException    if this builder has already built a sequence
     */
    @WikiMethod(order = 30, summary = "<key:scene.builder.when.summary>")
    @WikiReturn("<key:scene.builder.return.this>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.builder.time.invalid>")
    @WikiThrows(type = NullPointerException.class, description = "<key:scene.builder.configure.null>")
    @WikiThrows(type = IllegalStateException.class, description = "<key:scene.builder.already.built>")
    @AiDoc(order = 30, summary = "Runs the configuration callback once and records its Step commands at an absolute time in seconds.")
    public SceneBuilder when(
            @WikiParameter(description = "<key:scene.builder.when.seconds>") float seconds,
            @WikiParameter(description = "<key:scene.builder.when.configure>") Consumer<Step> configure) {
        requireOpen();
        SceneTimeline.requireTime(seconds);
        var step = new Step(seconds == 0 ? 0 : seconds);
        Objects.requireNonNull(configure, "configure").accept(step);
        steps.add(new ScheduledStep(step.seconds, List.copyOf(step.commands)));
        return this;
    }

    /**
     * Creates a timeline whose local time zero is at the given start in the built sequence.
     *
     * @param startSeconds finite nonnegative start time in seconds
     * @return child timeline for configuring the stage
     * @throws IllegalArgumentException if the start is negative or nonfinite
     * @throws IllegalStateException    if this builder has already built a sequence
     */
    @WikiMethod(order = 40, summary = "<key:scene.builder.stage.summary>")
    @WikiReturn("<key:scene.builder.stage.return>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.builder.time.invalid>")
    @WikiThrows(type = IllegalStateException.class, description = "<key:scene.builder.already.built>")
    @AiDoc(order = 40, summary = "Creates a child SceneTimeline whose zero is at the given time in the sequence.")
    public SceneTimeline stage(@WikiParameter(description = "<key:scene.builder.stage.seconds>") float startSeconds) {
        requireOpen();
        SceneTimeline.requireTime(startSeconds);
        var child = new SceneTimeline();
        steps.add(new ScheduledStep(startSeconds, List.of(timeline -> timeline.addStage(startSeconds, child))));
        return child;
    }

    /**
     * Sets an explicit playback duration, for example to hold the final state beyond the last command.
     *
     * @param seconds finite nonnegative duration in seconds
     * @return this builder
     * @throws IllegalArgumentException if the duration is negative or nonfinite
     * @throws IllegalStateException    if this builder has already built a sequence
     */
    @WikiMethod(order = 50, summary = "<key:scene.builder.duration.summary>")
    @WikiReturn("<key:scene.builder.return.this>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.builder.time.invalid>")
    @WikiThrows(type = IllegalStateException.class, description = "<key:scene.builder.already.built>")
    @AiDoc(order = 50, summary = "Overrides the inferred playback duration with a finite, nonnegative time in seconds.")
    public SceneBuilder duration(@WikiParameter(description = "<key:scene.builder.duration.seconds>") float seconds) {
        requireOpen();
        SceneTimeline.requireTime(seconds);
        duration = seconds;
        return this;
    }

    /**
     * Compiles the commands into timeline keys, attaches the nodes and applies the first frame.
     * Subsequent calls return the same scene and timeline.
     *
     * @return the completed sequence
     * @throws IllegalArgumentException if a move exceeds the float range or position commands overlap a timed move
     */
    @WikiMethod(order = 60, summary = "<key:scene.builder.build.summary>")
    @WikiReturn("<key:scene.builder.build.return>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.builder.build.invalid.move>")
    @AiDoc(order = 60, summary = "Compiles the timeline, attaches scene nodes and applies frame zero. Subsequent calls return the same sequence.")
    public SceneSequence build() {
        if (sequence != null) return sequence;
        var timeline = new SceneTimeline();
        steps.sort(Comparator.comparingDouble(ScheduledStep::seconds));
        for (ScheduledStep step : steps) {
            for (Consumer<SceneTimeline> command : step.commands()) command.accept(timeline);
        }
        if (duration >= 0) timeline.duration(duration);

        var scene = new Scene();
        for (SceneNode root : roots) scene.addRoot(root);
        timeline.sample(0);
        sequence = new SceneSequence(scene, timeline);
        steps.clear();
        roots.clear();
        return sequence;
    }

    /**
     * @throws IllegalStateException if the sequence has already been built
     */
    private void requireOpen() {
        if (sequence != null) throw new IllegalStateException("Scene sequence has already been built");
    }

    /**
     * @param seconds  inclusive timestamp in seconds
     * @param commands recorded operations for this time, in declaration order
     */
    private record ScheduledStep(float seconds, List<Consumer<SceneTimeline>> commands) {
    }

    /**
     * Records commands at a timestamp supplied to {@link SceneBuilder#when(float, Consumer)}.
     */
    public static final class Step {
        /**
         * Inclusive command time in seconds.
         */
        private final float seconds;
        /**
         * Operations to add to the timeline in declaration order.
         */
        private final List<Consumer<SceneTimeline>> commands = new ArrayList<>();

        /**
         * @param seconds inclusive command time in seconds
         */
        private Step(float seconds) {
            this.seconds = seconds;
        }

        /**
         * Moves a node to an absolute position in the parent's coordinate system.
         *
         * @param node node to move
         * @param x    finite parent-space X in scene units
         * @param y    finite parent-space Y in scene units
         * @param z    finite parent-space Z in scene units
         * @return this step
         * @throws IllegalArgumentException if a coordinate is nonfinite
         * @throws NullPointerException     if the node is null
         */
        public Step moveTo(SceneNode node, float x, float y, float z) {
            Objects.requireNonNull(node, "node");
            requireCoordinates(x, y, z);
            commands.add(timeline -> timeline.track(node).stepPosition(seconds, x, y, z, false));
            return this;
        }

        /**
         * Starts an eased move from the sampled position at this step's time and reaches the destination
         * after the given duration. With no preceding position key, the move starts from the node's
         * position at build time. Schedule subsequent position commands for this node at or after arrival.
         *
         * @param node            node to move
         * @param x               finite destination X in parent-space scene units
         * @param y               finite destination Y in parent-space scene units
         * @param z               finite destination Z in parent-space scene units
         * @param durationSeconds finite positive duration in seconds
         * @param easing          movement curve, or null for linear interpolation
         * @return this step
         * @throws IllegalArgumentException if a coordinate is nonfinite or the duration is not finite and positive
         *                                  with a representable end strictly after the start
         * @throws NullPointerException     if the node is null
         */
        public Step moveTo(SceneNode node, float x, float y, float z, float durationSeconds, Easing easing) {
            Objects.requireNonNull(node, "node");
            requireCoordinates(x, y, z);
            float end = seconds + durationSeconds;
            if (!Float.isFinite(end) || end <= seconds) {
                throw new IllegalArgumentException("Move duration must be positive and reach a finite time after its start");
            }
            commands.add(timeline -> timeline.track(node).movePosition(seconds, end, x, y, z, easing));
            return this;
        }

        /**
         * Adds a position key interpolated from the preceding key. The first key also sets the initial position.
         *
         * @param node   node to animate
         * @param x      finite parent-space X in scene units
         * @param y      finite parent-space Y in scene units
         * @param z      finite parent-space Z in scene units
         * @param easing interpolation into this key, or null for linear
         * @return this step
         * @throws IllegalArgumentException if a coordinate is nonfinite
         * @throws NullPointerException     if the node is null
         */
        public Step position(SceneNode node, float x, float y, float z, Easing easing) {
            Objects.requireNonNull(node, "node");
            requireCoordinates(x, y, z);
            commands.add(timeline -> timeline.track(node).stepPositionKey(seconds, x, y, z, easing));
            return this;
        }

        /**
         * Adds a body rotation key interpolated from the preceding key. The first key also sets the initial rotation.
         *
         * @param node   node to animate
         * @param yaw    finite rotation about Y in radians
         * @param pitch  finite rotation about local X in radians
         * @param easing interpolation into this key, or null for linear
         * @return this step
         * @throws IllegalArgumentException if either angle is nonfinite
         * @throws NullPointerException     if the node is null
         */
        public Step rotation(SceneNode node, float yaw, float pitch, Easing easing) {
            Objects.requireNonNull(node, "node");
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                throw new IllegalArgumentException("Scene rotation angles must be finite");
            }
            commands.add(timeline -> timeline.track(node).rotation(seconds, yaw, pitch, easing));
            return this;
        }

        /**
         * Adds an offset to each node's position in its parent's coordinate system.
         *
         * @param x     finite delta X in scene units
         * @param y     finite delta Y in scene units
         * @param z     finite delta Z in scene units
         * @param nodes nodes to move
         * @return this step
         * @throws IllegalArgumentException if a delta is nonfinite
         * @throws NullPointerException     if the array or any element is null
         */
        public Step moveBy(float x, float y, float z, SceneNode... nodes) {
            requireCoordinates(x, y, z);
            var targets = List.of(nodes);
            commands.add(timeline -> {
                for (SceneNode node : targets) timeline.track(node).stepPosition(seconds, x, y, z, true);
            });
            return this;
        }

        /**
         * Shows or hides the supplied nodes at this time.
         *
         * @param shown true to show each node, false to hide it
         * @param nodes nodes whose visibility changes
         * @return this step
         * @throws NullPointerException if the array or any element is null
         */
        public Step visible(boolean shown, SceneNode... nodes) {
            var targets = List.of(nodes);
            commands.add(timeline -> {
                for (SceneNode node : targets) timeline.track(node).stepVisibility(seconds, shown);
            });
            return this;
        }

        /**
         * @param x parent-space X coordinate or delta in scene units
         * @param y parent-space Y coordinate or delta in scene units
         * @param z parent-space Z coordinate or delta in scene units
         * @throws IllegalArgumentException if any component is nonfinite
         */
        private static void requireCoordinates(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalArgumentException("Scene coordinates must be finite");
            }
        }
    }
}
