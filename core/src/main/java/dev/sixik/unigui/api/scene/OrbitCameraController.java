package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Vec3View;

import java.util.Objects;

/**
 * Orbits a camera around a point of interest. Rotation, panning and zoom accept processed input.
 */
@WikiPage(
        title = "OrbitCameraController", slug = "orbit-camera-controller",
        category = "API/3D Scene", order = 50,
        summary = "Camera controller that orbits, pans and zooms around a world-space target.")
@WikiSection(title = "Orbit gestures", order = 10, markdown = """
        `Scene3D.orbit()` exposes the controller used by pointer gestures. `pan(right, up)` slides camera and target
        in camera-local axes. `setOrbit` sets target, radius and both angles in one update.
        """)
@WikiExample(title = "Frame a block structure", code = """
        viewport.orbit().setOrbit(new MutableVec3(3.5f, 2, 3.5f), 14,
                Math.toRadians(40), Math.toRadians(25));
        """)
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "FreeFlyCameraController", target = "page:free-fly-camera-controller")
@AiDoc(
        summary = "Camera controller that orbits, pans and zooms around a world-space target.",
        purpose = "Set the initial angle of a 3D preview or drive the orbit from custom input.",
        criticalRules = {
                "Yaw rotates about world +Y. Positive pitch is elevation above the target.",
                "`zoom(factor)` scales the orbit distance in perspective and the view height in orthographic mode. Factors above one zoom out."
        },
        related = {"Camera3D", "Scene3D", "FreeFlyCameraController"},
        examples = @AiDoc.Example(title = "Frame a block structure", code = """
                viewport.orbit().setOrbit(new MutableVec3(3.5f, 2, 3.5f), 14,
                        Math.toRadians(40), Math.toRadians(25));
                """))
public final class OrbitCameraController {
    /**
     * Controlled camera.
     */
    private final Camera3D camera;
    /**
     * Point of interest in world units.
     */
    private final MutableVec3 target = new MutableVec3();
    /**
     * Reusable rotation from camera space to world space.
     */
    private final MutableQuat rotation = new MutableQuat();
    /**
     * Reusable offset vector in scene units.
     */
    private final MutableVec3 offset = new MutableVec3();
    /**
     * Positive distance from the camera to the target in scene units.
     */
    private float distance;
    /**
     * Rotation about world Y in radians, from -π to π.
     */
    private double yaw;
    /**
     * Elevation above the target in radians, from -π/2 to π/2.
     */
    private double pitch;

    /**
     * Places the camera on the +Z side of the target.
     *
     * @param camera controlled camera.
     * @param target initial point of interest in scene units.
     * @param distance finite positive distance to the target in scene units.
     * @throws IllegalArgumentException if the resulting pose or target is nonfinite or the distance is nonpositive.
     */
    @WikiMethod(section = "Orbit", sectionOrder = 10, order = 10,
            summary = "Places the camera on the +Z side of the target.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the resulting pose or target is nonfinite or the distance is nonpositive.")
    @AiDoc(section = "Orbit", sectionOrder = 10, order = 10,
            summary = "Places the camera on the +Z side of the target.")
    public OrbitCameraController(
            @WikiParameter(description = "controlled camera.") Camera3D camera,
            @WikiParameter(description = "initial point of interest in scene units.") Vec3View target,
            @WikiParameter(description = "finite positive distance to the target in scene units.") float distance) {
        this.camera = Objects.requireNonNull(camera, "camera");
        setOrbit(target, distance, 0.0, 0.0);
    }

    /**
     * Sets the camera position to {@code target + R*(0, 0, distance)}, where R is
     * {@code qY(yaw) * qX(-pitch)}. Camera-local -Z points at the target. Positive pitch raises the camera.
     *
     * @param target finite world-space point of interest.
     * @param distance finite positive distance in scene units.
     * @param yaw finite angle about world Y in radians.
     * @param pitch finite elevation in radians, clamped to ±π/2.
     * @throws IllegalArgumentException if an argument or the pose is nonfinite or distance is nonpositive.
     */
    @WikiMethod(section = "Orbit", sectionOrder = 10, order = 20,
            summary = "Sets the camera position to `target + R*(0, 0, distance)`, where R is `qY(yaw) * qX(-pitch)`. Camera-local -Z points at the target. Positive pitch raises the camera.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if an argument or the pose is nonfinite or distance is nonpositive.")
    @AiDoc(section = "Orbit", sectionOrder = 10, order = 20,
            summary = "Sets the camera position to `target + R*(0, 0, distance)`, where R is `qY(yaw) * qX(-pitch)`. Camera-local -Z points at the target. Positive pitch raises the camera.")
    public void setOrbit(
            @WikiParameter(description = "finite world-space point of interest.") Vec3View target,
            @WikiParameter(description = "finite positive distance in scene units.") float distance,
            @WikiParameter(description = "finite angle about world Y in radians.") double yaw,
            @WikiParameter(description = "finite elevation in radians, clamped to ±π/2.") double pitch) {
        setOrbit(target.x(), target.y(), target.z(), distance, yaw, pitch);
    }

    /**
     * Adds angular deltas to the current orbit.
     *
     * @param yawDelta rotation about world Y in radians.
     * @param pitchDelta elevation change in radians.
     * @throws IllegalArgumentException if an angle or the resulting pose is nonfinite.
     */
    @WikiMethod(section = "Gestures", sectionOrder = 20, order = 10,
            summary = "Adds angular deltas to the current orbit.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if an angle or the resulting pose is nonfinite.")
    @AiDoc(section = "Gestures", sectionOrder = 20, order = 10,
            summary = "Adds angular deltas to the current orbit.")
    public void rotate(
            @WikiParameter(description = "rotation about world Y in radians.") double yawDelta,
            @WikiParameter(description = "elevation change in radians.") double pitchDelta) {
        setOrbit(target, distance, yaw + yawDelta, pitch + pitchDelta);
    }

    /**
     * Moves the target and camera by {@code R*(right, up, 0)}, where R is the camera orientation.
     *
     * @param right offset along camera +X in scene units.
     * @param up offset along camera +Y in scene units.
     * @throws IllegalArgumentException if an offset or the resulting pose is nonfinite.
     */
    @WikiMethod(section = "Gestures", sectionOrder = 20, order = 20,
            summary = "Moves the target and camera by `R*(right, up, 0)`, where R is the camera orientation.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if an offset or the resulting pose is nonfinite.")
    @AiDoc(section = "Gestures", sectionOrder = 20, order = 20,
            summary = "Moves the target and camera by `R*(right, up, 0)`, where R is the camera orientation.")
    public void pan(
            @WikiParameter(description = "offset along camera +X in scene units.") float right,
            @WikiParameter(description = "offset along camera +Y in scene units.") float up) {
        rotation.setYawPitch(yaw, -pitch).rotate(right, up, 0.0f, offset);
        setOrbit(target.x() + offset.x(), target.y() + offset.y(), target.z() + offset.z(), distance, yaw, pitch);
    }

    /**
     * Multiplies the perspective orbit radius or orthographic view height by factor.
     *
     * @param factor finite positive multiplier. Values above one zoom out.
     * @throws IllegalArgumentException if factor or the result is nonpositive or nonfinite.
     */
    @WikiMethod(section = "Gestures", sectionOrder = 20, order = 30,
            summary = "Multiplies the perspective orbit radius or orthographic view height by factor.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if factor or the result is nonpositive or nonfinite.")
    @AiDoc(section = "Gestures", sectionOrder = 20, order = 30,
            summary = "Multiplies the perspective orbit radius or orthographic view height by factor.")
    public void zoom(
            @WikiParameter(description = "finite positive multiplier. Values above one zoom out.") float factor) {
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
     * Copies the world-space target.
     *
     * @param out reusable vector in scene units.
     * @return out.
     */
    @WikiMethod(section = "Orbit", sectionOrder = 10, order = 30,
            summary = "Copies the world-space target.")
    @WikiReturn("out")
    @AiDoc(section = "Orbit", sectionOrder = 10, order = 30,
            summary = "Copies the world-space target.")
    public MutableVec3 target(
            @WikiParameter(description = "reusable vector in scene units.") MutableVec3 out) {
        return out.set(target);
    }

    /**
     * Reads the orbit radius.
     *
     * @return distance from camera to target in scene units.
     */
    @WikiMethod(section = "Orbit", sectionOrder = 10, order = 40,
            summary = "Reads the orbit radius.")
    @WikiReturn("distance from camera to target in scene units")
    @AiDoc(section = "Orbit", sectionOrder = 10, order = 40,
            summary = "Reads the orbit radius.")
    public float distance() {
        return distance;
    }

    /**
     * Reads the orbit angle about world Y.
     *
     * @return angle in radians.
     */
    @WikiMethod(section = "Orbit", sectionOrder = 10, order = 50,
            summary = "Reads the orbit angle about world Y.")
    @WikiReturn("angle in radians")
    @AiDoc(section = "Orbit", sectionOrder = 10, order = 50,
            summary = "Reads the orbit angle about world Y.")
    public double yaw() {
        return yaw;
    }

    /**
     * Reads the elevation above the target.
     *
     * @return angle in radians.
     */
    @WikiMethod(section = "Orbit", sectionOrder = 10, order = 60,
            summary = "Reads the elevation above the target.")
    @WikiReturn("angle in radians")
    @AiDoc(section = "Orbit", sectionOrder = 10, order = 60,
            summary = "Reads the elevation above the target.")
    public double pitch() {
        return pitch;
    }

    /**
     * Sets the absolute orbit angles.
     *
     * @param yaw rotation about world Y in radians.
     * @param pitch elevation in radians.
     */
    @WikiMethod(section = "Orbit", sectionOrder = 10, order = 70,
            summary = "Sets the absolute orbit angles.")
    @AiDoc(section = "Orbit", sectionOrder = 10, order = 70,
            summary = "Sets the absolute orbit angles.")
    public void setAngles(
            @WikiParameter(description = "rotation about world Y in radians.") double yaw,
            @WikiParameter(description = "elevation in radians.") double pitch) {
        setOrbit(target, distance, yaw, pitch);
    }

    /**
     * Sets the distance to the current target.
     *
     * @param distance finite positive distance in scene units.
     */
    @WikiMethod(section = "Orbit", sectionOrder = 10, order = 80,
            summary = "Sets the distance to the current target.")
    @AiDoc(section = "Orbit", sectionOrder = 10, order = 80,
            summary = "Sets the distance to the current target.")
    public void setDistance(
            @WikiParameter(description = "finite positive distance in scene units.") float distance) {
        setOrbit(target, distance, yaw, pitch);
    }

    /**
     * Recomputes the camera pose from the target and orbit parameters.
     *
     * @param x target world X in scene units.
     * @param y target world Y in scene units.
     * @param z target world Z in scene units.
     * @param distance finite positive distance in scene units.
     * @param yaw finite angle about world Y in radians.
     * @param pitch finite elevation in radians.
     * @throws IllegalArgumentException if an argument or the pose is nonfinite or distance is nonpositive.
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
