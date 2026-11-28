package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class CleanroomSceneVerticesTest {
    @Test
    void append_itemFormat_appliesPosePreservesWindingAndUnpacksColor() {
        var format = DefaultVertexFormats.ITEM;
        var data = ByteBuffer.allocate(format.getSize() * 4).order(ByteOrder.nativeOrder());
        for (int i = 0; i < 4; i++) {
            data.putFloat(i == 1 || i == 2 ? 1 : 0).putFloat(i >= 2 ? 1 : 0).putFloat(0);
            data.put((byte) 0xAB).put((byte) 0x34).put((byte) 0xEF).put((byte) 0x80);
            data.putFloat(0.25f).putFloat(0.75f);
            data.put((byte) 64).put((byte) -64).put((byte) 0).put((byte) 0);
        }
        var decoder = new CleanroomSceneVertices();
        assertTrue(decoder.format(format));
        var pose = new MutableMat4().setTransform(new MutableVec3(1, 2, 3), new MutableQuat(), new MutableVec3(2, 4, 1));
        var out = new LegacySceneQuads();
        assertTrue(decoder.append(out, data.flip(), 4, pose, -1));
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

    @Test
    void append_positionFormat_windingSuppliesNormalAndSingularPoseIsRejected() {
        var data = ByteBuffer.allocate(48).order(ByteOrder.nativeOrder());
        data.putFloat(0).putFloat(0).putFloat(0);
        data.putFloat(0).putFloat(1).putFloat(0);
        data.putFloat(1).putFloat(1).putFloat(0);
        data.putFloat(1).putFloat(0).putFloat(0).flip();
        var decoder = new CleanroomSceneVertices();
        assertTrue(decoder.format(DefaultVertexFormats.POSITION));
        var pose = new MutableMat4();
        var out = new LegacySceneQuads();
        assertTrue(decoder.append(out, data, 4, pose, 0xFFE0A030));
        assertEquals(-1, out.build().normal(0, 2), 0);
        assertEquals(0xFFE0A030, out.build().colorArgb(3));

        var singular = new MutableMat4().setTransform(new MutableVec3(), new MutableQuat(), new MutableVec3(0, 1, 1));
        assertFalse(decoder.append(out, data, 4, singular, -1));
        assertEquals(4, out.build().vertexCount());
    }
}
