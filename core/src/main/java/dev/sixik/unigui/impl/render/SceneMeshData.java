package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneMaterialEffect;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Упаковка вершин и блока std140 для пайплайна индексированных мешей.
 */
public final class SceneMeshData {
    /**
     * Размер вершины XYZ (float), UV (float), RGBA (UNORM8) и нормали (SNORM8 × 3 + 1 байт).
     */
    public static final int VERTEX_BYTES = 28;
    /**
     * Шаг матрицы mat4 в массиве std140, в байтах.
     */
    public static final int CLIP_BYTES = 64;
    /**
     * Размер std140 блока SceneMesh: mat4, mat3 с выровненными столбцами и шесть vec4.
     */
    public static final int UNIFORM_BYTES = 208;
    /**
     * Size of the bounds and effect clock tail of the surface block, in bytes.
     */
    public static final int EFFECT_BYTES = 3 * 4 * Float.BYTES;

    private SceneMeshData() {
    }

    /**
     * Записывает вершины меша последовательно. Нормали делятся на длину и квантуются как
     * {@code round(127 * component / |normal|)}. Для нулевой нормали записываются нули.
     *
     * @param mesh геометрия в локальных единицах.
     * @param out  буфер с порядком байтов устройства и местом для всех вершин.
     * @throws IndexOutOfBoundsException если в out недостаточно места.
     * @throws ArithmeticException       если размер данных превышает максимальный размер ByteBuffer.
     */
    public static void writeVertices(Mesh3D mesh, ByteBuffer out) {
        int count = mesh.vertexCount();
        Objects.checkFromIndexSize(out.position(), Math.multiplyExact(count, VERTEX_BYTES), out.limit());
        for (int vertex = 0; vertex < count; vertex++) {
            out.putFloat(mesh.position(vertex, 0)).putFloat(mesh.position(vertex, 1)).putFloat(mesh.position(vertex, 2));
            out.putFloat(mesh.uv(vertex, 0)).putFloat(mesh.uv(vertex, 1));
            int color = mesh.colorArgb(vertex);
            out.put((byte) (color >>> 16)).put((byte) (color >>> 8)).put((byte) color).put((byte) (color >>> 24));
            double x = mesh.normal(vertex, 0), y = mesh.normal(vertex, 1), z = mesh.normal(vertex, 2);
            double length = Math.sqrt(x * x + y * y + z * z);
            double quantization = length == 0 ? 0 : 127.0 / length;
            out.put((byte) Math.round(x * quantization)).put((byte) Math.round(y * quantization))
                    .put((byte) Math.round(z * quantization)).put((byte) 0);
        }
    }

    /**
     * Записывает индексы треугольников как 32-битные целые.
     *
     * @param mesh источник индексов.
     * @param out  буфер с порядком байтов устройства и местом для всех индексов.
     * @throws IndexOutOfBoundsException если в out недостаточно места.
     * @throws ArithmeticException       если размер данных превышает максимальный размер ByteBuffer.
     */
    public static void writeIndices(Mesh3D mesh, ByteBuffer out) {
        int count = mesh.indexCount();
        Objects.checkFromIndexSize(out.position(), Math.multiplyExact(count, Integer.BYTES), out.limit());
        for (int index = 0; index < count; index++) out.putInt(mesh.index(index));
    }

    /**
     * Записывает полный блок SceneMesh. Матрицы пишутся по столбцам, цвета в порядке RGBA,
     * а RGB источника света предварительно умножается на его интенсивность.
     *
     * @param out           буфер с порядком байтов устройства и местом для {@link #UNIFORM_BYTES} байт.
     * @param clip          произведение projection * view * world.
     * @param world         матрица из локальных координат в мировые.
     * @param material      параметры поверхности и альфа-канала.
     * @param ambient       окружающий свет.
     * @param light         направленный свет или null.
     * @param viewportX     доля ширины цели, занимаемая viewport.
     * @param viewportY     доля высоты цели, занимаемая viewport.
     * @param zeroToOne     ожидает ли GPU глубину NDC от 0 до 1.
     * @param flipY         нужно ли отразить вертикальную координату текстуры.
     * @param premultiplied хранит ли текстура цвет, уже умноженный на альфу.
     * @throws IndexOutOfBoundsException если в out недостаточно места.
     */
    public static void writeUniform(ByteBuffer out, Mat4View clip, Mat4View world, Material material,
                                    AmbientLight ambient, DirectionalLight light, float viewportX, float viewportY,
                                    boolean zeroToOne, boolean flipY, boolean premultiplied) {
        Objects.checkFromIndexSize(out.position(), UNIFORM_BYTES, out.limit());
        writeClipMatrix(out, clip);
        writeNormals(out, world);
        int tint = material.colorArgb();
        putColor(out, tint, 1, (tint >>> 24) / 255.0f);
        putColor(out, ambient.colorRgb(), ambient.intensity(), 0);
        putColor(out, light == null ? 0 : light.colorRgb(), light == null ? 0 : light.intensity(), 0);
        putVector(out, light == null ? 0 : -light.x(), light == null ? 0 : -light.y(),
                light == null ? 0 : -light.z(), material.shading() == Material.Shading.LIT ? 1 : 0);
        putVector(out, viewportX, viewportY, zeroToOne ? 1 : 0, material.alphaMode().ordinal());
        putVector(out, material.alphaCutoff(), flipY ? 1 : 0, premultiplied ? 1 : 0, material.culling().ordinal());
    }

    /**
     * Записывает матрицу преобразования в clip space по столбцам в формате std140.
     *
     * @param out  буфер с порядком байтов устройства и местом для {@link #CLIP_BYTES} байт.
     * @param clip произведение projection * view * world.
     * @throws IndexOutOfBoundsException если в out недостаточно места.
     */
    public static void writeClipMatrix(ByteBuffer out, Mat4View clip) {
        Objects.checkFromIndexSize(out.position(), CLIP_BYTES, out.limit());
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) out.putFloat((float) clip.get(row, column));
        }
    }

    /**
     * Writes the local bounds and effect clock as three std140 vec4s.
     *
     * @param out          native-order buffer with {@link #EFFECT_BYTES} writable bytes
     * @param bounds       the complete local bounds shared by the node's rendered layers
     * @param effect       the surface effect, or null when using the default surface
     * @param sceneSeconds finite scene clock time in seconds
     * @throws IndexOutOfBoundsException if there is insufficient space in the buffer
     */
    public static void writeEffectUniform(ByteBuffer out, Aabb bounds, SceneMaterialEffect effect, float sceneSeconds) {
        writeEffectUniform(out, bounds, effect, sceneSeconds, false);
    }

    /**
     * Writes the local bounds, effect clock and spotlight receiver flag as three std140 vec4s.
     *
     * @param out                native-order buffer with {@link #EFFECT_BYTES} writable bytes
     * @param bounds             the complete local bounds shared by the node's rendered layers
     * @param effect             the surface effect, or null when using the default surface
     * @param sceneSeconds       finite scene clock time in seconds
     * @param receiveSpotShadows whether to sample the perspective shadow maps on this surface
     * @throws IndexOutOfBoundsException if there is insufficient space in the buffer
     */
    public static void writeEffectUniform(ByteBuffer out, Aabb bounds, SceneMaterialEffect effect, float sceneSeconds,
                                          boolean receiveSpotShadows) {
        Objects.checkFromIndexSize(out.position(), EFFECT_BYTES, out.limit());
        putVector(out, bounds.minX(), bounds.minY(), bounds.minZ(), 0);
        putVector(out, bounds.maxX(), bounds.maxY(), bounds.maxZ(), 0);
        SceneMaterialEffect.Timing timing = effect == null ? null : effect.timing();
        float elapsed = timing == null ? Math.max(0, sceneSeconds) : timing.elapsedAt(sceneSeconds);
        float progress = timing == null ? 0 : timing.progressAt(sceneSeconds);
        putVector(out, elapsed, progress, effect == null ? 0 : effect.alphaCutoff(), receiveSpotShadows ? 1 : 0);
    }

    /**
     * Записывает матрицу нормалей. Если столбцы линейной части world равны a, b и c,
     * столбцы матрицы кофакторов равны {@code b × c}, {@code c × a}, {@code a × b}.
     * При ненулевом определителе умножение на его знак даёт нормалям направление
     * {@code (world^-1)^T}, а общий положительный множитель устраняется нормализацией шейдера.
     * Вырожденный масштаб оставляет определённый кофактор для плоскости, пережившей преобразование.
     *
     * @param out   буфер std140 в начале mat3.
     * @param world исходное аффинное преобразование.
     */
    private static void writeNormals(ByteBuffer out, Mat4View world) {
        double ax = world.get(0, 0), ay = world.get(1, 0), az = world.get(2, 0);
        double bx = world.get(0, 1), by = world.get(1, 1), bz = world.get(2, 1);
        double cx = world.get(0, 2), cy = world.get(1, 2), cz = world.get(2, 2);
        double nx0 = by * cz - bz * cy, ny0 = bz * cx - bx * cz, nz0 = bx * cy - by * cx;
        double nx1 = cy * az - cz * ay, ny1 = cz * ax - cx * az, nz1 = cx * ay - cy * ax;
        double nx2 = ay * bz - az * by, ny2 = az * bx - ax * bz, nz2 = ax * by - ay * bx;
        double largest = Math.max(maxAbs(nx0, ny0, nz0), Math.max(maxAbs(nx1, ny1, nz1), maxAbs(nx2, ny2, nz2)));
        if (!(largest > 0) || !Double.isFinite(largest)) {
            for (int i = 0; i < 3; i++) putVector(out, 0, 0, 0, 0);
            return;
        }
        double sign = ax * nx0 + ay * ny0 + az * nz0 < 0.0 ? -1.0 : 1.0;
        putVector(out, (float) (sign * (nx0 / largest)), (float) (sign * (ny0 / largest)),
                (float) (sign * (nz0 / largest)), 0);
        putVector(out, (float) (sign * (nx1 / largest)), (float) (sign * (ny1 / largest)),
                (float) (sign * (nz1 / largest)), 0);
        putVector(out, (float) (sign * (nx2 / largest)), (float) (sign * (ny2 / largest)),
                (float) (sign * (nz2 / largest)), 0);
    }

    /**
     * Находит масштаб для нормализации трёх компонент.
     *
     * @param x первая компонента.
     * @param y вторая компонента.
     * @param z третья компонента.
     * @return максимальный модуль.
     */
    private static double maxAbs(double x, double y, double z) {
        return Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
    }

    /**
     * Записывает цвет из младших 24 бит RGB.
     *
     * @param out       выход для vec4.
     * @param rgb       упакованный RGB.
     * @param intensity множитель RGB.
     * @param alpha     значение четвёртой компоненты.
     */
    private static void putColor(ByteBuffer out, int rgb, float intensity, float alpha) {
        putVector(out, (rgb >>> 16 & 255) / 255.0f * intensity, (rgb >>> 8 & 255) / 255.0f * intensity,
                (rgb & 255) / 255.0f * intensity, alpha);
    }

    /**
     * Записывает четыре компоненты подряд.
     *
     * @param out выход для vec4.
     * @param x   первая компонента.
     * @param y   вторая компонента.
     * @param z   третья компонента.
     * @param w   четвёртая компонента.
     */
    private static void putVector(ByteBuffer out, float x, float y, float z, float w) {
        out.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
    }
}
