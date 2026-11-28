package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the old chest texture layout, winding, and facing independently of the client.
 */
class LegacyChestModelTest {
    /**
     * Verifies the latch ends on the requested block face and has an outward normal.
     *
     * @param facing Minecraft horizontal facing metadata
     * @param axis   front-facing coordinate, X or Z
     * @param edge   block boundary at 0 or 1
     * @param normal outward normal along the front-facing axis
     */
    @ParameterizedTest
    @CsvSource({"2, 2, 0, -1", "3, 2, 1, 1", "4, 0, 0, -1", "5, 0, 1, 1", "0, 2, 1, 1"})
    void bake_latchFollowsFacing(int facing, int axis, int edge, int normal) {
        var mesh = LegacyChestModel.bake("normal", facing, true).mesh();

        assertEquals(72, mesh.vertexCount());
        assertEquals(108, mesh.indexCount());
        // The latch is the second box. Its front is the fifth quad in ModelBox order.
        for (int vertex = 40; vertex < 44; vertex++) {
            assertEquals(edge, mesh.position(vertex, axis), 0);
            assertEquals(normal, mesh.normal(vertex, axis), 0);
        }
        assertEquals(3 / 64f, mesh.uv(40, 0), 0);
        assertEquals(1 / 64f, mesh.uv(40, 1), 0);
        assertOutwardTriangles(mesh);
    }

    /**
     * The chest samples vanilla's standalone texture and uses the selected shading path.
     */
    @Test
    void bake_trappedChestUsesItsOwnTexture() {
        var material = LegacyChestModel.bake("trapped", 3, false).material();
        assertEquals("minecraft:textures/entity/chest/trapped.png", material.texture().id());
        assertEquals(Material.Shading.UNLIT, material.shading());
    }

    /**
     * The cross product of two triangle edges must point in the stored normal direction.
     *
     * @param mesh chest geometry to check
     */
    private static void assertOutwardTriangles(Mesh3D mesh) {
        for (int i = 0; i < mesh.indexCount(); i += 3) {
            int a = mesh.index(i), b = mesh.index(i + 1), c = mesh.index(i + 2);
            float abx = mesh.position(b, 0) - mesh.position(a, 0);
            float aby = mesh.position(b, 1) - mesh.position(a, 1);
            float abz = mesh.position(b, 2) - mesh.position(a, 2);
            float acx = mesh.position(c, 0) - mesh.position(a, 0);
            float acy = mesh.position(c, 1) - mesh.position(a, 1);
            float acz = mesh.position(c, 2) - mesh.position(a, 2);
            assertTrue((aby * acz - abz * acy) * mesh.normal(a, 0)
                + (abz * acx - abx * acz) * mesh.normal(a, 1)
                + (abx * acy - aby * acx) * mesh.normal(a, 2) > 0);
        }
    }
}
