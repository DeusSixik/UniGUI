package dev.sixik.unigui.api.scene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that fog settings participate in the retained scene lifecycle. */
class FogVolumeTest {
    /** Tests invalid coefficients before they can reach a GPU uniform.
     *
     * @param value negative or nonfinite fog coefficient
     */
    @ParameterizedTest
    @ValueSource(floats = {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
    void coefficients_invalid_rejected(float value) {
        assertThrows(IllegalArgumentException.class, () -> new FogVolume(FogVolume.Shape.SPHERE, 0xFFFFFF, value));
        var fog = new FogVolume(FogVolume.Shape.BOX, 0xFFFFFF, 0);
        assertThrows(IllegalArgumentException.class, () -> fog.setLightScattering(value));
        assertThrows(IllegalArgumentException.class, () -> fog.setEdgeSoftness(value));
    }

    /** Checks bounds on smoothing and noise parameters. */
    @Test
    void noiseAndEdge_outOfRange_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new FogVolume.Noise(1.01f, 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new FogVolume.Noise(0.5f, Float.POSITIVE_INFINITY, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new FogVolume.Noise(0.5f, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new FogVolume.Noise(0.5f, 1, 0, Float.NaN, 0));
        var fog = new FogVolume(FogVolume.Shape.BOX, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> fog.setEdgeSoftness(1.01f));
    }

    /** Checks that fog edits invalidate on-change viewports and that hidden ancestors suppress the volume. */
    @Test
    void volume_editAndHide_updatesScene() {
        var fog = new FogVolume(FogVolume.Shape.SPHERE, 0xAA445566, 0.3f);
        var parent = new SceneNode().addChild(fog);
        // addChild returns the parent for chaining.
        var scene = new Scene().addRoot(parent);
        long revision = scene.revision();
        assertEquals(0x445566, fog.colorRgb());
        assertNull(fog.bounds());
        assertSame(fog, fog.setColorRgb(0xFF445566).setDensity(0.3f).setNoise(FogVolume.Noise.NONE));
        assertEquals(revision, scene.revision());
        fog.setDensity(0);
        assertEquals(0, fog.density(), 0f);
        assertTrue(scene.revision() > revision);

        var visited = new ArrayList<SceneNode>();
        scene.visitVisible(null, visited::add);
        assertEquals(List.of(parent, fog), visited);
        parent.setVisible(false);
        visited.clear();
        scene.visitVisible(null, visited::add);
        assertTrue(visited.isEmpty());
    }
}
