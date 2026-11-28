package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneTimeline;
import net.minecraft.SharedConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the absolute-time schedule before the version-specific renderer samples native sprites. */
final class MinecraftParticleEmitterTest {
    /** Tolerance for seconds calculated with nonbinary birth intervals. */
    private static final double EPSILON = 1e-12;

    /** Initializes vanilla's registry for ParticleOptions. */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** The start is inclusive, the end exclusive, and old particles expire independently of emission. */
    @Test
    void emissionLimit_atBoundaries_includesOnlyScheduledBirths() {
        var emitter = new MinecraftParticleEmitter(ParticleTypes.FLAME, 4, 4.5, 10);
        assertEquals(0, emitter.setTime(Math.nextDown(4.0)).emissionLimit());
        assertEquals(1, emitter.setTime(4).emissionLimit());
        assertEquals(2, emitter.setTime(4.1).emissionLimit());
        assertEquals(5, emitter.setTime(4.5).emissionLimit());
        int limit = emitter.setTime(4.65).emissionLimit();
        assertEquals(5, limit);
        assertEquals(4, emitter.firstEmission(limit, 0.3));
        assertEquals(0.25, emitter.age(4), EPSILON);
        assertEquals(limit, emitter.setTime(5).firstEmission(limit, 0.3));
    }

    /** Timeline seeks restore the same ages, even when the display skips over the births. */
    @Test
    void seek_afterForwardAndBackwardsJump_restoresAgesAndInvalidatesScene() {
        var emitter = new MinecraftParticleEmitter(ParticleTypes.SMOKE, 2, 6, 8);
        var scene = new Scene().addRoot(emitter);
        var timeline = new SceneTimeline().duration(12).onSample(emitter::setTime);
        timeline.sample(4);
        int limit = emitter.emissionLimit();
        double age = emitter.age(limit - 2);
        long revision = scene.revision();
        timeline.sample(4);
        assertEquals(revision, scene.revision());
        timeline.sample(12);
        timeline.sample(0);
        assertEquals(0, emitter.emissionLimit());
        timeline.sample(4);
        assertTrue(scene.revision() > revision);
        assertEquals(limit, emitter.emissionLimit());
        assertEquals(age, emitter.age(limit - 2), EPSILON);
    }

    /** An ongoing source samples a bounded number of recent births. */
    @Test
    void firstEmission_continuousSource_boundsWorkAtLateTimes() {
        var emitter = new MinecraftParticleEmitter(ParticleTypes.ASH, 0, Double.POSITIVE_INFINITY, 128);
        int limit = emitter.setTime(3600).emissionLimit();
        assertEquals(460801, limit);
        assertEquals(128, limit - emitter.firstEmission(limit, 2));
        assertEquals(32, limit - emitter.firstEmission(limit, 0.25));
    }

    /** Invalid configuration and particles with a different renderer are rejected. */
    @Test
    void configuration_invalidInputsAreRejected() {
        assertFalse(MinecraftParticleEmitter.supports(ParticleTypes.ELDER_GUARDIAN));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter(ParticleTypes.ELDER_GUARDIAN, 0, 1, 12));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter(ParticleTypes.FLAME, 0, 0, 12));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter(ParticleTypes.FLAME, 0, 1, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter(ParticleTypes.FLAME, 0, 1, 12).setTime(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter.Motion(0, 0, 0, -1, 0, 1));
    }

    /** The renderer setting is opt-in and changes invalidate retained viewport output. */
    @Test
    void softParticles_changedSettings_invalidateSceneAndCanBeDisabled() {
        var emitter = new MinecraftParticleEmitter(ParticleTypes.SMOKE, 0, 1, 12);
        var scene = new Scene().addRoot(emitter);
        long revision = scene.revision();
        assertNull(emitter.softParticles());
        var settings = new MinecraftParticleEmitter.SoftParticles(0.35f, 2, 0.6f, 0.4f);
        assertSame(emitter, emitter.setSoftParticles(settings));
        assertSame(settings, emitter.softParticles());
        assertTrue(scene.revision() > revision);
        revision = scene.revision();
        emitter.setSoftParticles(new MinecraftParticleEmitter.SoftParticles(0.35f, 2, 0.6f, 0.4f));
        assertEquals(revision, scene.revision());
        emitter.setSoftParticles(null);
        assertNull(emitter.softParticles());
        assertTrue(scene.revision() > revision);
    }

    /** Invalid depth fades, billboard scales and alpha values cannot reach the shader. */
    @Test
    void softParticles_invalidSettingsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter.SoftParticles(0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter.SoftParticles(1, Float.POSITIVE_INFINITY, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter.SoftParticles(1, 1, -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter.SoftParticles(1, 1, 1, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new MinecraftParticleEmitter.SoftParticles(1, 1, 1, 1.01f));
    }
}
