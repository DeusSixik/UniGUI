package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.scene.Mesh3D;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Converts Minecraft's ordered groups of four vertices to an indexed triangle mesh.
 */
public final class LegacySceneQuads {
    /**
     * XYZ positions in local block units.
     */
    private final FloatArrayList positions = new FloatArrayList();
    /**
     * XYZ surface normals.
     */
    private final FloatArrayList normals = new FloatArrayList();
    /**
     * Normalized UV pairs in the block atlas.
     */
    private final FloatArrayList uv = new FloatArrayList();
    /**
     * Packed ARGB vertex colors.
     */
    private final IntArrayList colors = new IntArrayList();

    /**
     * Appends a vertex. The four vertices of each quad use the original model's winding.
     *
     * @param x     block-local X coordinate.
     * @param y     block-local Y coordinate.
     * @param z     block-local Z coordinate.
     * @param u     normalized horizontal atlas coordinate.
     * @param v     normalized vertical atlas coordinate.
     * @param nx    X component of the face normal.
     * @param ny    Y component of the face normal.
     * @param nz    Z component of the face normal.
     * @param color packed ARGB vertex color.
     */
    public void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz, int color) {
        positions.add(x);
        positions.add(y);
        positions.add(z);
        normals.add(nx);
        normals.add(ny);
        normals.add(nz);
        uv.add(u);
        uv.add(v);
        colors.add(color);
    }

    /**
     * Applies the game's fixed face shading, the way blocks look in the world: top 1.0, north and south 0.8,
     * east and west 0.6, bottom 0.5. For a unit normal n the factor is
     * {@code min(1, 0.6 nx² + 0.8 nz² + ny² (3 + ny) / 4)}, which gives exactly those values on the axes and blends
     * them for slanted faces.
     *
     * @param argb packed ARGB color; alpha is kept.
     * @param nx   X component of the unit face normal.
     * @param ny   Y component of the unit face normal.
     * @param nz   Z component of the unit face normal.
     * @return the shaded color.
     */
    public static int shade(int argb, float nx, float ny, float nz) {
        float factor = Math.min(1, nx * nx * 0.6f + nz * nz * 0.8f + ny * ny * (3 + ny) / 4);
        int r = Math.round(((argb >>> 16) & 0xFF) * factor);
        int g = Math.round(((argb >>> 8) & 0xFF) * factor);
        int b = Math.round((argb & 0xFF) * factor);
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    /**
     * Splits each quad along diagonal 0–2 without changing its winding.
     *
     * @return immutable mesh, or null when no vertices were added.
     * @throws IllegalStateException if the final quad is incomplete.
     */
    public Mesh3D build() {
        int count = colors.size();
        if (count == 0) return null;
        if (count % 4 != 0) throw new IllegalStateException("Incomplete scene quad");
        int[] indices = new int[count / 4 * 6];
        for (int vertex = 0, index = 0; vertex < count; vertex += 4, index += 6) {
            indices[index] = vertex;
            indices[index + 1] = vertex + 1;
            indices[index + 2] = vertex + 2;
            indices[index + 3] = vertex;
            indices[index + 4] = vertex + 2;
            indices[index + 5] = vertex + 3;
        }
        return new Mesh3D(positions.toFloatArray(), normals.toFloatArray(), uv.toFloatArray(),
                colors.toIntArray(), indices);
    }
}
