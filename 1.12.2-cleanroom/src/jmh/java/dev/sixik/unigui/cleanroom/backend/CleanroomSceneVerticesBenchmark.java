package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/** CPU cost of decoding and building a 256-vertex native model layer, without a GL context. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class CleanroomSceneVerticesBenchmark {
    /** Four vertices per quad, with 64 quads in the sample layer. */
    private static final int VERTICES = 256;
    /** Decoder used for each captured layer. */
    private final CleanroomSceneVertices decoder = new CleanroomSceneVertices();
    /** Nonuniform model pose in scene units. */
    private final MutableMat4 pose = new MutableMat4()
            .setTransform(new MutableVec3(1, 2, 3), new MutableQuat(), new MutableVec3(1, 2, 0.5f));
    /** Native-order model vertex stream. */
    private ByteBuffer data;

    /** Prepares one native-format sample outside the timed benchmark. */
    @Setup
    public void setup() {
        var format = DefaultVertexFormats.ITEM;
        data = ByteBuffer.allocateDirect(format.getSize() * VERTICES).order(ByteOrder.nativeOrder());
        for (int i = 0; i < VERTICES; i++) {
            data.putFloat(i % 2).putFloat(i % 4 / 2).putFloat(i / 4f);
            data.putInt(-1).putFloat(0.2f).putFloat(0.5f);
            data.put((byte) 0).put((byte) 0).put((byte) 127).put((byte) 0);
        }
        data.flip();
    }

    /** @return the indexed model layer consumed by the renderer */
    @Benchmark
    public Mesh3D decodeLayer() {
        var out = new LegacySceneQuads();
        decoder.format(DefaultVertexFormats.ITEM);
        decoder.append(out, data, VERTICES, pose, -1);
        return out.build();
    }
}
