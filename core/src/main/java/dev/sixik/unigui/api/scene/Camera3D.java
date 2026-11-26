package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.QuatView;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.Vec3View;

/**
 * Камера правой системы координат. В нейтральной позе она смотрит вдоль −Z, верх направлен вдоль +Y.
 * Координаты экрана локальны относительно верхнего левого угла viewport, X растёт вправо, Y вниз.
 * Матрицы используют глубину NDC от −1 до 1, методы экрана используют глубину от 0 до 1.
 */
public final class Camera3D {
    /**
     * Мировая позиция камеры в единицах сцены.
     */
    private final MutableVec3 position = new MutableVec3();
    /**
     * Единичное вращение из координат камеры в мировые.
     */
    private final MutableQuat orientation = new MutableQuat();
    /**
     * Отображение из мировых координат в координаты камеры.
     */
    private final MutableMat4 view = new MutableMat4();
    /**
     * Отображение из координат камеры в clip space.
     */
    private final MutableMat4 projection = new MutableMat4();
    /**
     * Кэш произведения projection * view.
     */
    private final MutableMat4 viewProjection = new MutableMat4();
    /**
     * Переиспользуемая мировая точка на ближней плоскости для построения луча.
     */
    private final MutableVec3 rayOrigin = new MutableVec3();

    /**
     * Ширина viewport в логических единицах UI, больше нуля.
     */
    private float width = 1.0f;
    /**
     * Высота viewport в логических единицах UI, больше нуля.
     */
    private float height = 1.0f;
    /**
     * Вертикальный угол перспективы в радианах, от 0 до π.
     */
    private float fovY = (float) (Math.PI / 3.0);
    /**
     * Высота ортографического объёма в единицах сцены, больше нуля.
     */
    private float orthographicHeight = 2.0f;
    /**
     * Расстояние до ближней плоскости в единицах сцены, больше нуля.
     */
    private float near = 0.1f;
    /**
     * Расстояние до дальней плоскости в единицах сцены, больше near.
     */
    private float far = 1000.0f;
    /**
     * Выбрана ли ортографическая проекция.
     */
    private boolean orthographic;
    /**
     * Требуется ли пересчитать прямые матрицы.
     */
    private boolean matricesDirty = true;
    /**
     * Номер изменения позы, проекции или viewport, начиная с нуля.
     */
    private long revision;

    /**
     * Создаёт перспективную камеру в начале координат с вертикальным углом 60° и viewport 1×1.
     */
    public Camera3D() {
    }

    /**
     * Читает версию состояния камеры для проверки актуальности кадра.
     *
     * @return счётчик изменений позы, проекции и viewport.
     */
    public long revision() {
        return revision;
    }

    /**
     * Копирует мировую позицию камеры.
     *
     * @param out переиспользуемый выход в единицах сцены.
     * @return out.
     */
    public MutableVec3 position(MutableVec3 out) {
        return out.set(position);
    }

    /**
     * Копирует вращение камеры.
     *
     * @param out переиспользуемый выход.
     * @return out с вращением из координат камеры в мировые.
     */
    public MutableQuat orientation(MutableQuat out) {
        return out.set(orientation);
    }

    /**
     * Задаёт позицию и вращение камеры одним изменением.
     *
     * @param x        конечная мировая координата X в единицах сцены.
     * @param y        конечная мировая координата Y в единицах сцены.
     * @param z        конечная мировая координата Z в единицах сцены.
     * @param rotation единичное вращение из координат камеры в мировые.
     * @return эта камера.
     * @throws IllegalArgumentException если позиция не конечна.
     */
    public Camera3D setPose(float x, float y, float z, QuatView rotation) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Camera position must be finite");
        }
        if (position.x() == x && position.y() == y && position.z() == z
                && orientation.x() == rotation.x() && orientation.y() == rotation.y()
                && orientation.z() == rotation.z() && orientation.w() == rotation.w()) {
            return this;
        }
        orientation.set(rotation);
        position.set(x, y, z);
        changed();
        return this;
    }

    /**
     * Перемещает камеру с сохранением ориентации.
     *
     * @param x конечная мировая координата X в единицах сцены.
     * @param y конечная мировая координата Y в единицах сцены.
     * @param z конечная мировая координата Z в единицах сцены.
     * @return эта камера.
     * @throws IllegalArgumentException если позиция не конечна.
     */
    public Camera3D setPosition(float x, float y, float z) {
        return setPose(x, y, z, orientation);
    }

    /**
     * Задаёт размер viewport для преобразований экрана и расчёта пропорций проекции.
     *
     * @param width  конечная положительная ширина в логических единицах UI.
     * @param height конечная положительная высота в логических единицах UI.
     * @return эта камера.
     * @throws IllegalArgumentException если размер не конечен или не положителен.
     */
    public Camera3D setViewport(float width, float height) {
        if (!(width > 0.0f && height > 0.0f) || !Float.isFinite(width) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("Viewport dimensions must be finite and positive");
        }
        if (this.width == width && this.height == height) return this;
        this.width = width;
        this.height = height;
        changed();
        return this;
    }

    /**
     * Выбирает перспективную проекцию.
     *
     * @param fovY вертикальный угол в радианах, строго между 0 и π.
     * @param near ближняя плоскость на положительном расстоянии в единицах сцены.
     * @param far  конечное расстояние до дальней плоскости, больше near.
     * @return эта камера.
     * @throws IllegalArgumentException если нарушены указанные диапазоны.
     */
    public Camera3D setPerspective(float fovY, float near, float far) {
        checkClipPlanes(near, far);
        if (!(fovY > 0.0f && fovY < Math.PI)) throw new IllegalArgumentException("Expected 0 < fovY < pi");
        if (!orthographic && this.fovY == fovY && this.near == near && this.far == far) return this;
        this.fovY = fovY;
        this.near = near;
        this.far = far;
        orthographic = false;
        changed();
        return this;
    }

    /**
     * Выбирает ортографическую проекцию.
     *
     * @param viewHeight конечная положительная высота видимой области в единицах сцены.
     * @param near       ближняя плоскость на положительном расстоянии в единицах сцены.
     * @param far        конечное расстояние до дальней плоскости, больше near.
     * @return эта камера.
     * @throws IllegalArgumentException если нарушены указанные диапазоны.
     */
    public Camera3D setOrthographic(float viewHeight, float near, float far) {
        checkClipPlanes(near, far);
        if (!(viewHeight > 0.0f) || !Float.isFinite(viewHeight)) {
            throw new IllegalArgumentException("Orthographic height must be finite and positive");
        }
        if (orthographic && orthographicHeight == viewHeight && this.near == near && this.far == far) return this;
        orthographicHeight = viewHeight;
        this.near = near;
        this.far = far;
        orthographic = true;
        changed();
        return this;
    }

    /**
     * Читает режим проекции.
     *
     * @return true для ортографической проекции, false для перспективной.
     */
    public boolean orthographic() {
        return orthographic;
    }

    /**
     * Читает настроенную высоту ортографической проекции.
     *
     * @return высота в единицах сцены.
     */
    public float orthographicHeight() {
        return orthographicHeight;
    }

    /**
     * Читает положение ближней плоскости.
     *
     * @return расстояние от камеры в единицах сцены.
     */
    public float near() {
        return near;
    }

    /**
     * Читает положение дальней плоскости.
     *
     * @return расстояние от камеры в единицах сцены.
     */
    public float far() {
        return far;
    }

    /**
     * Копирует матрицу вида.
     *
     * @param out переиспользуемая матрица результата.
     * @return out с преобразованием из мировых координат в координаты камеры.
     */
    public MutableMat4 view(MutableMat4 out) {
        updateMatrices();
        return out.set(view);
    }

    /**
     * Копирует матрицу проекции.
     *
     * @param out переиспользуемая матрица результата.
     * @return out с преобразованием из координат камеры в clip space.
     */
    public MutableMat4 projection(MutableMat4 out) {
        updateMatrices();
        return out.set(projection);
    }

    /**
     * Копирует {@code projection * view}.
     *
     * @param out переиспользуемая матрица результата.
     * @return out с преобразованием из мировых координат в clip space.
     */
    public MutableMat4 viewProjection(MutableMat4 out) {
        updateMatrices();
        return out.set(viewProjection);
    }

    /**
     * Проецирует точку в координаты viewport. После деления clip xyz на clip w
     * вычисляет {@code x = (ndc.x+1)*width/2}, {@code y = (1-ndc.y)*height/2} и {@code depth = (ndc.z+1)/2}.
     *
     * @param world точка в единицах сцены, может совпадать с out.
     * @param out   переиспользуемый вектор с координатами экрана в X/Y и глубиной в Z.
     *              Выход за viewport или плоскости отсечения выражается координатами вне соответствующего диапазона.
     * @return true при конечном результате и {@code clip.w > 0}. При отказе out сохраняется.
     */
    public boolean project(Vec3View world, MutableVec3 out) {
        updateMatrices();
        double x = world.x(), y = world.y(), z = world.z();
        double w = viewProjection.transformComponent(3, x, y, z, 1.0);
        if (!(w > 0.0) || !Double.isFinite(w)) return false;
        float sx = (float) ((viewProjection.transformComponent(0, x, y, z, 1.0) / w + 1.0) * width * 0.5);
        float sy = (float) ((1.0 - viewProjection.transformComponent(1, x, y, z, 1.0) / w) * height * 0.5);
        float depth = (float) ((viewProjection.transformComponent(2, x, y, z, 1.0) / w + 1.0) * 0.5);
        if (!Float.isFinite(sx) || !Float.isFinite(sy) || !Float.isFinite(depth)) return false;
        out.set(sx, sy, depth);
        return true;
    }

    /**
     * Восстанавливает мировую точку. Переводит координаты экрана в
     * {@code (ndcX, ndcY) = (2*x/width - 1, 1 - 2*y/height)}. Расстояние вдоль −Z камеры в перспективе
     * равно {@code near / (1 - depth + depth*near/far)}, в ортографической проекции равно
     * {@code (1-depth)*near + depth*far}. Деление координат NDC на масштабы проекции даёт координаты
     * в плоскости камеры. В перспективе они дополнительно умножаются на расстояние.
     * Обратное преобразование вида переводит точку в мировые координаты.
     *
     * @param x     локальная координата экрана по X в единицах UI.
     * @param y     локальная координата экрана по Y в единицах UI.
     * @param depth безразмерная глубина, 0 на ближней плоскости, 1 на дальней.
     * @param out   переиспользуемый вектор в единицах сцены.
     * @return true при конечном результате. При отказе out сохраняется.
     */
    public boolean unproject(float x, float y, double depth, MutableVec3 out) {
        updateMatrices();
        double remaining = 1.0 - depth;
        double distance = orthographic
                ? remaining * near + depth * far
                : near / (remaining + depth * ((double) near / far));
        double scale = orthographic ? 1.0 : distance;
        return viewToWorld(
                (2.0 * x / width - 1.0) * scale / projection.get(0, 0),
                (1.0 - 2.0 * y / height) * scale / projection.get(1, 1), -distance, out);
    }

    /**
     * Строит луч от соответствующей экранной точки на ближней плоскости.
     * В перспективе направление в координатах камеры равно
     * {@code (ndc.x / projection[0,0], ndc.y / projection[1,1], -1)}, в ортографической
     * проекции {@code (0, 0, -1)}. Транспонированное вращение матрицы вида переводит его в мировые координаты.
     *
     * @param x   локальная координата экрана по X в единицах UI.
     * @param y   локальная координата экрана по Y в единицах UI.
     * @param out переиспользуемый луч результата.
     * @return true при конечном результате. При отказе out сохраняется.
     */
    public boolean screenRay(float x, float y, Ray out) {
        updateMatrices();
        double vx = (2.0 * x / width - 1.0) / projection.get(0, 0);
        double vy = (1.0 - 2.0 * y / height) / projection.get(1, 1);
        double scale = orthographic ? 1.0 : near;
        if (!viewToWorld(vx * scale, vy * scale, -near, rayOrigin)) return false;
        if (orthographic) {
            vx = 0.0;
            vy = 0.0;
        }
        return out.set(rayOrigin,
                worldDirectionComponent(0, vx, vy, -1),
                worldDirectionComponent(1, vx, vy, -1),
                worldDirectionComponent(2, vx, vy, -1));
    }

    /**
     * Переводит точку p из координат камеры в мировое положение {@code eye + R^T * p}.
     * Здесь R — вращение матрицы вида, а eye — мировая позиция камеры.
     *
     * @param x   горизонтальная координата p в единицах сцены.
     * @param y   вертикальная координата p в единицах сцены.
     * @param z   координата p по глубине в единицах сцены.
     * @param out переиспользуемый вектор в единицах сцены.
     * @return true при конечном результате. При отказе out сохраняется.
     */
    private boolean viewToWorld(double x, double y, double z, MutableVec3 out) {
        float wx = (float) (position.x() + worldDirectionComponent(0, x, y, z));
        float wy = (float) (position.y() + worldDirectionComponent(1, x, y, z));
        float wz = (float) (position.z() + worldDirectionComponent(2, x, y, z));
        if (!Float.isFinite(wx) || !Float.isFinite(wy) || !Float.isFinite(wz)) return false;
        out.set(wx, wy, wz);
        return true;
    }

    /**
     * Вычисляет компоненту {@code R^T * v}, где R — вращение матрицы вида, v — вектор в системе камеры.
     *
     * @param axis индекс мировой оси от 0 до 2.
     * @param x    горизонтальная компонента v.
     * @param y    вертикальная компонента v.
     * @param z    компонента v по глубине.
     * @return компонента мирового вектора в единицах v.
     */
    private double worldDirectionComponent(int axis, double x, double y, double z) {
        return view.get(0, axis) * x + view.get(1, axis) * y + view.get(2, axis) * z;
    }

    /**
     * Отмечает производные матрицы устаревшими и увеличивает версию состояния.
     */
    private void changed() {
        revision++;
        matricesDirty = true;
    }

    /**
     * Пересчитывает матрицы вида и проекции при первом обращении после изменения.
     */
    private void updateMatrices() {
        if (!matricesDirty) return;
        double aspect = (double) width / height;
        if (orthographic) {
            projection.setOrthographic(orthographicHeight, aspect, near, far);
        } else {
            projection.setPerspective(fovY, aspect, near, far);
        }
        view.setView(position, orientation);
        viewProjection.setProduct(projection, view);
        matricesDirty = false;
    }

    /**
     * Проверяет плоскости отсечения.
     *
     * @param near ближняя плоскость в единицах сцены.
     * @param far  дальняя плоскость в единицах сцены.
     * @throws IllegalArgumentException если значения не конечны или нарушено {@code 0 < near < far}.
     */
    private static void checkClipPlanes(float near, float far) {
        if (!(near > 0.0f && far > near) || !Float.isFinite(far)) {
            throw new IllegalArgumentException("Clip planes require 0 < near < far");
        }
    }
}
