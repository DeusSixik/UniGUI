package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.Material;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/** Измеряет подготовку uniforms для одного видимого меша в переиспользуемом буфере. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SceneMeshDataBenchmark {
    /** Число экземпляров в моделируемой группе GPU. */
    private static final int INSTANCES = 32;
    /** Матрица преобразования из локальных координат в мир. */
    private MutableMat4 world;
    /** Матрица из локальных координат в clip space. */
    private MutableMat4 clip;
    /** Поверхность с освещением. */
    private Material material;
    /** Материал без динамического освещения, используемый для сравнения вызовов. */
    private Material blockMaterial;
    /** Мировой направленный источник света. */
    private DirectionalLight light;
    /** Целевой блок std140, выделяемый до замеров. */
    private ByteBuffer uniforms;
    /** Преобразования различных экземпляров одного меша. */
    private MutableMat4[] worlds;
    /** Clip-матрицы экземпляров, предварительно вычисленные одинаково для обоих вариантов. */
    private MutableMat4[] clips;
    /** Общий переиспользуемый выход с местом для отдельных uniform-блоков. */
    private ByteBuffer batch;

    /** Готовит матрицы и данные одного меша. */
    @Setup
    public void setup() {
        world = new MutableMat4().setTransform(new MutableVec3(2, 3, -8),
                new MutableQuat().setYawPitch(0.4, 0.7), new MutableVec3(1, 2, -3));
        MutableMat4 projection = new MutableMat4().setPerspective(1.0, 1.6, 0.1, 100);
        clip = new MutableMat4().setProduct(projection, world);
        material = new Material(0xFF3296E8, null, Material.Shading.LIT,
                Material.AlphaMode.OPAQUE, 0.5f, Material.Culling.BACK);
        blockMaterial = Material.DEFAULT;
        light = new DirectionalLight(1, -1, -2, 0xFFEAD0, 0.8f);
        uniforms = ByteBuffer.allocateDirect(SceneMeshData.UNIFORM_BYTES).order(ByteOrder.nativeOrder());
        worlds = new MutableMat4[INSTANCES];
        clips = new MutableMat4[INSTANCES];
        MutableQuat rotation = new MutableQuat().setYawPitch(0.4, 0.7);
        MutableVec3 scale = new MutableVec3(1, 2, -3);
        for (int i = 0; i < INSTANCES; i++) {
            worlds[i] = new MutableMat4().setTransform(new MutableVec3(i, 3, -8), rotation, scale);
            clips[i] = new MutableMat4().setProduct(projection, worlds[i]);
        }
        batch = ByteBuffer.allocateDirect(INSTANCES * SceneMeshData.UNIFORM_BYTES).order(ByteOrder.nativeOrder());
    }

    /**
     * Записывает блок, включая нормали, свет и режимы поверхности.
     * @return переиспользуемый буфер с результатом.
     */
    @Benchmark
    public ByteBuffer writeUniform() {
        uniforms.clear();
        SceneMeshData.writeUniform(uniforms, clip, world, material, AmbientLight.WHITE, light, 1, 1,
                true, false, false);
        return uniforms;
    }

    /**
     * Сериализует данные отдельных вызовов с одним материалом.
     * @return переиспользуемый буфер с результатом.
     */
    @Benchmark
    public ByteBuffer writeIndividualDraws() {
        batch.clear();
        for (int i = 0; i < INSTANCES; i++) {
            SceneMeshData.writeUniform(batch, clips[i], worlds[i], blockMaterial, AmbientLight.WHITE, light, 1, 1,
                    true, false, false);
        }
        return batch;
    }

    /**
     * Сериализует те же данные для одного инстансированного вызова.
     * @return переиспользуемый буфер с результатом.
     */
    @Benchmark
    public ByteBuffer writeInstancedDraw() {
        batch.clear();
        SceneMeshData.writeUniform(batch, clips[0], worlds[0], blockMaterial, AmbientLight.WHITE, light, 1, 1,
                true, false, false);
        for (int i = 0; i < INSTANCES; i++) SceneMeshData.writeClipMatrix(batch, clips[i]);
        return batch;
    }
}
