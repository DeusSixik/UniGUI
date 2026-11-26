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
