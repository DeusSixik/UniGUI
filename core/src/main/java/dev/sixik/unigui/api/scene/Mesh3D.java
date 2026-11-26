package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.math.Aabb;

import java.util.Objects;

/**
 * Immutable indexed triangles stored in separate primitive arrays. A mesh can be shared
 * between nodes, and a renderer can cache GPU buffers by mesh identity.
 */
@WikiPage(
        title = "Mesh3D", slug = "mesh-3d",
        category = "API/3D Scene", order = 90,
        summary = "Immutable indexed triangle geometry for scene nodes.")
@WikiSection(title = "Geometry", order = 10, markdown = """
        Supply at least three vertices and indices in groups of three. The bounds enclose all positions, including
        unused vertices. Set a `Material` on the node to control texture, lighting, alpha and culling.
        """)
@WikiExample(title = "Create a triangle", code = """
        Mesh3D triangle = new Mesh3D(
                new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0},
                null, null, null, new int[]{0, 1, 2});
        scene.addRoot(new SceneNode().setMesh(triangle));
        """)
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "Material", target = "page:scene-material")
@WikiLink(label = "Scene3DRenderer", target = "page:scene-3d-renderer")
@AiDoc(
        summary = "Immutable indexed triangle geometry for scene nodes.",
        purpose = "Supply custom surfaces or reusable geometry for meshes and highlights.",
        criticalRules = {
                "Use counterclockwise triangle winding viewed from the front.",
                "Positions and normals use XYZ triples, UVs use pairs, and colors are ARGB. The constructor copies all arrays.",
                "A missing normal defaults to +Z, missing UV to (0, 0), and missing color to opaque white."
        },
        related = {"SceneNode", "Material", "Scene3DRenderer"},
        examples = @AiDoc.Example(title = "Create a triangle", code = """
                Mesh3D triangle = new Mesh3D(
                        new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0},
                        null, null, null, new int[]{0, 1, 2});
                scene.addRoot(new SceneNode().setMesh(triangle));
                """))
public final class Mesh3D {
    /**
     * Vertex XYZ positions in local units, three components per vertex.
     */
    private final float[] positions;
    /**
     * Vertex normal directions, three components per vertex, or null for +Z.
     */
    private final float[] normals;
    /**
     * Texture coordinates, two components per vertex, or null for (0, 0).
     */
    private final float[] uv;
    /**
     * Vertex colors, one ARGB value per vertex, or null for opaque white.
     */
    private final int[] colors;
    /**
     * Vertex indices from zero to vertexCount - 1, three per triangle.
     */
    private final int[] indices;
    /**
     * Local AABB of all positions, including unreferenced vertices.
     */
    private final Aabb bounds;

    /**
     * Copies and validates mesh data. Front-facing triangles have counterclockwise winding
     * when viewed from the front.
     *
     * @param positions finite local XYZ triples for at least three vertices.
     * @param normals finite normal XYZ triples or null. Directions are copied as provided.
     * @param uv finite UV pairs or null.
     * @param colors ARGB values or null.
     * @param indices vertex indices in triples for at least one triangle.
     * @throws NullPointerException if positions or indices is null.
     * @throws IllegalArgumentException if an attribute has an invalid length, a component is nonfinite,
     * or an index refers to a nonexistent vertex.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 10,
            summary = "Copies and validates mesh data. Front-facing triangles have counterclockwise winding when viewed from the front.")
    @WikiThrows(type = NullPointerException.class, description = "if positions or indices is null.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if an attribute has an invalid length, a component is nonfinite, or an index refers to a nonexistent vertex.")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 10,
            summary = "Copies and validates mesh data. Front-facing triangles have counterclockwise winding when viewed from the front.")
    public Mesh3D(
            @WikiParameter(description = "finite local XYZ triples for at least three vertices.") float[] positions,
            @WikiParameter(description =
                    "finite normal XYZ triples or null. Directions are copied as provided.")
            float[] normals,
            @WikiParameter(description = "finite UV pairs or null.") float[] uv,
            @WikiParameter(description = "ARGB values or null.") int[] colors,
            @WikiParameter(description = "vertex indices in triples for at least one triangle.") int[] indices) {
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
     * Reads the vertex count.
     *
     * @return number of XYZ triples.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 20,
            summary = "Reads the vertex count.")
    @WikiReturn("number of XYZ triples")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 20,
            summary = "Reads the vertex count.")
    public int vertexCount() {
        return positions.length / 3;
    }

    /**
     * Reads the index count.
     *
     * @return number of indices, a multiple of three.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 30,
            summary = "Reads the index count.")
    @WikiReturn("number of indices, a multiple of three")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 30,
            summary = "Reads the index count.")
    public int indexCount() {
        return indices.length;
    }

    /**
     * Reads a vertex index.
     *
     * @param index position in the index list.
     * @return vertex index.
     * @throws IndexOutOfBoundsException if index is outside the index list.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 40,
            summary = "Reads a vertex index.")
    @WikiReturn("vertex index")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if index is outside the index list.")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 40,
            summary = "Reads a vertex index.")
    public int index(
            @WikiParameter(description = "position in the index list.") int index) {
        return indices[index];
    }

    /**
     * Reads a position component.
     *
     * @param vertex vertex index.
     * @param axis component index from 0 to 2.
     * @return position component in mesh units.
     * @throws IndexOutOfBoundsException if vertex or axis is outside its range.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 50,
            summary = "Reads a position component.")
    @WikiReturn("position component in mesh units")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if vertex or axis is outside its range.")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 50,
            summary = "Reads a position component.")
    public float position(
            @WikiParameter(description = "vertex index.") int vertex,
            @WikiParameter(description = "component index from 0 to 2.") int axis) {
        return positions[Objects.checkIndex(vertex, vertexCount()) * 3 + Objects.checkIndex(axis, 3)];
    }

    /**
     * Reads a normal component.
     *
     * @param vertex vertex index.
     * @param axis component index from 0 to 2.
     * @return dimensionless normal component, using +Z for a missing attribute.
     * @throws IndexOutOfBoundsException if vertex or axis is outside its range.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 60,
            summary = "Reads a normal component.")
    @WikiReturn("dimensionless normal component, using +Z for a missing attribute")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if vertex or axis is outside its range.")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 60,
            summary = "Reads a normal component.")
    public float normal(
            @WikiParameter(description = "vertex index.") int vertex,
            @WikiParameter(description = "component index from 0 to 2.") int axis) {
        int offset = Objects.checkIndex(vertex, vertexCount()) * 3 + Objects.checkIndex(axis, 3);
        return normals == null ? (axis == 2 ? 1.0f : 0.0f) : normals[offset];
    }

    /**
     * Reads a texture coordinate component.
     *
     * @param vertex vertex index.
     * @param axis 0 for U or 1 for V.
     * @return coordinate in texture units, which may be outside [0, 1].
     * @throws IndexOutOfBoundsException if vertex or axis is outside its range.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 70,
            summary = "Reads a texture coordinate component.")
    @WikiReturn("coordinate in texture units, which may be outside [0, 1]")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if vertex or axis is outside its range.")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 70,
            summary = "Reads a texture coordinate component.")
    public float uv(
            @WikiParameter(description = "vertex index.") int vertex,
            @WikiParameter(description = "0 for U or 1 for V.") int axis) {
        int offset = Objects.checkIndex(vertex, vertexCount()) * 2 + Objects.checkIndex(axis, 2);
        return uv == null ? 0.0f : uv[offset];
    }

    /**
     * Reads a vertex color.
     *
     * @param vertex vertex index.
     * @return ARGB color, or opaque white for a missing attribute.
     * @throws IndexOutOfBoundsException if vertex is outside its range.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 80,
            summary = "Reads a vertex color.")
    @WikiReturn("ARGB color, or opaque white for a missing attribute")
    @WikiThrows(type = IndexOutOfBoundsException.class, description = "if vertex is outside its range.")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 80,
            summary = "Reads a vertex color.")
    public int colorArgb(
            @WikiParameter(description = "vertex index.") int vertex) {
        Objects.checkIndex(vertex, vertexCount());
        return colors == null ? 0xFFFFFFFF : colors[vertex];
    }

    /**
     * Reads the local bounds.
     *
     * @return immutable AABB of the positions in mesh coordinates.
     */
    @WikiMethod(section = "Geometry", sectionOrder = 10, order = 90,
            summary = "Reads the local bounds.")
    @WikiReturn("immutable AABB of the positions in mesh coordinates")
    @AiDoc(section = "Geometry", sectionOrder = 10, order = 90,
            summary = "Reads the local bounds.")
    public Aabb bounds() {
        return bounds;
    }

    /**
     * Copies an optional attribute after checking that its components are finite.
     *
     * @param attribute source components, or null.
     * @return a copy of the attribute, or null.
     * @throws IllegalArgumentException if a component is nonfinite.
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
     * Scans the minimum and maximum position along each axis.
     *
     * @param positions XYZ triples for at least one vertex.
     * @return the local AABB.
     * @throws IllegalArgumentException if any coordinate is nonfinite.
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
