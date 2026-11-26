package dev.sixik.unigui.api.math;

/**
 * Шесть полупространств видимости для перспективной или ортографической камеры.
 */
public final class Frustum3D {
    /**
     * Небольшой запас в единицах clip.w для округления при построении матрицы проекции.
     */
    private static final double CLIP_BOUNDARY = 1.0 + 8.0 * Math.ulp(1.0);
    /**
     * Коэффициенты (a, b, c, d) плоскостей лево, право, низ, верх, ближняя и дальняя.
     */
    private final double[] planes = {1, 0, 0, 1, -1, 0, 0, 1, 0, 1, 0, 1,
            0, -1, 0, 1, 0, 0, 1, 1, 0, 0, -1, 1};

    /**
     * Создаёт объём видимости единичной матрицы проекции, от −1 до 1 по каждой оси.
     */
    public Frustum3D() {
    }

    /**
     * Обновляет плоскости по матрице V = projection * view. Неравенства
     * {@code -clip.w <= clip.x,y,z <= clip.w} дают строки {@code V[3] ± V[i]},
     * где i — соответствующая ось clip space. Множитель {@link #CLIP_BOUNDARY} немного расширяет
     * границы относительно clip.w, сохраняя касания при округлении коэффициентов проекции.
     *
     * @param viewProjection матрица из мировых координат в clip space с глубиной NDC от −1 до 1.
     * @return этот объём видимости.
     */
    public Frustum3D set(Mat4View viewProjection) {
        for (int axis = 0; axis < 3; axis++) {
            int plane = axis * 8;
            for (int column = 0; column < 4; column++) {
                double w = CLIP_BOUNDARY * viewProjection.get(3, column);
                double coordinate = viewProjection.get(axis, column);
                planes[plane + column] = w + coordinate;
                planes[plane + 4 + column] = w - coordinate;
            }
        }
        return this;
    }

    /**
     * Проверяет, можно ли отсечь локальный AABB после преобразования M. Для каждой мировой
     * плоскости p вычисляет локальное уравнение {@code p * M}, затем берёт максимальное
     * значение уравнения на AABB. Максимум достигается на вершине, выбранной по знакам
     * коэффициентов X, Y, Z. Если максимум отрицателен, весь AABB лежит за плоскостью.
     *
     * @param bounds границы в координатах узла.
     * @param world  матрица M из координат узла в мировые.
     * @return false, если AABB целиком за одной из плоскостей. true сохраняет объекты на границе
     * и объекты, для которых проверка шести плоскостей не доказала невидимость.
     */
    public boolean intersects(Aabb bounds, Mat4View world) {
        for (int plane = 0; plane < planes.length; plane += 4) {
            double x = localCoefficient(plane, world, 0);
            double y = localCoefficient(plane, world, 1);
            double z = localCoefficient(plane, world, 2);
            double wx = x * (x >= 0.0 ? bounds.maxX() : bounds.minX());
            double wy = y * (y >= 0.0 ? bounds.maxY() : bounds.minY());
            double wz = z * (z >= 0.0 ? bounds.maxZ() : bounds.minZ());
            double d = localCoefficient(plane, world, 3);
            double support = wx + wy + wz + d;
            if (support < 0.0) {
                // Компенсируем округление при сложении вкладов в локальном уравнении.
                double roundoff = 8.0 * Math.ulp(Math.max(Math.max(Math.abs(wx), Math.abs(wy)),
                        Math.max(Math.abs(wz), Math.abs(d))));
                if (support < -roundoff) return false;
            }
        }
        return true;
    }

    /**
     * Умножает мировую плоскость как строку на столбец матрицы узла.
     *
     * @param plane  начало четырёх коэффициентов мировой плоскости в массиве.
     * @param world  матрица из координат узла в мировые.
     * @param column индекс коэффициента локальной плоскости от 0 до 3.
     * @return коэффициент при X, Y, Z или свободный член.
     */
    private double localCoefficient(int plane, Mat4View world, int column) {
        return planes[plane] * world.get(0, column) + planes[plane + 1] * world.get(1, column)
                + planes[plane + 2] * world.get(2, column) + planes[plane + 3] * world.get(3, column);
    }
}
