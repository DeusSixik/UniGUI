package dev.sixik.unigui.api.math;

/**
 * Изменяемый единичный кватернион активного вращения в правой системе координат.
 */
public final class MutableQuat implements QuatView {
    /**
     * Компонента X векторной части, безразмерная.
     */
    private float x;
    /**
     * Компонента Y векторной части, безразмерная.
     */
    private float y;
    /**
     * Компонента Z векторной части, безразмерная.
     */
    private float z;
    /**
     * Скалярная часть, безразмерная.
     */
    private float w = 1.0f;

    /**
     * Создаёт кватернион без вращения.
     */
    public MutableQuat() {
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
     * {@inheritDoc}
     */
    @Override
    public float w() {
        return w;
    }

    /**
     * Записывает компоненты, деля их на длину {@code sqrt(x*x + y*y + z*z + w*w)}.
     *
     * @param x компонента X векторной части.
     * @param y компонента Y векторной части.
     * @param z компонента Z векторной части.
     * @param w скалярная часть.
     * @return этот кватернион.
     * @throws IllegalArgumentException если длина равна нулю или не конечна.
     */
    public MutableQuat set(float x, float y, float z, float w) {
        double length = Math.sqrt((double) x * x + (double) y * y + (double) z * z + (double) w * w);
        if (!(length > 0.0) || !Double.isFinite(length)) {
            throw new IllegalArgumentException("Quaternion must have a finite nonzero length");
        }
        this.x = (float) (x / length);
        this.y = (float) (y / length);
        this.z = (float) (z / length);
        this.w = (float) (w / length);
        return this;
    }

    /**
     * Копирует единичный кватернион.
     *
     * @param source исходное вращение.
     * @return этот кватернион.
     */
    public MutableQuat set(QuatView source) {
        x = source.x();
        y = source.y();
        z = source.z();
        w = source.w();
        return this;
    }

    /**
     * Записывает вращение {@code q = (axis / |axis| * sin(angle/2), cos(angle/2))}.
     *
     * @param axis  конечный ненулевой вектор оси.
     * @param angle конечный угол в радианах.
     * @return этот кватернион.
     * @throws IllegalArgumentException если ось нулевая или аргументы содержат не конечные числа.
     */
    public MutableQuat setAxisAngle(Vec3View axis, double angle) {
        double ax = axis.x();
        double ay = axis.y();
        double az = axis.z();
        double length = Math.sqrt(ax * ax + ay * ay + az * az);
        if (!(length > 0.0) || !Double.isFinite(length) || !Double.isFinite(angle)) {
            throw new IllegalArgumentException("Rotation requires a finite nonzero axis and a finite angle");
        }
        double scale = Math.sin(angle * 0.5) / length;
        return set((float) (ax * scale), (float) (ay * scale), (float) (az * scale),
                (float) Math.cos(angle * 0.5));
    }

    /**
     * Записывает {@code qY(yaw) * qX(pitch)}, где qY и qX задают вращения вокруг указанных осей.
     *
     * @param yaw   конечный угол вокруг мировой оси Y в радианах.
     * @param pitch конечный угол вокруг локальной оси X в радианах.
     * @return этот кватернион.
     * @throws IllegalArgumentException если любой угол не конечен.
     */
    public MutableQuat setYawPitch(double yaw, double pitch) {
        double sy = Math.sin(yaw * 0.5);
        double cy = Math.cos(yaw * 0.5);
        double sx = Math.sin(pitch * 0.5);
        double cx = Math.cos(pitch * 0.5);
        return set((float) (cy * sx), (float) (sy * cx), (float) (-sy * sx), (float) (cy * cx));
    }

    /**
     * Записывает произведение Гамильтона {@code a * b}, которое применяет b, затем a.
     * Векторная часть равна {@code a.w*b.xyz + b.w*a.xyz + a.xyz × b.xyz}, скалярная равна
     * {@code a.w*b.w - dot(a.xyz, b.xyz)}.
     *
     * @param a внешнее вращение, может совпадать с выходным.
     * @param b внутреннее вращение, может совпадать с выходным.
     * @return этот кватернион.
     */
    public MutableQuat setProduct(QuatView a, QuatView b) {
        double ax = a.x(), ay = a.y(), az = a.z(), aw = a.w();
        double bx = b.x(), by = b.y(), bz = b.z(), bw = b.w();
        return set((float) (aw * bx + bw * ax + ay * bz - az * by),
                (float) (aw * by + bw * ay + az * bx - ax * bz),
                (float) (aw * bz + bw * az + ax * by - ay * bx),
                (float) (aw * bw - ax * bx - ay * by - az * bz));
    }

    /**
     * Вращает вектор по формуле {@code v + w*t + q.xyz × t}, где {@code t = 2*(q.xyz × v)/|q|²}.
     * Деление на квадрат длины q компенсирует округление его float-компонент.
     *
     * @param vx  компонента X входного вектора v.
     * @param vy  компонента Y входного вектора v.
     * @param vz  компонента Z входного вектора v.
     * @param out переиспользуемый вектор для результата в единицах v.
     * @return out.
     */
    public MutableVec3 rotate(float vx, float vy, float vz, MutableVec3 out) {
        double factor = 2.0 / ((double) x * x + (double) y * y + (double) z * z + (double) w * w);
        double tx = factor * ((double) y * vz - (double) z * vy);
        double ty = factor * ((double) z * vx - (double) x * vz);
        double tz = factor * ((double) x * vy - (double) y * vx);
        return out.set((float) (vx + w * tx + y * tz - z * ty),
                (float) (vy + w * ty + z * tx - x * tz),
                (float) (vz + w * tz + x * ty - y * tx));
    }
}
