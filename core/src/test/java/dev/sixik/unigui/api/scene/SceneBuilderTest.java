package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers authoring and playback of timed scene commands. */
final class SceneBuilderTest {
    /** The coordinates and times used in the tests are exactly representable. */
    private static final float EXACT = 0f;

    /** Accounts for float rounding in the stored quaternion. */
    private static final float ANGLE_EPSILON = 1e-6f;

    /** Uses the preceding position and preserves the gap before the eased move starts. */
    @Test
    void timedMoveTo_afterPositionStep_holdsUntilStartAndSeeksWithEasing() {
        var node = new SceneNode().setPosition(1, 2, 0);
        var sequence = SceneBuilder.create().addElements(node)
                .when(2, step -> step.moveTo(node, 8, 2, 0, 2, AnimationEasing.QUAD_IN))
                .when(0.5f, step -> step.moveTo(node, 4, 2, 0))
                .build();
        var playback = sequence.newPlaybackController();

        assertEquals(4, sequence.timeline().duration(), EXACT);
        playback.seek(1.5f);
        assertEquals(4, x(node), EXACT);
        playback.seek(2);
        assertEquals(4, x(node), EXACT);
        playback.seek(3);
        assertEquals(5, x(node), EXACT);
        playback.seek(4);
        assertEquals(8, x(node), EXACT);
        playback.seek(3);
        assertEquals(5, x(node), EXACT);
        playback.replay();
        assertEquals(1, x(node), EXACT);
    }

    /** Chains moves at arrival times while allowing visibility changes during a move. */
    @Test
    void timedMoveTo_adjacentMoves_continueFromPreviousDestination() {
        var node = new SceneNode().setPosition(2, 0, 0);
        var sequence = SceneBuilder.create().addElements(node)
                .when(1, step -> step.moveTo(node, 4, 0, 0, 1, null))
                .when(1.5f, step -> step.visible(false, node))
                .when(2, step -> step.moveTo(node, 8, 0, 0, 2, null))
                .build();

        sequence.timeline().sample(0.5f);
        assertEquals(2, x(node), EXACT);
        sequence.timeline().sample(1.5f);
        assertEquals(3, x(node), EXACT);
        assertFalse(node.visible());
        sequence.timeline().sample(2);
        assertEquals(4, x(node), EXACT);
        sequence.timeline().sample(3);
        assertEquals(6, x(node), EXACT);
    }

    /** Rejects overlapping positions before the build attaches or moves the node. */
    @Test
    void timedMoveTo_overlappingPositionCommand_leavesSceneUntouched() {
        var node = new SceneNode();
        var builder = SceneBuilder.create().addElements(node)
                .when(1, step -> step.moveTo(node, 4, 0, 0, 2, null))
                .when(2, step -> step.position(node, 7, 0, 0, null));

        assertThrows(IllegalArgumentException.class, builder::build);
        assertNull(node.scene());
        assertEquals(0, x(node), EXACT);
    }

    /**
     * Rejects durations that cannot produce a finite key after the start.
     * @param duration invalid duration in seconds
     */
    @ParameterizedTest
    @ValueSource(floats = {0, -1, Float.NaN, Float.POSITIVE_INFINITY, Float.MIN_VALUE})
    void timedMoveTo_invalidDuration_rejectsCommand(float duration) {
        var node = new SceneNode();
        var builder = SceneBuilder.create().addElements(node);
        assertThrows(IllegalArgumentException.class,
                () -> builder.when(2, step -> step.moveTo(node, 4, 0, 0, duration, null)));
        assertEquals(0, builder.build().timeline().duration(), EXACT);
    }

    /** Interpolates position and rotation while driving local time for a nested stage. */
    @Test
    void build_animatedKeysAndStage_sharePlaybackClock() {
        var node = new SceneNode();
        var samples = new ArrayList<Float>();
        var builder = SceneBuilder.create().addElements(node)
                .when(2, step -> step.position(node, 0, 4, 0, null).rotation(node, 0, 0, null))
                .when(4, step -> step.position(node, 0, 0, 0, AnimationEasing.QUAD_IN)
                        .rotation(node, 1, 0, AnimationEasing.QUAD_IN));
        builder.stage(3).duration(2).onSample(samples::add);

        var sequence = builder.build();
        assertEquals(5, sequence.timeline().duration(), EXACT);
        assertEquals(4, node.position(new MutableVec3()).y(), EXACT);
        var playback = sequence.newPlaybackController();
        playback.seek(3);
        assertEquals(3, node.position(new MutableVec3()).y(), EXACT);
        assertEquals((float) Math.sin(0.125), node.rotation(new MutableQuat()).y(), ANGLE_EPSILON);
        playback.seek(4);
        assertEquals(0, node.position(new MutableVec3()).y(), EXACT);
        playback.seek(3);
        assertEquals(3, node.position(new MutableVec3()).y(), EXACT);
        assertEquals(List.of(-3f, -3f, 0f, 1f, 0f), samples);
    }

    /** Moves an array of blocks at the scheduled time and restores the starting frame on replay. */
    @Test
    void build_groupSteps_supportSeekAndReplay() {
        var first = new BlockNode("minecraft:stone");
        var second = new BlockNode("minecraft:glass").setPosition(2, 0, 0);
        BlockNode[] blocks = {first, second};
        var sequence = SceneBuilder.create().addElements(blocks)
                .when(2, step -> step.moveBy(1, 0, 0, blocks))
                .when(3, step -> step.visible(false, blocks))
                .build();
        assertEquals(List.of(first, second), sequence.scene().roots());
        assertEquals(3, sequence.timeline().duration(), EXACT);

        var playback = sequence.newPlaybackController();
        playback.seek(1.75f);
        assertEquals(0, x(first), EXACT);
        assertEquals(2, x(second), EXACT);
        playback.seek(2);
        playback.seek(2);
        assertEquals(1, x(first), EXACT);
        assertEquals(3, x(second), EXACT);
        playback.seek(3);
        assertFalse(first.visible());
        assertFalse(second.visible());
        playback.replay();
        assertTrue(playback.playing());
        assertEquals(0, x(first), EXACT);
        assertEquals(2, x(second), EXACT);
        assertTrue(first.visible());
        assertTrue(second.visible());
    }

    /** Resolves offsets from prior commands, with declaration order breaking ties. */
    @Test
    void when_outOfOrderSteps_compileDeterministically() {
        var node = new SceneNode().setVisible(false);
        var count = new AtomicInteger();
        var builder = SceneBuilder.create().addElements(node)
                .when(4, step -> step.moveBy(3, 0, 0, node))
                .when(2, step -> {
                    count.incrementAndGet();
                    step.moveTo(node, 5, 0, 0).visible(true, node);
                })
                .when(4, step -> step.moveBy(1, 0, 0, node))
                .when(0, step -> step.moveTo(node, 2, 0, 0))
                .duration(6);
        assertEquals(1, count.get());
        var sequence = builder.build();
        assertSame(sequence, builder.build());
        assertEquals(2, x(node), EXACT);
        assertFalse(node.visible());
        assertEquals(6, sequence.timeline().duration(), EXACT);

        var playback = sequence.newPlaybackController();
        playback.seek(4);
        assertEquals(9, x(node), EXACT);
        playback.seek(2);
        assertEquals(5, x(node), EXACT);
        assertTrue(node.visible());
        playback.seek(0);
        assertEquals(2, x(node), EXACT);
        assertFalse(node.visible());
        assertEquals(1, count.get());
    }

    /** Keeps finite endpoint coordinates discrete even when their difference overflows a float. */
    @Test
    void moveTo_extremeCoordinates_holdsPreviousKeyUntilTimestamp() {
        var node = new SceneNode().setPosition(Float.MAX_VALUE, 0, 0);
        var sequence = SceneBuilder.create().addElements(node)
                .when(2, step -> step.moveTo(node, -Float.MAX_VALUE, 0, 0))
                .build();
        sequence.timeline().sample(1);
        assertEquals(Float.MAX_VALUE, x(node), EXACT);
        sequence.timeline().sample(2);
        assertEquals(-Float.MAX_VALUE, x(node), EXACT);
    }

    /** Rejects invalid times and positions before attaching nodes to the new scene. */
    @Test
    void build_invalidCommand_doesNotReparentNodes() {
        var node = new SceneNode().setPosition(Float.MAX_VALUE, 0, 0);
        var builder = SceneBuilder.create().addElements(node);
        assertThrows(IllegalArgumentException.class, () -> builder.when(-1, step -> step.visible(false, node)));
        assertThrows(IllegalArgumentException.class, () -> builder.when(1, step -> step.moveTo(node, Float.NaN, 0, 0)));
        builder.when(1, step -> step.moveBy(Float.MAX_VALUE, 0, 0, node));
        assertThrows(IllegalArgumentException.class, builder::build);
        assertNull(node.scene());
        assertEquals(Float.MAX_VALUE, x(node), EXACT);
    }

    /**
     * @param node node in the scene
     * @return parent-space X coordinate in scene units
     */
    private static float x(SceneNode node) {
        return node.position(new MutableVec3()).x();
    }
}
