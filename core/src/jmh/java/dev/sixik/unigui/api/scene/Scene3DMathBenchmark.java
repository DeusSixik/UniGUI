package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/** Измеряет композицию матриц и построение луча с пересечением AABB. Для аллокаций запускается с {@code -prof gc}. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class Scene3DMathBenchmark {
    /** Левый множитель, пересчитываемый до начала замера. */
    private MutableMat4 viewProjection;
    /** Правый множитель с переносом, поворотом и неоднородным масштабом. */
    private MutableMat4 transform;
    /** Переиспользуемый результат умножения. */
    private MutableMat4 product;
    /** Перспективная камера с viewport 640×360 логических единиц UI. */
    private Camera3D camera;
    /** Переиспользуемый луч в мировых координатах. */
    private Ray ray;
    /** Нижняя вершина проверяемого AABB в единицах сцены. */
    private MutableVec3 min;
    /** Верхняя вершина проверяемого AABB в единицах сцены. */
    private MutableVec3 max;

    /** Создаёт входы и рабочие объекты до измерения. */
    @Setup
    public void setup() {
        camera = new Camera3D().setViewport(640, 360)
                .setPose(0, 1, 6, new MutableQuat().setYawPitch(0.1, -0.2));
        viewProjection = camera.viewProjection(new MutableMat4());
        transform = new MutableMat4().setTransform(new MutableVec3(4, 3, 2),
                new MutableQuat().setYawPitch(0.4, 0.6), new MutableVec3(2, 1, 3));
        product = new MutableMat4();
        ray = new Ray();
        min = new MutableVec3(-2, -2, -1);
        max = new MutableVec3(2, 2, 1);
    }

    /**
     * Компонирует матрицу объекта и матрицу вида-проекции.
     * @return переиспользуемая матрица с полным результатом.
     */
    @Benchmark
    public MutableMat4 multiply() {
        return product.setProduct(viewProjection, transform);
    }

    /**
     * Строит экранный луч и вычисляет расстояние до AABB.
     * @return расстояние в единицах сцены.
     */
    @Benchmark
    public double pickAabb() {
        if (!camera.screenRay(310, 188, ray)) return Double.POSITIVE_INFINITY;
        return ray.intersectAabb(min, max);
    }
}
