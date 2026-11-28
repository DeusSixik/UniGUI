package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.SceneTimeline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers seekable campfire-style smoke and its sampled paths.
 */
class LegacyCampfireSmokeTest {
    /**
     * Smoke emitted during ignition outlives the emission interval and replays from the same positions.
     */
    @Test
    void sample_ignitionRewindAndReplay_keepTheSameSmoke() {
        var timeline = new SceneTimeline();
        var smoke = new LegacyCampfireSmoke(0.06, 2.75, 6);
        timeline.stage(4).onSample(smoke::setTime);

        timeline.sample(5.75f);
        assertEquals(11, smoke.limit());
        double age = smoke.ageTicks(0);
        assertEquals((1.75 - 0.06) * 20, age, Math.ulp(age));
        int seed = smoke.seed(0);
        var sampled = smoke.path(seed)[(int) age];

        timeline.sample(8);
        assertEquals(17, smoke.limit());
        assertEquals(0, smoke.first(smoke.limit()));
        timeline.sample(3.95f);
        assertEquals(0, smoke.limit());
        timeline.sample(5.75f);
        assertEquals(age, smoke.ageTicks(0), 0);
        assertEquals(sampled, smoke.path(smoke.seed(0))[(int) smoke.ageTicks(0)]);
    }

    /**
     * Every mask has a steady initial opacity and interpolates to zero at the end of its path.
     */
    @Test
    void paths_matchCampfireLifespanSizeAndFade() {
        var smoke = new LegacyCampfireSmoke(0, 3, 6);
        for (int i = 0; i < 12; i++) {
            var frames = smoke.path(smoke.seed(i));
            int age = frames.length - 1;
            assertTrue(age >= 80 && age < 130);
            assertTrue(frames[0].size() >= 0.27f - Math.ulp(0.27f) && frames[0].size() < 0.54f);
            assertEquals(0.9f, frames[age - 60].alpha(), 0);
            assertEquals(0.45f, frames[age - 30].alpha(), Math.ulp(0.45f));
            assertEquals(0, frames[age].alpha(), Math.ulp(0.9f));
        }
    }
}
