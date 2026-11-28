package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests fire flicker sampling independently of the renderer. */
class LightFlickerTest {
    /** Warm point light matching the demo, with range in scene units. */
    private static final PointLight BASE = new PointLight(0xFF9E55, 1.65f, 3f);

    /** Checks the initial sample and its reproducibility after seeking through a flare. */
    @Test
    void sample_seekBack_restoresIntensityAndTint() {
        var flicker = LightFlicker.fire(42);
        var initial = flicker.sample(BASE, 0);
        // Expected from the preset formula for seed 42, with tolerance for final float rounding.
        assertEquals(1.8133134f, initial.intensity(), 1e-6f);
        assertEquals(0xFFA257, initial.colorRgb());
        var flare = flicker.sample(BASE, 1.89f);
        assertTrue(flare.intensity() > initial.intensity());
        assertTrue((flare.colorRgb() >>> 8 & 255) > (initial.colorRgb() >>> 8 & 255));
        assertEquals(initial, flicker.sample(BASE, 0));
        assertEquals(initial, flicker.sample(BASE, -5));
        assertNotEquals(initial, LightFlicker.fire(87).sample(BASE, 0));
    }

    /** Verifies there is no step in the intensity across a noise cell boundary. */
    @Test
    void sample_noiseBoundary_blendsBetweenSamples() {
        var flicker = LightFlicker.fire(42);
        var left = flicker.sample(BASE, 1 - 1e-6f);
        var right = flicker.sample(BASE, 1 + 1e-6f);
        // The probes are about 2 µs apart and the fastest noise channel changes at 11 samples per second.
        assertEquals(left.intensity(), right.intensity(), 1e-4f);
    }

    /** Checks that spot animation preserves the cone and its borrowed resources. */
    @Test
    void sample_spotLight_preservesGeometryAndBindings() {
        var mask = new SimpleTextureHandle("test:mask", 8, 8);
        var shadows = new SpotShadows();
        var base = new SpotLight(0xFF9955, 2f, 6f, 0.3f, 0.6f, mask, shadows);
        var animated = assertInstanceOf(SpotLight.class, LightFlicker.fire(42).sample(base, 1.25f));
        assertEquals(base.innerAngle(), animated.innerAngle(), 0f);
        assertEquals(base.outerAngle(), animated.outerAngle(), 0f);
        assertEquals(base.range(), animated.range(), 0f);
        assertSame(mask, animated.mask());
        assertSame(shadows, animated.shadows());
    }

    /** Checks disabled lights and prevents float overflow when a bright preset flares. */
    @Test
    void sample_extremeIntensity_preservesDisabledAndSaturates() {
        var flicker = LightFlicker.fire(42);
        var off = new PointLight(0xFFFFFF, 0, 2);
        var black = new PointLight(0, 1, 2);
        assertSame(off, flicker.sample(off, 1.89f));
        assertSame(black, flicker.sample(black, 1.89f));
        assertEquals(Float.MAX_VALUE, flicker.sample(new PointLight(0xFFFFFF, Float.MAX_VALUE, 1), 1.89f).intensity(), 0f);
        assertTrue(Float.isFinite(flicker.sample(BASE, Float.MAX_VALUE).intensity()));
    }

    /**
     * Rejects an unusable animation time.
     *
     * @param seconds nonfinite elapsed time in seconds
     */
    @ParameterizedTest
    @ValueSource(floats = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void sample_nonfiniteTime_throws(float seconds) {
        assertThrows(IllegalArgumentException.class, () -> LightFlicker.fire(42).sample(BASE, seconds));
    }
}
