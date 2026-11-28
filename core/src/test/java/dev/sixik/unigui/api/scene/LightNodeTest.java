package dev.sixik.unigui.api.scene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests validation and scene invalidation for local lights. */
class LightNodeTest {
    /**
     * Rejects light values that cannot produce a finite contribution.
     *
     * @param intensity brightness multiplier
     * @param range cutoff in scene units
     */
    @ParameterizedTest
    @CsvSource({"-1, 1", "NaN, 1", "Infinity, 1", "1, 0", "1, NaN", "1, Infinity"})
    void localLight_invalidIntensityOrRange_isRejected(float intensity, float range) {
        assertThrows(IllegalArgumentException.class, () -> new PointLight(0xFFFFFF, intensity, range));
        assertThrows(IllegalArgumentException.class,
                () -> new SpotLight(0xFFFFFF, intensity, range, 0.1f, 0.5f));
    }

    /**
     * Rejects angles outside the domain of the perspective projection.
     *
     * @param inner inner half-angle in radians
     * @param outer outer half-angle in radians
     */
    @ParameterizedTest
    @CsvSource({"-0.1, 0.5", "0.6, 0.5", "0, 0", "0, 1.5707964", "0, NaN", "0, Infinity"})
    void spotLight_invalidCone_isRejected(float inner, float outer) {
        assertThrows(IllegalArgumentException.class, () -> new SpotLight(0xFFFFFF, 1, 3, inner, outer));
    }

    /** Ensures replacement refreshes on-change viewports, and a hidden parent hides the light. */
    @Test
    void lightNode_changeAndHide_updatesScene() {
        var parent = new SceneNode();
        var point = new PointLight(0xAAFF7432, 1, 3);
        var node = new LightNode(point);
        parent.addChild(node);
        var scene = new Scene().addRoot(parent);
        assertEquals(0xFF7432, point.colorRgb());
        long revision = scene.revision();
        node.setLight(new PointLight(0xFF7432, 1, 3));
        assertEquals(revision, scene.revision());

        var spot = new SpotLight(0xD9FFFF, 2, 4, 0.4f, 0.4f).withShadows(new SpotShadows());
        node.setLight(spot);
        assertSame(spot, node.light());
        assertTrue(scene.revision() > revision);
        var visited = new ArrayList<SceneNode>();
        scene.visitVisible(null, visited::add);
        assertEquals(List.of(parent, node), visited);
        parent.setVisible(false);
        visited.clear();
        scene.visitVisible(null, visited::add);
        assertTrue(visited.isEmpty());
    }
}
