package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.QuatView;
import dev.sixik.unigui.api.math.Vec3View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Узел с локальной трансформацией и собственным содержимым. Используется в UI-потоке.
 */
public class SceneNode {
    /**
     * Принадлежность служебного корня сцене. У обычных узлов null.
     */
    private final Scene rootScene;
    /**
     * Локальная позиция в единицах сцены.
     */
    private final MutableVec3 position = new MutableVec3();
    /**
     * Единичное локальное вращение XYZW.
     */
    private final MutableQuat rotation = new MutableQuat();
    /**
     * Безразмерные коэффициенты масштаба по локальным осям.
     */
    private final MutableVec3 scale = new MutableVec3(1, 1, 1);
    /**
     * Кэш локальной матрицы T * R * S.
     */
    private final MutableMat4 local = new MutableMat4();
    /**
     * Кэш преобразования из локальной системы в мировую.
     */
    private final MutableMat4 world = new MutableMat4();

    /**
     * Список детей в порядке добавления, создаётся при первом добавлении.
     */
    private ArrayList<SceneNode> children;
    /**
     * Живое представление списка для чтения, создаётся при первом запросе.
     */
    private List<SceneNode> childrenView;
    /**
     * Родитель, служебный корень сцены или null для свободного узла.
     */
    private SceneNode parent;
    /**
     * Индекс в списке родителя при наличии родителя.
     */
    private int siblingIndex;
    /**
     * Нужно ли пересчитать T * R * S после изменения позы.
     */
    private boolean localDirty;
    /**
     * Нужно ли пересчитать мировую матрицу после изменения позы или родителя.
     */
    private boolean worldDirty;
    /**
     * Видимость узла и его детей при видимых родителях.
     */
    private boolean visible = true;
    /**
     * Меш в координатах узла или null.
     */
    private Mesh3D mesh;
    /**
     * Настройки отрисовки меша.
     */
    private Material material = Material.DEFAULT;
    /**
     * Явные границы содержимого узла или null для использования границ меша.
     */
    private Aabb boundsOverride;

    /**
     * Создаёт отсоединённый видимый узел с единичной трансформацией.
     */
    public SceneNode() {
        this(null);
    }

    /**
     * Создаёт узел с заданной принадлежностью служебного корня.
     *
     * @param rootScene сцена для служебного корня или null для обычного узла.
     */
    SceneNode(Scene rootScene) {
        this.rootScene = rootScene;
    }

    /**
     * Читает родителя узла.
     *
     * @return родительский узел или null у корневого либо отсоединённого узла.
     */
    public final SceneNode parent() {
        return parent == null || parent.rootScene != null ? null : parent;
    }

    /**
     * Находит сцену через верхний узел иерархии.
     *
     * @return сцена или null для отсоединённого дерева.
     */
    public final Scene scene() {
        SceneNode root = this;
        while (root.parent != null) root = root.parent;
        return root.rootScene;
    }

    /**
     * Читает детей в порядке добавления.
     *
     * @return живое представление списка для чтения.
     */
    public final List<SceneNode> children() {
        if (childrenView == null) {
            if (children == null) children = new ArrayList<>();
            childrenView = Collections.unmodifiableList(children);
        }
        return childrenView;
    }

    /**
     * Добавляет ребёнка, при необходимости перемещая его из прежней иерархии.
     * Локальная трансформация ребёнка сохраняется.
     *
     * @param child перемещаемый узел.
     * @return этот родитель.
     * @throws NullPointerException     если child равен null.
     * @throws IllegalArgumentException если добавление создаёт цикл.
     */
    public final SceneNode addChild(SceneNode child) {
        child.attachTo(this);
        return this;
    }

    /**
     * Отсоединяет собственного ребёнка со всем его поддеревом.
     *
     * @param child узел для удаления. Допускается null.
     * @return true, если этот узел был родителем child.
     */
    public final boolean removeChild(SceneNode child) {
        if (child == null || child.parent != this) return false;
        child.detach();
        return true;
    }

    /**
     * Отсоединяет этот узел вместе с его детьми от родителя или сцены.
     *
     * @return этот узел.
     */
    public final SceneNode detach() {
        attachTo(null);
        return this;
    }

    /**
     * Копирует локальную позицию.
     *
     * @param out переиспользуемый вектор в единицах сцены.
     * @return out.
     */
    public final MutableVec3 position(MutableVec3 out) {
        return out.set(position);
    }

    /**
     * Задаёт перенос относительно родителя.
     *
     * @param x конечная координата X в единицах сцены.
     * @param y конечная координата Y в единицах сцены.
     * @param z конечная координата Z в единицах сцены.
     * @return этот узел.
     * @throws IllegalArgumentException если координата не конечна.
     */
    public final SceneNode setPosition(float x, float y, float z) {
        requireFinite(x, y, z);
        if (position.x() != x || position.y() != y || position.z() != z) {
            position.set(x, y, z);
            transformChanged();
        }
        return this;
    }

    /**
     * Копирует локальное вращение.
     *
     * @param out переиспользуемый кватернион.
     * @return out.
     */
    public final MutableQuat rotation(MutableQuat out) {
        return out.set(rotation);
    }

    /**
     * Задаёт локальное вращение.
     *
     * @param value единичный кватернион активного вращения.
     * @return этот узел.
     */
    public final SceneNode setRotation(QuatView value) {
        if (rotation.x() != value.x() || rotation.y() != value.y()
                || rotation.z() != value.z() || rotation.w() != value.w()) {
            rotation.set(value);
            transformChanged();
        }
        return this;
    }

    /**
     * Копирует локальные коэффициенты масштаба.
     *
     * @param out переиспользуемый безразмерный вектор.
     * @return out.
     */
    public final MutableVec3 scale(MutableVec3 out) {
        return out.set(scale);
    }

    /**
     * Задаёт масштаб в локальных осях.
     *
     * @param x конечный безразмерный множитель по X, может быть нулевым или отрицательным.
     * @param y конечный безразмерный множитель по Y.
     * @param z конечный безразмерный множитель по Z.
     * @return этот узел.
     * @throws IllegalArgumentException если коэффициент не конечен.
     */
    public final SceneNode setScale(float x, float y, float z) {
        requireFinite(x, y, z);
        if (scale.x() != x || scale.y() != y || scale.z() != z) {
            scale.set(x, y, z);
            transformChanged();
        }
        return this;
    }

    /**
     * Копирует локальную матрицу {@code T * R * S}. S масштабирует, R вращает, T переносит.
     *
     * @param out переиспользуемая матрица.
     * @return out с преобразованием в координаты родителя.
     */
    public final MutableMat4 localMatrix(MutableMat4 out) {
        return out.set(resolvedLocal());
    }

    /**
     * Копирует мировую матрицу {@code parentWorld * local}. При отсутствии родителя
     * мировая матрица совпадает с локальной.
     *
     * @param out переиспользуемая матрица.
     * @return out с преобразованием в координаты сцены.
     */
    public final MutableMat4 worldMatrix(MutableMat4 out) {
        return out.set(resolvedWorld());
    }

    /**
     * Читает флаг видимости самого узла.
     *
     * @return true, если узел допускает показ себя и своего поддерева.
     */
    public final boolean visible() {
        return visible;
    }

    /**
     * Изменяет видимость всего поддерева при обходе сцены.
     *
     * @param visible true разрешает показ, false скрывает поддерево.
     * @return этот узел.
     */
    public final SceneNode setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            invalidateContent();
        }
        return this;
    }

    /**
     * Читает меш узла.
     *
     * @return неизменяемая геометрия или null.
     */
    public final Mesh3D mesh() {
        return mesh;
    }

    /**
     * Заменяет геометрию узла.
     *
     * @param mesh меш в координатах узла или null для удаления.
     * @return этот узел.
     */
    public final SceneNode setMesh(Mesh3D mesh) {
        if (this.mesh != mesh) {
            this.mesh = mesh;
            invalidateContent();
        }
        return this;
    }

    /**
     * Читает настройки поверхности.
     *
     * @return материал меша.
     */
    public final Material material() {
        return material;
    }

    /**
     * Заменяет настройки поверхности.
     *
     * @param material новый материал.
     * @return этот узел.
     * @throws NullPointerException если material равен null.
     */
    public final SceneNode setMaterial(Material material) {
        Objects.requireNonNull(material, "material");
        if (this.material != material) {
            this.material = material;
            invalidateContent();
        }
        return this;
    }

    /**
     * Читает границы собственного содержимого узла в локальных координатах.
     *
     * @return явный AABB, границы меша или null при отсутствии обоих источников.
     */
    public final Aabb bounds() {
        if (boundsOverride != null) return boundsOverride;
        return contentBounds();
    }

    /**
     * Читает границы собственной геометрии, когда явный AABB не задан.
     *
     * @return локальные границы содержимого или null.
     */
    protected Aabb contentBounds() {
        return mesh == null ? null : mesh.bounds();
    }

    /**
     * Проверяет геометрию узла, представленную границами или набором единичных ячеек.
     *
     * @param ray        луч в локальных координатах.
     * @param entry      расстояние входа в границы узла.
     * @param limit      максимальное расстояние поиска в локальных единицах.
     * @param cellOrigin нижний угол выбранной единичной ячейки. При обычном попадании сохраняет NaN.
     * @return ближайшее расстояние или +∞ при промахе. Треугольники меша проверяются отдельно.
     */
    protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
        return mesh == null ? entry : Double.POSITIVE_INFINITY;
    }

    /**
     * Возвращает собственную подсветку при попадании указателя в этот узел. Виджет вызывает метод
     * в UI-потоке при обновлении результата наведения, если подсветка для типа попадания включена.
     * Готовый результат можно переиспользовать. После изменения внешних данных подсветки вызовите
     * {@link #invalidateContent()}, чтобы обновить её под неподвижным указателем.
     *
     * @param cellOrigin нижний угол выбранной ячейки в координатах узла или null при попадании в объект.
     *                   Представление действительно в пределах вызова.
     * @return меш с материалом для подсветки или null для стандартной рамки AABB либо подсветки ячейки.
     */
    public HoverHighlight hoverHighlight(Vec3View cellOrigin) {
        return null;
    }

    /**
     * Геометрия и материал подсветки, которую виджет рисует с общей глубиной сцены.
     *
     * @param mesh     геометрия в локальных осях узла. Начало координат совпадает с началом выбранной
     *                 ячейки при попадании в неё и с началом узла при попадании в объект.
     * @param material материал подсветки.
     */
    public record HoverHighlight(Mesh3D mesh, Material material) {
        /**
         * Проверяет и сохраняет геометрию и материал.
         *
         * @param mesh     геометрия в локальных единицах сцены.
         * @param material материал подсветки.
         * @throws NullPointerException если меш или материал равен null.
         */
        public HoverHighlight {
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(material, "material");
        }
    }

    /**
     * Задаёт собственные границы для отсечения содержимого узла.
     *
     * @param bounds локальный AABB или null, чтобы брать границы из меша.
     * @return этот узел.
     */
    public final SceneNode setBounds(Aabb bounds) {
        if (!Objects.equals(boundsOverride, bounds)) {
            boundsOverride = bounds;
            invalidateContent();
        }
        return this;
    }

    /**
     * Отмечает внешний контент узла изменённым, например обновлённую текстуру или данные подкласса.
     */
    public final void invalidateContent() {
        Scene scene = scene();
        if (scene != null) scene.changed();
    }

    /**
     * Перемещает поддерево, сохраняя его локальные координаты. Обновляет обе сцены, если они различны.
     *
     * @param nextParent новый родитель, служебный корень сцены или null для отсоединения.
     * @throws IllegalArgumentException если новый родитель входит в перемещаемое поддерево.
     */
    final void attachTo(SceneNode nextParent) {
        if (parent == nextParent) return;
        for (SceneNode node = nextParent; node != null; node = node.parent) {
            if (node == this) throw new IllegalArgumentException("Scene hierarchy cannot contain a cycle");
        }
        Scene oldScene = scene();
        Scene newScene = nextParent == null ? null : nextParent.scene();
        int nextIndex = 0;
        if (nextParent != null) {
            if (nextParent.children == null) nextParent.children = new ArrayList<>();
            nextIndex = nextParent.children.size();
            nextParent.children.add(this);
        }
        if (parent != null) parent.unlinkChild(siblingIndex);
        parent = nextParent;
        siblingIndex = nextIndex;
        invalidateWorld();
        if (oldScene != null) oldScene.changed();
        if (newScene != null && newScene != oldScene) newScene.changed();
    }

    /**
     * Выбирает следующий узел прямого обхода дерева без рекурсии.
     *
     * @param root    граница обхода, текущий узел или его предок.
     * @param descend нужно ли сначала зайти в детей текущего узла.
     * @return следующий узел или null в конце поддерева root.
     */
    final SceneNode nextInSubtree(SceneNode root, boolean descend) {
        if (descend && children != null && !children.isEmpty()) return children.get(0);
        for (SceneNode node = this; node != root; node = node.parent) {
            int next = node.siblingIndex + 1;
            List<SceneNode> siblings = node.parent.children;
            if (next < siblings.size()) return siblings.get(next);
        }
        return null;
    }

    /**
     * Проверяет, может ли собственное содержимое быть видимым после мировой трансформации.
     *
     * @param frustum объём видимости текущей камеры.
     * @return true для узла без границ или при потенциальной видимости.
     */
    final boolean intersects(Frustum3D frustum) {
        MutableMat4 matrix = resolvedWorld();
        Aabb bounds = bounds();
        return bounds == null || frustum.intersects(bounds, matrix);
    }

    /**
     * Удаляет ребёнка из списка и корректирует индексы элементов после него.
     *
     * @param index позиция существующего ребёнка.
     */
    private void unlinkChild(int index) {
        children.remove(index);
        for (int i = index; i < children.size(); i++) {
            children.get(i).siblingIndex = i;
        }
    }

    /**
     * Помечает кэш локальной позы и производные мировые матрицы устаревшими.
     */
    private void transformChanged() {
        localDirty = true;
        invalidateWorld();
        invalidateContent();
    }

    /**
     * Помечает мировые матрицы поддерева устаревшими без обхода стека вызовов.
     */
    private void invalidateWorld() {
        for (SceneNode node = this; node != null; node = node.nextInSubtree(this, true)) {
            node.worldDirty = true;
        }
    }

    /**
     * Пересчитывает локальную матрицу при первом обращении после смены позы.
     *
     * @return внутренний кэш локальной матрицы.
     */
    private MutableMat4 resolvedLocal() {
        if (localDirty) {
            local.setTransform(position, rotation, scale);
            localDirty = false;
        }
        return local;
    }

    /**
     * Поднимается от узла к ближайшему предку с актуальной мировой матрицей,
     * последовательно домножая локальные матрицы слева.
     *
     * @return внутренний кэш мировой матрицы.
     */
    private MutableMat4 resolvedWorld() {
        if (!worldDirty) return world;
        world.set(resolvedLocal());
        for (SceneNode node = parent; node != null && node.rootScene == null; node = node.parent) {
            if (!node.worldDirty) {
                world.setProduct(node.world, world);
                break;
            }
            world.setProduct(node.resolvedLocal(), world);
        }
        worldDirty = false;
        return world;
    }

    /**
     * Проверяет компоненты переноса или масштаба.
     *
     * @param x компонента X.
     * @param y компонента Y.
     * @param z компонента Z.
     * @throws IllegalArgumentException если компонента не конечна.
     */
    private static void requireFinite(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Transform components must be finite");
        }
    }
}
