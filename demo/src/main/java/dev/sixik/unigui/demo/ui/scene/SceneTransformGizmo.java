package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.widgets.rendering.Scene3D;

/**
 * Draws transform handles in the viewport and applies edits in the selected coordinate space.
 */
public final class SceneTransformGizmo {
    /**
     * Способы преобразования выбранного узла.
     */
    public enum Mode {
        /**
         * Translation along a handle axis.
         */
        MOVE,
        /**
         * Rotation about a handle axis.
         */
        ROTATE,
        /**
         * Scaling along a handle axis.
         */
        SCALE
    }

    /** Coordinate frame used by the transform handles. */
    public enum Space {
        /** Axes follow the object's accumulated rotation. */
        LOCAL,
        /** Axes are aligned with the scene. */
        GLOBAL
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
    /** Applies the block showcase's original absolute limits when dragging a local scale handle. */
    private final boolean limitLocalScale;
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
     * Accumulated orientation of the selected node, independent of scale.
     */
    private final MutableQuat localAxes = new MutableQuat();
    /** Accumulated orientation of the parent in world space. */
    private final MutableQuat parentAxes = new MutableQuat();
    /** Scratch quaternion for accumulating parent orientations. */
    private final MutableQuat ancestorRotation = new MutableQuat();
    /** Unit handle axes expressed in world coordinates. */
    private final MutableVec3[] worldAxes = {new MutableVec3(), new MutableVec3(), new MutableVec3()};
    /**
     * Матрица родителя для преобразования точки опоры в мировые координаты.
     */
    private final MutableMat4 parentWorld = new MutableMat4();
    /**
     * Выбранный узел.
     */
    private SceneNode selected;
    /**
     * Тип ручек, показываемых для узла.
     */
    private Mode mode = Mode.MOVE;
    /** Coordinate frame of the current handles. */
    private Space space = Space.GLOBAL;
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
    public SceneTransformGizmo(Scene3D viewport) {
        this(viewport, false);
    }

    /**
     * Creates handles with optional absolute limits for the standalone block showcase.
     * @param viewport scene to which the handles are attached
     * @param limitLocalScale whether to constrain the edited local scale to [0.15, 6]
     */
    SceneTransformGizmo(Scene3D viewport, boolean limitLocalScale) {
        this.viewport = viewport;
        this.limitLocalScale = limitLocalScale;
        for (MutableVec3[] handle : handles) {
            for (int i = 0; i < handle.length; i++) handle[i] = new MutableVec3();
        }
    }

    /**
     * Переносит гизмо на другой объект.
     *
     * @param node выбранный узел или null для отключения ручек.
     */
    public void select(SceneNode node) {
        end();
        selected = node;
        invalidateGeometry();
    }

    /**
     * Меняет вид ручек и операцию перетаскивания.
     *
     * @param mode новый режим.
     */
    public void mode(Mode mode) {
        end();
        this.mode = mode;
        invalidateGeometry();
    }

    /**
     * Changes the axes shown by the gizmo.
     * @param space coordinate frame to use for the next gesture
     */
    public void space(Space space) {
        end();
        this.space = space;
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
    public void clearHover() {
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
    public void hover(float x, float y) {
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
    public boolean begin(float x, float y) {
        int hit = hit(x, y);
        if (hit < 0) return false;
        int axis = hit / RING_SEGMENTS;
        int segment = hit % RING_SEGMENTS;
        MutableVec3 start = handles[axis][segment], end = handles[axis][segment + 1];
        float dx = end.x() - start.x(), dy = end.y() - start.y();
        float length = (float) Math.hypot(dx, dy);
        float sensitivity = mode == Mode.ROTATE ? 1 / RADIUS : 1 / length;
        if (mode == Mode.MOVE) sensitivity *= worldRadius;
        MutableVec3 direction = new MutableVec3().set(worldAxes[axis]);
        SceneNode parent = selected.parent();
        if (mode == Mode.MOVE && parent != null) {
            parent.worldMatrix(parentWorld);
            MutableMat4 worldToParent = new MutableMat4();
            if (!worldToParent.setInverse(parentWorld)) return false;
            // A displacement uses w=0 so the inverse matrix's translation does not affect it.
            transformDirection(worldToParent, direction);
        } else if (mode == Mode.ROTATE && parent != null) {
            rotation.set(-parentAxes.x(), -parentAxes.y(), -parentAxes.z(), parentAxes.w());
            rotation.rotate(direction.x(), direction.y(), direction.z(), direction);
        }
        MutableQuat initialRotation = selected.rotation(new MutableQuat());
        MutableVec3 scaleWeights = mode == Mode.SCALE && space == Space.GLOBAL
                ? worldScaleWeights(axis, parent, initialRotation) : AXES[axis];
        drag = new Drag(axis, x, y, dx / length * sensitivity, dy / length * sensitivity,
                selected.position(new MutableVec3()), selected.scale(new MutableVec3()),
                initialRotation, direction, scaleWeights);
        viewport.requestRender();
        return true;
    }

    /**
     * Вычисляет преобразование относительно позы на момент нажатия.
     * {@code delta = (pointer - start) · gradient}. Для переноса gradient равен
     * единичному направлению экранной оси, умноженному на отношение её длины в блоках
     * к длине на холсте. Для масштаба делитель задаёт относительное изменение,
     * {@code scale_i = initialScale_i * sqrt(1 + (factor² - 1) * weight_i)}, где
     * {@code factor = 1 + delta}, а вес задаёт долю оси i вдоль направления ручки. Для вращения
     * gradient направлен по касательной к кольцу, а delta задаёт радианы
     * для {@code rotation = axisAngle(axis, delta) * initialRotation}.
     * Для вложенного узла перенос составляет {@code position = initialPosition + inverse(parentWorld) * (axis * delta, 0)}.
     * Последняя компонента равна нулю, потому что ось и величина переноса задают вектор, а не точку.
     *
     * @param x текущая координата указателя X в единицах холста.
     * @param y текущая координата указателя Y в единицах холста.
     */
    public void drag(float x, float y) {
        if (drag == null) return;
        float delta = (x - drag.x) * drag.gradientX + (y - drag.y) * drag.gradientY;
        if (!Float.isFinite(delta)) return;
        switch (mode) {
            case MOVE -> {
                float nextX = drag.position.x() + drag.direction.x() * delta;
                float nextY = drag.position.y() + drag.direction.y() * delta;
                float nextZ = drag.position.z() + drag.direction.z() * delta;
                if (Float.isFinite(nextX) && Float.isFinite(nextY) && Float.isFinite(nextZ)) {
                    selected.setPosition(nextX, nextY, nextZ);
                }
            }
            case ROTATE -> {
                if (delta == 0) selected.setRotation(drag.rotation);
                else selected.setRotation(rotation.setProduct(rotation.setAxisAngle(drag.direction, delta), drag.rotation));
            }
            case SCALE -> {
                double factor = 1.0 + delta;
                float sx = scaled(drag.scale.x(), drag.scaleWeights.x(), factor);
                float sy = scaled(drag.scale.y(), drag.scaleWeights.y(), factor);
                float sz = scaled(drag.scale.z(), drag.scaleWeights.z(), factor);
                if (Float.isFinite(sx) && Float.isFinite(sy) && Float.isFinite(sz)) selected.setScale(sx, sy, sz);
            }
        }
    }

    /**
     * Converts a directional stretch to the new length of a local basis vector.
     * @param initial signed local scale factor
     * @param weight squared projection of the unit basis vector onto the handle axis
     * @param factor requested linear stretch along the handle direction
     * @return scale factor after applying the stretch and any configured limits
     */
    private float scaled(float initial, float weight, double factor) {
        if (weight == 0) return initial;
        if (limitLocalScale && space == Space.LOCAL) return (float) Math.max(0.15, Math.min(6, initial * factor));
        factor = Math.max(0.15, factor);
        double stretch = (factor - 1) * (factor + 1);
        return (float) (initial * Math.sqrt(1 + stretch * weight));
    }

    /**
     * Measures how much each transformed local basis vector points along the world-axis handle.
     * The weight for vector b on world axis i is {@code b_i² / (b_x² + b_y² + b_z²)}.
     * The resulting scale preserves the stretched basis lengths when the transform can only store TRS.
     * @param axis world-axis index
     * @param parent selected node's parent, or null for a scene root
     * @param initialRotation selected node's local orientation
     * @return weights for the X, Y, Z scale factors
     */
    private MutableVec3 worldScaleWeights(int axis, SceneNode parent, MutableQuat initialRotation) {
        return new MutableVec3(scaleWeight(axis, AXES[0], parent, initialRotation),
                scaleWeight(axis, AXES[1], parent, initialRotation),
                scaleWeight(axis, AXES[2], parent, initialRotation));
    }

    /**
     * Computes one squared projection for world-axis scaling.
     * @param axis world-axis index
     * @param basis local unit basis vector
     * @param parent parent node, or null
     * @param initialRotation node orientation in parent space
     * @return a weight in [0, 1], or zero for a degenerate basis
     */
    private float scaleWeight(int axis, MutableVec3 basis, SceneNode parent, MutableQuat initialRotation) {
        initialRotation.rotate(basis.x(), basis.y(), basis.z(), point);
        if (parent != null) transformDirection(parentWorld, point);
        double x = point.x(), y = point.y(), z = point.z();
        double component = axis == 0 ? x : axis == 1 ? y : z;
        double squaredLength = x * x + y * y + z * z;
        return squaredLength > 0 && Double.isFinite(squaredLength) ? (float) (component * component / squaredLength) : 0;
    }

    /**
     * Applies {@code matrix * (direction, 0)} so matrix translation has no effect.
     * @param matrix transform whose linear part is used
     * @param direction input and output vector
     */
    private static void transformDirection(MutableMat4 matrix, MutableVec3 direction) {
        direction.set((float) matrix.transformComponent(0, direction.x(), direction.y(), direction.z(), 0),
                (float) matrix.transformComponent(1, direction.x(), direction.y(), direction.z(), 0),
                (float) matrix.transformComponent(2, direction.x(), direction.y(), direction.z(), 0));
    }

    /**
     * Освобождает активную ручку после отпускания указателя.
     */
    public void end() {
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
    public void paint(RenderContext context) {
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
        if (selected == null || selected.scene() != viewport.scene()) return false;
        Camera3D camera = viewport.camera();
        long nextScene = viewport.scene().revision(), nextCamera = camera.revision();
        if (nextScene == sceneRevision && nextCamera == cameraRevision) return visible;
        sceneRevision = nextScene;
        cameraRevision = nextCamera;
        visible = false;
        for (SceneNode node = selected; node != null; node = node.parent()) {
            if (!node.visible()) return false;
        }
        selected.position(pivot);
        SceneNode parent = selected.parent();
        if (parent != null && !parent.worldMatrix(parentWorld).transformPoint(pivot, pivot)) return false;
        visible = project(camera, pivot, center)
                && camera.unproject(center.x() + RADIUS, center.y(), center.z(), point);
        if (!visible) return false;
        float x = point.x() - pivot.x(), y = point.y() - pivot.y(), z = point.z() - pivot.z();
        worldRadius = (float) Math.sqrt(x * x + y * y + z * z);
        parentAxes.set(0, 0, 0, 1);
        for (SceneNode ancestor = parent; ancestor != null; ancestor = ancestor.parent()) {
            parentAxes.setProduct(ancestor.rotation(ancestorRotation), parentAxes);
        }
        localAxes.setProduct(parentAxes, selected.rotation(localAxes));
        for (int axis = 0; axis < AXES.length; axis++) {
            MutableVec3 unit = AXES[axis];
            if (space == Space.LOCAL) localAxes.rotate(unit.x(), unit.y(), unit.z(), worldAxes[axis]);
            else worldAxes[axis].set(unit);
        }
        for (int axis = 0; axis < handles.length; axis++) {
            MutableVec3 unit = worldAxes[axis];
            MutableVec3[] handle = handles[axis];
            if (mode == Mode.ROTATE) {
                MutableVec3 u = worldAxes[(axis + 1) % 3], v = worldAxes[(axis + 2) % 3];
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
                point.set(pivot.x() + worldRadius * unit.x(), pivot.y() + worldRadius * unit.y(),
                        pivot.z() + worldRadius * unit.z());
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
     * @param position  начальная позиция в системе родителя, в блоках.
     * @param scale     начальный относительный масштаб.
     * @param rotation  начальная ориентация.
     * @param direction handle axis expressed in parent space for translation or rotation
     * @param scaleWeights squared projections of the local axes onto the scale handle
     */
    private record Drag(int axis, float x, float y, float gradientX, float gradientY,
                        MutableVec3 position, MutableVec3 scale, MutableQuat rotation, MutableVec3 direction,
                        MutableVec3 scaleWeights) {
    }
}
