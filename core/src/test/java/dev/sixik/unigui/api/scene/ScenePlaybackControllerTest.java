package dev.sixik.unigui.api.scene;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests transport commands using exact binary fractions of a second. */
final class ScenePlaybackControllerTest {
    /** The times used here are exactly representable as floats. */
    private static final float EXACT = 0f;

    /** Advances only while playing and resumes at the same time after a pause. */
    @Test
    void play_afterPause_resumesFromCurrentTime() {
        var samples = new ArrayList<Float>();
        var controller = new ScenePlaybackController(2, samples::add);
        controller.update(0.25f);
        assertFalse(controller.playing());
        assertTrue(samples.isEmpty());

        controller.play();
        controller.update(0.25f);
        controller.pause();
        controller.update(0.25f);
        assertEquals(0.25f, controller.timeSeconds(), EXACT);
        controller.play();
        controller.update(0.125f);
        assertEquals(List.of(0.25f, 0.375f), samples);
    }

    /** Allows scrubbing in either state and reapplies the same frame after a preset changes. */
    @Test
    void seek_duringPlaybackAndPause_samplesAndKeepsState() {
        var samples = new ArrayList<Float>();
        var controller = new ScenePlaybackController(2, samples::add);
        controller.seek(0.75f);
        controller.seek(0.75f);
        assertFalse(controller.playing());
        controller.play();
        controller.seek(-1);
        assertTrue(controller.playing());
        controller.seek(3);
        assertFalse(controller.playing());
        assertEquals(2, controller.timeSeconds(), EXACT);
        assertEquals(List.of(0.75f, 0.75f, 0f, 2f), samples);
    }

    /** Samples the final frame once, then restarts only when replay is requested. */
    @Test
    void replay_afterEnd_resamplesZeroAndStarts() {
        var samples = new ArrayList<Float>();
        var controller = new ScenePlaybackController(0.375f, samples::add);
        controller.replay();
        controller.update(Float.NaN);
        controller.update(-1);
        controller.update(1);
        controller.update(0.25f);
        controller.update(0.25f);
        assertFalse(controller.playing());
        controller.play();
        assertFalse(controller.playing());
        controller.replay();
        assertTrue(controller.playing());
        assertEquals(0, controller.timeSeconds(), EXACT);
        assertEquals(List.of(0f, 0.25f, 0.375f, 0f), samples);
    }

    /** Applies a static frame on replay and rejects nonfinite times. */
    @Test
    void replay_zeroDuration_samplesWithoutAdvancing() {
        var samples = new ArrayList<Float>();
        var controller = new ScenePlaybackController(0, samples::add);
        controller.replay();
        controller.update(0.25f);
        assertEquals(List.of(0f), samples);
        assertFalse(controller.playing());
        assertThrows(IllegalArgumentException.class, () -> controller.seek(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new ScenePlaybackController(-1, samples::add));
        assertThrows(IllegalArgumentException.class, () -> new ScenePlaybackController(Float.NaN, samples::add));
    }
}
