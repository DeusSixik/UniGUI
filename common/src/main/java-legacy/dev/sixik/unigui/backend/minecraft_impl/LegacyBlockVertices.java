package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.Mesh3D;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Собирает квадовые вершины ванильной геометрии в координатах узла.
 */
abstract class LegacyBlockVertices {
    /**
     * Сохранённые компоненты XYZ.
     */
    private final FloatArrayList positions = new FloatArrayList();
    /**
     * UV-координаты атласа.
     */
    private final FloatArrayList uvs = new FloatArrayList();
    /**
     * Цвета ARGB, включая tint и AO.
     */
    private final IntArrayList colors = new IntArrayList();
    /**
     * Дополнительный перенос жидкостей из координат секции.
     */
    private int xOffset, yOffset, zOffset;

    /**
     * Задаёт смещение системы координат следующих вершин.
     *
     * @param x перенос по X в блоках.
     * @param y перенос по Y в блоках.
     * @param z перенос по Z в блоках.
     */
    final void offset(int x, int y, int z) {
        xOffset = x;
        yOffset = y;
        zOffset = z;
    }

    /**
     * Открывает вершину со значениями цвета и UV по умолчанию.
     *
     * @param x координата X в блоках.
     * @param y координата Y в блоках.
     * @param z координата Z в блоках.
     */
    final void position(float x, float y, float z) {
        positions.add(x + xOffset);
        positions.add(y + yOffset);
        positions.add(z + zOffset);
        colors.add(0xFFFFFFFF);
        uvs.add(0);
        uvs.add(0);
    }

    /**
     * Пишет цвет последней вершины.
     *
     * @param r красный, от 0 до 255.
     * @param g зелёный, от 0 до 255.
     * @param b синий, от 0 до 255.
     * @param a альфа, от 0 до 255.
     */
    final void rgba(int r, int g, int b, int a) {
        colors.set(colors.size() - 1, a << 24 | r << 16 | g << 8 | b);
    }

    /**
     * Пишет UV последней вершины.
     *
     * @param u координата по горизонтали.
     * @param v координата по вертикали.
     */
    final void tex(float u, float v) {
        uvs.set(uvs.size() - 2, u);
        uvs.set(uvs.size() - 1, v);
    }

    /**
     * Запекает квады по диагонали 0–2 в треугольники.
     *
     * @return меш с цветами ванильного освещения или null для пустого слоя.
     */
    final Mesh3D build() {
        int count = colors.size();
        if (count == 0) return null;
        if ((count & 3) != 0) throw new IllegalStateException("Minecraft emitted an incomplete block quad");
        int[] indices = new int[count / 4 * 6];
        for (int vertex = 0, index = 0; vertex < count; vertex += 4) {
            indices[index++] = vertex;
            indices[index++] = vertex + 1;
            indices[index++] = vertex + 2;
            indices[index++] = vertex;
            indices[index++] = vertex + 2;
            indices[index++] = vertex + 3;
        }
        return new Mesh3D(positions.toFloatArray(), null, uvs.toFloatArray(), colors.toIntArray(), indices);
    }
}
