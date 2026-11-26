package dev.sixik.unigui.api.math;

import java.util.Objects;

/**
 * Изменяемая матрица 4×4 для векторов-столбцов. Построение, композиция и инверсия
 * переиспользуют память результата. Проекции используют правую систему координат, направление
 * взгляда −Z и диапазон глубины NDC от −1 до 1.
 */
public final class MutableMat4 implements Mat4View {
    /**
     * 16 элементов в порядке столбцов. Единицы задаёт преобразование.
     */
    private final double[] elements = new double[16];

    /**
     * Создаёт единичную матрицу.
     */
    public MutableMat4() {
        identity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double get(int row, int column) {
        return elements[Objects.checkIndex(column, 4) * 4 + Objects.checkIndex(row, 4)];
    }

    /**
     * Записывает единицы на диагонали и нули в остальных элементах.
     *
     * @return эта матрица.
     */
    public MutableMat4 identity() {
        return setEntries(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
    }

    /**
     * Копирует исходную матрицу.
     *
     * @param source матрица, может совпадать с этой.
     * @return эта матрица.
     */
    public MutableMat4 set(Mat4View source) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                elements[column * 4 + row] = source.get(row, column);
            }
        }
        return this;
    }

    /**
     * Копирует элементы в порядке столбцов, приводя их к float для передачи рендереру.
     *
     * @param out    массив с местом для 16 последовательных значений.
     * @param offset индекс записи первого элемента.
     * @throws IndexOutOfBoundsException если диапазон выходит за границы массива.
     */
    public void copyColumnMajor(float[] out, int offset) {
        Objects.checkFromIndexSize(offset, 16, out.length);
        for (int i = 0; i < 16; i++) {
            out[offset + i] = (float) elements[i];
        }
    }

    /**
     * Записывает {@code T * R * S}, где S масштабирует каждую локальную ось,
     * R вращает вокруг начала координат, а T переносит в родительскую систему.
     *
     * @param position перенос в единицах сцены.
     * @param rotation единичный кватернион активного вращения.
     * @param scale    безразмерные коэффициенты масштаба по локальным осям.
     * @return эта матрица.
     */
    public MutableMat4 setTransform(Vec3View position, QuatView rotation, Vec3View scale) {
        setRotation(rotation.x(), rotation.y(), rotation.z(), rotation.w());
        for (int row = 0; row < 3; row++) {
            elements[row] *= scale.x();
            elements[4 + row] *= scale.y();
            elements[8 + row] *= scale.z();
        }
        elements[12] = position.x();
        elements[13] = position.y();
        elements[14] = position.z();
        return this;
    }

    /**
     * Записывает обратное преобразование позы {@code R^-1 * T(-eye)}.
     * Для единичного кватерниона R^-1 задаётся сопряжением, а столбец переноса равен {@code -R^-1 * eye}.
     *
     * @param eye         положение камеры в единицах сцены.
     * @param orientation единичный кватернион, переводящий локальные оси камеры в мировые.
     * @return эта матрица.
     */
    public MutableMat4 setView(Vec3View eye, QuatView orientation) {
        setRotation(-orientation.x(), -orientation.y(), -orientation.z(), orientation.w());
        for (int row = 0; row < 3; row++) {
            elements[12 + row] = -transformComponent(row, eye.x(), eye.y(), eye.z(), 0.0);
        }
        return this;
    }

    /**
     * Записывает симметричную перспективную проекцию. При {@code f = 1/tan(fovY/2)}
     * масштабы равны {@code f/aspect} и {@code f}, а {@code clip.w = -view.z}.
     * Коэффициенты глубины {@code -(far+near)/(far-near)} и {@code -2*far*near/(far-near)}
     * переводят плоскости {@code z = -near} и {@code z = -far} в NDC −1 и 1.
     *
     * @param fovY   вертикальный угол поля зрения в радианах, строго между 0 и π.
     * @param aspect положительное отношение ширины к высоте.
     * @param near   положительное расстояние до ближней плоскости в единицах сцены.
     * @param far    расстояние до дальней плоскости в единицах сцены, больше near.
     * @return эта матрица.
     * @throws IllegalArgumentException если аргументы вне указанных диапазонов или проекция не конечна.
     */
    public MutableMat4 setPerspective(double fovY, double aspect, double near, double far) {
        checkProjection(aspect, near, far);
        double f = 1.0 / Math.tan(fovY * 0.5);
        double scaleX = f / aspect;
        double nearRatio = near / (far - near);
        double depthScale = -1.0 - 2.0 * nearRatio;
        double depthOffset = -2.0 * near * (far / (far - near));
        if (!(fovY > 0.0 && fovY < Math.PI) || !Double.isFinite(f) || !(scaleX > 0.0) || !Double.isFinite(scaleX)
                || !Double.isFinite(depthScale) || !Double.isFinite(depthOffset)) {
            throw new IllegalArgumentException("Invalid perspective projection");
        }
        return setEntries(scaleX, 0, 0, 0, 0, f, 0, 0,
                0, 0, depthScale, -1, 0, 0, depthOffset, 0);
    }

    /**
     * Записывает симметричную ортографическую проекцию.
     * Масштабы X и Y равны {@code 2/(height*aspect)} и {@code 2/height}.
     * Глубина изменяется линейно по формуле {@code zNdc = -2*(view.z+near)/(far-near) - 1}.
     *
     * @param height положительная высота видимой области в единицах сцены.
     * @param aspect положительное отношение ширины к высоте.
     * @param near   положительное расстояние до ближней плоскости в единицах сцены.
     * @param far    расстояние до дальней плоскости в единицах сцены, больше near.
     * @return эта матрица.
     * @throws IllegalArgumentException если аргументы вне указанных диапазонов или проекция не конечна.
     */
    public MutableMat4 setOrthographic(double height, double aspect, double near, double far) {
        checkProjection(aspect, near, far);
        double scaleY = 2.0 / height;
        double scaleX = scaleY / aspect;
        double scaleZ = -2.0 / (far - near);
        double depthOffset = -1.0 + scaleZ * near;
        if (!(height > 0.0) || !Double.isFinite(height) || !(scaleX > 0.0)
                || !Double.isFinite(scaleX) || !Double.isFinite(scaleY)
                || !Double.isFinite(scaleZ) || !Double.isFinite(depthOffset)) {
            throw new IllegalArgumentException("Invalid orthographic projection");
        }
        return setEntries(scaleX, 0, 0, 0, 0, scaleY, 0, 0,
                0, 0, scaleZ, 0, 0, 0, depthOffset, 1);
    }

    /**
     * Записывает произведение {@code left * right}. Элемент (r,c) равен сумме
     * {@code left[r,k] * right[k,c]} по k от 0 до 3.
     *
     * @param left  левый множитель, может совпадать с результатом.
     * @param right правый множитель, может совпадать с результатом.
     * @return эта матрица.
     */
    public MutableMat4 setProduct(Mat4View left, Mat4View right) {
        return setEntries(
                dot(left, right, 0, 0), dot(left, right, 1, 0), dot(left, right, 2, 0), dot(left, right, 3, 0),
                dot(left, right, 0, 1), dot(left, right, 1, 1), dot(left, right, 2, 1), dot(left, right, 3, 1),
                dot(left, right, 0, 2), dot(left, right, 1, 2), dot(left, right, 2, 2), dot(left, right, 3, 2),
                dot(left, right, 0, 3), dot(left, right, 1, 3), dot(left, right, 2, 3), dot(left, right, 3, 3));
    }

    /**
     * Записывает обратную матрицу {@code adj(A)/det(A)}. Элементы adj(A) —
     * транспонированные миноры 3×3 со знаком {@code (-1)^(row+column)}. При разложении этих
     * миноров по одной строке переиспользуются миноры 2×2 верхней (s) и нижней (c) пары строк.
     *
     * @param source обратимая матрица A, может совпадать с результатом.
     * @return удалось ли вычислить конечную обратную матрицу. При отказе прежние элементы сохраняются.
     */
    public boolean setInverse(Mat4View source) {
        double a00 = source.get(0, 0), a01 = source.get(0, 1), a02 = source.get(0, 2), a03 = source.get(0, 3);
        double a10 = source.get(1, 0), a11 = source.get(1, 1), a12 = source.get(1, 2), a13 = source.get(1, 3);
        double a20 = source.get(2, 0), a21 = source.get(2, 1), a22 = source.get(2, 2), a23 = source.get(2, 3);
        double a30 = source.get(3, 0), a31 = source.get(3, 1), a32 = source.get(3, 2), a33 = source.get(3, 3);

        double s0 = a00 * a11 - a10 * a01;
        double s1 = a00 * a12 - a10 * a02;
        double s2 = a00 * a13 - a10 * a03;
        double s3 = a01 * a12 - a11 * a02;
        double s4 = a01 * a13 - a11 * a03;
        double s5 = a02 * a13 - a12 * a03;
        double c0 = a20 * a31 - a30 * a21;
        double c1 = a20 * a32 - a30 * a22;
        double c2 = a20 * a33 - a30 * a23;
        double c3 = a21 * a32 - a31 * a22;
        double c4 = a21 * a33 - a31 * a23;
        double c5 = a22 * a33 - a32 * a23;

        double det = s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0;
        if (det == 0.0 || !Double.isFinite(det)) return false;

        double r00 = (a11 * c5 - a12 * c4 + a13 * c3) / det;
        double r01 = (-a01 * c5 + a02 * c4 - a03 * c3) / det;
        double r02 = (a31 * s5 - a32 * s4 + a33 * s3) / det;
        double r03 = (-a21 * s5 + a22 * s4 - a23 * s3) / det;
        double r10 = (-a10 * c5 + a12 * c2 - a13 * c1) / det;
        double r11 = (a00 * c5 - a02 * c2 + a03 * c1) / det;
        double r12 = (-a30 * s5 + a32 * s2 - a33 * s1) / det;
        double r13 = (a20 * s5 - a22 * s2 + a23 * s1) / det;
        double r20 = (a10 * c4 - a11 * c2 + a13 * c0) / det;
        double r21 = (-a00 * c4 + a01 * c2 - a03 * c0) / det;
        double r22 = (a30 * s4 - a31 * s2 + a33 * s0) / det;
        double r23 = (-a20 * s4 + a21 * s2 - a23 * s0) / det;
        double r30 = (-a10 * c3 + a11 * c1 - a12 * c0) / det;
        double r31 = (a00 * c3 - a01 * c1 + a02 * c0) / det;
        double r32 = (-a30 * s3 + a31 * s1 - a32 * s0) / det;
        double r33 = (a20 * s3 - a21 * s1 + a22 * s0) / det;

        if (!Double.isFinite(r00) || !Double.isFinite(r01) || !Double.isFinite(r02) || !Double.isFinite(r03)
                || !Double.isFinite(r10) || !Double.isFinite(r11) || !Double.isFinite(r12) || !Double.isFinite(r13)
                || !Double.isFinite(r20) || !Double.isFinite(r21) || !Double.isFinite(r22) || !Double.isFinite(r23)
                || !Double.isFinite(r30) || !Double.isFinite(r31) || !Double.isFinite(r32) || !Double.isFinite(r33)) {
            return false;
        }
        setEntries(r00, r10, r20, r30, r01, r11, r21, r31, r02, r12, r22, r32, r03, r13, r23, r33);
        return true;
    }

    /**
     * Умножает строку матрицы на однородный вектор {@code (x, y, z, w)}.
     *
     * @param row индекс результирующей компоненты от 0 до 3.
     * @param x   первая компонента входного вектора.
     * @param y   вторая компонента входного вектора.
     * @param z   третья компонента входного вектора.
     * @param w   четвёртая компонента, обычно 1 для точки и 0 для направления.
     * @return скалярное произведение строки и вектора.
     * @throws IndexOutOfBoundsException если индекс вне диапазона.
     */
    public double transformComponent(int row, double x, double y, double z, double w) {
        return elements[Objects.checkIndex(row, 4)] * x + elements[row + 4] * y
                + elements[row + 8] * z + elements[row + 12] * w;
    }

    /**
     * Преобразует точку формулой {@code out = (M * (point, 1)).xyz / (M * (point, 1)).w}.
     *
     * @param point исходная точка, может совпадать с out.
     * @param out   выходная точка в системе координат матрицы.
     * @return true при конечном результате. При нулевом w или недопустимом результате out сохраняется.
     */
    public boolean transformPoint(Vec3View point, MutableVec3 out) {
        double x = point.x(), y = point.y(), z = point.z();
        double w = transformComponent(3, x, y, z, 1.0);
        if (w == 0.0 || !Double.isFinite(w)) return false;
        float px = (float) (transformComponent(0, x, y, z, 1.0) / w);
        float py = (float) (transformComponent(1, x, y, z, 1.0) / w);
        float pz = (float) (transformComponent(2, x, y, z, 1.0) / w);
        if (!Float.isFinite(px) || !Float.isFinite(py) || !Float.isFinite(pz)) return false;
        out.set(px, py, pz);
        return true;
    }

    /**
     * Записывает ортонормальный базис из кватерниона. Столбцы — результат вращения
     * единичных осей формулой {@code v + 2*w*(q.xyz × v) + 2*q.xyz × (q.xyz × v)}.
     * Масштаб 2 делится на квадрат длины q, чтобы компенсировать округление его float-компонент.
     *
     * @param x первая компонента векторной части q.
     * @param y вторая компонента векторной части q.
     * @param z третья компонента векторной части q.
     * @param w скалярная часть q.
     */
    private void setRotation(double x, double y, double z, double w) {
        double factor = 2.0 / (x * x + y * y + z * z + w * w);
        double xx = factor * x * x, yy = factor * y * y, zz = factor * z * z;
        double xy = factor * x * y, xz = factor * x * z, yz = factor * y * z;
        double wx = factor * w * x, wy = factor * w * y, wz = factor * w * z;
        setEntries(1 - yy - zz, xy + wz, xz - wy, 0,
                xy - wz, 1 - xx - zz, yz + wx, 0,
                xz + wy, yz - wx, 1 - xx - yy, 0,
                0, 0, 0, 1);
    }

    /**
     * Проверяет общий для двух проекций диапазон.
     *
     * @param aspect отношение ширины к высоте.
     * @param near   ближняя плоскость в единицах сцены.
     * @param far    дальняя плоскость в единицах сцены.
     * @throws IllegalArgumentException если значения не конечны или нарушено {@code aspect > 0, 0 < near < far}.
     */
    private static void checkProjection(double aspect, double near, double far) {
        if (!(aspect > 0.0 && near > 0.0 && far > near) || !Double.isFinite(aspect) || !Double.isFinite(far)) {
            throw new IllegalArgumentException("Projection requires a positive aspect ratio and 0 < near < far");
        }
    }

    /**
     * Вычисляет один элемент произведения матриц.
     *
     * @param left   левый множитель.
     * @param right  правый множитель.
     * @param row    строка левой матрицы.
     * @param column столбец правой матрицы.
     * @return скалярное произведение строки и столбца.
     */
    private static double dot(Mat4View left, Mat4View right, int row, int column) {
        return left.get(row, 0) * right.get(0, column) + left.get(row, 1) * right.get(1, column)
                + left.get(row, 2) * right.get(2, column) + left.get(row, 3) * right.get(3, column);
    }

    /**
     * Фиксирует все элементы после вычисления аргументов, в том числе при записи поверх источника.
     *
     * @param m00 элемент строки 0 столбца 0.
     * @param m10 элемент строки 1 столбца 0.
     * @param m20 элемент строки 2 столбца 0.
     * @param m30 элемент строки 3 столбца 0.
     * @param m01 элемент строки 0 столбца 1.
     * @param m11 элемент строки 1 столбца 1.
     * @param m21 элемент строки 2 столбца 1.
     * @param m31 элемент строки 3 столбца 1.
     * @param m02 элемент строки 0 столбца 2.
     * @param m12 элемент строки 1 столбца 2.
     * @param m22 элемент строки 2 столбца 2.
     * @param m32 элемент строки 3 столбца 2.
     * @param m03 элемент строки 0 столбца 3.
     * @param m13 элемент строки 1 столбца 3.
     * @param m23 элемент строки 2 столбца 3.
     * @param m33 элемент строки 3 столбца 3.
     * @return эта матрица.
     */
    private MutableMat4 setEntries(double m00, double m10, double m20, double m30,
                                   double m01, double m11, double m21, double m31,
                                   double m02, double m12, double m22, double m32,
                                   double m03, double m13, double m23, double m33) {
        elements[0] = m00;
        elements[1] = m10;
        elements[2] = m20;
        elements[3] = m30;
        elements[4] = m01;
        elements[5] = m11;
        elements[6] = m21;
        elements[7] = m31;
        elements[8] = m02;
        elements[9] = m12;
        elements[10] = m22;
        elements[11] = m32;
        elements[12] = m03;
        elements[13] = m13;
        elements[14] = m23;
        elements[15] = m33;
        return this;
    }

    /**
     * Заменяет одну компоненту столбца переноса.
     *
     * @param row   строка 0, 1 или 2 (X, Y, Z).
     * @param value новое значение.
     * @return эта матрица.
     * @throws IndexOutOfBoundsException если строка вне 0..2.
     */
    public MutableMat4 setTranslationComponent(int row, double value) {
        if (row < 0 || row > 2) throw new IndexOutOfBoundsException("Translation row must be 0..2: " + row);
        elements[12 + row] = value;
        return this;
    }
}
