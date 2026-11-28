package dev.sixik.unigui.api.scene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies action placement, retiming and seeking independently of Minecraft. */
final class SceneTimelineActionTest {
    /** Covers rounding of nonbinary timeline seconds converted to normalized phase. */
    private static final float EPSILON = 1e-6f;

    /** The clock samples a finite interval and resets at each repeat, including rounded boundaries. */
    @ParameterizedTest
    @CsvSource({"0.99, -1", "1.0, 0", "1.075, 0.5", "1.15, 0", "1.225, 0.5", "1.3, -1"})
    void action_retimedRepeats_sampleAbsolutePhase(float seconds, float expected) {
        var node = new EntityNode("minecraft:zombie");
        var scene = new Scene().addRoot(node);
        var timeline = new SceneTimeline();
        var clip = new EntityActionClip<>(0.3f, float[].class, (state, phase) -> state[0] = phase);
        timeline.track(node).action(1, clip, 2, 1);
        assertEquals(1.3f, timeline.duration(), EPSILON);

        timeline.sample(seconds);
        long revision = scene.revision();
        timeline.sample(seconds);
        assertEquals(revision, scene.revision());
        assertEquals(expected >= 0, node.hasAction());
        assertEquals(expected, readPose(node), EPSILON);
        timeline.sample(10);
        assertFalse(node.hasAction());
        timeline.sample(seconds);
        assertEquals(expected, readPose(node), EPSILON);
    }

    /** Active overlays are sorted by start time and expire back to the previous active action. */
    @Test
    void action_overlaps_useLatestStartAndRestoreBaseAtEnd() {
        var node = new EntityNode("minecraft:zombie");
        node.setNativeAnimation(float[].class, state -> state[0] = -2);
        var timeline = new SceneTimeline();
        timeline.track(node)
                .action(2, new EntityActionClip<>(1, float[].class, (state, phase) -> state[0] = 1))
                .action(0, new EntityActionClip<>(4, float[].class, (state, phase) -> state[0] = 0))
                .action(2, new EntityActionClip<>(0.5f, float[].class, (state, phase) -> state[0] = 2));
        timeline.sample(2.25f);
        assertEquals(2, readPose(node), EPSILON);
        node.sampleNativeAnimation("incompatible state");
        timeline.sample(2.5f);
        assertEquals(1, readPose(node), EPSILON);
        timeline.sample(3);
        assertEquals(0, readPose(node), EPSILON);
        timeline.sample(4);
        assertFalse(node.hasAction());
        assertEquals(-2, readPose(node), EPSILON);
        timeline.sample(0);
        assertTrue(node.hasAction());
        assertEquals(0, readPose(node), EPSILON);
    }

    /** Invalid timings, clips and track targets fail before changing the timeline. */
    @Test
    void action_invalidArguments_areRejected() {
        var timeline = new SceneTimeline();
        var track = timeline.track(new EntityNode("minecraft:zombie"));
        EntityActionClip.PoseSampler<Object> sampler = (state, phase) -> {};
        var clip = new EntityActionClip<>(1, Object.class, sampler);
        assertThrows(IllegalArgumentException.class, () -> new EntityActionClip<>(Float.NaN, Object.class, sampler));
        assertThrows(IllegalArgumentException.class, () -> new EntityActionClip<>(0, Object.class, sampler));
        assertThrows(NullPointerException.class, () -> new EntityActionClip<>(1, Object.class, null));
        assertThrows(IllegalArgumentException.class, () -> track.action(-1, clip));
        assertThrows(IllegalArgumentException.class, () -> track.action(0, clip, Float.POSITIVE_INFINITY, 0));
        assertThrows(IllegalArgumentException.class, () -> track.action(0, clip, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> track.action(0, clip, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> track.action(Float.MAX_VALUE, clip));
        assertThrows(IllegalArgumentException.class, () -> timeline.track(new SceneNode()).action(0, clip));
        assertEquals(0, timeline.duration(), EPSILON);
    }

    /**
     * Runs the same sampled pose path the renderer calls for a fresh frame.
     *
     * @param node node containing the last sampled action
     * @return value written by the test pose, or -1 if no pose writes it
     */
    private static float readPose(EntityNode node) {
        float[] state = {-1};
        node.sampleNativeAnimation(state);
        return state[0];
    }
}
