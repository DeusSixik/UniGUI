package dev.sixik.unigui.api.scene;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that each preset can be configured without changing the shared defaults. */
class SceneMaterialEffectsTest {
    /**
     * Enumerates the preset names and their public factory results.
     *
     * @return the available material effects
     */
    static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of("dissolve", SceneMaterialEffects.dissolve()),
                Arguments.of("scanline", SceneMaterialEffects.scanline()),
                Arguments.of("hologram", SceneMaterialEffects.hologram()),
                Arguments.of("pulse", SceneMaterialEffects.pulse()));
    }

    /**
     * Checks that tuning a preset preserves its embedded pipeline and supports seeking.
     *
     * @param name stable shader path suffix
     * @param preset factory default
     */
    @ParameterizedTest
    @MethodSource("presets")
    void preset_withTimingAndTint_reusesShader(String name, SceneMaterialEffect preset) {
        var custom = preset.withTiming(6, 2).withParameters(0.25f, 0.5f, 0.75f, 2);
        assertEquals("unigui:scene_effect/" + name, preset.shader().id());
        assertTrue(preset.shader().hasEmbeddedFragmentSource());
        assertNull(preset.timing());
        assertSame(preset.shader(), custom.shader());
        // All of these values are exactly representable in float.
        assertEquals(0.5f, custom.timing().progressAt(7), 0f);
        assertEquals(0f, custom.timing().progressAt(5), 0f);
        assertEquals(1f, custom.timing().progressAt(8), 0f);
        assertEquals(2f, custom.w(), 0f);
    }
}
