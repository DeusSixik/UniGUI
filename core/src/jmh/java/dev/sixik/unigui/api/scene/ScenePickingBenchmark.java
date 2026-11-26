package dev.sixik.unigui.api.scene;

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

/** Измеряет выбор по экранному лучу в сцене с 64 мешами и переиспользуемым picker. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ScenePickingBenchmark {
    /** Сцена с 64 узлами и общим мешем. */
    private Scene scene;
    /** Перспективная камера с viewport 640×480 логических единиц. */
    private Camera3D camera;
    /** Переиспользуемый набор рабочих объектов для тестов луча. */
    private ScenePicker picker;

    /** Создаёт граф с попаданием в центре экрана и прогревает преобразования. */
    @Setup
    public void setup() {
        scene = new Scene();
        camera = new Camera3D().setViewport(640, 480).setPosition(0, 0, 8);
        Mesh3D quad = new Mesh3D(new float[]{-1, -1, 0, 1, -1, 0, 1, 1, 0, -1, 1, 0},
                null, null, null, new int[]{0, 1, 2, 0, 2, 3});
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                scene.addRoot(new SceneNode().setMesh(quad).setPosition(3 * (x - 4), 3 * (y - 4), 0));
            }
        }
        picker = new ScenePicker();
        if (pickCenter() == null) throw new IllegalStateException("Expected center hit");
    }

    /**
     * Ищет ближайший меш по лучу в центре экрана.
     * @return выбранный узел.
     */
    @Benchmark
    public SceneNode pickCenter() {
        return picker.pick(scene, camera, 320, 240);
    }
}
