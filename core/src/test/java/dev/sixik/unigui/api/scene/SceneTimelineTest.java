package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет ключевые кадры сцены: интерполяцию, дискретные свойства и перемотку назад.
 */
class SceneTimelineTest {
    /** Allows rounding from a few float operations on coordinates, angles and phase. */
    private static final float FLOAT_EPS = 1e-5f;

    @Test
    void position_interpolatesWithTheEasingOfTheNextKey() {
        SceneNode node = new SceneNode();
        SceneTimeline timeline = new SceneTimeline();
        timeline.track(node).position(1, 0, 4, 0).position(3, 0, 0, 0, AnimationEasing.QUAD_IN);

        timeline.sample(0);
        assertEquals(4, y(node), 1e-6f, "before the first key the first value holds");
        timeline.sample(2);
        // quadIn(0.5) = 0.25: 4 + (0 - 4) * 0.25.
        assertEquals(3, y(node), 1e-6f);
        timeline.sample(10);
        assertEquals(0, y(node), 1e-6f, "after the last key the last value holds");
        assertEquals(3, timeline.duration());
    }

    @Test
    void keys_canBeAddedInAnyOrder() {
        SceneNode node = new SceneNode();
        SceneTimeline timeline = new SceneTimeline();
        timeline.track(node).position(2, 2, 0, 0).position(0, 0, 0, 0);
        timeline.sample(1);
        assertEquals(1, node.position(new MutableVec3()).x(), 1e-6f);
    }

    @Test
    void visible_switchesAtTheKeyAndRewinds() {
        SceneNode node = new SceneNode();
        SceneTimeline timeline = new SceneTimeline();
        timeline.track(node).visibleBetween(1, 2);

        timeline.sample(0.5f);
        assertFalse(node.visible());
        timeline.sample(1);
        assertTrue(node.visible(), "a discrete key applies from its own time");
        timeline.sample(1.99f);
        assertTrue(node.visible());
        timeline.sample(2);
        assertFalse(node.visible());
        timeline.sample(1.5f);
        assertTrue(node.visible(), "seeking backwards gives the same frame");
    }

    @Test
    void state_switchesBlockStatesOnlyWhenTheyChange() {
        BlockNode furnace = new BlockNode("minecraft:furnace");
        SceneTimeline timeline = new SceneTimeline();
        timeline.track(furnace).state(0, "minecraft:furnace").state(4, "minecraft:furnace[lit=true]");

        timeline.sample(3.9f);
        assertEquals("minecraft:furnace", furnace.spec().text());
        timeline.sample(4);
        assertEquals("minecraft:furnace[lit=true]", furnace.spec().text());
        timeline.sample(0);
        assertEquals("minecraft:furnace", furnace.spec().text());

        assertThrows(IllegalArgumentException.class, () -> timeline.track(new SceneNode()).state(0, "minecraft:stone"));
        assertThrows(IllegalArgumentException.class, () -> timeline.track(furnace).state(1, "minecraft:furnace[lit"));
    }

    @Test
    void rotation_usesYawAndPitch() {
        SceneNode node = new SceneNode();
        SceneTimeline timeline = new SceneTimeline();
        timeline.track(node).yaw(0, 0).yaw(2, (float) Math.PI);
        timeline.sample(1);
        MutableQuat expected = new MutableQuat().setYawPitch(Math.PI / 2, 0);
        MutableQuat actual = node.rotation(new MutableQuat());
        assertEquals(expected.y(), actual.y(), 1e-6f);
        assertEquals(expected.w(), actual.w(), 1e-6f);
    }

    /** Locomotion seeks through the same gait and returns to idle before playback starts. */
    @Test
    void walk_seek_resamplesGaitAndModelClock() {
        var mob = new EntityNode("minecraft:zombie");
        var scene = new Scene().addRoot(mob);
        var timeline = new SceneTimeline();
        timeline.track(mob).walk(0, 0, 0).walk(2, 12, 0.8f, AnimationEasing.QUAD_IN)
                .walk(3, 16, 0);

        timeline.sample(1);
        assertTrue(mob.hasAnimation());
        assertEquals(1, mob.animationSeconds(), 1e-6f);
        assertEquals(3, mob.walkPhase(), 1e-6f);
        assertEquals(0.2f, mob.walkStrength(), 1e-6f);
        long revision = scene.revision();
        timeline.sample(1);
        assertEquals(revision, scene.revision());
        timeline.sample(3);
        assertEquals(0, mob.walkStrength(), 1e-6f);
        timeline.sample(1);
        assertEquals(3, mob.walkPhase(), 1e-6f);
        assertTrue(scene.revision() > revision);
        timeline.sample(-1);
        assertEquals(0, mob.animationSeconds(), 1e-6f);
        assertEquals(0, mob.walkStrength(), 1e-6f);
        mob.clearAnimation();
        assertFalse(mob.hasAnimation());
    }

    /** Invalid gait inputs and unsupported track targets are rejected. */
    @Test
    void walk_invalidInput_isRejected() {
        var mob = new EntityNode("minecraft:spider");
        var timeline = new SceneTimeline();
        assertThrows(IllegalArgumentException.class, () -> timeline.track(new SceneNode()).walk(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> timeline.track(mob).walk(0, 0, 1.1f));
        assertThrows(IllegalArgumentException.class, () -> timeline.track(mob).walk(0, Float.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> mob.setAnimation(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> mob.setAnimation(0, 0, Float.NaN));
    }

    /** Checks diagonal facing, proportional gait with easing, and deterministic rewind. */
    @Test
    void walkTo_easedMovement_syncsFacingAndGaitWithDistance() {
        var mob = new EntityNode("minecraft:zombie").setPosition(1, 2, 3);
        var timeline = new SceneTimeline();
        timeline.track(mob).walkTo(2, 6, 4, 2, 7, AnimationEasing.QUAD_IN, 10, 0.8f);
        var position = new MutableVec3();
        var forward = new MutableVec3();

        timeline.sample(4);
        assertEquals(1.75f, mob.position(position).x(), FLOAT_EPS);
        assertEquals(4, position.z(), FLOAT_EPS);
        assertEquals(12.5f, mob.walkPhase(), FLOAT_EPS); // A quarter of a 3-4-5 segment, at 10 phase units per unit.
        assertEquals(0.8f, mob.walkStrength(), FLOAT_EPS);
        mob.rotation(new MutableQuat()).rotate(0, 0, 1, forward);
        assertEquals(0.6f, forward.x(), FLOAT_EPS);
        assertEquals(0.8f, forward.z(), FLOAT_EPS);

        timeline.sample(6);
        assertEquals(7, mob.position(position).z(), FLOAT_EPS);
        assertEquals(50, mob.walkPhase(), FLOAT_EPS);
        assertEquals(0, mob.walkStrength(), FLOAT_EPS);
        assertEquals(6, timeline.duration(), FLOAT_EPS);
        timeline.sample(1);
        assertEquals(3, mob.position(position).z(), FLOAT_EPS);
        assertEquals(0, mob.walkPhase(), FLOAT_EPS);
        assertEquals(0, mob.walkStrength(), FLOAT_EPS);
        timeline.sample(4);
        assertEquals(4, mob.position(position).z(), FLOAT_EPS);
        assertEquals(12.5f, mob.walkPhase(), FLOAT_EPS);
    }

    /** Checks adjacent segments, a vertical move and a manually keyed turn on arrival. */
    @Test
    void walkTo_adjacentSegments_carryPhaseAndAllowManualFacing() {
        var mob = new EntityNode("minecraft:spider");
        var timeline = new SceneTimeline();
        timeline.track(mob).position(0, 1, 1, 1).walk(0, 3, 0)
                .walkTo(0, 1, 1, 1, 2)
                .walkTo(1, 2, 0, 1, 2)
                .walkTo(2, 3, 0, 2, 2)
                .yaw(3, 0);
        var forward = new MutableVec3();

        timeline.sample(1);
        assertEquals(11, mob.walkPhase(), FLOAT_EPS);
        assertEquals(0, mob.walkStrength(), FLOAT_EPS);
        mob.rotation(new MutableQuat()).rotate(0, 0, 1, forward);
        assertEquals(-1, forward.x(), FLOAT_EPS);
        timeline.sample(1.5f);
        assertEquals(0.5f, mob.position(new MutableVec3()).x(), FLOAT_EPS);
        assertEquals(15, mob.walkPhase(), FLOAT_EPS);
        assertEquals(0.6f, mob.walkStrength(), FLOAT_EPS);
        timeline.sample(2.5f);
        assertEquals(1.5f, mob.position(new MutableVec3()).y(), FLOAT_EPS);
        assertEquals(19, mob.walkPhase(), FLOAT_EPS);
        assertEquals(0, mob.walkStrength(), FLOAT_EPS);
        timeline.sample(3);
        mob.rotation(new MutableQuat()).rotate(0, 0, 1, forward);
        assertEquals(1, forward.z(), FLOAT_EPS);
    }

    /** Checks that invalid arguments leave the timeline without a partially added walk. */
    @Test
    void walkTo_invalidInput_leavesNoKeys() {
        var timeline = new SceneTimeline();
        var mob = new EntityNode("minecraft:zombie");
        var track = timeline.track(mob);
        assertThrows(IllegalArgumentException.class, () -> track.walkTo(1, 1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> track.walkTo(1, 2, Float.POSITIVE_INFINITY, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> track.walkTo(1, 2, Float.MAX_VALUE, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> track.walkTo(1, 2, 1, 0, 0, null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> timeline.track(new SceneNode()).walkTo(0, 1, 1, 0, 0));
        assertEquals(0, timeline.duration(), FLOAT_EPS);
        timeline.sample(2);
        assertFalse(mob.hasAnimation());
        assertEquals(0, mob.position(new MutableVec3()).x(), FLOAT_EPS);
    }

    /** Checks the same nested time offset for tracks and callbacks during direct seeks and rewinds. */
    @Test
    void stage_nestedTimeline_usesLocalTimeAndExtendsParentDuration() {
        var node = new SceneNode();
        var sampled = new ArrayList<Float>();
        var timeline = new SceneTimeline().onSample(sampled::add);
        var stage = timeline.stage(4);
        stage.onSample(sampled::add);
        var nested = stage.stage(1);
        nested.track(node).visibleBetween(0, 2);
        nested.onSample(sampled::add);
        timeline.onSample(sampled::add);

        assertEquals(7, timeline.duration(), FLOAT_EPS);
        timeline.sample(5);
        assertTrue(node.visible());
        timeline.sample(7);
        assertFalse(node.visible());
        timeline.sample(3);
        assertFalse(node.visible());
        timeline.sample(6);
        assertTrue(node.visible());
        assertEquals(List.of(5f, 1f, 0f, 5f, 7f, 3f, 2f, 7f, 3f, -1f, -2f, 3f, 6f, 2f, 1f, 6f), sampled);
        nested.duration(4);
        assertEquals(9, timeline.duration(), FLOAT_EPS);
        assertEquals(8, timeline.duration(8).duration(), FLOAT_EPS);
        assertThrows(IllegalArgumentException.class, () -> timeline.stage(-1));
        assertThrows(IllegalArgumentException.class, () -> timeline.stage(Float.NaN));
    }

    @Test
    void during_clampsProgressAndOnSampleGetsTheTime() {
        List<Float> progress = new ArrayList<>();
        List<Float> times = new ArrayList<>();
        SceneTimeline timeline = new SceneTimeline()
                .during(1, 3, null, progress::add)
                .onSample(times::add);
        timeline.sample(0);
        timeline.sample(2);
        timeline.sample(5);
        assertEquals(List.of(0f, 0.5f, 1f), progress);
        assertEquals(List.of(0f, 2f, 5f), times);
        assertEquals(3, timeline.duration());
        assertEquals(6, timeline.duration(6).duration());
    }

    @Test
    void assemble_revealsLayersFromBottomToTop() {
        StructureNode structure = new StructureNode(new StringPlatform());
        for (int y = 0; y < 4; y++) structure.setBlock(0, y, 0, "minecraft:stone");
        SceneTimeline timeline = new SceneTimeline().assemble(structure, 0, 4);

        timeline.sample(0);
        assertEquals(-1, structure.topVisibleY(), "nothing is visible at the start");
        timeline.sample(2);
        // floor(-1 + 0.5 * 4) = 1.
        assertEquals(1, structure.topVisibleY());
        timeline.sample(4);
        assertEquals(Integer.MAX_VALUE, structure.topVisibleY());

        structure.setBlock(0, 7, 0, "minecraft:stone");
        timeline.sample(2);
        // The new top layer is seen: floor(-1 + 0.5 * 8) = 3.
        assertEquals(3, structure.topVisibleY());
    }

    @Test
    void negativeOrInfiniteTimes_areRejected() {
        SceneTimeline timeline = new SceneTimeline();
        SceneNode node = new SceneNode();
        assertThrows(IllegalArgumentException.class, () -> timeline.track(node).position(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> timeline.track(node).visible(Float.NaN, true));
        assertThrows(IllegalArgumentException.class, () -> timeline.during(2, 1, null, value -> {
        }));
    }

    /**
     * @param node узел. @return его координата Y.
     */
    private static float y(SceneNode node) {
        return node.position(new MutableVec3()).y();
    }

    /**
     * Платформа, у которой состояние блока — его строка.
     */
    private static final class StringPlatform implements ScenePlatform {
        @Override
        public Object resolveBlock(BlockSpec spec) {
            return spec.text();
        }

        @Override
        public Object nativeBlock(Object block, int meta) {
            return block;
        }
    }
}
