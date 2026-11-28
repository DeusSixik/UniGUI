package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Checks seekable GTNH particle schedules and vanilla sprite sizing. */
class GtnhParticleEmitterTest {
    /** Floating-point tolerance at 0.1 block sprite scale. */
    private static final float EPSILON = Math.ulp(0.1f);

    /** Seeking around schedule boundaries includes the start and excludes the end. */
    @Test
    void setTime_seekAndReplay_preservesBirthSchedule() {
        var emitter = new GtnhParticleEmitter(GtnhParticleEmitter.Type.SMOKE_LARGE, 1, 2, 4);
        assertEquals(0, emitter.setTime(0.99).limit());
        assertEquals(1, emitter.setTime(1).limit());
        assertEquals(4, emitter.setTime(2).limit());
        assertEquals(3, emitter.first(4, 0.5));
        assertEquals(0.25, emitter.age(3), 0);
        assertEquals(2, emitter.setTime(1.25).limit());
        assertEquals(0, emitter.age(1), 0);
    }

    /** Vanilla flames shrink and smoke grows from zero to its base sprite size. */
    @Test
    void nativeSize_flameShrinksAndSmokeGrows() {
        assertEquals(0.2f, GtnhSceneSprites.size(GtnhParticleEmitter.Type.FLAME, 2, 0), 2 * EPSILON);
        assertEquals(0.1f, GtnhSceneSprites.size(GtnhParticleEmitter.Type.FLAME, 2, 1), EPSILON);
        assertEquals(0, GtnhSceneSprites.size(GtnhParticleEmitter.Type.SMOKE_LARGE, 2, 0), 0);
        assertEquals(0.2f, GtnhSceneSprites.size(
                GtnhParticleEmitter.Type.SMOKE_NORMAL, 2, 1f / 32), 2 * EPSILON);
        assertFalse(GtnhParticleEmitter.supports(null));
        assertEquals(1.0f, GtnhSceneSprites.baseScale(GtnhParticleEmitter.Type.FLAME, 0), 0);
        assertEquals(0.75f, GtnhSceneSprites.baseScale(GtnhParticleEmitter.Type.SMOKE_NORMAL, 0), 0);
        assertEquals(1.875f, GtnhSceneSprites.baseScale(GtnhParticleEmitter.Type.SMOKE_LARGE, 0), 0);
        assertEquals(44, GtnhSceneSprites.lifetime(GtnhParticleEmitter.Type.FLAME, 0));
        assertEquals(100, GtnhSceneSprites.lifetime(GtnhParticleEmitter.Type.SMOKE_LARGE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new GtnhParticleEmitter(GtnhParticleEmitter.Type.FLAME, 1, 1, 4));
    }

    /** Replacing smoke tint does not alter the birth schedule. */
    @Test
    void setTintRgb_overrideAndReset_keepsSchedule() {
        var emitter = new GtnhParticleEmitter(GtnhParticleEmitter.Type.SMOKE_NORMAL, 1, 2, 4).setTime(1.5);
        assertEquals(-1, emitter.tintRgb());
        assertSame(emitter, emitter.setTintRgb(0x9DABC0));
        assertEquals(0x9DABC0, emitter.tintRgb());
        assertEquals(3, emitter.limit());
        assertThrows(IllegalArgumentException.class, () -> emitter.setTintRgb(0x1000000));
        assertEquals(0x9DABC0, emitter.tintRgb());
        assertEquals(-1, emitter.setTintRgb(-1).tintRgb());
    }
}
