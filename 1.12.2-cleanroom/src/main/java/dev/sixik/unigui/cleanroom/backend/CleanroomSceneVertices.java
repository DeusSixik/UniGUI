package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;

import java.nio.ByteBuffer;

/**
 * Decodes Minecraft's interleaved model vertices into local-space scene quads.
 */
final class CleanroomSceneVertices {
    /**
     * Component byte offsets in the current vanilla format, or -1 for absent optional components.
     */
    private int position, uv, normal, color;
    /**
     * Distance to the next vertex in bytes.
     */
    private int stride;
    /**
     * Inverted modelview reused for the normal transform.
     */
    private final MutableMat4 inverse = new MutableMat4();

    /**
     * @param format vanilla vertex format @return whether the stream can be captured
     */
    boolean format(VertexFormat format) {
        position = uv = normal = color = -1;
        stride = format.getSize();
        for (int i = 0; i < format.getElementCount(); i++) {
            var element = format.getElement(i);
            int offset = format.getOffset(i);
            var type = element.getType();
            int size = element.getElementCount();
            switch (element.getUsage()) {
                case POSITION -> {
                    if (type == EnumType.FLOAT && size == 3) position = offset;
                }
                case UV -> {
                    if (element.getIndex() == 0) {
                        if (type != EnumType.FLOAT || size != 2) return false;
                        uv = offset;
                    }
                }
                case NORMAL -> {
                    if (type == EnumType.BYTE && size == 3) normal = offset;
                }
                case COLOR -> {
                    if (type != EnumType.UBYTE || size != 4) return false;
                    color = offset;
                }
                default -> {
                }
            }
        }
        return position >= 0;
    }

    /**
     * @return whether the stream provides primary texture coordinates
     */
    boolean textured() {
        return uv >= 0;
    }

    /**
     * @return whether the stream supplies per-vertex colors
     */
    boolean colored() {
        return color >= 0;
    }

    /**
     * Applies {@code M * (x,y,z,1)} to positions and {@code (M^-1)^T * n} to normals. If a format has
     * no normals, the cross product {@code (p1 - p0) × (p2 - p0)} provides the face normal from its winding.
     * The GPU upload normalizes the result.
     *
     * @param out       destination quad stream
     * @param data      completed vanilla vertex buffer in native byte order
     * @param count     vertex count, a multiple of four
     * @param modelview native pose in scene-node coordinates
     * @param tint      fixed-function ARGB color when vertex colors are absent
     * @return whether the transform was invertible and the stream was appended
     */
    boolean append(LegacySceneQuads out, ByteBuffer data, int count, Mat4View modelview, int tint) {
        if (!inverse.setInverse(modelview)) return false;
        int quadStride = stride * 4;
        int end = count * stride;
        for (int quad = 0; quad < end; quad += quadStride) {
            float nx = 0, ny = 0, nz = 0;
            if (normal < 0) {
                int p = quad + position;
                float ax = data.getFloat(p), ay = data.getFloat(p + 4), az = data.getFloat(p + 8);
                float bx = data.getFloat(p + stride) - ax;
                float by = data.getFloat(p + stride + 4) - ay;
                float bz = data.getFloat(p + stride + 8) - az;
                float cx = data.getFloat(p + 2 * stride) - ax;
                float cy = data.getFloat(p + 2 * stride + 4) - ay;
                float cz = data.getFloat(p + 2 * stride + 8) - az;
                nx = by * cz - bz * cy;
                ny = bz * cx - bx * cz;
                nz = bx * cy - by * cx;
            }
            for (int vertex = quad; vertex < quad + quadStride; vertex += stride) {
                int p = vertex + position;
                float x = data.getFloat(p), y = data.getFloat(p + 4), z = data.getFloat(p + 8);
                float u = uv < 0 ? 0 : data.getFloat(vertex + uv);
                float v = uv < 0 ? 0 : data.getFloat(vertex + uv + 4);
                if (normal >= 0) {
                    int n = vertex + normal;
                    nx = data.get(n);
                    ny = data.get(n + 1);
                    nz = data.get(n + 2);
                }
                int argb = color < 0 ? tint : rgba(data, vertex + color);
                out.vertex(point(modelview, 0, x, y, z), point(modelview, 1, x, y, z), point(modelview, 2, x, y, z),
                        u, v, normal(0, nx, ny, nz), normal(1, nx, ny, nz), normal(2, nx, ny, nz), argb);
            }
        }
        return true;
    }

    /**
     * @param m   affine modelview
     * @param row output axis
     * @param x   local X coordinate in blocks
     * @param y   local Y coordinate in blocks
     * @param z   local Z coordinate in blocks
     * @return posed coordinate in blocks
     */
    private static float point(Mat4View m, int row, float x, float y, float z) {
        return (float) (m.get(row, 0) * x + m.get(row, 1) * y + m.get(row, 2) * z + m.get(row, 3));
    }

    /**
     * @param row output axis
     * @param x   local normal X
     * @param y   local normal Y
     * @param z   local normal Z
     * @return unnormalized posed normal component
     */
    private float normal(int row, float x, float y, float z) {
        return (float) (inverse.get(0, row) * x + inverse.get(1, row) * y + inverse.get(2, row) * z);
    }

    /**
     * @param data vanilla vertex bytes @param offset byte index of R @return packed ARGB
     */
    private static int rgba(ByteBuffer data, int offset) {
        return Byte.toUnsignedInt(data.get(offset + 3)) << 24 | Byte.toUnsignedInt(data.get(offset)) << 16
                | Byte.toUnsignedInt(data.get(offset + 1)) << 8 | Byte.toUnsignedInt(data.get(offset + 2));
    }
}
