package dev.sixik.unigui.api.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable-like triangle mesh для {@link DrawCommandType#MESH}.
 *
 * <p>Mesh хранит список вершин в порядке треугольников: каждые три вершины образуют один triangle.
 * Конструктор копирует входные вершины, чтобы draw list не зависел от внешних mutable объектов.</p>
 */
public final class DrawMesh {
    private final ObjectArrayList<DrawVertex> vertices = new ObjectArrayList<>();
    private final List<DrawVertex> verticesView = Collections.unmodifiableList(vertices);

    /**
     * Создаёт mesh из списка вершин.
     *
     * @param vertices вершины треугольников; {@code null} элементы игнорируются
     */
    public DrawMesh(List<DrawVertex> vertices) {
        if (vertices != null) {
            for (DrawVertex vertex : vertices) {
                if (vertex != null) this.vertices.add(vertex.copy());
            }
        }
    }

    /**
     * Создаёт triangle mesh.
     *
     * @param vertices вершины, сгруппированные по три
     * @return новый mesh
     */
    public static DrawMesh triangles(List<DrawVertex> vertices) {
        return new DrawMesh(vertices);
    }

    /** @return read-only view вершин */
    public List<DrawVertex> vertices() {
        return verticesView;
    }

    /**
     * Возвращает raw array fastutil-списка для горячих backend loops.
     *
     * @return внутренний массив; читать только первые {@link #vertexCount()} элементов
     */
    public Object[] vertexElements() {
        return vertices.elements();
    }

    /** @return количество вершин */
    public int vertexCount() {
        return vertices.size();
    }

    /** @return {@code true}, если mesh не содержит вершин */
    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    /** @return независимая копия mesh */
    public DrawMesh copy() {
        return new DrawMesh(vertices);
    }
}