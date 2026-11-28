package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.Mesh3D;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/** Measures CPU capture from a vanilla quad stream through indexed mesh construction. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class LegacySceneGeometryBenchmark {
    /** Simulates small item or larger equipped entity geometry. */
    @Param({"32", "256"})
    public int quadCount;

    /** Reusable vertex consumer for one captured layer. */
    private final SceneBlockVertexConsumer vertices = new SceneBlockVertexConsumer();

    /**
     * Captures representative textured quads and builds the indexed surface used by the shadow and color passes.
     *
     * @return completed mesh
     */
    @Benchmark
    public Mesh3D capture() {
        vertices.clear();
        for (int i = 0; i < quadCount; i++) {
            vertex(0, 0, i);
            vertex(1, 0, i);
            vertex(1, 1, i);
            vertex(0, 1, i);
        }
        return vertices.build();
    }

    /**
     * Emits one vertex of the layer.
     *
     * @param x local X in blocks and horizontal UV
     * @param y local Y in blocks and vertical UV
     * @param z quad depth in blocks
     */
    private void vertex(float x, float y, float z) {
        vertices.position(x, y, z);
        vertices.rgba(255, 255, 255, 255);
        vertices.tex(x, y);
        vertices.direction(0, 0, 1);
    }
}
