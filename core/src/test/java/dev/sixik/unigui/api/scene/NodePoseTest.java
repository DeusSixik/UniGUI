package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Точка опоры узла, расположение модели предмета и поворот головы сущности.
 */
class NodePoseTest {
    @AfterEach
    void uninstall() {
        ScenePlatform.install(null);
    }

    @Test
    void origin_isThePivotOfRotationAndScale() {
        SceneNode node = new SceneNode().setPosition(10, 0, 0).centerOrigin()
                .setRotation(new MutableQuat().setYawPitch(Math.PI, 0));
        MutableMat4 local = node.localMatrix(new MutableMat4());

        // T(p) * R * S * T(-o): the pivot lands on the position.
        assertPoint(local, 0.5f, 0, 0.5f, 10, 0, 0);
        // The block corner (0,0,0) is half a block away from the pivot and turns around it.
        assertPoint(local, 0, 0, 0, 10.5f, 0, 0.5f);

        node.setOrigin(0, 0, 0).setRotation(new MutableQuat()).setScale(2, 2, 2);
        assertPoint(node.localMatrix(new MutableMat4()), 1, 1, 1, 12, 2, 2);
    }

    @Test
    void origin_scalesAroundThePivot() {
        SceneNode node = new SceneNode().setOrigin(1, 0, 0).setScale(3, 1, 1);
        assertPoint(node.localMatrix(new MutableMat4()), 1, 0, 0, 0, 0, 0);
        assertPoint(node.localMatrix(new MutableMat4()), 2, 0, 0, 3, 0, 0);
    }

    @Test
    void setters_keepTheConcreteTypeForChaining() {
        EntityNode mob = new EntityNode("minecraft:zombie");
        EntityNode chained = mob.setPosition(1, 2, 3).setScale(2, 2, 2).setHead(0.1f, 0);
        assertSame(mob, chained);
        ItemNode item = new ItemNode("minecraft:apple");
        assertSame(item, item.setPosition(0, 1, 0).setDisplay(ItemNode.Display.GROUND));
    }

    @Test
    void itemDisplay_selectsModelBounds() {
        ScenePlatform.install(new DisplayPlatform());
        ItemNode node = new ItemNode("minecraft:apple");

        assertEquals(ItemNode.Display.FIXED, node.display());
        assertEquals(new Aabb(0, 0, 0, 1, 1, 1), node.bounds());
        node.setDisplay(ItemNode.Display.GROUND);
        assertEquals(new Aabb(0, 0, 0, 0.25f, 0.25f, 0.25f), node.bounds());
        assertThrows(NullPointerException.class, () -> node.setDisplay(null));
    }

    @Test
    void head_isOptionalAndValidated() {
        EntityNode mob = new EntityNode("minecraft:zombie");
        assertFalse(mob.hasHead());

        mob.setHead(0.5f, -0.25f);
        assertTrue(mob.hasHead());
        assertEquals(0.5f, mob.headYaw());
        assertEquals(-0.25f, mob.headPitch());
        assertThrows(IllegalArgumentException.class, () -> mob.setHead(Float.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> mob.setHead(0, Float.POSITIVE_INFINITY));

        mob.clearHead();
        assertFalse(mob.hasHead());
    }

    @Test
    void timelineHead_interpolatesAndNeedsAnEntity() {
        EntityNode mob = new EntityNode("minecraft:zombie");
        SceneTimeline timeline = new SceneTimeline();
        timeline.track(mob).head(0, 0, 0, null).head(2, 1, 0.5f, null);

        timeline.sample(1);
        assertTrue(mob.hasHead());
        assertEquals(0.5f, mob.headYaw(), 1e-6f);
        assertEquals(0.25f, mob.headPitch(), 1e-6f);
        assertThrows(IllegalArgumentException.class,
                () -> new SceneTimeline().track(new SceneNode()).head(0, 0, 0, null));
    }

    private static void assertPoint(MutableMat4 m, float x, float y, float z, float ex, float ey, float ez) {
        MutableVec3 out = new MutableVec3();
        m.transformPoint(new MutableVec3(x, y, z), out);
        assertEquals(ex, out.x(), 1e-5f, "x");
        assertEquals(ey, out.y(), 1e-5f, "y");
        assertEquals(ez, out.z(), 1e-5f, "z");
    }

    /**
     * Платформа, у которой границы модели зависят от расположения.
     */
    private static final class DisplayPlatform implements ScenePlatform {
        @Override
        public Object resolveBlock(BlockSpec spec) {
            return spec;
        }

        @Override
        public Object nativeBlock(Object block, int meta) {
            return block;
        }

        @Override
        public Object resolveItem(String id) {
            return id;
        }

        @Override
        public Aabb itemBounds(Object item, ItemNode.Display display) {
            float size = display == ItemNode.Display.GROUND ? 0.25f : 1;
            return new Aabb(0, 0, 0, size, size, size);
        }
    }
}
