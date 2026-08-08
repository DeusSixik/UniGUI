package dev.sixik.unigui.api.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DrawMesh {
    private final List<DrawVertex> vertices;

    public DrawMesh(List<DrawVertex> vertices) {
        List<DrawVertex> copy = new ArrayList<>();
        if (vertices != null) {
            for (DrawVertex vertex : vertices) {
                if (vertex != null) copy.add(vertex.copy());
            }
        }
        this.vertices = Collections.unmodifiableList(copy);
    }

    public static DrawMesh triangles(List<DrawVertex> vertices) {
        return new DrawMesh(vertices);
    }

    public List<DrawVertex> vertices() {
        return vertices;
    }

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    public DrawMesh copy() {
        return new DrawMesh(vertices);
    }
}
