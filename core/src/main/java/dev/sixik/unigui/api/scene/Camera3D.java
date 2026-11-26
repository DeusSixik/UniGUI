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
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.QuatView;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.Vec3View;

/**
 * Camera in a right-handed coordinate system. The neutral pose looks along -Z with +Y up.
 * Viewport coordinates start at the top left, with X increasing rightward and Y downward.
 * Matrices use NDC depth from -1 to 1. Screen methods use depth from 0 to 1.
 */
@WikiPage(
        title = "Camera3D", slug = "camera-3d",
        category = "API/3D Scene", order = 40,
        summary = "Perspective or orthographic camera with projection, unprojection and screen rays.")
@WikiSection(title = "Coordinate conventions", order = 10, markdown = """
        The world is right-handed. Screen coordinates are in logical canvas units from the top-left corner. Matrices
        use NDC depth in `[-1, 1]`, while `project` and `unproject` use `[0, 1]` at the near and far planes.
        """)
@WikiSection(title = "Projection and clipping", order = 20, markdown = """
        Perspective takes a vertical field of view in radians. Orthographic projection takes the visible height in
        scene units. `setClipPlanes` changes the depth range for either mode. A screen ray begins at the near plane.
        """)
@WikiExample(title = "Change the far plane without changing projection", code = """
        Camera3D camera = viewport.camera();
        camera.setClipPlanes(camera.near(), 160);
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "OrbitCameraController", target = "page:orbit-camera-controller")
@WikiLink(label = "FreeFlyCameraController", target = "page:free-fly-camera-controller")
@WikiLink(label = "ScenePicker", target = "page:scene-picker")
@AiDoc(
        summary = "Perspective or orthographic camera with projection, unprojection and screen rays.",
        purpose = "Set 3D view range and convert between scene points and logical viewport coordinates.",
        criticalRules = {
                "The neutral camera looks along -Z with +Y up. Angles in the Java APIs are radians.",
                "Keep `0 < near < far`. These values are scene-unit distances along the view axis.",
                "`project` can succeed with offscreen X/Y or out-of-range depth. Check logical bounds and depth in [0, 1] before placing visible overlays.",
                "Scene3D manages the camera's viewport dimensions. For a standalone camera set positive logical dimensions with `setViewport`."
        },
        related = {"Scene3D", "OrbitCameraController", "FreeFlyCameraController", "ScenePicker"},
        examples = @AiDoc.Example(title = "Change the far plane without changing projection", code = """
                Camera3D camera = viewport.camera();
                camera.setClipPlanes(camera.near(), 160);
                """))
public final class Camera3D {
    /**
     * World position in scene units.
     */
    private final MutableVec3 position = new MutableVec3();
    /**
     * Unit quaternion from camera space to world space.
     */
    private final MutableQuat orientation = new MutableQuat();
    /**
     * Transform from world space to camera space.
     */
    private final MutableMat4 view = new MutableMat4();
    /**
     * Transform from camera space to clip space.
     */
    private final MutableMat4 projection = new MutableMat4();
    /**
     * Cached product of projection and view.
     */
    private final MutableMat4 viewProjection = new MutableMat4();
    /**
     * Reusable world-space point on the near plane for ray construction.
     */
    private final MutableVec3 rayOrigin = new MutableVec3();

    /**
     * Positive viewport width in logical UI units.
     */
    private float width = 1.0f;
    /**
     * Positive viewport height in logical UI units.
     */
    private float height = 1.0f;
    /**
     * Vertical field of view in radians, strictly between zero and π.
     */
    private float fovY = (float) (Math.PI / 3.0);
    /**
     * Positive orthographic view height in scene units.
     */
    private float orthographicHeight = 2.0f;
    /**
     * Positive distance to the near plane in scene units.
     */
    private float near = 0.1f;
    /**
     * Distance to the far plane in scene units, greater than near.
     */
    private float far = 1000.0f;
    /**
     * Whether the projection is orthographic.
     */
    private boolean orthographic;
    /**
     * Whether the cached matrices need recomputing.
     */
    private boolean matricesDirty = true;
    /**
     * Revision of the pose, projection and viewport, starting at zero.
     */
    private long revision;

    /**
     * Creates a perspective camera at the origin with a 60° vertical field of view and a 1-by-1 viewport.
     */
    @WikiMethod(section = "Pose", sectionOrder = 10, order = 10,
            summary = "Creates a perspective camera at the origin with a 60° vertical field of view and a 1-by-1 viewport.")
    @AiDoc(section = "Pose", sectionOrder = 10, order = 10,
            summary = "Creates a perspective camera at the origin with a 60° vertical field of view and a 1-by-1 viewport.")
    public Camera3D() {
    }

    /**
     * Reads the camera revision for redraw decisions.
     *
     * @return change counter for pose, projection and viewport.
     */
    @WikiMethod(section = "Pose", sectionOrder = 10, order = 20,
            summary = "Reads the camera revision for redraw decisions.")
    @WikiReturn("change counter for pose, projection and viewport")
    @AiDoc(section = "Pose", sectionOrder = 10, order = 20,
            summary = "Reads the camera revision for redraw decisions.")
    public long revision() {
        return revision;
    }

    /**
     * Copies the world position.
     *
     * @param out reusable vector in scene units.
     * @return out.
     */
    @WikiMethod(section = "Pose", sectionOrder = 10, order = 30,
            summary = "Copies the world position.")
    @WikiReturn("out")
    @AiDoc(section = "Pose", sectionOrder = 10, order = 30,
            summary = "Copies the world position.")
    public MutableVec3 position(
            @WikiParameter(description = "reusable vector in scene units.") MutableVec3 out) {
        return out.set(position);
    }

    /**
     * Copies the orientation.
     *
     * @param out reusable quaternion.
     * @return out with the camera-to-world rotation.
     */
    @WikiMethod(section = "Pose", sectionOrder = 10, order = 40,
            summary = "Copies the orientation.")
    @WikiReturn("out with the camera-to-world rotation")
    @AiDoc(section = "Pose", sectionOrder = 10, order = 40,
            summary = "Copies the orientation.")
    public MutableQuat orientation(
            @WikiParameter(description = "reusable quaternion.") MutableQuat out) {
        return out.set(orientation);
    }

    /**
     * Sets the world position and orientation together.
     *
     * @param x finite world X in scene units.
     * @param y finite world Y in scene units.
     * @param z finite world Z in scene units.
     * @param rotation unit camera-to-world quaternion.
     * @return this camera.
     * @throws IllegalArgumentException if a coordinate is nonfinite or the quaternion is invalid.
     */
    @WikiMethod(section = "Pose", sectionOrder = 10, order = 50,
            summary = "Sets the world position and orientation together.")
    @WikiReturn("this camera")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a coordinate is nonfinite or the quaternion is invalid.")
    @AiDoc(section = "Pose", sectionOrder = 10, order = 50,
            summary = "Sets the world position and orientation together.")
    public Camera3D setPose(
            @WikiParameter(description = "finite world X in scene units.") float x,
            @WikiParameter(description = "finite world Y in scene units.") float y,
            @WikiParameter(description = "finite world Z in scene units.") float z,
            @WikiParameter(description = "unit camera-to-world quaternion.") QuatView rotation) {
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
     * Moves the camera without changing its orientation.
     *
     * @param x finite world X in scene units.
     * @param y finite world Y in scene units.
     * @param z finite world Z in scene units.
     * @return this camera.
     * @throws IllegalArgumentException if a coordinate is nonfinite.
     */
    @WikiMethod(section = "Pose", sectionOrder = 10, order = 60,
            summary = "Moves the camera without changing its orientation.")
    @WikiReturn("this camera")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a coordinate is nonfinite.")
    @AiDoc(section = "Pose", sectionOrder = 10, order = 60,
            summary = "Moves the camera without changing its orientation.")
    public Camera3D setPosition(
            @WikiParameter(description = "finite world X in scene units.") float x,
            @WikiParameter(description = "finite world Y in scene units.") float y,
            @WikiParameter(description = "finite world Z in scene units.") float z) {
        return setPose(x, y, z, orientation);
    }

    /**
     * Sets the viewport dimensions for screen transforms and projection aspect ratio.
     *
     * @param width finite positive width in logical UI units.
     * @param height finite positive height in logical UI units.
     * @return this camera.
     * @throws IllegalArgumentException if either dimension is nonpositive or nonfinite.
     */
    @WikiMethod(section = "Projection", sectionOrder = 20, order = 10,
            summary = "Sets the viewport dimensions for screen transforms and projection aspect ratio.")
    @WikiReturn("this camera")
    @WikiThrows(type = IllegalArgumentException.class, description = "if either dimension is nonpositive or nonfinite.")
    @AiDoc(section = "Projection", sectionOrder = 20, order = 10,
            summary = "Sets the viewport dimensions for screen transforms and projection aspect ratio.")
    public Camera3D setViewport(
            @WikiParameter(description = "finite positive width in logical UI units.") float width,
            @WikiParameter(description = "finite positive height in logical UI units.") float height) {
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
     * Selects perspective projection.
     *
     * @param fovY vertical field of view in radians, strictly between zero and π.
     * @param near positive near-plane distance in scene units.
     * @param far finite far-plane distance in scene units, greater than near.
     * @return this camera.
     * @throws IllegalArgumentException if the parameters are outside those ranges.
     */
    @WikiMethod(section = "Projection", sectionOrder = 20, order = 20,
            summary = "Selects perspective projection.")
    @WikiReturn("this camera")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the parameters are outside those ranges.")
    @AiDoc(section = "Projection", sectionOrder = 20, order = 20,
            summary = "Selects perspective projection.")
    public Camera3D setPerspective(
            @WikiParameter(description = "vertical field of view in radians, strictly between zero and π.") float fovY,
            @WikiParameter(description = "positive near-plane distance in scene units.") float near,
            @WikiParameter(description = "finite far-plane distance in scene units, greater than near.") float far) {
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
     * Selects orthographic projection.
     *
     * @param viewHeight finite positive view height in scene units.
     * @param near positive near-plane distance in scene units.
     * @param far finite far-plane distance in scene units, greater than near.
     * @return this camera.
     * @throws IllegalArgumentException if the parameters are outside those ranges.
     */
    @WikiMethod(section = "Projection", sectionOrder = 20, order = 30,
            summary = "Selects orthographic projection.")
    @WikiReturn("this camera")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the parameters are outside those ranges.")
    @AiDoc(section = "Projection", sectionOrder = 20, order = 30,
            summary = "Selects orthographic projection.")
    public Camera3D setOrthographic(
            @WikiParameter(description = "finite positive view height in scene units.") float viewHeight,
            @WikiParameter(description = "positive near-plane distance in scene units.") float near,
            @WikiParameter(description = "finite far-plane distance in scene units, greater than near.") float far) {
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
     * Reads the projection mode.
     *
     * @return true for orthographic projection and false for perspective projection.
     */
    @WikiMethod(section = "Projection", sectionOrder = 20, order = 40,
            summary = "Reads the projection mode.")
    @WikiReturn("true for orthographic projection and false for perspective projection")
    @AiDoc(section = "Projection", sectionOrder = 20, order = 40,
            summary = "Reads the projection mode.")
    public boolean orthographic() {
        return orthographic;
    }

    /**
     * Reads the configured orthographic view height.
     *
     * @return height in scene units.
     */
    @WikiMethod(section = "Projection", sectionOrder = 20, order = 50,
            summary = "Reads the configured orthographic view height.")
    @WikiReturn("height in scene units")
    @AiDoc(section = "Projection", sectionOrder = 20, order = 50,
            summary = "Reads the configured orthographic view height.")
    public float orthographicHeight() {
        return orthographicHeight;
    }

    /**
     * Reads the near plane.
     *
     * @return distance from the camera in scene units.
     */
    @WikiMethod(section = "Projection", sectionOrder = 20, order = 70,
            summary = "Reads the near plane.")
    @WikiReturn("distance from the camera in scene units")
    @AiDoc(section = "Projection", sectionOrder = 20, order = 70,
            summary = "Reads the near plane.")
    public float near() {
        return near;
    }

    /**
     * Reads the far plane.
     *
     * @return distance from the camera in scene units.
     */
    @WikiMethod(section = "Projection", sectionOrder = 20, order = 80,
            summary = "Reads the far plane.")
    @WikiReturn("distance from the camera in scene units")
    @AiDoc(section = "Projection", sectionOrder = 20, order = 80,
            summary = "Reads the far plane.")
    public float far() {
        return far;
    }

    /**
     * Updates the clipping planes for the current projection mode.
     *
     * @param near positive near-plane distance in scene units.
     * @param far finite far-plane distance in scene units, greater than near.
     * @return this camera.
     * @throws IllegalArgumentException if distances are nonfinite or violate {@code 0 < near < far}.
     */
    @WikiMethod(section = "Projection", sectionOrder = 20, order = 60,
            summary = "Updates the clipping planes for the current projection mode.")
    @WikiReturn("this camera")
    @WikiThrows(type = IllegalArgumentException.class, description = "if distances are nonfinite or violate `0 < near < far`.")
    @AiDoc(section = "Projection", sectionOrder = 20, order = 60,
            summary = "Updates the clipping planes for the current projection mode.")
    public Camera3D setClipPlanes(
            @WikiParameter(description = "positive near-plane distance in scene units.") float near,
            @WikiParameter(description = "finite far-plane distance in scene units, greater than near.") float far) {
        checkClipPlanes(near, far);
        if (this.near == near && this.far == far) return this;
        this.near = near;
        this.far = far;
        changed();
        return this;
    }

    /**
     * Copies the view matrix.
     *
     * @param out reusable matrix.
     * @return out with the world-to-camera transform.
     */
    @WikiMethod(section = "Matrices and screen conversion", sectionOrder = 30, order = 10,
            summary = "Copies the view matrix.")
    @WikiReturn("out with the world-to-camera transform")
    @AiDoc(section = "Matrices and screen conversion", sectionOrder = 30, order = 10,
            summary = "Copies the view matrix.")
    public MutableMat4 view(
            @WikiParameter(description = "reusable matrix.") MutableMat4 out) {
        updateMatrices();
        return out.set(view);
    }

    /**
     * Copies the projection matrix.
     *
     * @param out reusable matrix.
     * @return out with the camera-to-clip transform.
     */
    @WikiMethod(section = "Matrices and screen conversion", sectionOrder = 30, order = 20,
            summary = "Copies the projection matrix.")
    @WikiReturn("out with the camera-to-clip transform")
    @AiDoc(section = "Matrices and screen conversion", sectionOrder = 30, order = 20,
            summary = "Copies the projection matrix.")
    public MutableMat4 projection(
            @WikiParameter(description = "reusable matrix.") MutableMat4 out) {
        updateMatrices();
        return out.set(projection);
    }

    /**
     * Copies {@code projection * view}.
     *
     * @param out reusable matrix.
     * @return out with the world-to-clip transform.
     */
    @WikiMethod(section = "Matrices and screen conversion", sectionOrder = 30, order = 30,
            summary = "Copies `projection * view`.")
    @WikiReturn("out with the world-to-clip transform")
    @AiDoc(section = "Matrices and screen conversion", sectionOrder = 30, order = 30,
            summary = "Copies `projection * view`.")
    public MutableMat4 viewProjection(
            @WikiParameter(description = "reusable matrix.") MutableMat4 out) {
        updateMatrices();
        return out.set(viewProjection);
    }

    /**
     * Projects a world point to the viewport. NDC equals clip XYZ divided by clip W. The result is
     * {@code x = (ndc.x+1)*width/2}, {@code y = (1-ndc.y)*height/2}, {@code depth = (ndc.z+1)/2}.
     *
     * @param world point in scene units. It may alias out.
     * @param out reusable vector for logical viewport X/Y and normalized depth Z.
     * Coordinates outside the viewport or clip range remain outside those ranges.
     * @return true for a finite result with {@code clip.w > 0}. On failure out is unchanged.
     */
    @WikiMethod(section = "Matrices and screen conversion", sectionOrder = 30, order = 40,
            summary = "Projects a world point to the viewport. NDC equals clip XYZ divided by clip W. The result is `x = (ndc.x+1)*width/2`, `y = (1-ndc.y)*height/2`, `depth = (ndc.z+1)/2`.")
    @WikiReturn("true for a finite result with `clip.w > 0`. On failure out is unchanged")
    @AiDoc(section = "Matrices and screen conversion", sectionOrder = 30, order = 40,
            summary = "Projects a world point to the viewport. NDC equals clip XYZ divided by clip W. The result is `x = (ndc.x+1)*width/2`, `y = (1-ndc.y)*height/2`, `depth = (ndc.z+1)/2`.")
    public boolean project(
            @WikiParameter(description = "point in scene units. It may alias out.") Vec3View world,
            @WikiParameter(description =
                    "reusable vector for logical viewport X/Y and normalized depth Z. Coordinates outside the viewport or clip range remain outside those ranges.")
            MutableVec3 out) {
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
     * Reconstructs a world point. The input maps to {@code (ndcX, ndcY) = (2*x/width - 1, 1 - 2*y/height)}.
     * The distance along camera -Z is {@code near / (1 - depth + depth*near/far)} in perspective,
     * or {@code (1-depth)*near + depth*far} in orthographic projection. Divide NDC X/Y by the corresponding
     * projection scales and, in perspective, multiply by the distance. The inverse view transforms the point to world space.
     *
     * @param x viewport X in logical UI units.
     * @param y viewport Y in logical UI units.
     * @param depth dimensionless depth, zero at the near plane and one at the far plane.
     * @param out reusable world-space vector.
     * @return true for a finite result. On failure out is unchanged.
     */
    @WikiMethod(section = "Matrices and screen conversion", sectionOrder = 30, order = 50,
            summary = "Reconstructs a world point. The input maps to `(ndcX, ndcY) = (2*x/width - 1, 1 - 2*y/height)`. The distance along camera -Z is `near / (1 - depth + depth*near/far)` in perspective, or `(1-depth)*near + depth*far` in orthographic projection. Divide NDC X/Y by the corresponding projection scales and, in perspective, multiply by the distance. The inverse view transforms the point to world space.")
    @WikiReturn("true for a finite result. On failure out is unchanged")
    @AiDoc(section = "Matrices and screen conversion", sectionOrder = 30, order = 50,
            summary = "Reconstructs a world point. The input maps to `(ndcX, ndcY) = (2*x/width - 1, 1 - 2*y/height)`. The distance along camera -Z is `near / (1 - depth + depth*near/far)` in perspective, or `(1-depth)*near + depth*far` in orthographic projection. Divide NDC X/Y by the corresponding projection scales and, in perspective, multiply by the distance. The inverse view transforms the point to world space.")
    public boolean unproject(
            @WikiParameter(description = "viewport X in logical UI units.") float x,
            @WikiParameter(description = "viewport Y in logical UI units.") float y,
            @WikiParameter(description = "dimensionless depth, zero at the near plane and one at the far plane.") double depth,
            @WikiParameter(description = "reusable world-space vector.") MutableVec3 out) {
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
     * Builds a ray starting on the near plane at the given viewport point. Its camera-space direction is
     * {@code (ndc.x / projection[0,0], ndc.y / projection[1,1], -1)} in perspective and
     * {@code (0, 0, -1)} in orthographic projection. The transpose of the view rotation maps it to world space.
     *
     * @param x viewport X in logical UI units.
     * @param y viewport Y in logical UI units.
     * @param out reusable ray.
     * @return true for a finite result. On failure out is unchanged.
     */
    @WikiMethod(section = "Matrices and screen conversion", sectionOrder = 30, order = 60,
            summary = "Builds a ray starting on the near plane at the given viewport point. Its camera-space direction is `(ndc.x / projection[0,0], ndc.y / projection[1,1], -1)` in perspective and `(0, 0, -1)` in orthographic projection. The transpose of the view rotation maps it to world space.")
    @WikiReturn("true for a finite result. On failure out is unchanged")
    @AiDoc(section = "Matrices and screen conversion", sectionOrder = 30, order = 60,
            summary = "Builds a ray starting on the near plane at the given viewport point. Its camera-space direction is `(ndc.x / projection[0,0], ndc.y / projection[1,1], -1)` in perspective and `(0, 0, -1)` in orthographic projection. The transpose of the view rotation maps it to world space.")
    public boolean screenRay(
            @WikiParameter(description = "viewport X in logical UI units.") float x,
            @WikiParameter(description = "viewport Y in logical UI units.") float y,
            @WikiParameter(description = "reusable ray.") Ray out) {
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
     * Maps a camera-space point p to {@code eye + R^T * p}, where R is the view rotation
     * and eye is the world position of the camera.
     *
     * @param x horizontal component of p in scene units.
     * @param y vertical component of p in scene units.
     * @param z depth component of p in scene units.
     * @param out reusable vector in scene units.
     * @return true for a finite result. On failure out is unchanged.
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
     * Computes one component of {@code R^T * v}, where R is the view rotation and v is a camera-space vector.
     *
     * @param axis world axis index from 0 to 2.
     * @param x horizontal component of v.
     * @param y vertical component of v.
     * @param z depth component of v.
     * @return world-space component in the units of v.
     */
    private double worldDirectionComponent(int axis, double x, double y, double z) {
        return view.get(0, axis) * x + view.get(1, axis) * y + view.get(2, axis) * z;
    }

    /**
     * Marks the matrices dirty and increments the camera revision.
     */
    private void changed() {
        revision++;
        matricesDirty = true;
    }

    /**
     * Recomputes the view and projection matrices on demand after a change.
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
     * Validates the clipping planes.
     *
     * @param near near-plane distance in scene units.
     * @param far far-plane distance in scene units.
     * @throws IllegalArgumentException if either value is nonfinite or violates {@code 0 < near < far}.
     */
    private static void checkClipPlanes(float near, float far) {
        if (!(near > 0.0f && far > near) || !Float.isFinite(far)) {
            throw new IllegalArgumentException("Clip planes require 0 < near < far");
        }
    }
}
