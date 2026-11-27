package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.scene.Scene;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Verifies the editable scene objects used by the Unity inspector demo. */
class UnitySceneObjectTest {
    /** Allows float rounding in the quaternion rotation of a unit vector. */
    private static final float EPSILON = 1e-6f;

    /** Verifies mesh-backed create-menu entries expose usable geometry and bounds. */
    @Test
    void meshTypes_createRenderableBounds() {
        for (UnityEditorHierarchyView.GameObjectType type : new UnityEditorHierarchyView.GameObjectType[]{
                UnityEditorHierarchyView.GameObjectType.CUBE,
                UnityEditorHierarchyView.GameObjectType.SPHERE,
                UnityEditorHierarchyView.GameObjectType.CAPSULE,
                UnityEditorHierarchyView.GameObjectType.PLANE}) {
            UnitySceneObject object = new UnitySceneObject(type.name(), type);
            assertNotNull(object.mesh(), type.name());
            assertNotNull(object.bounds(), type.name());
        }
    }

    /** Verifies local transform edits remain attached to their shared scene graph. */
    @Test
    void transformEdits_preserveSceneOwnership() {
        Scene scene = new Scene();
        UnitySceneObject parent = new UnitySceneObject("Parent", UnityEditorHierarchyView.GameObjectType.EMPTY);
        UnitySceneObject child = new UnitySceneObject("Child", UnityEditorHierarchyView.GameObjectType.CUBE);
        scene.addRoot(parent);
        parent.addChild(child);

        child.setPosition(1.0f, 2.0f, 3.0f);
        child.setAngles(0, 90, 0);
        child.setScale(2.0f, 3.0f, 4.0f);

        assertSame(scene, child.scene());
        assertSame(parent, child.parent());
        assertEquals(1.0f, child.position(new MutableVec3()).x(), EPSILON);
        assertEquals(90.0f, child.angles().y(), EPSILON);
        assertEquals(4.0f, child.scale(new MutableVec3()).z(), EPSILON);
        MutableVec3 rotated = child.rotation(new MutableQuat()).rotate(1, 0, 0, new MutableVec3());
        assertEquals(0.0f, rotated.x(), EPSILON);
        assertEquals(-1.0f, rotated.z(), EPSILON);
    }

    /**
     * Verifies the Inspector's Euler solution reconstructs the orientation after a gizmo edit.
     * @param pitch rotation about X in degrees
     * @param yaw rotation about Y in degrees
     * @param roll rotation about Z in degrees
     */
    @ParameterizedTest
    @CsvSource({"0, 0, 0", "30, -47, 98", "35, 90, -70", "35, -90, -70"})
    void angles_fromQuaternion_roundTripOrientation(float pitch, float yaw, float roll) {
        var object = new UnitySceneObject("Cube", UnityEditorHierarchyView.GameObjectType.CUBE);
        var inspector = new UnitySceneObject("Inspector", UnityEditorHierarchyView.GameObjectType.CUBE);
        object.setAngles(pitch, yaw, roll);
        var expected = object.rotation(new MutableQuat());
        inspector.setRotation(expected);

        MutableVec3 angles = inspector.angles();
        inspector.setAngles(angles.x(), angles.y(), angles.z());

        MutableQuat reconstructed = inspector.rotation(new MutableQuat());
        double dot = expected.x() * reconstructed.x() + expected.y() * reconstructed.y()
                + expected.z() * reconstructed.z() + expected.w() * reconstructed.w();
        assertEquals(1, Math.abs(dot), EPSILON);
    }
}
