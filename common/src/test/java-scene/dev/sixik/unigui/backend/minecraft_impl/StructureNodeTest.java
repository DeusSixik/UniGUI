package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.StructureNode;
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
    void helpers_readAndWriteVanillaStatesWithAirAsEmpty() {
        StructureNode structure = new StructureNode(MinecraftScene.PLATFORM);
        BlockPos position = new BlockPos(-1, 20, 3);
        MinecraftScene.setBlock(structure, position, Blocks.OAK_LEAVES.defaultBlockState());

        assertSame(Blocks.OAK_LEAVES.defaultBlockState(), MinecraftScene.block(structure, position));
        assertSame(Blocks.AIR.defaultBlockState(), MinecraftScene.block(structure, BlockPos.ZERO));
        structure.setTopVisibleY(19);
        assertSame(Blocks.AIR.defaultBlockState(), MinecraftScene.visibleBlock(structure, position));
        structure.setBlock(-1, 20, 3, Blocks.AIR);
        assertEquals(0, structure.blockCount());
        structure.setBlock(0, 0, 0, "minecraft:furnace[lit=true]");
        assertSame(Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.LIT, true),
                MinecraftScene.block(structure, BlockPos.ZERO));
    }

    /** Проверяет сохранение данных block entity при обновлении свойств блока. */
    @Test
    void setBlock_withCompatibleState_preservesBlockEntity() {
        StructureNode structure = new StructureNode(MinecraftScene.PLATFORM);
        BlockPos pos = new BlockPos(2, 1, -3);
        BlockState furnace = Blocks.FURNACE.defaultBlockState();
        MinecraftScene.setBlock(structure, pos, furnace);
        var entity = MinecraftScene.blockEntity(structure, pos);
        BlockState lit = furnace.setValue(FurnaceBlock.LIT, true);
        MinecraftScene.setBlock(structure, pos, lit);

        assertSame(entity, MinecraftScene.blockEntity(structure, pos));
        assertSame(lit, entity.getBlockState());
        assertEquals(pos, entity.getBlockPos());
        MinecraftScene.setBlock(structure, pos, Blocks.STONE.defaultBlockState());
        assertNull(MinecraftScene.blockEntity(structure, pos));
    }

    @Test
    void readStructure_compressedMultiPalette_usesFirstPaletteAndReadsStateProperties() throws IOException {
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

        StructureNode structure = MinecraftScene.readStructure(new ByteArrayInputStream(bytes.toByteArray()));

        assertSame(furnace, MinecraftScene.block(structure, new BlockPos(1, 0, 2)));
    }

    @Test
    void readStructure_invalidPaletteIndex_reportsInvalidTemplate() {
        CompoundTag template = template(Blocks.STONE.defaultBlockState(), 1);
        assertThrows(IOException.class, () -> MinecraftScene.readStructure(template));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void readStructure_acrossPaletteFormats_preservesBlockProperties(boolean newFormat) throws IOException {
        CompoundTag template = template(Blocks.STONE.defaultBlockState(), 0);
        CompoundTag state = new CompoundTag();
        state.putString(newFormat ? "id" : "Name", "minecraft:furnace");
        CompoundTag properties = new CompoundTag();
        properties.putString("lit", "true");
        state.put(newFormat ? "properties" : "Properties", properties);
        ((ListTag) template.get("palette")).set(0, state);

        StructureNode structure = MinecraftScene.readStructure(template);

        assertSame(Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.LIT, true),
                MinecraftScene.block(structure, new BlockPos(1, 0, 2)));
    }

    @Test
    void setState_onBlockNode_onlyMarksActualChanges() {
        BlockNode node = BlockNode.of(Blocks.STONE.defaultBlockState());
        Scene scene = new Scene().addRoot(node);
        long initial = scene.revision();

        node.setState(Blocks.STONE.defaultBlockState());
        assertEquals(initial, scene.revision());
        node.setState(Blocks.GRASS_BLOCK.defaultBlockState());
        assertEquals(initial + 1, scene.revision());
        assertSame(Blocks.GRASS_BLOCK.defaultBlockState(), MinecraftScene.state(node));
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
