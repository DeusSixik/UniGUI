package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.PointLight;
import dev.sixik.unigui.api.scene.SpotLight;
import dev.sixik.unigui.api.scene.SpotShadows;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/** Checks CPU staging layout consumed by the scene shaders of both render backends. */
final class SceneEnvironmentDataTest {
    /** Precision allows a few float rounding steps during RGB normalization and cosine evaluation. */
    private static final float EPSILON = 1e-6f;

    /** Ensures one light entry occupies the same byte size for both light types. */
    @Test
    void writeLight_afterSpotToPointChange_clearsProjectionAndSamplerData() {
        var data = new SceneLightData();
        var spot = new SpotLight(0xFF8000, 2, 8, 0.25f, 0.5f)
                .withShadows(new SpotShadows(256, 0.01f, 0.75f));
        var light = new LightNode(spot);
        light.setPosition(2, 3, 4);
        assertTrue(data.transform.update(light));
        data.light = spot;
        data.maskIndex = 3;
        data.shadowIndex = 2;
        data.premultiplied = true;
        data.uv.set(0.25f, 0.5f, 0.5f, 0.25f);

        var out = ByteBuffer.allocate(SceneLightData.BYTES).order(ByteOrder.nativeOrder());
        data.write(out);
        assertEquals(SceneLightData.BYTES, out.position());
        assertEquals(2, out.getFloat(0), EPSILON);
        assertEquals(8, out.getFloat(12), EPSILON);
        assertEquals(256f / 255, out.getFloat(20), EPSILON);
        assertEquals((float) Math.cos(0.5f), out.getFloat(44), EPSILON);
        assertEquals(3, out.getFloat(52), EPSILON);
        assertEquals(2, out.getFloat(56), EPSILON);
        assertEquals(0.75f, out.getFloat(84), EPSILON);

        data.reset();
        data.light = new PointLight(0xffffff, 1, 4);
        out.clear();
        data.write(out);
        assertEquals(SceneLightData.BYTES, out.position());
        assertEquals(-1, out.getFloat(44), EPSILON);
        for (int offset = 48; offset < SceneLightData.BYTES; offset += Long.BYTES) {
            assertEquals(0L, out.getLong(offset));
        }
    }

    /** Checks the std140 frame header and the inverse transformation of the visible fog volume. */
    @Test
    void writeFog_visibleVolume_packsCameraAndInverseLocalTransform() {
        var data = new SceneFogData();
        var camera = new Camera3D().setOrthographic(8, 0.25f, 20);
        var clip = camera.viewProjection(new MutableMat4());
        var frustum = new Frustum3D().set(clip);
        var volume = new FogVolume(FogVolume.Shape.SPHERE, 0xFF8000, 0.4f).setLightScattering(0.8f);
        volume.setPosition(0, 0, -3);
        data.collect(volume, frustum);
        assertTrue(data.prepare(clip));
        assertTrue(data.scattering());

        var out = data.write(0.75f, 0.5f, camera, 2, 1);
        assertEquals(SceneFogShaders.UNIFORM_BYTES, out.remaining());
        assertEquals(0.75f, out.getFloat(64), EPSILON);
        assertEquals(0.25f, out.getFloat(72), EPSILON);
        assertEquals(20, out.getFloat(76), EPSILON);
        assertEquals(1, out.getFloat(84), EPSILON);
        assertEquals(1, out.getFloat(88), EPSILON);
        assertEquals(3, out.getFloat(96 + 14 * Float.BYTES), EPSILON);
        assertEquals(128f / 255, out.getFloat(164), EPSILON);
        assertEquals(0.4f, out.getFloat(172), EPSILON);
        assertEquals(0.8f, out.getFloat(204), EPSILON);

        data.reset();
        assertFalse(data.prepare(clip));
        assertFalse(data.scattering());
    }
}
