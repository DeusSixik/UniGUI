package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Общая логика {@link StructureNode} на платформе со строковыми состояниями.
 */
class StructureNodeTest {
    @AfterEach
    void uninstall() {
        ScenePlatform.install(null);
    }

    @Test
    void setBlock_atSectionCorner_invalidatesNearbySectionsAndScene() {
        StructureNode structure = new StructureNode(new FakePlatform());
        structure.setBlock(15, 15, 15, "minecraft:stone");
        structure.setBlock(16, 16, 16, "minecraft:stone");
        structure.setBlock(32, 32, 32, "minecraft:stone");
        Scene scene = new Scene().addRoot(structure);
        var sections = structure.sections().iterator();
        StructureNode.Section changed = sections.next();
        StructureNode.Section diagonal = sections.next();
        StructureNode.Section far = sections.next();
        long changedBefore = changed.revision();
        long diagonalBefore = diagonal.revision();
        long farBefore = far.revision();
        long sceneBefore = scene.revision();

        structure.setBlock(15, 15, 15, "minecraft:glass");
        structure.setBlock(15, 15, 15, "minecraft:glass");

        assertEquals(changedBefore + 1, changed.revision());
        assertEquals(diagonalBefore + 1, diagonal.revision());
        assertEquals(farBefore, far.revision());
        assertEquals(sceneBefore + 1, scene.revision());
        assertEquals("minecraft:glass", structure.block(15, 15, 15));

        structure.setBlock(15, 15, 15, "minecraft:air");
        assertEquals(2, structure.sections().size());
        assertNull(structure.block(15, 15, 15));
        assertEquals(diagonalBefore + 2, diagonal.revision());
        assertEquals(2, structure.blockCount());
    }

    @Test
    void cells_useLocalIndicesForNegativeCoordinates() {
        StructureNode structure = new StructureNode(new FakePlatform()).setBlock(-1, -17, 5, "minecraft:dirt");
        StructureNode.Section section = structure.sections().iterator().next();
        int index = StructureNode.Section.index(-1, -17, 5);

        assertEquals(-16, section.minBlockX());
        assertEquals(-32, section.minBlockY());
        assertEquals(0, section.minBlockZ());
        assertEquals("minecraft:dirt", section.state(index));
        assertEquals(-1, section.minBlockX() + StructureNode.Section.localX(index));
        assertEquals(-17, section.minBlockY() + StructureNode.Section.localY(index));
        assertEquals(5, section.minBlockZ() + StructureNode.Section.localZ(index));
        assertEquals(1, section.blockCount());
    }

    @Test
    void positions_outsideMinecraftRange_areRejectedAndReadAsEmpty() {
        StructureNode structure = new StructureNode(new FakePlatform());

        assertThrows(IllegalArgumentException.class, () -> structure.setBlock(0, 2048, 0, "minecraft:stone"));
        assertThrows(IllegalArgumentException.class, () -> structure.setBlock(1 << 25, 0, 0, "minecraft:stone"));
        structure.setBlock(StructureNode.MIN_HORIZONTAL, StructureNode.MIN_Y, StructureNode.MAX_HORIZONTAL, "minecraft:stone");
        assertEquals("minecraft:stone",
                structure.block(StructureNode.MIN_HORIZONTAL, StructureNode.MIN_Y, StructureNode.MAX_HORIZONTAL));
        assertNull(structure.block(0, 5000, 0));
    }

    @Test
    void platform_isTakenFromInstalledOneOnFirstWrite() {
        StructureNode structure = new StructureNode();
        assertThrows(IllegalStateException.class, () -> structure.setBlock(0, 0, 0, "minecraft:stone"));
        assertNull(structure.platform());

        FakePlatform platform = new FakePlatform();
        ScenePlatform.install(platform);
        structure.setBlock(0, 0, 0, "minecraft:stone");
        ScenePlatform.install(new FakePlatform());
        structure.setBlock(1, 0, 0, "minecraft:stone");

        assertSame(platform, structure.platform());
        assertEquals(2, platform.blockCalls);
    }

    @Test
    void invalidOrUnavailableBlocks_areReported() {
        FakePlatform platform = new FakePlatform();
        StructureNode structure = new StructureNode(platform);

        assertThrows(IllegalArgumentException.class, () -> structure.setBlock(0, 0, 0, "minecraft:missing"));
        assertThrows(IllegalArgumentException.class, () -> structure.setBlock(0, 0, 0, "bad id["));
        platform.ready = false;
        assertThrows(IllegalStateException.class, () -> structure.setBlock(0, 0, 0, "minecraft:stone"));
        assertEquals(0, structure.blockCount());
    }

    @Test
    void nativeBlocks_passThroughPlatformWithMetadata() {
        StructureNode structure = new StructureNode(new FakePlatform());
        structure.setBlock(0, 0, 0, (Object) "wool", 14).setBlock(1, 0, 0, (Object) "stone");

        assertEquals("native:wool@14", structure.block(0, 0, 0));
        assertEquals("native:stone", structure.block(1, 0, 0));
    }

    @Test
    void blockEntities_areCreatedAtTheirPositionAndKeptForCompatibleStates() {
        FakePlatform platform = new FakePlatform();
        StructureNode structure = new StructureNode(platform);
        structure.setBlock(3, 4, 5, "minecraft:chest[facing=north]");
        Object entity = structure.blockEntity(3, 4, 5);
        structure.setBlock(3, 4, 5, "minecraft:chest[facing=south]");
        StructureNode.Section section = structure.sections().iterator().next();

        assertSame(entity, structure.blockEntity(3, 4, 5));
        assertEquals(List.of("3,4,5", "3,4,5"), platform.blockEntityPositions);
        assertEquals(1, section.blockEntityCount());

        Object loaded = new Object();
        structure.setBlockEntity(3, 4, 5, loaded);
        assertSame(loaded, structure.blockEntity(3, 4, 5));
        assertThrows(IllegalArgumentException.class, () -> structure.setBlockEntity(0, 0, 0, loaded));

        structure.removeBlock(3, 4, 5);
        assertNull(structure.blockEntity(3, 4, 5));
        assertEquals(0, structure.sections().size());
    }

    /** Проверяет границы для picking и отсечения после удаления крайнего блока. */
    @Test
    void bounds_whenRemovingOuterBlocks_shrinksAndClears() {
        StructureNode structure = new StructureNode(new FakePlatform());
        assertNull(structure.bounds());

        structure.setBlock(-2, -1, 0, "minecraft:stone").setBlock(16, 2, 4, "minecraft:stone");
        assertEquals(new Aabb(-2, -1, 0, 17, 3, 5), structure.bounds());
        structure.removeBlock(-2, -1, 0);
        assertEquals(new Aabb(16, 2, 4, 17, 3, 5), structure.bounds());
        structure.setBlock(16, 2, 4, "minecraft:air");
        assertNull(structure.bounds());
    }

    /** Проверяет отсечение слоёв, изменение границ и перестроение граней на стыке секций. */
    @Test
    void setTopVisibleY_atSectionBoundary_updatesVisibleBlocksAndBounds() {
        StructureNode structure = new StructureNode(new FakePlatform());
        structure.setBlock(0, 15, 0, "minecraft:stone").setBlock(0, 16, 0, "minecraft:stone");
        var sections = structure.sections().iterator();
        var lower = sections.next();
        var upper = sections.next();
        long lowerRevision = lower.revision();
        long upperRevision = upper.revision();

        structure.setTopVisibleY(15);
        assertEquals("minecraft:stone", structure.block(0, 16, 0));
        assertNull(structure.visibleBlock(0, 16, 0));
        assertEquals("minecraft:stone", structure.visibleBlock(0, 15, 0));
        assertEquals(new Aabb(0, 15, 0, 1, 16, 1), structure.bounds());
        assertEquals(lowerRevision + 1, lower.revision());
        assertEquals(upperRevision + 1, upper.revision());
        structure.setTopVisibleY(Integer.MIN_VALUE);
        assertNull(structure.bounds());
        structure.setTopVisibleY(Integer.MAX_VALUE);
        assertEquals(new Aabb(0, 15, 0, 1, 17, 1), structure.bounds());
    }

    /** Проверяет время открытия слоёв и сохранение конечного среза. */
    @Test
    void playAssembly_withNegativeAndHiddenLayers_revealsOnlyTheRequestedSlice() {
        Scene3D viewport = new Scene3D();
        StructureNode structure = new StructureNode(new FakePlatform());
        structure.setBlock(0, -2, 0, "minecraft:stone").setBlock(0, 0, 0, "minecraft:stone")
                .setBlock(0, 4, 0, "minecraft:stone");
        viewport.scene().addRoot(structure);
        structure.playAssembly(viewport, 0.75f, 2);

        assertNull(structure.bounds());
        viewport.tick(new FrameContext(1, 0.25f, 0, FramePhase.ANIMATION));
        assertEquals(-2, structure.topVisibleY());
        assertEquals(new Aabb(0, -2, 0, 1, -1, 1), structure.bounds());
        viewport.tick(new FrameContext(2, 0.25f, 0, FramePhase.ANIMATION));
        assertEquals(-1, structure.topVisibleY());
        viewport.tick(new FrameContext(3, 0.25f, 0, FramePhase.ANIMATION));
        assertEquals(2, structure.topVisibleY());
        assertEquals(new Aabb(0, -2, 0, 1, 1, 1), structure.bounds());
        assertNull(structure.visibleBlock(0, 4, 0));
    }

    /** Проверяет замену активной анимации мгновенным показом и проверку аргументов. */
    @Test
    void playAssembly_whenReplacedWithZeroDuration_keepsNewLimit() {
        Scene3D viewport = new Scene3D();
        StructureNode structure = new StructureNode(new FakePlatform()).setBlock(0, 0, 0, "minecraft:stone");
        assertThrows(IllegalArgumentException.class, () -> structure.playAssembly(viewport, 1, 0));
        viewport.scene().addRoot(structure);
        assertThrows(IllegalArgumentException.class, () -> structure.playAssembly(viewport, Float.NaN, 0));
        structure.playAssembly(viewport, 1, 0);
        structure.playAssembly(viewport, 0, -5);

        viewport.tick(new FrameContext(1, 0.25f, 0, FramePhase.ANIMATION));
        assertEquals(-5, structure.topVisibleY());
    }

    /** Проверяет дальность от мировых границ и совместимость с послойным просмотром. */
    @Test
    void distantMesh_forTransformedStructure_switchesByWorldDistanceAndRespectsSlice() {
        Mesh3D proxy = new Mesh3D(new float[]{0, 0, 0, 2, 0, 0, 0, 1, 0}, null, null, null, new int[]{0, 1, 2});
        StructureNode structure = new StructureNode(new FakePlatform()).setBlock(0, 0, 0, "minecraft:stone")
                .setBlock(1, 0, 0, "minecraft:stone").setDistantMesh(proxy, null, 3);
        structure.setPosition(10, 0, 20).setRotation(new MutableQuat().setYawPitch(Math.PI / 2, 0)).setScale(2, 1, 1);
        MutableMat4 world = structure.worldMatrix(new MutableMat4());
        MutableVec3 eye = new MutableVec3(10, 0.5f, 14);

        assertNull(structure.distantMesh(eye, world));
        eye.set(10, 0.5f, 12);
        var distant = structure.distantMesh(eye, world);
        assertSame(proxy, distant.mesh());
        assertSame(Material.DEFAULT, distant.material());
        assertNull(structure.distantMesh(null, world));
        structure.setTopVisibleY(0);
        assertNull(structure.distantMesh(eye, world));
        structure.setTopVisibleY(Integer.MAX_VALUE).removeBlock(1, 0, 0);
        assertNull(structure.distantMesh(eye, world));
    }

    /** Проверяет свободные ячейки, срез высоты и координаты видимого блока. */
    @Test
    void pick_onSparseStructure_findsNearestVisibleCell() {
        StructureNode structure = new StructureNode(new FakePlatform());
        structure.setBlock(0, 0, 0, "minecraft:stone")
                .setBlock(0, 0, -17, "minecraft:stone")
                .setBlock(2, 0, 0, "minecraft:stone");
        Camera3D camera = new Camera3D().setViewport(100, 100).setOrthographic(4, 0.1f, 40);
        new OrbitCameraController(camera, new MutableVec3(1.5f, 0.5f, 0), 5);
        var picker = new ScenePicker();
        var cell = new MutableVec3();
        Scene scene = new Scene().addRoot(structure);

        assertNull(picker.pick(scene, camera, 50, 50, cell));
        assertSame(structure, picker.pick(scene, camera, 25, 50, cell));
        assertEquals(0, cell.z(), 0);
        structure.removeBlock(0, 0, 0);
        assertSame(structure, picker.pick(scene, camera, 25, 50, cell));
        assertEquals(-17, cell.z(), 0);
        structure.setTopVisibleY(-1);
        assertNull(picker.pick(scene, camera, 25, 50, cell));
    }

    @Test
    void sections_viewIsReadOnly() {
        StructureNode structure = new StructureNode(new FakePlatform()).setBlock(0, 0, 0, "minecraft:stone");
        assertThrows(UnsupportedOperationException.class, () -> structure.sections().clear());
    }

    /**
     * Платформа на строках: состояние — текст блока, воздух — {@code minecraft:air}, block entity есть у сундуков.
     */
    private static final class FakePlatform implements ScenePlatform {
        private int blockCalls;
        private boolean ready = true;
        private final List<String> blockEntityPositions = new ArrayList<>();

        @Override
        public Object resolveBlock(BlockSpec spec) {
            blockCalls++;
            if (spec.id().contains("missing")) throw new IllegalArgumentException("Unknown block: " + spec);
            return ready ? spec.text() : null;
        }

        @Override
        public Object nativeBlock(Object block, int meta) {
            return "native:" + block + (meta >= 0 ? "@" + meta : "");
        }

        @Override
        public boolean isAirBlock(Object state) {
            return state.equals("minecraft:air");
        }

        @Override
        public Object blockEntity(Object state, Object previous, int x, int y, int z) {
            if (!state.toString().startsWith("minecraft:chest")) return null;
            blockEntityPositions.add(x + "," + y + "," + z);
            return previous != null ? previous : new Object();
        }
    }
}
