package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.Vec3View;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class Camera3DTest {
    // Ошибка нескольких ULP у экранных координат порядка 100 и при round-trip глубины через float.
    private static final float EPS = 5e-5f;
    private static final float RIGHT_ANGLE = (float) (Math.PI / 2);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void project_clipPlanesAndViewportAxes_matchScreenConvention(boolean orthographic) {
        Camera3D camera = camera(orthographic);
        MutableVec3 screen = new MutableVec3();
        assertTrue(camera.project(new MutableVec3(-2, 1, -1), screen));
        assertVector(screen, 0, 0, 0);
        assertTrue(camera.project(new MutableVec3(2, -1, -1), screen));
        assertVector(screen, 200, 100, 0);
        assertTrue(camera.project(new MutableVec3(0, 0, -11), screen));
        assertVector(screen, 100, 50, 1);
        assertTrue(camera.unproject(100, 50, 0, screen));
        assertVector(screen, 0, 0, -1);
        assertTrue(camera.unproject(100, 50, 1, screen));
        assertVector(screen, 0, 0, -11);
    }

    /** Проверяет обновление глубины с сохранением текущего режима и масштаба проекции. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void setClipPlanes_withExistingProjection_updatesDepthWithoutChangingMagnification(boolean orthographic) {
        Camera3D camera = camera(orthographic);
        MutableVec3 screen = new MutableVec3();
        MutableVec3 point = new MutableVec3(0.5f, 0, -48);
        assertTrue(camera.project(point, screen));
        assertTrue(screen.z() > 1);
        float originalX = screen.x();
        long revision = camera.revision();

        camera.setClipPlanes(2, 128).setClipPlanes(2, 128);

        assertEquals(revision + 1, camera.revision());
        assertEquals(orthographic, camera.orthographic());
        assertTrue(camera.project(point, screen));
        assertEquals(originalX, screen.x(), EPS);
        assertTrue(screen.z() > 0 && screen.z() < 1);
        assertTrue(camera.unproject(100, 50, 1, screen));
        assertVector(screen, 0, 0, -128);
        assertThrows(IllegalArgumentException.class, () -> camera.setClipPlanes(2, 2));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unproject_projectedPointWithMovedCamera_recoversWorldPoint(boolean orthographic) {
        Camera3D camera = camera(orthographic).setPose(4, 5, 6, new MutableQuat().setYawPitch(0.3, -0.2));
        MutableVec3 original = new MutableVec3(2.5f, 4, 2);
        MutableVec3 screen = new MutableVec3();
        assertTrue(camera.project(original, screen));

        assertTrue(camera.unproject(screen.x(), screen.y(), screen.z(), screen));

        assertVector(screen, original.x(), original.y(), original.z());
    }

    @Test
    void unproject_farPlaneWithLargeDepthRange_recoversConfiguredDistance() {
        float near = 0.125f;
        float far = 1e20f;
        Camera3D camera = camera(false).setPerspective(RIGHT_ANGLE, near, far);
        MutableVec3 out = new MutableVec3();

        assertTrue(camera.unproject(100, 50, 1, out));

        assertEquals(-far, out.z(), Math.ulp(far));
    }

    @Test
    void screenRay_perspectiveCornerAndYaw_convergesAtEyeAndPointsForward() {
        Camera3D camera = camera(false).setPose(4, 2, 3, new MutableQuat().setYawPitch(Math.PI / 2, 0));
        Ray center = new Ray();
        Ray corner = new Ray();
        MutableVec3 out = new MutableVec3();

        assertTrue(camera.screenRay(100, 50, center));
        assertTrue(camera.screenRay(200, 0, corner));

        assertVector(center.origin(out), 3, 2, 3);
        assertVector(center.direction(out), -1, 0, 0);
        assertVector(corner.origin(out), 3, 3, 1);
        assertVector(corner.pointAt(-Math.sqrt(6), out), 4, 2, 3);
    }

    @Test
    void screenRay_orthographicPairs_startAtDifferentPointsWithParallelDirections() {
        Camera3D camera = camera(true);
        Ray left = new Ray();
        Ray right = new Ray();
        MutableVec3 out = new MutableVec3();

        assertTrue(camera.screenRay(0, 50, left));
        assertTrue(camera.screenRay(200, 50, right));

        assertVector(left.origin(out), -2, 0, -1);
        assertVector(right.origin(out), 2, 0, -1);
        assertVector(left.direction(out), 0, 0, -1);
        assertVector(right.direction(out), 0, 0, -1);
    }

    @Test
    void project_pointBehindPerspectiveCameraAndInvalidInputs_preservesOutput() {
        Camera3D camera = camera(false);
        MutableVec3 out = new MutableVec3(1, 2, 3);
        assertFalse(camera.project(new MutableVec3(0, 0, 1), out));
        assertFalse(camera.project(new MutableVec3(Float.NaN, 0, -1), out));
        assertFalse(camera.unproject(20, 30, Double.NaN, out));
        assertVector(out, 1, 2, 3);

        Ray ray = new Ray();
        assertFalse(camera.screenRay(Float.POSITIVE_INFINITY, 50, ray));
        assertVector(ray.origin(out), 0, 0, 0);
    }

    @Test
    void setViewportAndPose_updatesProjectionAndTracksOnlyChanges() {
        Camera3D camera = camera(true);
        long revision = camera.revision();
        Ray ray = new Ray();
        MutableVec3 out = new MutableVec3();
        assertTrue(camera.screenRay(0, 50, ray));
        assertVector(ray.origin(out), -2, 0, -1);

        camera.setViewport(200, 100).setPosition(0, 0, 0).setOrthographic(2, 1, 11);
        assertEquals(revision, camera.revision());
        assertThrows(IllegalArgumentException.class, () -> camera.setViewport(0, 100));
        assertThrows(IllegalArgumentException.class, () -> camera.setPerspective(1, 1, Float.POSITIVE_INFINITY));
        assertEquals(revision, camera.revision());

        camera.setViewport(100, 100).setPosition(3, 0, 2);
        assertEquals(revision + 2, camera.revision());
        assertTrue(camera.screenRay(0, 50, ray));
        assertVector(ray.origin(out), 2, 0, 1);
    }

    @Test
    void orbit_rotatePanAndZoom_keepsTargetAtViewportCenter() {
        Camera3D camera = camera(false);
        OrbitCameraController orbit = new OrbitCameraController(camera, new MutableVec3(1, 2, 3), 4);
        MutableVec3 out = new MutableVec3();

        assertVector(camera.position(out), 1, 2, 7);
        orbit.rotate(Math.PI / 2, 0);
        assertVector(camera.position(out), 5, 2, 3);
        orbit.pan(1, 2);
        assertVector(orbit.target(out), 1, 4, 2);
        assertVector(camera.position(out), 5, 4, 2);
        orbit.zoom(0.5f);
        assertEquals(2.0f, orbit.distance(), EPS);
        assertVector(camera.position(out), 3, 4, 2);
        orbit.rotate(0, Math.PI / 6);
        assertVector(camera.position(out), (float) (1 + Math.sqrt(3)), 5, 2);
        assertTrue(camera.project(orbit.target(out), out));
        assertEquals(100.0f, out.x(), EPS);
        assertEquals(50.0f, out.y(), EPS);
    }

    @Test
    void orbit_orthographicZoom_changesMagnificationWithSamePose() {
        Camera3D camera = camera(true);
        OrbitCameraController orbit = new OrbitCameraController(camera, new MutableVec3(), 4);

        orbit.zoom(0.5f);

        assertEquals(1.0f, camera.orthographicHeight(), EPS);
        assertVector(camera.position(new MutableVec3()), 0, 0, 4);
    }

    @Test
    void freeFly_lookAndMove_followsLocalAxes() {
        Camera3D camera = camera(false);
        FreeFlyCameraController fly = new FreeFlyCameraController(camera);

        fly.look(Math.PI / 2, 0);
        fly.move(1, 2, 3);
        assertVector(camera.position(new MutableVec3()), -3, 2, -1);

        fly.look(0, Math.PI);
        Ray ray = new Ray();
        assertTrue(camera.screenRay(100, 50, ray));
        assertTrue(ray.direction(new MutableVec3()).y() > 0.999f);
        fly.look(0, -Math.PI);
        assertTrue(camera.screenRay(100, 50, ray));
        assertTrue(ray.direction(new MutableVec3()).y() < -0.999f);
    }

    private static Camera3D camera(boolean orthographic) {
        Camera3D camera = new Camera3D().setViewport(200, 100);
        return orthographic ? camera.setOrthographic(2, 1, 11) : camera.setPerspective(RIGHT_ANGLE, 1, 11);
    }

    private static void assertVector(Vec3View vector, float x, float y, float z) {
        assertEquals(x, vector.x(), EPS);
        assertEquals(y, vector.y(), EPS);
        assertEquals(z, vector.z(), EPS);
    }
}
