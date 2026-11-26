package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableQuat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScenePickerTest {
    private static final Aabb BOX = new Aabb(-1, -1, -1, 1, 1, 1);
    private final Camera3D camera = new Camera3D().setViewport(100, 100).setOrthographic(4, 1, 10);
    private final ScenePicker picker = new ScenePicker();

    @Test
    void pick_scaledParents_selectsByWorldDistanceAndIgnoresHiddenBranch() {
        Scene scene = new Scene();
        SceneNode near = new SceneNode().setBounds(BOX).setPosition(0, 0, -2);
        SceneNode far = new SceneNode().setBounds(BOX).setPosition(0, 0, -1.75f);
        SceneNode nearParent = new SceneNode().setScale(-1, 1, 0.5f).setPosition(0, 0, -3)
                .setRotation(new MutableQuat().setYawPitch(0, 0.3)).addChild(near);
        SceneNode farParent = new SceneNode().setScale(1, 1, 4).setPosition(0, 0, -1.6f).addChild(far);
        scene.addRoot(farParent).addRoot(nearParent);

        assertSame(near, picker.pick(scene, camera, 50, 50));
        nearParent.setVisible(false);
        assertSame(far, picker.pick(scene, camera, 50, 50));
        farParent.setVisible(false);
        assertNull(picker.pick(scene, camera, 50, 50));
    }

    @Test
    void pick_meshInsideBoundsButOutsideTriangle_skipsMesh() {
        SceneNode mesh = triangle().setPosition(0, 0, -2);
        SceneNode behind = new SceneNode().setBounds(BOX).setPosition(0, 0, -5);
        Scene scene = new Scene().addRoot(behind).addRoot(mesh);

        assertSame(mesh, picker.pick(scene, camera, 40, 60));
        assertSame(behind, picker.pick(scene, camera, 65, 35));
    }

    @Test
    void pick_meshFlattenedAlongDepth_stillTestsTransformedTriangles() {
        SceneNode mesh = triangle().setPosition(0, 0, -2).setScale(1, 1, 0);
        Scene scene = new Scene().addRoot(mesh);

        assertSame(mesh, picker.pick(scene, camera, 40, 60));
        assertNull(picker.pick(scene, camera, 65, 35));
    }

    @Test
    void pick_bothClipPlanes_boundTheRaySegment() {
        SceneNode beforeNear = triangle().setPosition(0, 0, -0.5f);
        SceneNode beyondFar = triangle().setPosition(0, 0, -10.25f);
        Scene scene = new Scene().addRoot(beforeNear).addRoot(beyondFar);

        assertNull(picker.pick(scene, camera, 40, 60));
        beyondFar.setPosition(0, 0, -10);
        assertSame(beyondFar, picker.pick(scene, camera, 40, 60));
        beforeNear.setPosition(0, 0, -1);
        assertSame(beforeNear, picker.pick(scene, camera, 40, 60));
        assertNull(picker.pick(scene, camera, Float.NaN, 50));
    }

    private static SceneNode triangle() {
        return new SceneNode().setMesh(new Mesh3D(new float[]{-1, -1, 0, 1, -1, 0, -1, 1, 0},
                null, null, null, new int[]{0, 1, 2}));
    }
}
