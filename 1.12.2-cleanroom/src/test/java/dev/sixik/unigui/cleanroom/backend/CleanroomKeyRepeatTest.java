package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomKeyRepeatTest {
    private static final class Fake implements CleanroomKeyRepeat.Repeat {
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
        CleanroomKeyRepeat repeat = new CleanroomKeyRepeat(fake);
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
        CleanroomKeyRepeat repeat = new CleanroomKeyRepeat(fake);
        repeat.update(true);
        repeat.update(false);
        assertTrue(fake.value, "the screen enabled repeats itself and must keep them");
        assertTrue(fake.writes == 0);
    }

    @Test
    void repeatedUpdatesWriteOnce() {
        Fake fake = new Fake();
        CleanroomKeyRepeat repeat = new CleanroomKeyRepeat(fake);
        for (int i = 0; i < 5; i++) repeat.update(true);
        assertTrue(fake.writes == 1);
        repeat.release();
        repeat.release();
        assertTrue(fake.writes == 2);
    }
}