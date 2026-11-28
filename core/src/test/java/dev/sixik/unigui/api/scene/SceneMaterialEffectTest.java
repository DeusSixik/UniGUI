package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Covers authoring and seeking material intervals without a graphics device. */
final class SceneMaterialEffectTest {
    /** Exact tolerance for integer seconds, midpoint fractions and saturation. */
    private static final float EXACT = 0f;

    /**
     * Samples the interval both inside and outside its boundaries.
     *
     * @param seconds scene clock in seconds
     * @param elapsed expected nonnegative elapsed seconds
     * @param progress expected interval fraction
     */
    @ParameterizedTest
    @CsvSource({"5, 0, 0", "6, 0, 0", "7, 1, 0.5", "8, 2, 1", "10, 4, 1"})
    void timing_samplesElapsedAndClampedProgress(float seconds, float elapsed, float progress) {
        var timing = new SceneMaterialEffect.Timing(6f, 2f);
        assertEquals(elapsed, timing.elapsedAt(seconds), EXACT);
        assertEquals(progress, timing.progressAt(seconds), EXACT);
    }

    /** Verifies that relative time stays representable and tiny intervals can reach completion. */
    @Test
    void timing_finiteExtremes_saturateWithoutOverflow() {
        var longInterval = new SceneMaterialEffect.Timing(-Float.MAX_VALUE, Float.MAX_VALUE);
        assertEquals(Float.MAX_VALUE, longInterval.elapsedAt(Float.MAX_VALUE), EXACT);
        assertEquals(1, longInterval.progressAt(Float.MAX_VALUE), EXACT);

        var shortInterval = new SceneMaterialEffect.Timing(0, Float.MIN_VALUE);
        assertEquals(0, shortInterval.progressAt(0), EXACT);
        assertEquals(1, shortInterval.progressAt(Float.MIN_VALUE), EXACT);
    }

    /** Verifies that reconfiguration preserves shader identity and independent settings. */
    @Test
    void effect_builders_preserveOriginalAndCloneOtherSettings() {
        var shader = ShaderHandle.fragmentSource("test:timed", "void testFunction() {}");
        var original = new SceneMaterialEffect(shader, 1, 2, 3, 4);
        var modified = original.withTiming(-1, 2).withAlphaCutoff(0.25f).withParameters(5, 6, 7, 8);
        assertNull(original.timing());
        assertEquals(0, original.alphaCutoff(), EXACT);
        assertSame(shader, modified.shader());
        assertEquals(0.5f, modified.timing().progressAt(0), EXACT);
        assertEquals(0.25f, modified.alphaCutoff(), EXACT);
        assertEquals(5, modified.x(), EXACT);
        assertEquals(8, modified.w(), EXACT);
    }

    /** Rejects invalid inputs at the API boundary. */
    @Test
    void effect_invalidTimingAndCutoff_throw() {
        var effect = new SceneMaterialEffect(ShaderHandle.fragmentSource("test:invalid", "void testFunction() {}"));
        assertThrows(IllegalArgumentException.class, () -> effect.withTiming(Float.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> effect.withTiming(0, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> effect.withTiming(0, 0));
        assertThrows(IllegalArgumentException.class, () -> effect.withTiming(0, -1));
        assertThrows(IllegalArgumentException.class, () -> effect.withAlphaCutoff(-0.125f));
        assertThrows(IllegalArgumentException.class, () -> effect.withAlphaCutoff(1.125f));
        assertThrows(IllegalArgumentException.class, () -> effect.withAlphaCutoff(Float.NaN));
        var timing = effect.withTiming(0, 1).timing();
        assertThrows(IllegalArgumentException.class, () -> timing.elapsedAt(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> timing.progressAt(Float.NEGATIVE_INFINITY));
    }
}
