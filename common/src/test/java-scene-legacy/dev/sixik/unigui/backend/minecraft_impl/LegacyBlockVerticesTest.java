package dev.sixik.unigui.backend.minecraft_impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the vertex attributes supplied to the OpenGL surface shader. */
final class LegacyBlockVerticesTest {
    /** Exact unit-length axis directions should retain their component values. */
    private static final float EPSILON = 1e-7f;

    /** Ensures indexed surfaces retain native normals and the collector can be reused. */
    @Test
    void build_afterCapture_preservesNormalsAndQuadWinding() {
        var input = new SceneBlockVertexConsumer();
        input.position(0, 0, 0);
        input.direction(0, 1, 0);
        input.position(0, 0, 1);
        input.direction(0, 1, 0);
        input.position(1, 0, 1);
        input.direction(0, 1, 0);
        input.position(1, 0, 0);
        input.direction(0, 1, 0);
        var mesh = input.build();
        assertNotNull(mesh);
        assertEquals(4, mesh.vertexCount());
        assertEquals(6, mesh.indexCount());
        assertEquals(0, mesh.index(3));
        assertEquals(2, mesh.index(4));
        assertEquals(3, mesh.index(5));
        for (int i = 0; i < 4; i++) {
            assertEquals(0, mesh.normal(i, 0), EPSILON);
            assertEquals(1, mesh.normal(i, 1), EPSILON);
            assertEquals(0, mesh.normal(i, 2), EPSILON);
        }

        input.clear();
        assertNull(input.build());
        for (int i = 0; i < 4; i++) input.position(i, 0, 0);
        var reused = input.build();
        assertNotNull(reused);
        assertEquals(1, reused.normal(0, 2), EPSILON);
    }
}
