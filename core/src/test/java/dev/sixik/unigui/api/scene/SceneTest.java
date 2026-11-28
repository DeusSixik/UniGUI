package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SceneTest {
    // Допуск округления мировой точки до float после композиции нескольких матриц.
    private static final float EPS = 1e-5f;
    private static final Aabb UNIT_BOUNDS = new Aabb(-1, -1, -1, 1, 1, 1);

    /** Confirms that changing render inputs invalidates the graph only when the value changes. */
    @Test
    void sceneAndSurfaceSettings_changedValuesInvalidateCachedRender() {
        SceneNode block = new BlockNode("minecraft:stone");
        Scene scene = new Scene().addRoot(block);
        long revision = scene.revision();
        assertNull(scene.directionalShadows());
        assertFalse(scene.minecraftBlockLighting());
        assertTrue(block.castsShadow());
        assertTrue(block.receivesShadow());

        DirectionalShadows shadows = new DirectionalShadows(0, 1, 0, 8);
        SceneMaterialEffect effect = new SceneMaterialEffect(ShaderHandle.fragmentSource("test:surface", "void example() {}"));
        scene.setDirectionalShadows(shadows).setMinecraftBlockLighting(true).setMaterialTime(2);
        block.setCastShadow(false).setReceiveShadow(false).setMaterialEffect(effect);
        assertEquals(revision + 6, scene.revision());
        assertSame(shadows, scene.directionalShadows());
        assertSame(effect, block.materialEffect());
        assertEquals(2, scene.materialTime(), EPS);

        scene.setDirectionalShadows(new DirectionalShadows(0, 1, 0, 8))
                .setMinecraftBlockLighting(true).setMaterialTime(2);
        block.setCastShadow(false).setReceiveShadow(false).setMaterialEffect(effect);
        assertEquals(revision + 6, scene.revision());
        assertThrows(IllegalArgumentException.class, () -> scene.setMaterialTime(Float.NaN));
        assertEquals(revision + 6, scene.revision());

        scene.setDirectionalShadows(null);
        block.setMaterialEffect(null);
        assertEquals(revision + 8, scene.revision());
    }

    /** Rejects settings that cannot produce a finite projection or valid shadow comparison. */
    @Test
    void directionalShadows_invalidSettingsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DirectionalShadows(Float.NaN, 0, 0, 8));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalShadows(0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalShadows(0, 0, 0, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalShadows(0, 0, 0, 8, 127, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalShadows(0, 0, 0, 8, 1024, -0.01f, 1));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalShadows(0, 0, 0, 8, 1024, 0, 1.01f));
    }

    @Test
    void worldMatrix_parentRotationScaleAndTranslation_composeWithChildPose() {
        SceneNode parent = new SceneNode().setPosition(10, 2, 0).setScale(2, 3, 1)
                .setRotation(new MutableQuat().setAxisAngle(new MutableVec3(0, 0, 1), Math.PI / 2));
        SceneNode child = new SceneNode().setPosition(1, 2, 3).setScale(4, 1, 1);
        new Scene().addRoot(parent.addChild(child));
        MutableMat4 matrix = new MutableMat4();
        MutableVec3 out = new MutableVec3();

        assertTrue(child.localMatrix(matrix).transformPoint(new MutableVec3(1, 0, 0), out));
        assertPoint(out, 5, 2, 3);
        assertTrue(child.worldMatrix(matrix).transformPoint(new MutableVec3(1, 0, 0), out));
        assertPoint(out, 4, 12, 3);

        matrix.identity();
        assertWorldOrigin(child, 4, 4, 3);
    }

    @Test
    void worldMatrix_multipleAncestorEdits_invalidatePreviouslyReadDescendants() {
        SceneNode root = new SceneNode().setPosition(2, 0, 0);
        SceneNode middle = new SceneNode().setPosition(0, 3, 0);
        SceneNode leaf = new SceneNode().setPosition(0, 0, 4);
        root.addChild(middle.addChild(leaf));

        assertWorldOrigin(leaf, 2, 3, 4);
        root.setPosition(5, 0, 0);
        assertWorldOrigin(leaf, 5, 3, 4);
        root.setPosition(7, 0, 0);
        middle.setScale(-1, 2, 0);
        assertWorldOrigin(leaf, 7, 3, 0);
        assertWorldOrigin(middle, 7, 3, 0);
    }

    @Test
    void attach_moveWithinScene_reusesLocalPoseAndUpdatesSiblingOrder() {
        SceneNode parent = new SceneNode().setPosition(1, 0, 0);
        SceneNode sibling = new SceneNode();
        SceneNode child = new SceneNode().setPosition(0, 2, 0);
        SceneNode grandchild = new SceneNode().setPosition(0, 0, 3);
        Scene scene = new Scene().addRoot(parent).addRoot(child.addChild(grandchild)).addRoot(sibling);
        List<SceneNode> roots = scene.roots();
        long revision = scene.revision();

        assertWorldOrigin(grandchild, 0, 2, 3);
        parent.addChild(child);
        assertWorldOrigin(grandchild, 1, 2, 3);
        assertEquals(List.of(parent, sibling), roots);
        assertSame(parent, child.parent());
        assertSame(scene, grandchild.scene());
        assertEquals(revision + 1, scene.revision());
        assertFalse(scene.removeRoot(child));
        parent.addChild(child);
        assertEquals(revision + 1, scene.revision());

        assertEquals(List.of(parent, child, grandchild, sibling), visible(scene, null));
        assertTrue(scene.removeRoot(parent));
        assertEquals(List.of(sibling), visible(scene, null));
        assertThrows(UnsupportedOperationException.class, roots::clear);
        assertThrows(UnsupportedOperationException.class, () -> parent.children().add(sibling));
    }

    @Test
    void attach_moveBetweenScenesAndDetach_invalidatesBothSceneOwners() {
        SceneNode root = new SceneNode().setPosition(4, 0, 0);
        SceneNode child = new SceneNode().setPosition(0, 2, 0);
        SceneNode otherRoot = new SceneNode().setPosition(-3, 0, 0);
        Scene first = new Scene().addRoot(root.addChild(child));
        Scene second = new Scene().addRoot(otherRoot);
        long firstRevision = first.revision();
        long secondRevision = second.revision();

        assertWorldOrigin(child, 4, 2, 0);
        otherRoot.addChild(child);
        assertWorldOrigin(child, -3, 2, 0);
        assertEquals(firstRevision + 1, first.revision());
        assertEquals(secondRevision + 1, second.revision());
        assertTrue(root.children().isEmpty());
        assertSame(second, child.scene());

        assertTrue(otherRoot.removeChild(child));
        assertNull(child.parent());
        assertNull(child.scene());
        assertWorldOrigin(child, 0, 2, 0);
        assertEquals(secondRevision + 2, second.revision());
    }

    @Test
    void addChild_cycleProposal_preservesTheOriginalGraphAndSceneRevision() {
        SceneNode root = new SceneNode();
        SceneNode branch = new SceneNode();
        SceneNode leaf = new SceneNode();
        branch.addChild(leaf);
        Scene scene = new Scene().addRoot(root.addChild(branch));
        long revision = scene.revision();

        assertThrows(IllegalArgumentException.class, () -> leaf.addChild(root));
        assertThrows(IllegalArgumentException.class, () -> branch.addChild(branch));

        assertSame(root, branch.parent());
        assertSame(branch, leaf.parent());
        assertNull(root.parent());
        assertSame(scene, leaf.scene());
        assertEquals(revision, scene.revision());
    }

    @Test
    void worldMatrix_deepTreeTraversesWithoutRecursion() {
        SceneNode root = new SceneNode().setPosition(1, 2, 3);
        SceneNode leaf = root;
        int depth = 5000;
        for (int i = 0; i < depth; i++) {
            SceneNode child = new SceneNode();
            leaf.addChild(child);
            leaf = child;
        }
        Scene scene = new Scene().addRoot(root);
        leaf.setPosition(0, 1, 0);

        assertWorldOrigin(leaf, 1, 3, 3);
        assertEquals(depth + 1, visible(scene, null).size());
        root.setPosition(0, 0, 0);
        assertWorldOrigin(leaf, 0, 1, 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void visitVisible_frustumRejectsAllSixDirectionsAndKeepsIntersectingBounds(boolean orthographic) {
        Camera3D camera = new Camera3D().setViewport(100, 100);
        if (orthographic) {
            camera.setOrthographic(4, 1, 10);
        } else {
            camera.setPerspective((float) (Math.PI / 2), 1, 10);
        }
        Frustum3D frustum = new Frustum3D().set(camera.viewProjection(new MutableMat4()));
        Scene scene = new Scene();
        SceneNode inside = new SceneNode().setBounds(UNIT_BOUNDS).setPosition(0, 0, -3);
        SceneNode nearOverlap = new SceneNode().setBounds(UNIT_BOUNDS).setPosition(0, 0, -0.5f);
        SceneNode farOverlap = new SceneNode().setBounds(UNIT_BOUNDS).setPosition(0, 0, -10.5f);
        scene.addRoot(inside).addRoot(nearOverlap).addRoot(farOverlap);
        for (float[] position : new float[][]{
                {-20, 0, -3}, {20, 0, -3}, {0, -20, -3}, {0, 20, -3}, {0, 0, 2}, {0, 0, -12}}) {
            scene.addRoot(new SceneNode().setBounds(UNIT_BOUNDS).setPosition(position[0], position[1], position[2]));
        }

        assertEquals(List.of(inside, nearOverlap, farOverlap), visible(scene, frustum));
    }

    @Test
    void visitVisible_pointOnFarPlane_survivesPlaneExtractionRoundoff() {
        Camera3D camera = new Camera3D().setPerspective(1, 1, 51);
        Frustum3D frustum = new Frustum3D().set(camera.viewProjection(new MutableMat4()));
        Aabb point = new Aabb(0, 0, 0, 0, 0, 0);
        SceneNode atFar = new SceneNode().setBounds(point).setPosition(0, 0, -51);
        SceneNode beyondFar = new SceneNode().setBounds(point).setPosition(0, 0, -51.01f);
        Scene scene = new Scene().addRoot(atFar).addRoot(beyondFar);

        assertEquals(List.of(atFar), visible(scene, frustum));
    }

    @Test
    void visitVisible_culledParentAndHiddenBranch_differInWhetherChildrenAreVisited() {
        Frustum3D frustum = new Frustum3D();
        SceneNode outside = new SceneNode().setBounds(UNIT_BOUNDS).setPosition(4, 0, 0);
        SceneNode childInside = new SceneNode().setBounds(UNIT_BOUNDS).setPosition(-4, 0, 0);
        SceneNode hidden = new SceneNode().setVisible(false);
        SceneNode other = new SceneNode();
        Scene scene = new Scene().addRoot(outside.addChild(childInside))
                .addRoot(hidden.addChild(new SceneNode())).addRoot(other);

        assertEquals(List.of(childInside, other), visible(scene, frustum));
        assertEquals(List.of(outside, childInside, other), visible(scene, null));
    }

    @Test
    void visitVisible_rotatedAndReflectedScale_testsBoundsInWorldCoordinates() {
        Frustum3D frustum = new Frustum3D();
        SceneNode parent = new SceneNode().setScale(-2, 0.5f, 1)
                .setRotation(new MutableQuat().setAxisAngle(new MutableVec3(0, 0, 1), Math.PI / 2));
        SceneNode child = new SceneNode().setBounds(new Aabb(-1, -0.1f, 0, 1, 0.1f, 0))
                .setPosition(1.25f, 0, 0);
        Scene scene = new Scene().addRoot(parent.addChild(child));

        assertEquals(List.of(parent, child), visible(scene, frustum));
        parent.setScale(-8, 0.5f, 1);
        assertEquals(List.of(parent), visible(scene, frustum));
    }

    @Test
    void visitVisible_viewMatrixChange_movesTheCullingVolume() {
        Camera3D camera = new Camera3D().setOrthographic(4, 1, 10)
                .setPose(4, 0, 0, new MutableQuat().setYawPitch(Math.PI / 2, 0));
        MutableMat4 matrix = new MutableMat4();
        Frustum3D frustum = new Frustum3D().set(camera.viewProjection(matrix));
        SceneNode node = new SceneNode().setBounds(UNIT_BOUNDS);
        Scene scene = new Scene().addRoot(node);
        assertEquals(List.of(node), visible(scene, frustum));

        camera.setPosition(-4, 0, 0);
        assertEquals(List.of(), visible(scene, frustum.set(camera.viewProjection(matrix))));
    }

    @Test
    void visitVisible_visitorChangesStructure_failsBeforeAdvancingPastInvalidLinks() {
        SceneNode root = new SceneNode();
        Scene scene = new Scene().addRoot(root.addChild(new SceneNode()));

        assertThrows(ConcurrentModificationException.class, () -> scene.visitVisible(null, SceneNode::detach));
        assertTrue(scene.roots().isEmpty());
    }

    private static List<SceneNode> visible(Scene scene, Frustum3D frustum) {
        List<SceneNode> nodes = new ArrayList<>();
        scene.visitVisible(frustum, nodes::add);
        return nodes;
    }

    private static void assertWorldOrigin(SceneNode node, float x, float y, float z) {
        MutableVec3 point = new MutableVec3();
        assertTrue(node.worldMatrix(new MutableMat4()).transformPoint(point, point));
        assertPoint(point, x, y, z);
    }

    private static void assertPoint(MutableVec3 point, float x, float y, float z) {
        assertEquals(x, point.x(), EPS);
        assertEquals(y, point.y(), EPS);
        assertEquals(z, point.z(), EPS);
    }
}
