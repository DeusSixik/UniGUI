package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;

import java.util.Objects;

/**
 * Неизменяемая геометрия индексированных треугольников. Атрибуты лежат в отдельных
 * массивах примитивов. Один меш можно использовать в нескольких узлах, а бэкенд
 * может кэшировать его GPU-буферы по идентичности объекта.
 */
public final class Mesh3D {
    /**
     * XYZ вершин в локальных единицах, три значения на вершину.
     */
    private final float[] positions;
    /**
     * Компоненты направлений нормалей, три на вершину, либо null для нормали +Z.
     */
    private final float[] normals;
    /**
     * UV, два значения на вершину, либо null для UV (0, 0).
     */
    private final float[] uv;
    /**
     * Цвета по одному ARGB на вершину, либо null для непрозрачного белого.
     */
    private final int[] colors;
    /**
     * Индексы от 0 до vertexCount−1, три на треугольник.
     */
    private final int[] indices;
    /**
     * Локальный AABB всех позиций, включая позиции вне списка индексов.
     */
    private final Aabb bounds;

    /**
     * Копирует и проверяет данные. Лицевая сторона треугольника задана порядком
     * вершин против часовой стрелки при взгляде на неё.
     *
     * @param positions конечные координаты XYZ, не менее трёх вершин.
     * @param normals   конечные XYZ нормалей или null. Передаются без нормализации.
     * @param uv        конечные UV или null.
     * @param colors    цвета ARGB или null.
     * @param indices   группы по три индекса, не менее одного треугольника.
     * @throws NullPointerException     если positions или indices равен null.
     * @throws IllegalArgumentException если длина атрибута не совпадает с числом вершин,
     *                                  координаты не конечны или индекс не указывает на существующую вершину.
     */
    public Mesh3D(float[] positions, float[] normals, float[] uv, int[] colors, int[] indices) {
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(indices, "indices");
        int vertices = positions.length / 3;
        if (vertices < 3 || positions.length % 3 != 0 || indices.length == 0 || indices.length % 3 != 0) {
            throw new IllegalArgumentException("Mesh requires XYZ positions and complete indexed triangles");
        }
        if ((normals != null && normals.length != positions.length)
                || (uv != null && uv.length != vertices * 2)
                || (colors != null && colors.length != vertices)) {
            throw new IllegalArgumentException("Attribute lengths must match vertex count");
        }
        this.positions = positions.clone();
        this.normals = copyFinite(normals);
        this.uv = copyFinite(uv);
        this.colors = colors == null ? null : colors.clone();
        this.indices = indices.clone();
        for (int index : this.indices) {
            if (index < 0 || index >= vertices) throw new IllegalArgumentException("Mesh index outside vertex range");
        }
        bounds = calculateBounds(this.positions);
    }

    /**
     * Читает число вершин.
     *
     * @return число групп XYZ.
     */
    public int vertexCount() {
        return positions.length / 3;
    }

    /**
     * Читает число индексов.
     *
     * @return число индексов, кратное трём.
     */
    public int indexCount() {
        return indices.length;
    }

    /**
     * Читает индекс вершины.
     *
     * @param index позиция в списке индексов.
     * @return номер вершины.
     * @throws IndexOutOfBoundsException если позиция вне списка.
     */
    public int index(int index) {
        return indices[index];
    }

    /**
     * Читает компоненту позиции.
     *
     * @param vertex номер вершины.
     * @param axis   индекс компоненты, от 0 до 2.
     * @return координата в единицах меша.
     * @throws IndexOutOfBoundsException если индекс вершины или компоненты вне диапазона.
     */
    public float position(int vertex, int axis) {
        return positions[Objects.checkIndex(vertex, vertexCount()) * 3 + Objects.checkIndex(axis, 3)];
    }

    /**
     * Читает компоненту нормали.
     *
     * @param vertex номер вершины.
     * @param axis   индекс компоненты, от 0 до 2.
     * @return безразмерная компонента нормали. Для отсутствующего атрибута используется +Z.
     * @throws IndexOutOfBoundsException если индекс вершины или компоненты вне диапазона.
     */
    public float normal(int vertex, int axis) {
        int offset = Objects.checkIndex(vertex, vertexCount()) * 3 + Objects.checkIndex(axis, 3);
        return normals == null ? (axis == 2 ? 1.0f : 0.0f) : normals[offset];
    }

    /**
     * Читает текстурную координату.
     *
     * @param vertex номер вершины.
     * @param axis   индекс компоненты, 0 для U и 1 для V.
     * @return UV в долях размера текстуры, допускаются значения вне [0, 1].
     * @throws IndexOutOfBoundsException если индекс вершины или компоненты вне диапазона.
     */
    public float uv(int vertex, int axis) {
        int offset = Objects.checkIndex(vertex, vertexCount()) * 2 + Objects.checkIndex(axis, 2);
        return uv == null ? 0.0f : uv[offset];
    }

    /**
     * Читает цвет вершины.
     *
     * @param vertex номер вершины.
     * @return ARGB или непрозрачный белый при отсутствии атрибута.
     * @throws IndexOutOfBoundsException если индекс вершины вне диапазона.
     */
    public int colorArgb(int vertex) {
        Objects.checkIndex(vertex, vertexCount());
        return colors == null ? 0xFFFFFFFF : colors[vertex];
    }

    /**
     * Читает локальные границы.
     *
     * @return неизменяемый AABB позиций в координатах меша.
     */
    public Aabb bounds() {
        return bounds;
    }

    /**
     * Копирует необязательный атрибут и проверяет конечность компонент.
     *
     * @param attribute исходные компоненты или null.
     * @return копия атрибута или null.
     * @throws IllegalArgumentException если есть не конечная компонента.
     */
    private static float[] copyFinite(float[] attribute) {
        if (attribute == null) return null;
        float[] copy = attribute.clone();
        for (float value : copy) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("Mesh attributes must be finite");
        }
        return copy;
    }

    /**
     * Находит минимум и максимум компонент по каждой оси.
     *
     * @param positions группы XYZ, содержащие хотя бы одну вершину.
     * @return локальный AABB.
     * @throws IllegalArgumentException если хотя бы одна координата не конечна.
     */
    private static Aabb calculateBounds(float[] positions) {
        float minX = positions[0], minY = positions[1], minZ = positions[2];
        float maxX = minX, maxY = minY, maxZ = minZ;
        for (int i = 3; i < positions.length; i += 3) {
            float x = positions[i], y = positions[i + 1], z = positions[i + 2];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        return new Aabb(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
