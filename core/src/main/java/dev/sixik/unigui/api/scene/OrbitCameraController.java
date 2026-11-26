package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Vec3View;

import java.util.Objects;

/**
 * Управляет камерой вокруг точки интереса. Вращение, панорама и зум принимают уже обработанный ввод.
 */
public final class OrbitCameraController {
    /**
     * Управляемая камера.
     */
    private final Camera3D camera;
    /**
     * Точка интереса в мировых единицах.
     */
    private final MutableVec3 target = new MutableVec3();
    /**
     * Рабочее вращение для преобразования из координат камеры в мировые.
     */
    private final MutableQuat rotation = new MutableQuat();
    /**
     * Рабочий вектор смещения в единицах сцены.
     */
    private final MutableVec3 offset = new MutableVec3();
    /**
     * Положительное расстояние от камеры до цели в единицах сцены.
     */
    private float distance;
    /**
     * Поворот вокруг мировой оси Y в радианах, от −π до π.
     */
    private double yaw;
    /**
     * Угол возвышения камеры над целью в радианах, от −π/2 до π/2.
     */
    private double pitch;

    /**
     * Настраивает камеру смотреть на цель со стороны +Z.
     *
     * @param camera   управляемая камера.
     * @param target   исходная точка интереса в единицах сцены.
     * @param distance конечное положительное расстояние до цели в единицах сцены.
     * @throws IllegalArgumentException если поза или цель не конечна либо расстояние не положительно.
     */
    public OrbitCameraController(Camera3D camera, Vec3View target, float distance) {
        this.camera = Objects.requireNonNull(camera, "camera");
        setOrbit(target, distance, 0.0, 0.0);
    }

    /**
     * Задаёт орбиту. Камера располагается в {@code target + R*(0, 0, distance)}, где R —
     * вращение {@code qY(yaw) * qX(-pitch)}. Локальная ось −Z указывает на цель, положительный pitch поднимает камеру.
     *
     * @param target   конечная мировая точка интереса в единицах сцены.
     * @param distance конечное положительное расстояние до цели в единицах сцены.
     * @param yaw      конечный угол вокруг мировой оси Y в радианах.
     * @param pitch    конечный угол возвышения в радианах, ограничивается диапазоном ±π/2.
     * @throws IllegalArgumentException если поза или аргументы не конечны либо расстояние не положительно.
     */
    public void setOrbit(Vec3View target, float distance, double yaw, double pitch) {
        setOrbit(target.x(), target.y(), target.z(), distance, yaw, pitch);
    }

    /**
     * Складывает угловые приращения с параметрами текущей орбиты.
     *
     * @param yawDelta   приращение вокруг мировой оси Y в радианах.
     * @param pitchDelta приращение угла возвышения в радианах.
     * @throws IllegalArgumentException если углы или результирующая поза не конечны.
     */
    public void rotate(double yawDelta, double pitchDelta) {
        setOrbit(target, distance, yaw + yawDelta, pitch + pitchDelta);
    }

    /**
     * Смещает цель и камеру на {@code R*(right, up, 0)}, где R — вращение камеры.
     *
     * @param right смещение вдоль правой оси камеры в единицах сцены.
     * @param up    смещение вверх по оси камеры в единицах сцены.
     * @throws IllegalArgumentException если смещение или результирующая поза не конечны.
     */
    public void pan(float right, float up) {
        rotation.setYawPitch(yaw, -pitch).rotate(right, up, 0.0f, offset);
        setOrbit(target.x() + offset.x(), target.y() + offset.y(), target.z() + offset.z(), distance, yaw, pitch);
    }

    /**
     * Умножает расстояние до цели (перспектива) или высоту видимой области (ортографическая проекция) на factor.
     *
     * @param factor конечный положительный коэффициент. Значение больше 1 отдаляет изображение.
     * @throws IllegalArgumentException если коэффициент или результат не конечен либо не положителен.
     */
    public void zoom(float factor) {
        if (!(factor > 0.0f) || !Float.isFinite(factor)) {
            throw new IllegalArgumentException("Zoom factor must be finite and positive");
        }
        if (camera.orthographic()) {
            camera.setOrthographic(camera.orthographicHeight() * factor, camera.near(), camera.far());
        } else {
            setOrbit(target, distance * factor, yaw, pitch);
        }
    }

    /**
     * Копирует мировую точку интереса.
     *
     * @param out переиспользуемый вектор в единицах сцены.
     * @return out.
     */
    public MutableVec3 target(MutableVec3 out) {
        return out.set(target);
    }

    /**
     * Читает радиус орбиты.
     *
     * @return расстояние от камеры до цели в единицах сцены.
     */
    public float distance() {
        return distance;
    }

    /**
     * Читает поворот орбиты вокруг мировой оси Y.
     *
     * @return угол в радианах.
     */
    public double yaw() {
        return yaw;
    }

    /**
     * Читает возвышение камеры над целью.
     *
     * @return угол в радианах.
     */
    public double pitch() {
        return pitch;
    }

    /**
     * Задаёт абсолютные углы текущей орбиты.
     *
     * @param yaw   угол вокруг мировой оси Y в радианах.
     * @param pitch угол возвышения в радианах.
     */
    public void setAngles(double yaw, double pitch) {
        setOrbit(target, distance, yaw, pitch);
    }

    /**
     * Задаёт расстояние до текущей цели.
     *
     * @param distance конечное положительное расстояние в единицах сцены.
     */
    public void setDistance(float distance) {
        setOrbit(target, distance, yaw, pitch);
    }

    /**
     * Пересчитывает позу камеры при изменении цели или углов орбиты.
     *
     * @param x        мировая координата X цели в единицах сцены.
     * @param y        мировая координата Y цели в единицах сцены.
     * @param z        мировая координата Z цели в единицах сцены.
     * @param distance конечное положительное расстояние до цели в единицах сцены.
     * @param yaw      конечный угол вокруг мировой оси Y в радианах.
     * @param pitch    конечный угол возвышения в радианах.
     * @throws IllegalArgumentException если аргументы или поза не конечны либо расстояние не положительно.
     */
    private void setOrbit(float x, float y, float z, float distance, double yaw, double pitch) {
        if (!(distance > 0.0f) || !Float.isFinite(distance) || !Double.isFinite(yaw) || !Double.isFinite(pitch)) {
            throw new IllegalArgumentException("Orbit requires a finite positive distance and finite angles");
        }
        double wrappedYaw = Math.IEEEremainder(yaw, 2.0 * Math.PI);
        double clampedPitch = Math.max(-Math.PI * 0.5, Math.min(Math.PI * 0.5, pitch));
        rotation.setYawPitch(wrappedYaw, -clampedPitch).rotate(0.0f, 0.0f, distance, offset);
        camera.setPose(x + offset.x(), y + offset.y(), z + offset.z(), rotation);
        target.set(x, y, z);
        this.distance = distance;
        this.yaw = wrappedYaw;
        this.pitch = clampedPitch;
    }
}
