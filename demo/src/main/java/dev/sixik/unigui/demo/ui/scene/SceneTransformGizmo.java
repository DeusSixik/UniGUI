package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.widgets.rendering.Scene3D;

/**
 * Рисует ручки трансформации поверх вьюпорта и применяет жесты к выбранному корневому узлу.
 * Перенос и вращение используют мировые оси, масштабирование использует локальные оси объекта.
 */
final class SceneTransformGizmo {
    /**
     * Способы преобразования выбранного узла.
     */
    enum Mode {
        /**
         * Перенос вдоль мировой оси.
         */
        MOVE,
        /**
         * Вращение вокруг мировой оси.
         */
        ROTATE,
        /**
         * Масштабирование вдоль локальной оси.
         */
        SCALE
    }

    /**
     * Базовый радиус гизмо в единицах UI.
     */
    private static final float RADIUS = 62;
    /**
     * Допуск попадания в ручку в единицах UI.
     */
    private static final float HIT_RADIUS = 8;
    /**
     * Число отрезков, аппроксимирующих окружность.
     */
    private static final int RING_SEGMENTS = 40;
    /**
     * Единичные направления осей X, Y, Z.
     */
    private static final MutableVec3[] AXES = {
            new MutableVec3(1, 0, 0), new MutableVec3(0, 1, 0), new MutableVec3(0, 0, 1)
    };
    /**
     * Цвет ручки для каждой оси.
     */
    private static final MutableColor[] COLORS = {
            MutableColor.fromHex("#FF646B"), MutableColor.fromHex("#6CE696"), MutableColor.fromHex("#68ACFF")
    };
    /**
     * Цвет наведённой или захваченной ручки.
     */
    private static final MutableColor ACTIVE = MutableColor.fromHex("#FFF0C0");
    /**
     * Цвет контура стрелок и отметки центра.
     */
    private static final MutableColor OUTLINE = MutableColor.fromHex("#151D2B");

    /**
     * Вьюпорт, чья камера задаёт проекцию.
     */
    private final Scene3D viewport;
    /**
     * Точки ручек [ось][вершина] в координатах холста, с глубиной или NaN в Z.
     */
    private final MutableVec3[][] handles = new MutableVec3[AXES.length][RING_SEGMENTS + 1];
    /**
     * Мировая точка опоры выбранного узла, в блоках.
     */
    private final MutableVec3 pivot = new MutableVec3();
    /**
     * Точка опоры на холсте, с глубиной в Z.
     */
    private final MutableVec3 center = new MutableVec3();
    /**
     * Рабочая точка в мировых координатах, в блоках.
     */
    private final MutableVec3 point = new MutableVec3();
    /**
     * Рабочая ориентация узла для расчёта локальных осей.
     */
    private final MutableQuat localAxes = new MutableQuat();
    /**
     * Выбранный корневой узел.
     */
    private SceneNode selected;
    /**
     * Тип ручек, показываемых для узла.
     */
    private Mode mode = Mode.MOVE;
    /**
     * Версии сцены и камеры, из которых рассчитана геометрия.
     */
    private long sceneRevision = -1, cameraRevision = -1;
    /**
     * Видна ли точка опоры между ближней и дальней плоскостями.
     */
    private boolean visible;
    /**
     * Базовый радиус гизмо в блоках при текущей глубине.
     */
    private float worldRadius;
    /**
     * Индекс оси под указателем, либо −1.
     */
    private int hovered = -1;
    /**
     * Параметры начатого преобразования или null.
     */
    private Drag drag;
    /**
     * Рабочий кватернион для приращения и результата вращения.
     */
    private final MutableQuat rotation = new MutableQuat();

    /**
     * Подключает гизмо к камере и запросам перерисовки сцены.
     *
     * @param viewport сцена, поверх которой рисуются ручки.
     */
    SceneTransformGizmo(Scene3D viewport) {
        this.viewport = viewport;
        for (MutableVec3[] handle : handles) {
            for (int i = 0; i < handle.length; i++) handle[i] = new MutableVec3();
        }
    }

    /**
     * Переносит гизмо на другой объект.
     *
     * @param node выбранный узел, принадлежащий корню сцены.
     */
    void select(SceneNode node) {
        selected = node;
        invalidateGeometry();
    }

    /**
     * Меняет вид ручек и операцию перетаскивания.
     *
     * @param mode новый режим.
     */
    void mode(Mode mode) {
        this.mode = mode;
        invalidateGeometry();
    }

    /**
     * Сбрасывает кэш проекции после смены объекта или инструмента.
     */
    private void invalidateGeometry() {
        sceneRevision = -1;
        hovered = -1;
        viewport.requestRender();
    }

    /**
     * Снимает подсветку, когда указатель покидает сцену.
     */
    void clearHover() {
        if (hovered == -1) return;
        hovered = -1;
        viewport.requestRender();
    }

    /**
     * Подсвечивает ближайшую ручку.
     *
     * @param x координата указателя X в единицах холста.
     * @param y координата указателя Y в единицах холста.
     */
    void hover(float x, float y) {
        int segment = hit(x, y);
        int axis = segment < 0 ? -1 : segment / RING_SEGMENTS;
        if (hovered == axis) return;
        hovered = axis;
        viewport.requestRender();
    }

    /**
     * Захватывает начальную позу объекта и направление экранного жеста.
     *
     * @param x координата нажатия X в единицах холста.
     * @param y координата нажатия Y в единицах холста.
     * @return true при попадании в доступную ручку.
     */
    boolean begin(float x, float y) {
        int hit = hit(x, y);
        if (hit < 0) return false;
        int axis = hit / RING_SEGMENTS;
        int segment = hit % RING_SEGMENTS;
        MutableVec3 start = handles[axis][segment], end = handles[axis][segment + 1];
        float dx = end.x() - start.x(), dy = end.y() - start.y();
        float length = (float) Math.hypot(dx, dy);
        float sensitivity = mode == Mode.ROTATE ? 1 / RADIUS : 1 / length;
        if (mode == Mode.MOVE) sensitivity *= worldRadius;
        drag = new Drag(axis, x, y, dx / length * sensitivity, dy / length * sensitivity,
                selected.position(new MutableVec3()), selected.scale(new MutableVec3()),
                selected.rotation(new MutableQuat()));
        viewport.requestRender();
        return true;
    }

    /**
     * Вычисляет преобразование относительно позы на момент нажатия.
     * {@code delta = (pointer - start) · gradient}. Для переноса gradient равен
     * единичному направлению экранной оси, умноженному на отношение её длины в блоках
     * к длине на холсте. Для масштаба делитель задаёт относительное изменение,
     * {@code scale = initialScale * (1 + delta)} по выбранной оси. Для вращения
     * gradient направлен по касательной к кольцу, а delta задаёт радианы
     * для {@code rotation = axisAngle(axis, delta) * initialRotation}.
     *
     * @param x текущая координата указателя X в единицах холста.
     * @param y текущая координата указателя Y в единицах холста.
     */
    void drag(float x, float y) {
        if (drag == null) return;
        float delta = (x - drag.x) * drag.gradientX + (y - drag.y) * drag.gradientY;
        if (!Float.isFinite(delta)) return;
        MutableVec3 axis = AXES[drag.axis];
        switch (mode) {
            case MOVE -> selected.setPosition(drag.position.x() + axis.x() * delta,
                    drag.position.y() + axis.y() * delta, drag.position.z() + axis.z() * delta);
            case ROTATE -> {
                rotation.setAxisAngle(axis, delta);
                selected.setRotation(rotation.setProduct(rotation, drag.rotation));
            }
            case SCALE -> {
                float sx = drag.scale.x(), sy = drag.scale.y(), sz = drag.scale.z();
                float factor = 1 + delta;
                switch (drag.axis) {
                    case 0 -> sx = clampScale(sx * factor);
                    case 1 -> sy = clampScale(sy * factor);
                    case 2 -> sz = clampScale(sz * factor);
                }
                selected.setScale(sx, sy, sz);
            }
        }
    }

    /**
     * Ограничивает масштаб, не допуская вырожденных или чрезмерно больших объектов.
     */
    private static float clampScale(float value) {
        return Math.max(0.15f, Math.min(6.0f, value));
    }

    /**
     * Освобождает активную ручку после отпускания указателя.
     */
    void end() {
        if (drag == null) return;
        drag = null;
        hovered = -1;
        viewport.requestRender();
    }

    /**
     * Рисует проекцию осей или колец поверх изображения сцены.
     *
     * @param context контекст вывода в логических координатах холста.
     */
    void paint(RenderContext context) {
        if (!prepareGeometry()) return;
        int active = drag == null ? hovered : drag.axis;
        for (int axis = 0; axis < handles.length; axis++) {
            MutableColor color = axis == active ? ACTIVE : COLORS[axis];
            int count = mode == Mode.ROTATE ? RING_SEGMENTS : 1;
            for (int segment = 0; segment < count; segment++) {
                MutableVec3 a = handles[axis][segment], b = handles[axis][segment + 1];
                if (!drawable(a, b)) continue;
                if (mode == Mode.ROTATE) {
                    context.addLine(a.x(), a.y(), b.x(), b.y(), color, axis == active ? 3.5f : 2);
                } else {
                    paintAxis(context, a, b, color);
                }
            }
        }
        context.addCircleFilled(center.x(), center.y(), 4, OUTLINE, 16);
        context.addCircle(center.x(), center.y(), 3, ACTIVE, 16, 1);
    }

    /**
     * Рисует ось и наконечник её ручки.
     *
     * @param context контекст вывода.
     * @param a       начало оси на холсте.
     * @param b       конец оси на холсте.
     * @param color   цвет оси и наконечника.
     */
    private void paintAxis(RenderContext context, MutableVec3 a, MutableVec3 b, MutableColor color) {
        float x = b.x(), y = b.y();
        context.addLine(a.x(), a.y(), x, y, OUTLINE, 5);
        context.addLine(a.x(), a.y(), x, y, color, 2.5f);
        if (mode == Mode.SCALE) {
            context.addRectFilled(x - 5, y - 5, 10, 10, OUTLINE);
            context.addRectFilled(x - 4, y - 4, 8, 8, color);
        } else {
            float dx = x - a.x(), dy = y - a.y();
            float length = (float) Math.hypot(dx, dy);
            dx /= length;
            dy /= length;
            float baseX = x - 11 * dx, baseY = y - 11 * dy;
            context.addTriangleFilled(new DrawPoint(x, y), new DrawPoint(baseX - 5 * dy, baseY + 5 * dx),
                    new DrawPoint(baseX + 5 * dy, baseY - 5 * dx), color);
        }
    }

    /**
     * Ищет ближайший отрезок. При проекции указателя p на отрезок ab параметр
     * {@code t = clamp((p - a) · (b - a) / |b - a|², 0, 1)} задаёт ближайшую точку.
     *
     * @param x координата указателя X в единицах холста.
     * @param y координата указателя Y в единицах холста.
     * @return индекс оси, умноженный на RING_SEGMENTS, плюс индекс отрезка, либо −1.
     */
    private int hit(float x, float y) {
        if (!prepareGeometry()) return -1;
        int result = -1;
        float best = HIT_RADIUS * HIT_RADIUS;
        for (int axis = 0; axis < handles.length; axis++) {
            int count = mode == Mode.ROTATE ? RING_SEGMENTS : 1;
            for (int segment = 0; segment < count; segment++) {
                MutableVec3 a = handles[axis][segment], b = handles[axis][segment + 1];
                if (!drawable(a, b)) continue;
                float dx = b.x() - a.x(), dy = b.y() - a.y();
                float lengthSquared = dx * dx + dy * dy;
                float start = mode == Mode.ROTATE ? 0 : 12 / (float) Math.sqrt(lengthSquared);
                float t = Math.max(start, Math.min(1, ((x - a.x()) * dx + (y - a.y()) * dy) / lengthSquared));
                float ox = a.x() + t * dx - x, oy = a.y() + t * dy - y;
                float distance = ox * ox + oy * oy;
                if (distance >= best) continue;
                best = distance;
                result = axis * RING_SEGMENTS + segment;
            }
        }
        return result;
    }

    /**
     * Обновляет проекцию при изменении камеры или сцены. Базовый радиус в блоках равен
     * расстоянию от точки опоры до точки на той же глубине, смещённой на RADIUS по экрану.
     * Точки кольца расположены по {@code pivot + r * (cos(angle) * u + sin(angle) * v)},
     * где u и v — две оси, перпендикулярные оси вращения, в порядке правой тройки.
     *
     * @return true, если точка опоры находится в объёме проекции.
     */
    private boolean prepareGeometry() {
        if (selected == null) return false;
        Camera3D camera = viewport.camera();
        long nextScene = viewport.scene().revision(), nextCamera = camera.revision();
        if (nextScene == sceneRevision && nextCamera == cameraRevision) return visible;
        sceneRevision = nextScene;
        cameraRevision = nextCamera;
        selected.position(pivot);
        visible = project(camera, pivot, center)
                && camera.unproject(center.x() + RADIUS, center.y(), center.z(), point);
        if (!visible) return false;
        float x = point.x() - pivot.x(), y = point.y() - pivot.y(), z = point.z() - pivot.z();
        worldRadius = (float) Math.sqrt(x * x + y * y + z * z);
        selected.rotation(localAxes);
        for (int axis = 0; axis < handles.length; axis++) {
            MutableVec3 unit = AXES[axis];
            MutableVec3[] handle = handles[axis];
            if (mode == Mode.ROTATE) {
                MutableVec3 u = AXES[(axis + 1) % 3], v = AXES[(axis + 2) % 3];
                float radius = worldRadius * (1 + 0.12f * axis);
                for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                    double angle = 2 * Math.PI * segment / RING_SEGMENTS;
                    float c = radius * (float) Math.cos(angle), s = radius * (float) Math.sin(angle);
                    point.set(pivot).add(c * u.x() + s * v.x(), c * u.y() + s * v.y(), c * u.z() + s * v.z());
                    project(camera, point, handle[segment]);
                }
                handle[RING_SEGMENTS].set(handle[0]);
            } else {
                handle[0].set(center);
                point.set(unit);
                if (mode == Mode.SCALE) localAxes.rotate(unit.x(), unit.y(), unit.z(), point);
                point.set(pivot.x() + worldRadius * point.x(), pivot.y() + worldRadius * point.y(),
                        pivot.z() + worldRadius * point.z());
                project(camera, point, handle[1]);
            }
        }
        return true;
    }

    /**
     * Проецирует точку, отмечая недоступную глубину значением NaN.
     *
     * @param camera камера сцены.
     * @param world  точка в блоках.
     * @param out    приёмник координат холста и глубины.
     * @return true между плоскостями отсечения.
     */
    private static boolean project(Camera3D camera, MutableVec3 world, MutableVec3 out) {
        if (camera.project(world, out) && out.z() >= 0 && out.z() <= 1) return true;
        out.set(0, 0, Float.NaN);
        return false;
    }

    /**
     * Отбрасывает вырожденные отрезки и слишком короткие экранные проекции линейных осей.
     *
     * @param a начало отрезка на холсте.
     * @param b конец отрезка на холсте.
     * @return true для доступного отрезка достаточной длины.
     */
    private boolean drawable(MutableVec3 a, MutableVec3 b) {
        if (!Float.isFinite(a.z()) || !Float.isFinite(b.z())) return false;
        float dx = b.x() - a.x(), dy = b.y() - a.y();
        return dx * dx + dy * dy > (mode == Mode.ROTATE ? 0.25f : 20 * 20);
    }

    /**
     * Снимок преобразования на момент нажатия.
     *
     * @param axis      индекс оси от 0 до 2.
     * @param x         начальная координата указателя X в единицах холста.
     * @param y         начальная координата указателя Y в единицах холста.
     * @param gradientX перевод горизонтального смещения в величину преобразования.
     * @param gradientY перевод вертикального смещения в величину преобразования.
     * @param position  начальная мировая позиция в блоках.
     * @param scale     начальный относительный масштаб.
     * @param rotation  начальная ориентация.
     */
    private record Drag(int axis, float x, float y, float gradientX, float gradientY,
                        MutableVec3 position, MutableVec3 scale, MutableQuat rotation) {
    }
}
