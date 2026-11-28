package dev.sixik.unigui.api.scene;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the temporary flat lighting used by editor viewports with lights turned off. */
class SceneRenderUnlitTest {
    /** Inside the pass the scene reads as unlit and light nodes are skipped; afterwards nothing changed. */
    @Test
    void renderUnlit_hidesLightsOnlyDuringThePass() {
        AmbientLight ambient = new AmbientLight(0x336699, 0.3f);
        DirectionalLight sun = new DirectionalLight(0, -1, 0, 0xFFFFFF, 0.8f);
        DirectionalShadows shadows = new DirectionalShadows(0, 0, 0, 8);
        Scene scene = new Scene().setAmbientLight(ambient).setDirectionalLight(sun).setDirectionalShadows(shadows)
                .setMinecraftBlockLighting(true);
        SceneNode group = new SceneNode();
        LightNode lamp = new LightNode(new PointLight(0xFFFFFF, 1, 4));
        SceneNode child = new SceneNode();
        lamp.addChild(child);
        group.addChild(lamp);
        scene.addRoot(group);
        long revision = scene.revision();

        List<SceneNode> visited = new ArrayList<>();
        scene.renderUnlit(() -> {
            assertSame(AmbientLight.WHITE, scene.ambientLight());
            assertNull(scene.directionalLight());
            assertNull(scene.directionalShadows());
            assertFalse(scene.minecraftBlockLighting());
            scene.visitVisible(null, visited::add);
        });

        assertEquals(List.of(group, child), visited);
        assertSame(ambient, scene.ambientLight());
        assertSame(sun, scene.directionalLight());
        assertSame(shadows, scene.directionalShadows());
        assertTrue(scene.minecraftBlockLighting());
        assertEquals(revision, scene.revision());
        visited.clear();
        scene.visitVisible(null, visited::add);
        assertEquals(List.of(group, lamp, child), visited);
    }

    /** A failing pass still restores the lit state, and nested passes stay unlit until the outer one ends. */
    @Test
    void renderUnlit_restoresAfterExceptionsAndNesting() {
        DirectionalLight sun = new DirectionalLight(1, -1, 0, 0xFFFFFF, 1);
        Scene scene = new Scene().setDirectionalLight(sun);
        assertThrows(IllegalStateException.class, () -> scene.renderUnlit(() -> {
            throw new IllegalStateException("pass failed");
        }));
        assertSame(sun, scene.directionalLight());
        scene.renderUnlit(() -> {
            scene.renderUnlit(() -> assertNull(scene.directionalLight()));
            assertNull(scene.directionalLight());
        });
        assertSame(sun, scene.directionalLight());
        assertThrows(NullPointerException.class, () -> scene.renderUnlit(null));
    }
}
