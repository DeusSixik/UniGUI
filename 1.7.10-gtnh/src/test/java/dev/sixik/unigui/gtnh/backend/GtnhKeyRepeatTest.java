package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhKeyRepeatTest {
    private static final class Fake implements GtnhKeyRepeat.Repeat {
        boolean value;
        int writes;

        @Override
        public boolean enabled() {
            return value;
        }

        @Override
        public void enabled(boolean enabled) {
            value = enabled;
            writes++;
        }
    }

    @Test
    void enablesWhileWantedAndRestoresAfterwards() {
        Fake fake = new Fake();
        GtnhKeyRepeat repeat = new GtnhKeyRepeat(fake);
        repeat.update(true);
        assertTrue(fake.value);
        assertTrue(repeat.owns());
        repeat.update(false);
        assertFalse(fake.value);
        assertFalse(repeat.owns());
    }

    @Test
    void doesNotTouchRepeatThatTheScreenAskedFor() {
        Fake fake = new Fake();
        fake.value = true;
        GtnhKeyRepeat repeat = new GtnhKeyRepeat(fake);
        repeat.update(true);
        repeat.update(false);
        assertTrue(fake.value, "the screen enabled repeats itself and must keep them");
        assertTrue(fake.writes == 0);
    }

    @Test
    void repeatedUpdatesWriteOnce() {
        Fake fake = new Fake();
        GtnhKeyRepeat repeat = new GtnhKeyRepeat(fake);
        for (int i = 0; i < 5; i++) repeat.update(true);
        assertTrue(fake.writes == 1);
        repeat.release();
        repeat.release();
        assertTrue(fake.writes == 2);
    }
}