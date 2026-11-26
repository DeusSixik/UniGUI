package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.OrbitCameraController;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.ScenePicker;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StructureNodeTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void setBlock_atSectionCorner_invalidatesNearbySectionsAndScene() {
        StructureNode structure = new StructureNode();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockPos position = new BlockPos(15, 15, 15);
        structure.setBlock(position, stone);
        structure.setBlock(new BlockPos(16, 16, 16), stone);
        structure.setBlock(new BlockPos(32, 32, 32), stone);
        Scene scene = new Scene().addRoot(structure);
        var sections = structure.sections().iterator();
        StructureNode.Section changed = sections.next();
        StructureNode.Section diagonal = sections.next();
        StructureNode.Section far = sections.next();
        long changedBefore = changed.revision;
        long diagonalBefore = diagonal.revision;
        long farBefore = far.revision;
        long sceneBefore = scene.revision();

        structure.setBlock(position, Blocks.GLASS.defaultBlockState());
        structure.setBlock(position, Blocks.GLASS.defaultBlockState());

        assertEquals(changedBefore + 1, changed.revision);
        assertEquals(diagonalBefore + 1, diagonal.revision);
        assertEquals(farBefore, far.revision);
        assertEquals(sceneBefore + 1, scene.revision());
        assertSame(Blocks.GLASS.defaultBlockState(), structure.block(position));

        structure.setBlock(position, Blocks.AIR.defaultBlockState());
        assertEquals(2, structure.sections().size());
        assertSame(Blocks.AIR.defaultBlockState(), structure.block(position));
        assertEquals(diagonalBefore + 2, diagonal.revision);
    }

    @Test
    void setBlock_withMutablePosition_storesAnImmutableKey() {
        StructureNode structure = new StructureNode();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(-1, -1, -1);
        structure.setBlock(position, Blocks.OAK_LEAVES.defaultBlockState());
        position.set(16, 16, 16);

        assertSame(Blocks.OAK_LEAVES.defaultBlockState(), structure.block(new BlockPos(-1, -1, -1)));
        assertSame(Blocks.AIR.defaultBlockState(), structure.block(position));
    }

    /** Проверяет границы для picking и отсечения после удаления крайнего блока. */
    @Test
    void bounds_whenRemovingOuterBlocks_shrinksAndClears() {
        StructureNode structure = new StructureNode();
        BlockPos left = new BlockPos(-2, -1, 0);
        BlockPos right = new BlockPos(16, 2, 4);
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        assertNull(structure.bounds());

        structure.setBlock(left, stone).setBlock(right, stone);
        assertEquals(new Aabb(-2, -1, 0, 17, 3, 5), structure.bounds());
        structure.setBlock(left, air);
        assertEquals(new Aabb(16, 2, 4, 17, 3, 5), structure.bounds());
        structure.setBlock(right, air);
        assertNull(structure.bounds());
    }

    /** Проверяет отсечение слоёв, изменение границ и перестроение граней на стыке секций. */
    @Test
    void setTopVisibleY_atSectionBoundary_updatesVisibleGeometryAndBounds() {
        StructureNode structure = new StructureNode();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockPos below = new BlockPos(0, 15, 0);
        BlockPos above = new BlockPos(0, 16, 0);
        structure.setBlock(below, stone).setBlock(above, stone);
        var sections = structure.sections().iterator();
        var lower = sections.next();
        var upper = sections.next();
        long lowerRevision = lower.revision;
        long upperRevision = upper.revision;

        structure.setTopVisibleY(15);
        assertSame(stone, structure.block(above));
        assertSame(Blocks.AIR.defaultBlockState(), structure.visibleBlock(above));
        assertSame(stone, structure.visibleBlock(below));
        assertEquals(new Aabb(0, 15, 0, 1, 16, 1), structure.bounds());
        assertEquals(lowerRevision + 1, lower.revision);
        assertEquals(upperRevision + 1, upper.revision);
        structure.setTopVisibleY(Integer.MIN_VALUE);
        assertNull(structure.bounds());
        structure.setTopVisibleY(Integer.MAX_VALUE);
        assertEquals(new Aabb(0, 15, 0, 1, 17, 1), structure.bounds());
    }

    /** Проверяет время открытия слоёв и сохранение конечного среза. */
    @Test
    void playAssembly_withNegativeAndHiddenLayers_revealsOnlyTheRequestedSlice() {
        Scene3D viewport = new Scene3D();
        StructureNode structure = new StructureNode();
        BlockState stone = Blocks.STONE.defaultBlockState();
        structure.setBlock(new BlockPos(0, -2, 0), stone).setBlock(BlockPos.ZERO, stone)
                .setBlock(new BlockPos(0, 4, 0), stone);
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
        assertSame(Blocks.AIR.defaultBlockState(), structure.visibleBlock(new BlockPos(0, 4, 0)));
    }

    /** Проверяет замену активной анимации мгновенным показом и проверку аргументов. */
    @Test
    void playAssembly_whenReplacedWithZeroDuration_keepsNewLimit() {
        Scene3D viewport = new Scene3D();
        StructureNode structure = new StructureNode().setBlock(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
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
        BlockState stone = Blocks.STONE.defaultBlockState();
        StructureNode structure = new StructureNode().setBlock(BlockPos.ZERO, stone)
                .setBlock(new BlockPos(1, 0, 0), stone).setDistantMesh(proxy, null, 3);
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
        structure.setTopVisibleY(Integer.MAX_VALUE).setBlock(new BlockPos(1, 0, 0), Blocks.AIR.defaultBlockState());
        assertNull(structure.distantMesh(eye, world));
    }

    /** Проверяет сохранение данных block entity при обновлении свойств блока. */
    @Test
    void setBlock_withCompatibleState_preservesBlockEntity() {
        StructureNode structure = new StructureNode();
        BlockPos pos = BlockPos.ZERO;
        BlockState furnace = Blocks.FURNACE.defaultBlockState();
        structure.setBlock(pos, furnace);
        var entity = structure.blockEntity(pos);
        BlockState lit = furnace.setValue(FurnaceBlock.LIT, true);
        structure.setBlock(pos, lit);

        assertSame(entity, structure.blockEntity(pos));
        assertSame(lit, entity.getBlockState());
        structure.setBlock(pos, Blocks.STONE.defaultBlockState());
        assertNull(structure.blockEntity(pos));
    }

    /** Проверяет свободные ячейки, срез высоты и координаты видимого блока. */
    @Test
    void pick_onSparseStructure_findsNearestVisibleCell() {
        StructureNode structure = new StructureNode();
        BlockState stone = Blocks.STONE.defaultBlockState();
        structure.setBlock(new BlockPos(0, 0, 0), stone)
                .setBlock(new BlockPos(0, 0, -17), stone)
                .setBlock(new BlockPos(2, 0, 0), stone);
        Camera3D camera = new Camera3D().setViewport(100, 100).setOrthographic(4, 0.1f, 40);
        new OrbitCameraController(camera, new MutableVec3(1.5f, 0.5f, 0), 5);
        var picker = new ScenePicker();
        var cell = new MutableVec3();
        Scene scene = new Scene().addRoot(structure);

        assertNull(picker.pick(scene, camera, 50, 50, cell));
        assertSame(structure, picker.pick(scene, camera, 25, 50, cell));
        assertEquals(0, cell.z(), 0);
        structure.setBlock(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        assertSame(structure, picker.pick(scene, camera, 25, 50, cell));
        assertEquals(-17, cell.z(), 0);
        structure.setTopVisibleY(-1);
        assertNull(picker.pick(scene, camera, 25, 50, cell));
    }

    @Test
    void fromNbt_compressedMultiPalette_usesFirstPaletteAndReadsStateProperties() throws IOException {
        BlockState furnace = Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.LIT, true);
        CompoundTag template = template(furnace, 0);
        ListTag alternatives = new ListTag();
        alternatives.add(template.get("palette"));
        template.remove("palette");
        ListTag second = new ListTag();
        second.add(NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
        alternatives.add(second);
        template.put("palettes", alternatives);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(template, bytes);

        StructureNode structure = StructureNode.fromNbt(new ByteArrayInputStream(bytes.toByteArray()));

        assertSame(furnace, structure.block(new BlockPos(1, 0, 2)));
    }

    @Test
    void fromNbt_invalidPaletteIndex_reportsInvalidTemplate() {
        CompoundTag template = template(Blocks.STONE.defaultBlockState(), 1);
        assertThrows(IOException.class, () -> StructureNode.fromNbt(template));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fromNbt_acrossPaletteFormats_preservesBlockProperties(boolean newFormat) throws IOException {
        CompoundTag template = template(Blocks.STONE.defaultBlockState(), 0);
        CompoundTag state = new CompoundTag();
        state.putString(newFormat ? "id" : "Name", "minecraft:furnace");
        CompoundTag properties = new CompoundTag();
        properties.putString("lit", "true");
        state.put(newFormat ? "properties" : "Properties", properties);
        ((ListTag) template.get("palette")).set(0, state);

        StructureNode structure = StructureNode.fromNbt(template);

        assertSame(Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.LIT, true),
                structure.block(new BlockPos(1, 0, 2)));
    }

    @Test
    void setState_onBlockNode_onlyMarksActualChanges() {
        BlockNode node = new BlockNode(Blocks.STONE.defaultBlockState());
        Scene scene = new Scene().addRoot(node);
        long initial = scene.revision();

        node.setState(Blocks.STONE.defaultBlockState());
        assertEquals(initial, scene.revision());
        node.setState(Blocks.GRASS_BLOCK.defaultBlockState());
        assertEquals(initial + 1, scene.revision());
        assertSame(Blocks.GRASS_BLOCK.defaultBlockState(), node.state());
    }

    private static CompoundTag template(BlockState state, int index) {
        CompoundTag template = new CompoundTag();
        ListTag palette = new ListTag();
        palette.add(NbtUtils.writeBlockState(state));
        template.put("palette", palette);
        ListTag positions = new ListTag();
        positions.add(IntTag.valueOf(1));
        positions.add(IntTag.valueOf(0));
        positions.add(IntTag.valueOf(2));
        CompoundTag block = new CompoundTag();
        block.put("pos", positions);
        block.putInt("state", index);
        ListTag blocks = new ListTag();
        blocks.add(block);
        template.put("blocks", blocks);
        return template;
    }
}
