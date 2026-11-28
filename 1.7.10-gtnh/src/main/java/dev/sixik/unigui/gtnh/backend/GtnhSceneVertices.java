package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;

import java.nio.ByteOrder;

/**
 * Reads the fixed 1.7.10 Tessellator layout without copying its buffer.
 */
final class GtnhSceneVertices {
    /**
     * XYZ, UV, RGBA, normal, lightmap. Each slot occupies four bytes.
     */
    private static final int STRIDE = 8;
    /**
     * Whether Minecraft stores RGBA with red in the least significant byte.
     */
    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    /**
     * Inverted modelview for transforming normals.
     */
    private final MutableMat4 inverse = new MutableMat4();

    /**
     * Calculates posed positions as {@code M * (x,y,z,1)} and normals as {@code (M^-1)^T * n},
     * where {@code M} is the modelview and {@code n} is the native normal or
     * {@code (p1 - p0) × (p2 - p0)} from the first three vertices of the original quad.
     *
     * @param out       destination completed quad stream
     * @param data      tessellator vertices in Minecraft's fixed layout
     * @param count     number of vertices, divisible by four
     * @param modelview local pose including rotation, scale and translation in blocks
     * @param textured  whether primary UVs are present
     * @param colored   whether RGBA bytes are present
     * @param normals   whether packed normals are present
     * @param tint      fixed-function ARGB tint when vertex colors are absent
     * @return whether the pose was invertible and the vertices were appended
     */
    boolean append(LegacySceneQuads out, int[] data, int count, Mat4View modelview,
                   boolean textured, boolean colored, boolean normals, int tint) {
        if (!inverse.setInverse(modelview)) return false;
        int quadStride = STRIDE * 4;
        for (int quad = 0; quad < count * STRIDE; quad += quadStride) {
            float nx = 0, ny = 0, nz = 0;
            if (!normals) {
                float ax = component(data, quad), ay = component(data, quad + 1), az = component(data, quad + 2);
                float bx = component(data, quad + STRIDE) - ax;
                float by = component(data, quad + STRIDE + 1) - ay;
                float bz = component(data, quad + STRIDE + 2) - az;
                float cx = component(data, quad + 2 * STRIDE) - ax;
                float cy = component(data, quad + 2 * STRIDE + 1) - ay;
                float cz = component(data, quad + 2 * STRIDE + 2) - az;
                nx = by * cz - bz * cy;
                ny = bz * cx - bx * cz;
                nz = bx * cy - by * cx;
            }
            for (int vertex = quad; vertex < quad + quadStride; vertex += STRIDE) {
                float x = component(data, vertex), y = component(data, vertex + 1), z = component(data, vertex + 2);
                float u = textured ? component(data, vertex + 3) : 0;
                float v = textured ? component(data, vertex + 4) : 0;
                if (normals) {
                    int packed = data[vertex + 6];
                    nx = (byte) packed;
                    ny = (byte) (packed >>> 8);
                    nz = (byte) (packed >>> 16);
                }
                int argb = colored ? rgba(data[vertex + 5]) : tint;
                out.vertex(point(modelview, 0, x, y, z), point(modelview, 1, x, y, z), point(modelview, 2, x, y, z),
                    u, v, normal(0, nx, ny, nz), normal(1, nx, ny, nz), normal(2, nx, ny, nz), argb);
            }
        }
        return true;
    }

    /**
     * @param data tessellator vertices @param index offset in ints @return decoded float component
     */
    private static float component(int[] data, int index) {
        return Float.intBitsToFloat(data[index]);
    }

    /**
     * @param color native RGBA bytes as an int @return packed ARGB
     */
    private static int rgba(int color) {
        return Integer.rotateRight(LITTLE_ENDIAN ? Integer.reverseBytes(color) : color, 8);
    }

    /**
     * @param matrix affine pose
     * @param row    output axis
     * @param x      local X in blocks
     * @param y      local Y in blocks
     * @param z      local Z in blocks
     * @return posed coordinate in blocks
     */
    private static float point(Mat4View matrix, int row, float x, float y, float z) {
        return (float) (matrix.get(row, 0) * x + matrix.get(row, 1) * y + matrix.get(row, 2) * z + matrix.get(row, 3));
    }

    /**
     * @param row output axis
     * @param x   normal X
     * @param y   normal Y
     * @param z   normal Z
     * @return unnormalized posed normal component
     */
    private float normal(int row, float x, float y, float z) {
        return (float) (inverse.get(0, row) * x + inverse.get(1, row) * y + inverse.get(2, row) * z);
    }
}
