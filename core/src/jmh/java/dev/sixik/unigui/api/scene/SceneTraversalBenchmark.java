package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
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
import java.util.function.Consumer;

/** Измеряет обход и отсечение сцены со 256 мешами, в том числе после перемещения общего родителя. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SceneTraversalBenchmark {
    /** Сцена из одного общего корня, 16 групп и 256 узлов меша. */
    private Scene scene;
    /** Общий родитель всех групп. */
    private SceneNode root;
    /** Фиксированная пирамида видимости. */
    private Frustum3D frustum;
    /** Переиспользуемый счётчик видимых мешей. */
    private Consumer<SceneNode> countMesh;
    /** Число прошедших отсечение мешей в текущем обходе. */
    private int visibleMeshes;
    /** Смещение общего родителя по X в единицах сцены, 0 или 1. */
    private float offset;

    /** Создаёт дерево и прогревает мировые матрицы видимых узлов. */
    @Setup
    public void setup() {
        scene = new Scene();
        root = new SceneNode();
        scene.addRoot(root);
        Mesh3D mesh = new Mesh3D(new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0}, null, null, null, new int[]{0, 1, 2});
        for (int groupIndex = 0; groupIndex < 16; groupIndex++) {
            SceneNode group = new SceneNode().setPosition(0, groupIndex - 7, -10);
            group.setVisible(groupIndex % 4 != 0);
            for (int i = 0; i < 16; i++) {
                group.addChild(new SceneNode().setMesh(mesh).setPosition(2 * (i - 8), 0, 0));
            }
            root.addChild(group);
        }
        Camera3D camera = new Camera3D().setViewport(640, 480);
        frustum = new Frustum3D().set(camera.viewProjection(new MutableMat4()));
        countMesh = node -> {
            if (node.mesh() != null) visibleMeshes++;
        };
        cullCachedTransforms();
    }

    /**
     * Обходит видимую часть графа и читает закэшированные мировые матрицы.
     * @return число видимых мешей.
     */
    @Benchmark
    public int cullCachedTransforms() {
        visibleMeshes = 0;
        scene.visitVisible(frustum, countMesh);
        return visibleMeshes;
    }

    /**
     * Двигает общий корень и пересчитывает мировые матрицы по мере обхода.
     * @return число видимых мешей после движения.
     */
    @Benchmark
    public int moveRootAndCull() {
        offset = 1.0f - offset;
        root.setPosition(offset, 0, 0);
        return cullCachedTransforms();
    }
}
