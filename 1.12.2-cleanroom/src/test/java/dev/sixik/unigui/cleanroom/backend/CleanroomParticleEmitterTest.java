package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.util.EnumParticleTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CleanroomParticleEmitterTest {
    @Test
    void setTime_seekAndReplay_keepTheSameBirthsAndRespectExclusiveEnd() {
        var emitter = new CleanroomParticleEmitter(EnumParticleTypes.SMOKE_LARGE, 1, 2, 4);
        assertEquals(0, emitter.setTime(0.99).limit());
        assertEquals(1, emitter.setTime(1).limit());
        assertEquals(4, emitter.setTime(2).limit());
        assertEquals(3, emitter.first(4, 0.5));
        assertEquals(0.25, emitter.age(3), 0);
        assertEquals(2, emitter.setTime(1.25).limit());
        assertEquals(0, emitter.age(1), 0);
    }

    @Test
    void nativeSize_flameShrinksAndSmokeGrowsFromBirth() {
        assertEquals(0.2f, CleanroomSceneSprites.size(EnumParticleTypes.FLAME, 2, 0), Math.ulp(0.2f));
        assertEquals(0.1f, CleanroomSceneSprites.size(EnumParticleTypes.FLAME, 2, 1), Math.ulp(0.1f));
        assertEquals(0, CleanroomSceneSprites.size(EnumParticleTypes.SMOKE_LARGE, 2, 0), 0);
        assertEquals(0.2f, CleanroomSceneSprites.size(EnumParticleTypes.SMOKE_NORMAL, 2, 1f / 32), Math.ulp(0.2f));
        assertThrows(IllegalArgumentException.class,
                () -> new CleanroomParticleEmitter(EnumParticleTypes.CLOUD, 0, 1, 5));
        assertThrows(IllegalArgumentException.class, () -> new CleanroomParticleEmitter.SoftParticles(Float.NaN, 1, 1, 1));
    }

    @Test
    void setTintRgb_overrideAndReset_keepTheParticleSchedule() {
        var emitter = new CleanroomParticleEmitter(EnumParticleTypes.SMOKE_LARGE, 1, 2, 4).setTime(1.5);
        assertEquals(-1, emitter.tintRgb());
        assertSame(emitter, emitter.setTintRgb(0x9DABC0));
        assertEquals(0x9DABC0, emitter.tintRgb());
        assertEquals(3, emitter.limit());
        assertThrows(IllegalArgumentException.class, () -> emitter.setTintRgb(0x1000000));
        assertEquals(0x9DABC0, emitter.tintRgb());
        assertEquals(-1, emitter.setTintRgb(-1).tintRgb());
        assertEquals(3, emitter.limit());
    }
}
