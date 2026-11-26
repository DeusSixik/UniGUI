package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;

import java.util.function.Consumer;

/**
 * Переиспользуемый поиск ближайшего узла вдоль экранного луча в UI-потоке.
 */
public final class ScenePicker {
    /**
     * Обходчик кандидатов, создаётся один раз.
     */
    private final Consumer<SceneNode> visit = this::testNode;
    /**
     * Мировой луч, начинающийся на ближней плоскости.
     */
    private final Ray worldRay = new Ray();
    /**
     * Луч в координатах проверяемого узла.
     */
    private final Ray localRay = new Ray();
    /**
     * Мировое начало луча.
     */
    private final MutableVec3 origin = new MutableVec3();
    /**
     * Единичное мировое направление луча.
     */
    private final MutableVec3 direction = new MutableVec3();
    /**
     * Точка на дальней плоскости, затем рабочая точка при переходе в локальные координаты.
     */
    private final MutableVec3 point = new MutableVec3();
    /**
     * Нижний угол выбранной ячейки кандидата или NaN для попадания в обычный узел.
     */
    private final MutableVec3 candidateCell = new MutableVec3();
    /**
     * Нижний угол ячейки победителя или NaN для попадания в обычный узел.
     */
    private final MutableVec3 hitCell = new MutableVec3();
    /**
     * Рабочие координаты первой вершины треугольника.
     */
    private final MutableVec3 a = new MutableVec3();
    /**
     * Рабочие координаты второй вершины треугольника.
     */
    private final MutableVec3 b = new MutableVec3();
    /**
     * Рабочие координаты третьей вершины треугольника.
     */
    private final MutableVec3 c = new MutableVec3();
    /**
     * Преобразование кандидата в мировую систему.
     */
    private final MutableMat4 world = new MutableMat4();
    /**
     * Преобразование мировых координат в локальную систему кандидата.
     */
    private final MutableMat4 inverse = new MutableMat4();
    /**
     * Ближайшее найденное расстояние в мировых единицах, до первого попадания — дальняя плоскость.
     */
    private double closest;
    /**
     * Найденный узел в пределах текущего обхода или null.
     */
    private SceneNode hit;

    /**
     * Создаёт рабочие объекты для последовательных вызовов поиска.
     */
    public ScenePicker() {
    }

    /**
     * Ищет ближайшее попадание между плоскостями отсечения. Меш проверяется по треугольникам
     * с обеих сторон после теста границ. Для узла без меша проверяется его локальный AABB,
     * если мировое преобразование обратимо.
     *
     * @param scene  граф с иерархией и видимостью узлов.
     * @param camera камера с viewport холста.
     * @param x      горизонтальная координата холста в единицах UI.
     * @param y      вертикальная координата холста в единицах UI.
     * @return ближайший узел или null при промахе.
     */
    public SceneNode pick(Scene scene, Camera3D camera, float x, float y) {
        return pick(scene, camera, x, y, null);
    }

    /**
     * Ищет узел и при попадании в единичную ячейку копирует её локальный нижний угол.
     *
     * @param scene  граф сцены.
     * @param camera камера с viewport холста.
     * @param x      горизонтальная координата холста.
     * @param y      вертикальная координата холста.
     * @param cell   выход для ячейки или null. При отсутствии ячейки заполняется NaN.
     * @return ближайший узел или null.
     */
    public SceneNode pick(Scene scene, Camera3D camera, float x, float y, MutableVec3 cell) {
        if (cell != null) cell.set(Float.NaN, Float.NaN, Float.NaN);
        if (!camera.screenRay(x, y, worldRay) || !camera.unproject(x, y, 1.0, point)) return null;
        worldRay.origin(origin);
        worldRay.direction(direction);
        double dx = (double) point.x() - origin.x();
        double dy = (double) point.y() - origin.y();
        double dz = (double) point.z() - origin.z();
        closest = Math.sqrt(dx * dx + dy * dy + dz * dz);
        try {
            scene.visitVisible(null, visit);
            if (cell != null && hit != null) cell.set(hitCell);
            return hit;
        } finally {
            hit = null;
        }
    }

    /**
     * Переводит луч в координаты узла и сравнивает расстояния в общей мировой системе.
     * Вектор локального луча до нормализации равен {@code inverse * (worldDirection, 0)}.
     * Деление локального расстояния на длину этого вектора отменяет нормализацию.
     *
     * @param node видимый кандидат.
     */
    private void testNode(SceneNode node) {
        Aabb bounds = node.bounds();
        if (bounds == null) return;
        Mesh3D mesh = node.mesh();
        candidateCell.set(Float.NaN, Float.NaN, Float.NaN);
        node.worldMatrix(world);
        if (!inverse.setInverse(world)) {
            if (mesh != null) accept(node, intersectTriangles(mesh, worldRay, world));
            return;
        }

        double dx = inverse.transformComponent(0, direction.x(), direction.y(), direction.z(), 0);
        double dy = inverse.transformComponent(1, direction.x(), direction.y(), direction.z(), 0);
        double dz = inverse.transformComponent(2, direction.x(), direction.y(), direction.z(), 0);
        double directionLength = Math.hypot(Math.hypot(dx, dy), dz);
        if (!(directionLength > 0.0) || !Double.isFinite(directionLength)
                || !inverse.transformPoint(origin, point) || !localRay.set(point, dx, dy, dz)) {
            return;
        }
        double limit = closest * directionLength;
        double boxDistance = localRay.intersectAabb(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ());
        if (!Double.isFinite(boxDistance) || boxDistance > limit) return;
        double distance = node.intersectPickBounds(localRay, boxDistance, limit, candidateCell);
        if (mesh != null) {
            double triangle = intersectTriangles(mesh, localRay, null);
            if (triangle < distance) {
                distance = triangle;
                candidateCell.set(Float.NaN, Float.NaN, Float.NaN);
            }
        }
        accept(node, distance / directionLength);
    }

    /**
     * Находит ближайший треугольник индексированного меша.
     *
     * @param mesh      геометрия в локальных координатах.
     * @param ray       луч в системе координат тестируемых вершин.
     * @param transform преобразование вершин мира для вырожденной матрицы узла или null для локального теста.
     * @return ближайшее расстояние вдоль ray или +∞ при промахе.
     */
    private double intersectTriangles(Mesh3D mesh, Ray ray, MutableMat4 transform) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0, size = mesh.indexCount(); i < size; i += 3) {
            vertex(mesh, mesh.index(i), a);
            vertex(mesh, mesh.index(i + 1), b);
            vertex(mesh, mesh.index(i + 2), c);
            if (transform != null && (!transform.transformPoint(a, a) || !transform.transformPoint(b, b)
                    || !transform.transformPoint(c, c))) {
                continue;
            }
            best = Math.min(best, ray.intersectTriangle(a, b, c));
        }
        return best;
    }

    /**
     * Запоминает ближайшее попадание, сохраняя порядок обхода при одинаковых расстояниях.
     *
     * @param node     проверяемый узел.
     * @param distance расстояние в мировых единицах сцены.
     */
    private void accept(SceneNode node, double distance) {
        if (distance <= closest && (hit == null || distance < closest)) {
            closest = distance;
            hit = node;
            hitCell.set(candidateCell);
        }
    }

    /**
     * Читает позицию вершины в переиспользуемый буфер.
     *
     * @param mesh  источник позиции.
     * @param index индекс вершины.
     * @param out   выход в локальных единицах меша.
     */
    private static void vertex(Mesh3D mesh, int index, MutableVec3 out) {
        out.set(mesh.position(index, 0), mesh.position(index, 1), mesh.position(index, 2));
    }
}
