package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;

import java.util.Objects;

/**
 * Turns a camera without roll and moves it along its own axes.
 */
@WikiPage(
        title = "FreeFlyCameraController", slug = "free-fly-camera-controller",
        category = "API/3D Scene", order = 60,
        summary = "Free camera movement along local axes with yaw and pitch control.")
@WikiSection(title = "Spectator input", order = 10, markdown = """
        Use a focusable viewport. When the fly mode is active and the viewport has focus, sample held keys each
        frame and form a normalized direction. Multiply it by speed in scene units per second and elapsed seconds.
        Convert pointer deltas to radians and pass them to `look`. Restore the orbit with
        `viewport.orbit().rotate(0, 0)` when leaving fly mode.
        """)
@WikiExample(title = "Switch to a free camera and apply input deltas", code = """
        viewport.cameraControls(false);
        FreeFlyCameraController fly = new FreeFlyCameraController(viewport.camera());
        // In focused input handlers, using already processed pointer and key input:
        fly.look(-mouseDeltaX * 0.006, -mouseDeltaY * 0.006);
        fly.move(rightDistance, upDistance, forwardDistance);
        """)
@WikiLink(label = "Camera3D", target = "page:camera-3d")
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "OrbitCameraController", target = "page:orbit-camera-controller")
@AiDoc(
        summary = "Free camera movement along local axes with yaw and pitch control.",
        purpose = "Build a spectator camera by translating UI input into movement and look deltas.",
        criticalRules = {
                "Disable Scene3D orbit gestures while using this controller.",
                "The controller consumes motion and look deltas. The UI owns focus, held keys, pointer capture and frame timing.",
                "Move arguments are distances in scene units. Normalize diagonal input before multiplying by speed and delta time.",
                "Positive forward moves along camera-local -Z. Positive pitch looks up."
        },
        related = {"Camera3D", "Scene3D", "OrbitCameraController"},
        examples = @AiDoc.Example(title = "Switch to a free camera and apply input deltas", code = """
                viewport.cameraControls(false);
                FreeFlyCameraController fly = new FreeFlyCameraController(viewport.camera());
                // In focused input handlers, using already processed pointer and key input:
                fly.look(-mouseDeltaX * 0.006, -mouseDeltaY * 0.006);
                fly.move(rightDistance, upDistance, forwardDistance);
                """))
public final class FreeFlyCameraController {
    /**
     * Pitch limit in radians that preserves a nonzero horizontal forward component.
     */
    private static final double MAX_PITCH = Math.PI * 0.5 - 0.001;

    /**
     * Controlled camera.
     */
    private final Camera3D camera;
    /**
     * Reusable copy of the camera's world position in scene units.
     */
    private final MutableVec3 position = new MutableVec3();
    /**
     * Reusable world-space direction or displacement.
     */
    private final MutableVec3 vector = new MutableVec3();
    /**
     * Reusable rotation from camera space to world space.
     */
    private final MutableQuat rotation = new MutableQuat();

    /**
     * Attaches the controller without changing the current camera pose.
     *
     * @param camera controlled camera.
     * @throws NullPointerException if camera is null.
     */
    @WikiMethod(section = "Free camera", sectionOrder = 10, order = 10,
            summary = "Attaches the controller without changing the current camera pose.")
    @WikiThrows(type = NullPointerException.class, description = "if camera is null.")
    @AiDoc(section = "Free camera", sectionOrder = 10, order = 10,
            summary = "Attaches the controller without changing the current camera pose.")
    public FreeFlyCameraController(
            @WikiParameter(description = "controlled camera.") Camera3D camera) {
        this.camera = Objects.requireNonNull(camera, "camera");
    }

    /**
     * Turns the camera. For world-space forward direction f, derives yaw as {@code atan2(-f.x, -f.z)}
     * and pitch as {@code atan2(f.y, sqrt(f.x*f.x + f.z*f.z))}. It adds the input deltas, clamps pitch
     * away from the poles and rebuilds the orientation without roll.
     *
     * @param yawDelta finite angle about world Y in radians.
     * @param pitchDelta finite pitch delta in radians. Positive values look up.
     * @throws IllegalArgumentException if either delta is nonfinite.
     */
    @WikiMethod(section = "Free camera", sectionOrder = 10, order = 20,
            summary = "Turns the camera. For world-space forward direction f, derives yaw as `atan2(-f.x, -f.z)` and pitch as `atan2(f.y, sqrt(f.x*f.x + f.z*f.z))`. It adds the input deltas, clamps pitch away from the poles and rebuilds the orientation without roll.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if either delta is nonfinite.")
    @AiDoc(section = "Free camera", sectionOrder = 10, order = 20,
            summary = "Turns the camera. For world-space forward direction f, derives yaw as `atan2(-f.x, -f.z)` and pitch as `atan2(f.y, sqrt(f.x*f.x + f.z*f.z))`. It adds the input deltas, clamps pitch away from the poles and rebuilds the orientation without roll.")
    public void look(
            @WikiParameter(description = "finite angle about world Y in radians.") double yawDelta,
            @WikiParameter(description = "finite pitch delta in radians. Positive values look up.") double pitchDelta) {
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
     * Moves the camera by {@code R*(right, up, -forward)}, where R is its current orientation.
     *
     * @param right finite rightward displacement in scene units.
     * @param up finite upward displacement in scene units.
     * @param forward finite forward displacement in scene units.
     * @throws IllegalArgumentException if an offset or the resulting position is nonfinite.
     */
    @WikiMethod(section = "Free camera", sectionOrder = 10, order = 30,
            summary = "Moves the camera by `R*(right, up, -forward)`, where R is its current orientation.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if an offset or the resulting position is nonfinite.")
    @AiDoc(section = "Free camera", sectionOrder = 10, order = 30,
            summary = "Moves the camera by `R*(right, up, -forward)`, where R is its current orientation.")
    public void move(
            @WikiParameter(description = "finite rightward displacement in scene units.") float right,
            @WikiParameter(description = "finite upward displacement in scene units.") float up,
            @WikiParameter(description = "finite forward displacement in scene units.") float forward) {
        camera.orientation(rotation).rotate(right, up, -forward, vector);
        camera.position(position);
        camera.setPosition(position.x() + vector.x(), position.y() + vector.y(), position.z() + vector.z());
    }
}
