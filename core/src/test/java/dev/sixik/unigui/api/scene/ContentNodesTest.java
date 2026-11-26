package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Единые узлы {@link BlockNode}, {@link ItemNode} и {@link EntityNode} с фейковой платформой.
 */
class ContentNodesTest {
    @AfterEach
    void uninstall() {
        ScenePlatform.install(null);
    }

    @Test
    void blockString_isResolvedLazilyOnceAndCached() {
        FakePlatform platform = new FakePlatform();
        BlockNode node = new BlockNode("minecraft:furnace[lit=true]");

        assertEquals(0, platform.blockCalls);
        Object state = node.nativeState(platform);
        assertEquals("state:minecraft:furnace[lit=true]", state);
        assertSame(state, node.nativeState(platform));
        assertEquals(1, platform.blockCalls);
        assertNull(node.nativeState(null));
    }

    @Test
    void blockNative_isPassedWithMetadata() {
        FakePlatform platform = new FakePlatform();

        assertEquals("native:furnace@3", BlockNode.of("furnace-object", 3).nativeState(platform).toString()
                .replace("furnace-object", "furnace"));
        assertEquals("state:minecraft:stone", BlockNode.of("minecraft:stone").nativeState(platform));
    }

    @Test
    void unavailableContent_isRetriedUntilPlatformResolvesIt() {
        FakePlatform platform = new FakePlatform();
        platform.ready = false;
        BlockNode node = new BlockNode("minecraft:stone");

        assertNull(node.nativeState(platform));
        assertNull(node.nativeState(platform));
        platform.ready = true;
        assertEquals("state:minecraft:stone", node.nativeState(platform));
        assertEquals(3, platform.blockCalls);
    }

    @Test
    void invalidContent_isRememberedAndRethrownOnlyOnRequest() {
        FakePlatform platform = new FakePlatform();
        BlockNode node = new BlockNode("minecraft:missing");

        assertNull(node.nativeState(platform));
        assertNull(node.nativeState(platform));
        assertThrows(IllegalArgumentException.class, () -> node.requireNativeState(platform));
        assertEquals(1, platform.blockCalls);
    }

    @Test
    void setState_onlyMarksActualChangesAndResetsCache() {
        FakePlatform platform = new FakePlatform();
        BlockNode node = new BlockNode("minecraft:stone");
        Scene scene = new Scene().addRoot(node);
        long revision = scene.revision();
        node.nativeState(platform);

        node.setState("minecraft:stone");
        assertEquals(revision, scene.revision());
        node.setState("minecraft:dirt");
        assertEquals(revision + 1, scene.revision());
        assertEquals("state:minecraft:dirt", node.nativeState(platform));

        Object nativeState = new Object();
        node.setState(nativeState);
        node.setState(nativeState);
        assertEquals(revision + 2, scene.revision());
        assertNull(node.spec());
        assertSame(nativeState, node.nativeSource());
    }

    @Test
    void blockEntity_isReusedAcrossCompatibleStateChanges() {
        FakePlatform platform = new FakePlatform();
        BlockNode node = new BlockNode("minecraft:chest[facing=north]");

        Object first = node.blockEntity(platform);
        assertSame(first, node.blockEntity(platform));
        node.setState("minecraft:chest[facing=south]");
        Object second = node.blockEntity(platform);

        assertSame(first, second);
        assertEquals(List.of("minecraft:chest[facing=north]", "minecraft:chest[facing=south]"),
                platform.blockEntityStates);
        assertNull(new BlockNode("minecraft:stone").blockEntity(null));
    }

    @Test
    void blockBounds_areFullCubeExceptAirOnInstalledPlatform() {
        BlockNode stone = new BlockNode("minecraft:stone");
        BlockNode air = new BlockNode("minecraft:air");
        assertEquals(new Aabb(0, 0, 0, 1, 1, 1), air.bounds());

        ScenePlatform.install(new FakePlatform());
        assertEquals(new Aabb(0, 0, 0, 1, 1, 1), stone.bounds());
        assertNull(air.bounds());
    }

    @Test
    void itemBounds_useModelEmptyStackOrFallback() {
        ItemNode sword = new ItemNode("minecraft:diamond_sword");
        ItemNode empty = new ItemNode("minecraft:empty");
        Aabb fallback = new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
        assertEquals(fallback, sword.bounds());

        ScenePlatform.install(new FakePlatform());
        assertEquals(new Aabb(0, 0, 0, 2, 2, 0.1f), sword.bounds());
        assertNull(empty.bounds());
    }

    @Test
    void itemNative_andSetItemUseTheSameCache() {
        FakePlatform platform = new FakePlatform();
        Object stack = new Object();
        ItemNode node = ItemNode.of(stack);

        assertEquals(List.of("native-item", stack), node.nativeItem(platform));
        node.setItem("minecraft:apple");
        assertEquals("item:minecraft:apple", node.nativeItem(platform));
        assertNull(node.nativeSource());
    }

    @Test
    void entity_unsupportedPlatform_drawsNothingAndRequireReportsIt() {
        ScenePlatform blocksOnly = new ScenePlatform() {
            @Override
            public Object resolveBlock(BlockSpec spec) {
                return spec;
            }

            @Override
            public Object nativeBlock(Object block, int meta) {
                return block;
            }
        };
        EntityNode node = new EntityNode("minecraft:zombie");

        assertNull(node.nativeEntity(blocksOnly));
        assertThrows(UnsupportedOperationException.class, () -> node.requireNativeEntity(blocksOnly));
        assertThrows(UnsupportedOperationException.class, () -> new ItemNode("minecraft:apple")
                .requireNativeItem(blocksOnly));
    }

    @Test
    void entityBounds_comeFromPlatform() {
        ScenePlatform.install(new FakePlatform());
        EntityNode node = new EntityNode("minecraft:zombie");

        assertEquals(new Aabb(-0.3f, 0, -0.3f, 0.3f, 1.95f, 0.3f), node.bounds());
        assertNotSame(node.nativeEntity(new FakePlatform()), node.nativeEntity(ScenePlatform.current()));
    }

    @Test
    void emptyIds_areRejectedEagerly() {
        assertThrows(IllegalArgumentException.class, () -> new BlockNode(" "));
        assertThrows(IllegalArgumentException.class, () -> new ItemNode(""));
        assertThrows(IllegalArgumentException.class, () -> new EntityNode(" "));
    }

    /**
     * Платформа на строках: состояние блока — {@code "state:" + текст}, ошибочные идентификаторы содержат
     * {@code missing}.
     */
    private static final class FakePlatform implements ScenePlatform {
        private int blockCalls;
        private boolean ready = true;
        private final List<Object> blockEntityStates = new ArrayList<>();

        @Override
        public Object resolveBlock(BlockSpec spec) {
            blockCalls++;
            if (spec.id().contains("missing")) throw new IllegalArgumentException("Unknown block: " + spec);
            return ready ? "state:" + spec.text() : null;
        }

        @Override
        public Object nativeBlock(Object block, int meta) {
            blockCalls++;
            return "native:" + block + (meta >= 0 ? "@" + meta : "");
        }

        @Override
        public boolean isAirBlock(Object state) {
            return state.equals("state:minecraft:air");
        }

        @Override
        public Object blockEntity(Object state, Object previous, int x, int y, int z) {
            blockEntityStates.add(state.toString().substring("state:".length()));
            return previous != null ? previous : new Object();
        }

        @Override
        public Object resolveItem(String id) {
            return "item:" + id;
        }

        @Override
        public Object nativeItem(Object item) {
            return List.of("native-item", item);
        }

        @Override
        public Aabb itemBounds(Object item) {
            return item.equals("item:minecraft:empty") ? null : new Aabb(0, 0, 0, 2, 2, 0.1f);
        }

        @Override
        public Object resolveEntity(String id) {
            return new Object();
        }

        @Override
        public Aabb entityBounds(Object entity) {
            return new Aabb(-0.3f, 0, -0.3f, 0.3f, 1.95f, 0.3f);
        }
    }
}
