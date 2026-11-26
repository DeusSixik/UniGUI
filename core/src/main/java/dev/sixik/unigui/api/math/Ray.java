package dev.sixik.unigui.api.math;

/**
 * Изменяемый луч {@code origin + t * direction}, где {@code t >= 0}.
 * Направление единичное, поэтому t измеряется в единицах сцены.
 * Методы пересечения возвращают {@link Double#POSITIVE_INFINITY} при промахе.
 */
public final class Ray {
    /**
     * Начало луча в единицах сцены.
     */
    private final MutableVec3 origin = new MutableVec3();
    /**
     * Безразмерная компонента X единичного направления.
     */
    private double dx;
    /**
     * Безразмерная компонента Y единичного направления.
     */
    private double dy;
    /**
     * Безразмерная компонента Z единичного направления.
     */
    private double dz = -1.0;

    /**
     * Создаёт луч из начала координат вдоль −Z.
     */
    public Ray() {
    }

    /**
     * Задаёт луч и нормализует направление делением на длину. Сначала компоненты
     * делятся на наибольшую по модулю, чтобы квадрат длины оставался конечным.
     *
     * @param origin начало в единицах сцены.
     * @param x      компонента X направления.
     * @param y      компонента Y направления.
     * @param z      компонента Z направления.
     * @return удалось ли записать луч. При нулевой длине или не конечном аргументе прежний луч сохраняется.
     */
    public boolean set(Vec3View origin, double x, double y, double z) {
        float ox = origin.x(), oy = origin.y(), oz = origin.z();
        double largest = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
        if (!(largest > 0.0) || !Double.isFinite(largest)
                || !Float.isFinite(ox) || !Float.isFinite(oy) || !Float.isFinite(oz)) {
            return false;
        }
        double sx = x / largest, sy = y / largest, sz = z / largest;
        double length = Math.sqrt(sx * sx + sy * sy + sz * sz);
        this.origin.set(ox, oy, oz);
        dx = sx / length;
        dy = sy / length;
        dz = sz / length;
        return true;
    }

    /**
     * Копирует начало луча.
     *
     * @param out переиспользуемый вектор в единицах сцены.
     * @return out.
     */
    public MutableVec3 origin(MutableVec3 out) {
        return out.set(origin);
    }

    /**
     * Копирует единичное направление.
     *
     * @param out переиспользуемый безразмерный вектор.
     * @return out.
     */
    public MutableVec3 direction(MutableVec3 out) {
        return out.set((float) dx, (float) dy, (float) dz);
    }

    /**
     * Вычисляет точку {@code origin + t * direction}.
     *
     * @param t   расстояние от начала луча в единицах сцены. Отрицательное значение продолжает прямую назад.
     * @param out переиспользуемый вектор в единицах сцены.
     * @return out.
     */
    public MutableVec3 pointAt(double t, MutableVec3 out) {
        return out.set((float) (origin.x() + t * dx), (float) (origin.y() + t * dy),
                (float) (origin.z() + t * dz));
    }

    /**
     * Пересекает луч с замкнутым AABB методом интервалов. По каждой оси грани дают
     * интервал между {@code (min - origin) / direction} и {@code (max - origin) / direction}.
     * Искомое t — начало пересечения трёх интервалов с {@code [0, +∞)}.
     * При нулевой компоненте направления начало луча должно лежать между гранями этой оси.
     *
     * @param min нижние границы параллелепипеда в единицах сцены.
     * @param max верхние границы параллелепипеда в единицах сцены.
     * @return расстояние до входа, 0 внутри или на границе, +∞ при промахе или недопустимых границах.
     */
    public double intersectAabb(Vec3View min, Vec3View max) {
        return intersectAabb(min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    /**
     * Пересекает замкнутый AABB, заданный компонентами двух углов.
     * Использует тот же метод интервалов, что и {@link #intersectAabb(Vec3View, Vec3View)}.
     *
     * @param minX минимальная координата X.
     * @param minY минимальная координата Y.
     * @param minZ минимальная координата Z.
     * @param maxX максимальная координата X.
     * @param maxY максимальная координата Y.
     * @param maxZ максимальная координата Z.
     * @return расстояние до входа, ноль внутри или +∞ при промахе.
     */
    public double intersectAabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        double enter = 0.0;
        double exit = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            float lower = axis == 0 ? minX : axis == 1 ? minY : minZ;
            float upper = axis == 0 ? maxX : axis == 1 ? maxY : maxZ;
            if (!(lower <= upper) || !Float.isFinite(lower) || !Float.isFinite(upper)) {
                return Double.POSITIVE_INFINITY;
            }
            double start = component(origin, axis);
            double direction = axis == 0 ? dx : axis == 1 ? dy : dz;
            if (direction == 0.0) {
                if (start < lower || start > upper) return Double.POSITIVE_INFINITY;
            } else {
                double first = (lower - start) / direction;
                double second = (upper - start) / direction;
                enter = Math.max(enter, Math.min(first, second));
                exit = Math.min(exit, Math.max(first, second));
                if (enter > exit) return Double.POSITIVE_INFINITY;
            }
        }
        return enter;
    }

    /**
     * Пересекает луч с обеими сторонами треугольника методом Мёллера — Трумбора.
     * Для рёбер {@code e1 = b - a}, {@code e2 = c - a} находит решение
     * {@code origin + t*d = a + u*e1 + v*e2}. При {@code p = d × e2}, {@code s = origin - a}
     * и {@code q = s × e1} барицентрические координаты равны {@code u = dot(s,p)/dot(e1,p)}
     * и {@code v = dot(d,q)/dot(e1,p)}, а {@code t = dot(e2,q)/dot(e1,p)}.
     * Точка принадлежит треугольнику при {@code u >= 0, v >= 0, u + v <= 1}.
     *
     * @param a первая вершина в единицах сцены.
     * @param b вторая вершина в единицах сцены.
     * @param c третья вершина в единицах сцены.
     * @return расстояние до пересечения или +∞ при промахе, параллельности или вырожденном треугольнике.
     */
    public double intersectTriangle(Vec3View a, Vec3View b, Vec3View c) {
        double ax = a.x(), ay = a.y(), az = a.z();
        double e1x = b.x() - ax, e1y = b.y() - ay, e1z = b.z() - az;
        double e2x = c.x() - ax, e2y = c.y() - ay, e2z = c.z() - az;
        double px = dy * e2z - dz * e2y;
        double py = dz * e2x - dx * e2z;
        double pz = dx * e2y - dy * e2x;
        double det = e1x * px + e1y * py + e1z * pz;
        if (det == 0.0 || !Double.isFinite(det)) return Double.POSITIVE_INFINITY;

        double sx = origin.x() - ax, sy = origin.y() - ay, sz = origin.z() - az;
        double u = (sx * px + sy * py + sz * pz) / det;
        if (!(u >= 0.0 && u <= 1.0)) return Double.POSITIVE_INFINITY;
        double qx = sy * e1z - sz * e1y;
        double qy = sz * e1x - sx * e1z;
        double qz = sx * e1y - sy * e1x;
        double v = (dx * qx + dy * qy + dz * qz) / det;
        if (!(v >= 0.0 && u + v <= 1.0)) return Double.POSITIVE_INFINITY;
        double t = (e2x * qx + e2y * qy + e2z * qz) / det;
        return t >= 0.0 && Double.isFinite(t) ? t : Double.POSITIVE_INFINITY;
    }

    /**
     * Пересекает луч с плоскостью по формуле {@code t = dot(point - origin, n) / dot(d, n)},
     * где n — нормаль плоскости, а d — единичное направление луча.
     *
     * @param point  точка плоскости в единицах сцены.
     * @param normal конечная ненулевая нормаль, произвольной длины.
     * @return расстояние до пересечения или +∞, если пересечение позади, параллельно или не определено.
     */
    public double intersectPlane(Vec3View point, Vec3View normal) {
        double nx = normal.x(), ny = normal.y(), nz = normal.z();
        double denominator = dx * nx + dy * ny + dz * nz;
        if (denominator == 0.0 || !Double.isFinite(denominator)) return Double.POSITIVE_INFINITY;
        double t = ((point.x() - (double) origin.x()) * nx + (point.y() - (double) origin.y()) * ny
                + (point.z() - (double) origin.z()) * nz) / denominator;
        return t >= 0.0 && Double.isFinite(t) ? t : Double.POSITIVE_INFINITY;
    }

    /**
     * Читает компоненту вектора по числовому индексу оси.
     *
     * @param vector входной вектор.
     * @param axis   индекс 0, 1 или 2.
     * @return компонента X, Y или Z соответственно.
     */
    private static float component(Vec3View vector, int axis) {
        return axis == 0 ? vector.x() : axis == 1 ? vector.y() : vector.z();
    }
}
