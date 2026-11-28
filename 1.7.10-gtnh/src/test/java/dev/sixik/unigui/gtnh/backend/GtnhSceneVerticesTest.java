package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/** Checks native vertex decoding and pose transforms independently of an OpenGL context. */
class GtnhSceneVerticesTest {
    /**
     * Decodes packed RGBA, UVs and normals after a translated, nonuniform scale.
     */
    @Test
    void append_tessellatorItemVertices_preservesColorUvAndInverseScaleNormal() {
        int[] data = quad();
        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            data[offset + 5] = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 0x80EF34AB : 0xAB34EF80;
            data[offset + 6] = 64 | (-64 & 255) << 8;
            data[offset + 3] = Float.floatToRawIntBits(0.25f);
            data[offset + 4] = Float.floatToRawIntBits(0.75f);
        }
        var pose = new MutableMat4().setTransform(new MutableVec3(1, 2, 3), new MutableQuat(), new MutableVec3(2, 4, 1));
        var out = new LegacySceneQuads();

        assertTrue(new GtnhSceneVertices().append(out, data, 4, pose, true, true, true, -1));

        var mesh = out.build();
        assertEquals(3, mesh.position(2, 0), 0);
        assertEquals(6, mesh.position(2, 1), 0);
        assertEquals(3, mesh.position(2, 2), 0);
        assertEquals(32, mesh.normal(2, 0), 0);
        assertEquals(-16, mesh.normal(2, 1), 0);
        assertEquals(0.25f, mesh.uv(2, 0), 0);
        assertEquals(0.75f, mesh.uv(2, 1), 0);
        assertEquals(0x80AB34EF, mesh.colorArgb(2));
        assertEquals(0, mesh.index(0));
        assertEquals(2, mesh.index(2));
        assertEquals(3, mesh.index(5));
    }

    /**
     * Creates geometric normals from the original quad winding and rejects singular poses.
     */
    @Test
    void append_withoutNormals_usesQuadWinding_andRejectsSingularPose() {
        int[] data = quad();
        var out = new LegacySceneQuads();

        assertTrue(new GtnhSceneVertices().append(out, data, 4, new MutableMat4(), false, false, false, 0xFFE0A030));

        var mesh = out.build();
        assertEquals(-1, mesh.normal(0, 2), 0);
        assertEquals(0xFFE0A030, mesh.colorArgb(3));
        assertFalse(new GtnhSceneVertices().append(out, data, 4,
                new MutableMat4().setTransform(new MutableVec3(), new MutableQuat(), new MutableVec3(0, 1, 1)),
                false, false, false, -1));
        assertEquals(4, out.build().vertexCount());
    }

    /**
     * @return four positions forming a clockwise XY-plane quad in native tessellator layout
     */
    private static int[] quad() {
        int[] data = new int[32];
        putPosition(data, 0, 0, 0, 0);
        putPosition(data, 8, 0, 1, 0);
        putPosition(data, 16, 1, 1, 0);
        putPosition(data, 24, 1, 0, 0);
        return data;
    }

    /**
     * @param data destination tessellator data
     * @param offset vertex offset in ints
     * @param x position in blocks
     * @param y position in blocks
     * @param z position in blocks
     */
    private static void putPosition(int[] data, int offset, float x, float y, float z) {
        data[offset] = Float.floatToRawIntBits(x);
        data[offset + 1] = Float.floatToRawIntBits(y);
        data[offset + 2] = Float.floatToRawIntBits(z);
    }
}
