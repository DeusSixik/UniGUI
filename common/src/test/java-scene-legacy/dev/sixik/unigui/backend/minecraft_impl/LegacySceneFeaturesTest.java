package dev.sixik.unigui.backend.minecraft_impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checks the alpha rules preserved while capturing vanilla render layers. */
final class LegacySceneFeaturesTest {
    /**
     * Checks the cutoff for standard item layers and native layers that retain smooth alpha.
     *
     * @param shaderName vanilla shader identifier
     * @param cutoff whether alpha below 0.1 is discarded
     */
    @ParameterizedTest
    @CsvSource({
            "rendertype_item_entity_translucent_cull, true",
            "minecraft:rendertype_entity_translucent_cull, true",
            "rendertype_entity_translucent, true",
            "rendertype_entity_translucent_emissive, true",
            "rendertype_eyes, false",
            "rendertype_glint, false"
    })
    void usesTranslucentCutout_matchesVanillaShader(String shaderName, boolean cutoff) {
        assertEquals(cutoff, LegacySceneFeatures.usesTranslucentCutout(shaderName));
    }
}
