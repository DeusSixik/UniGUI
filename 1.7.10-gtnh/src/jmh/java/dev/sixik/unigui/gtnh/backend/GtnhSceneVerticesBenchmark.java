package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/** CPU cost of decoding one 256-vertex 1.7.10 model layer without a GL context. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class GtnhSceneVerticesBenchmark {
    /** Four vertices per quad, with 64 quads in the representative model part. */
    private static final int VERTICES = 256;
    /** Decoder reused between native model captures. */
    private final GtnhSceneVertices decoder = new GtnhSceneVertices();
    /** Translation and nonuniform scale applied by an entity model part. */
    private final MutableMat4 pose =
            new MutableMat4().setTransform(new MutableVec3(1, 2, 3), new MutableQuat(), new MutableVec3(1, 1, 0.5f));
    /** Raw packed vanilla vertex buffer. */
    private int[] data;

    /** Fills one representative tessellator layer outside the measured invocation. */
    @Setup
    public void setup() {
        data = new int[VERTICES * 8];
        for (int i = 0; i < VERTICES; i++) {
            int offset = i * 8;
            data[offset] = Float.floatToRawIntBits(i & 1);
            data[offset + 1] = Float.floatToRawIntBits(i % 4 / 2);
            data[offset + 2] = Float.floatToRawIntBits(i / 4f);
            data[offset + 3] = Float.floatToRawIntBits(0.2f);
            data[offset + 4] = Float.floatToRawIntBits(0.5f);
            data[offset + 5] = -1;
            data[offset + 6] = 127;
        }
    }

    /** @return indexed mesh consumed by the scene renderer */
    @Benchmark
    public Mesh3D decodeLayer() {
        var out = new LegacySceneQuads();
        decoder.append(out, data, VERTICES, pose, true, true, true, -1);
        return out.build();
    }
}
