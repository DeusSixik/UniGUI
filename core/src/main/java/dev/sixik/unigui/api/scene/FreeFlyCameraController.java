package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;

import java.util.Objects;

/**
 * Поворачивает камеру без крена и перемещает её вдоль собственных осей.
 */
public final class FreeFlyCameraController {
    /**
     * Предел вертикального угла в радианах, оставляющий ненулевую горизонтальную проекцию взгляда.
     */
    private static final double MAX_PITCH = Math.PI * 0.5 - 0.001;

    /**
     * Управляемая камера.
     */
    private final Camera3D camera;
    /**
     * Переиспользуемая копия мировой позиции камеры в единицах сцены.
     */
    private final MutableVec3 position = new MutableVec3();
    /**
     * Переиспользуемый мировой вектор направления или смещения.
     */
    private final MutableVec3 vector = new MutableVec3();
    /**
     * Переиспользуемое вращение из координат камеры в мировые.
     */
    private final MutableQuat rotation = new MutableQuat();

    /**
     * Подключает контроллер к существующей позе.
     *
     * @param camera управляемая камера.
     */
    public FreeFlyCameraController(Camera3D camera) {
        this.camera = Objects.requireNonNull(camera, "camera");
    }

    /**
     * Поворачивает направление взгляда. Для мирового направления f вычисляет текущие углы
     * {@code yaw = atan2(-f.x, -f.z)} и {@code pitch = atan2(f.y, sqrt(f.x*f.x + f.z*f.z))},
     * добавляет приращения и строит вращение без крена.
     *
     * @param yawDelta   конечное приращение вокруг мировой оси Y в радианах.
     * @param pitchDelta конечное приращение вокруг локальной оси X в радианах.
     * @throws IllegalArgumentException если приращение не конечно.
     */
    public void look(double yawDelta, double pitchDelta) {
        if (!Double.isFinite(yawDelta) || !Double.isFinite(pitchDelta)) {
            throw new IllegalArgumentException("Look deltas must be finite");
        }
        if (yawDelta == 0.0 && pitchDelta == 0.0) return;
        camera.orientation(rotation).rotate(0.0f, 0.0f, -1.0f, vector);
        double horizontal = Math.hypot(vector.x(), vector.z());
        double yaw = Math.atan2(-vector.x(), -vector.z()) + Math.IEEEremainder(yawDelta, 2.0 * Math.PI);
        double pitch = Math.atan2(vector.y(), horizontal) + pitchDelta;
        rotation.setYawPitch(yaw, Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch)));
        camera.position(position);
        camera.setPose(position.x(), position.y(), position.z(), rotation);
    }

    /**
     * Смещает камеру на {@code R*(right, up, -forward)}, где R — её текущее вращение.
     *
     * @param right   конечное смещение вправо в единицах сцены.
     * @param up      конечное смещение вверх в единицах сцены.
     * @param forward конечное смещение вперёд в единицах сцены.
     * @throws IllegalArgumentException если смещение или результирующая позиция не конечны.
     */
    public void move(float right, float up, float forward) {
        camera.orientation(rotation).rotate(right, up, -forward, vector);
        camera.position(position);
        camera.setPosition(position.x() + vector.x(), position.y() + vector.y(), position.z() + vector.z());
    }
}
