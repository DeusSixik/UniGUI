package dev.sixik.unigui.api.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

public final class DrawMesh {
    private final ObjectArrayList<DrawVertex> vertices = new ObjectArrayList<>();
    private final List<DrawVertex> verticesView = Collections.unmodifiableList(vertices);

    public DrawMesh(List<DrawVertex> vertices) {
        if (vertices != null) {
            for (DrawVertex vertex : vertices) {
                if (vertex != null) this.vertices.add(vertex.copy());
            }
        }
    }

    public static DrawMesh triangles(List<DrawVertex> vertices) {
        return new DrawMesh(vertices);
    }

    public List<DrawVertex> vertices() {
        return verticesView;
    }

    public Object[] vertexElements() {
        return vertices.elements();
    }

    public int vertexCount() {
        return vertices.size();
    }

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    public DrawMesh copy() {
        return new DrawMesh(vertices);
    }
}
