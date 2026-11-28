package dev.sixik.unigui.backend.minecraft_impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies native scene capture policy for material and directional-shadow passes. */
final class MinecraftScene3DRendererTest {
    /**
     * Keeps supported native models on the shadow-aware surface path after an effect ends.
     *
     * @param surfaceEffects backend native surface support
     * @param hasEffect      node effect state
     * @param shadowPass     shadow-map collection state
     * @param shadowsActive  color-pass shadow-map state
     * @param expected       expected capture result
     */
    @ParameterizedTest
    @CsvSource({
            "false, false, false, false, false",
            "false, true,  true,  true,  false",
            "true,  false, false, false, false",
            "true,  true,  false, false, true",
            "true,  false, true,  false, true",
            "true,  false, false, true,  true"
    })
    void shouldCaptureNative_shadowAwarePolicy(boolean surfaceEffects, boolean hasEffect,
                                               boolean shadowPass, boolean shadowsActive, boolean expected) {
        boolean actual = MinecraftScene3DRenderer.shouldCaptureNative(
                surfaceEffects, hasEffect, shadowPass, shadowsActive);
        assertEquals(expected, actual);
    }
}
