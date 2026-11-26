package dev.sixik.unigui.api.math;

/**
 * Изменяемый 3D-вектор, пригодный для повторного использования как выходной параметр.
 */
public final class MutableVec3 implements Vec3View {
    /**
     * Компонента X в единицах вектора.
     */
    private float x;
    /**
     * Компонента Y в единицах вектора.
     */
    private float y;
    /**
     * Компонента Z в единицах вектора.
     */
    private float z;

    /**
     * Создаёт нулевой вектор.
     */
    public MutableVec3() {
    }

    /**
     * Создаёт вектор с указанными компонентами.
     *
     * @param x компонента X.
     * @param y компонента Y.
     * @param z компонента Z.
     */
    public MutableVec3(float x, float y, float z) {
        set(x, y, z);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float x() {
        return x;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float y() {
        return y;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float z() {
        return z;
    }

    /**
     * Записывает компоненты вектора.
     *
     * @param x компонента X.
     * @param y компонента Y.
     * @param z компонента Z.
     * @return этот вектор.
     */
    public MutableVec3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Копирует компоненты.
     *
     * @param source исходный вектор.
     * @return этот вектор.
     */
    public MutableVec3 set(Vec3View source) {
        return set(source.x(), source.y(), source.z());
    }

    /**
     * Складывает компоненты этого вектора с приращениями в тех же единицах.
     *
     * @param x приращение по X.
     * @param y приращение по Y.
     * @param z приращение по Z.
     * @return этот вектор.
     */
    public MutableVec3 add(float x, float y, float z) {
        return set(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Записывает векторное произведение a × b, где первая компонента равна
     * {@code a.y * b.z - a.z * b.y}, а две следующие получаются циклической перестановкой осей.
     *
     * @param a первый вектор, может совпадать с выходным.
     * @param b второй вектор, может совпадать с выходным.
     * @return этот вектор.
     */
    public MutableVec3 setCross(Vec3View a, Vec3View b) {
        return set(
                (float) ((double) a.y() * b.z() - (double) a.z() * b.y()),
                (float) ((double) a.z() * b.x() - (double) a.x() * b.z()),
                (float) ((double) a.x() * b.y() - (double) a.y() * b.x()));
    }

    /**
     * Делит компоненты на длину {@code sqrt(x*x + y*y + z*z)}.
     *
     * @return получилось ли нормализовать вектор. Нулевой или не конечный вектор сохраняется.
     */
    public boolean normalize() {
        double length = Math.sqrt((double) x * x + (double) y * y + (double) z * z);
        if (!(length > 0.0) || !Double.isFinite(length)) return false;
        set((float) (x / length), (float) (y / length), (float) (z / length));
        return true;
    }
}
