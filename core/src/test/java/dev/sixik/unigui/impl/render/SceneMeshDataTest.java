package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneMaterialEffect;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class SceneMeshDataTest {
    private static final float EPS = 1e-6f;

    @Test
    void writeVertices_interleavesPositionUvRgbaAndSignedNormal() {
        Mesh3D mesh = new Mesh3D(new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9},
                new float[]{0, -3, 4, 0, 0, 0, 1, 0, 0},
                new float[]{0.25f, 0.75f, 0, 0, 1, 1},
                new int[]{0x7F123456, 0, -1}, new int[]{2, 0, 1});
        ByteBuffer vertices = buffer(3 * SceneMeshData.VERTEX_BYTES + 4);
        vertices.position(4);

        SceneMeshData.writeVertices(mesh, vertices);

        assertEquals(4 + 3 * SceneMeshData.VERTEX_BYTES, vertices.position());
        assertEquals(1.0f, vertices.getFloat(4), EPS);
        assertEquals(3.0f, vertices.getFloat(12), EPS);
        assertEquals(0.25f, vertices.getFloat(16), EPS);
        assertEquals(0.75f, vertices.getFloat(20), EPS);
        assertEquals(0x12, vertices.get(24) & 255);
        assertEquals(0x34, vertices.get(25) & 255);
        assertEquals(0x56, vertices.get(26) & 255);
        assertEquals(0x7F, vertices.get(27) & 255);
        assertEquals(-76, vertices.get(29));
        assertEquals(102, vertices.get(30));
        assertEquals(0, vertices.get(31));
        assertEquals(0, vertices.getInt(4 + SceneMeshData.VERTEX_BYTES + 24));

        ByteBuffer indices = buffer(12);
        SceneMeshData.writeIndices(mesh, indices);
        assertEquals(2, indices.getInt(0));
        assertEquals(0, indices.getInt(4));
        assertEquals(1, indices.getInt(8));
    }

    @Test
    void writeUniform_respectsStd140LayoutAndPremultipliedTextureFlags() {
        MutableMat4 clip = new MutableMat4().set((row, column) -> column * 4 + row + 1);
        MutableMat4 world = transform(-2, 4, 8);
        Material material = new Material(0x80FF8040, null, Material.Shading.LIT,
                Material.AlphaMode.CUTOUT, 0.35f, Material.Culling.FRONT);
        AmbientLight ambient = new AmbientLight(0xFF8000, 0.5f);
        DirectionalLight light = new DirectionalLight(0, 0, -2, 0xFF0000, 0.25f);
        ByteBuffer out = buffer(SceneMeshData.UNIFORM_BYTES);

        SceneMeshData.writeUniform(out, clip, world, material, ambient, light, 0.75f, 1,
                true, true, true);

        assertEquals(SceneMeshData.UNIFORM_BYTES, out.position());
        for (int i = 0; i < 16; i++) assertEquals(i + 1.0f, out.getFloat(i * 4), EPS);
        assertVector(out, 64, -1, 0, 0, 0);
        assertVector(out, 80, 0, 0.5f, 0, 0);
        assertVector(out, 96, 0, 0, 0.25f, 0);
        assertVector(out, 112, 1, 128 / 255.0f, 64 / 255.0f, 128 / 255.0f);
        assertVector(out, 128, 0.5f, 64 / 255.0f, 0, 0);
        assertVector(out, 144, 0.25f, 0, 0, 0);
        assertVector(out, 160, 0, 0, 1, 1);
        assertVector(out, 176, 0.75f, 1, 1, 1);
        assertVector(out, 192, 0.35f, 1, 1, 2);
    }

    @Test
    void writeUniform_shearAndCollapsedAxis_preserveCorrectNormalDirections() {
        ByteBuffer out = buffer(SceneMeshData.UNIFORM_BYTES);
        MutableMat4 world = transform(2, 1, 1).set((row, column) -> {
            if (row == 0 && column == 1) return 1;
            if (row != column) return 0;
            return row == 0 ? 2 : 1;
        });
        writeDefault(out, world);
        assertVector(out, 64, 0.5f, -0.5f, 0, 0);

        out.clear();
        writeDefault(out, transform(0, 2, 3));
        assertVector(out, 64, 1, 0, 0, 0);
        assertVector(out, 80, 0, 0, 0, 0);
        assertVector(out, 96, 0, 0, 0, 0);
        assertVector(out, 144, 0, 0, 0, 0);
    }

    @Test
    void writeUniform_smallDestination_failsBeforeWriting() {
        ByteBuffer out = buffer(SceneMeshData.UNIFORM_BYTES - 1);
        assertThrows(IndexOutOfBoundsException.class, () -> writeDefault(out, new MutableMat4()));
        assertEquals(0, out.position());
    }

    /** Verifies consecutive std140 bounds and clock vectors, including a plane with zero Y extent. */
    @Test
    void writeEffectUniform_timedSurface_packsBoundsProgressAndCutoff() {
        var effect = new SceneMaterialEffect(ShaderHandle.fragmentSource("test:effect", "void testFunction() {}"))
                .withTiming(6, 2).withAlphaCutoff(0.25f);
        ByteBuffer out = buffer(16 + SceneMeshData.EFFECT_BYTES);
        out.position(16);
        SceneMeshData.writeEffectUniform(out, new Aabb(-2, 4, -6, 8, 4, 9), effect, 7);

        assertEquals(16 + SceneMeshData.EFFECT_BYTES, out.position());
        assertVector(out, 16, -2, 4, -6, 0);
        assertVector(out, 32, 8, 4, 9, 0);
        assertVector(out, 48, 1, 0.5f, 0.25f, 0);
    }

    /** Verifies untimed draws and the validation done before writing to a short buffer. */
    @Test
    void writeEffectUniform_defaultSurface_usesZeroProgressAndCutoff() {
        var bounds = new Aabb(0, 0, 0, 1, 1, 1);
        ByteBuffer out = buffer(SceneMeshData.EFFECT_BYTES);
        SceneMeshData.writeEffectUniform(out, bounds, null, 7);
        assertVector(out, 32, 7, 0, 0, 0);

        out.clear();
        out.limit(SceneMeshData.EFFECT_BYTES - 1);
        assertThrows(IndexOutOfBoundsException.class, () -> SceneMeshData.writeEffectUniform(out, bounds, null, 7));
        assertEquals(0, out.position());
    }

    /** Packs the spotlight receiver flag even when the material has no custom effect. */
    @Test
    void writeEffectUniform_spotShadowReceiver_setsLastClockComponent() {
        var out = buffer(SceneMeshData.EFFECT_BYTES);
        SceneMeshData.writeEffectUniform(out, new Aabb(0, 0, 0, 1, 1, 1), null, 2, true);
        assertVector(out, 32, 2, 0, 0, 1);
    }

    /** Проверяет смещение матриц второго экземпляра в массиве std140. */
    @Test
    void writeClipMatrix_consecutiveModels_preserveStd140Stride() {
        ByteBuffer out = buffer(2 * SceneMeshData.CLIP_BYTES);
        MutableMat4 first = new MutableMat4();
        MutableMat4 second = transform(-2, 4, 8);
        SceneMeshData.writeClipMatrix(out, first);
        SceneMeshData.writeClipMatrix(out, second);

        assertEquals(128, out.position());
        assertVector(out, 0, 1, 0, 0, 0);
        assertVector(out, 64, -2, 0, 0, 0);
        assertVector(out, 96, 0, 0, 8, 0);
    }

    private static ByteBuffer buffer(int capacity) {
        return ByteBuffer.allocate(capacity).order(ByteOrder.nativeOrder());
    }

    private static MutableMat4 transform(float x, float y, float z) {
        return new MutableMat4().setTransform(new MutableVec3(), new MutableQuat(), new MutableVec3(x, y, z));
    }

    private static void writeDefault(ByteBuffer out, MutableMat4 world) {
        SceneMeshData.writeUniform(out, world, world, Material.DEFAULT, AmbientLight.WHITE, null, 1, 1,
                false, false, false);
    }

    private static void assertVector(ByteBuffer out, int offset, float x, float y, float z, float w) {
        assertEquals(x, out.getFloat(offset), EPS);
        assertEquals(y, out.getFloat(offset + 4), EPS);
        assertEquals(z, out.getFloat(offset + 8), EPS);
        assertEquals(w, out.getFloat(offset + 12), EPS);
    }
}
