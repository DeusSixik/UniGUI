package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.StructureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;

/**
 * Читает шаблоны структур с API NBT Minecraft.
 */
final class SceneStructureNbt {
    private SceneStructureNbt() {
    }

    /**
     * Распаковывает структуру с ограничением размера.
     *
     * @param source gzip-сжатый поток.
     * @param limit  допустимый объём NBT в байтах.
     * @return корневой тег шаблона.
     * @throws IOException если поток не читается.
     */
    static CompoundTag readCompressed(InputStream source, long limit) throws IOException {
        return SceneNbtCompat.readCompressed(source, limit);
    }

    /**
     * Читает блоки и объекты блоков из шаблона.
     *
     * @param template корневой тег с палитрой и блоками.
     * @return готовая структура.
     * @throws IOException если шаблон содержит некорректные позиции или ссылки на палитру.
     */
    static StructureNode read(CompoundTag template) throws IOException {
        Tag paletteTag = template.get("palette");
        if (paletteTag == null && template.get("palettes") instanceof ListTag alternatives && !alternatives.isEmpty()) {
            paletteTag = alternatives.get(0);
        }
        if (!(paletteTag instanceof ListTag paletteTags)) throw new IOException("Missing structure palette");
        BlockState[] palette = new BlockState[paletteTags.size()];
        for (int i = 0; i < palette.length; i++) {
            if (!(paletteTags.get(i) instanceof CompoundTag state)) {
                throw new IOException("Invalid structure palette entry " + i);
            }
            palette[i] = readState(state, i);
        }
        StructureNode structure = new StructureNode(MinecraftScene.PLATFORM);
        if (!(template.get("blocks") instanceof ListTag blocks)) throw new IOException("Missing structure block list");
        for (int i = 0; i < blocks.size(); i++) {
            if (!(blocks.get(i) instanceof CompoundTag entry)) throw new IOException("Invalid structure block");
            int index = SceneNbtCompat.integer(entry.get("state"), -1);

            if (index < 0 || index >= palette.length || !(entry.get("pos") instanceof ListTag pos) || pos.size() != 3) {
                throw new IOException("Invalid structure block entry " + i);
            }
            int x = coordinate(pos, 0), y = coordinate(pos, 1), z = coordinate(pos, 2);
            try {
                structure.setBlock(x, y, z, palette[index]);
            } catch (IllegalArgumentException outside) {
                throw new IOException("Structure coordinates exceed Minecraft block range", outside);
            }

            if (entry.get("nbt") instanceof CompoundTag data && structure.blockEntity(x, y, z) != null) {
                BlockEntity loaded = SceneNbtCompat.blockEntity(new BlockPos(x, y, z), palette[index], data);
                if (loaded != null) structure.setBlockEntity(x, y, z, loaded);
            }
        }
        return structure;
    }

    /**
     * Читает одну компоненту позиции блока.
     *
     * @param coordinates три координаты блока.
     * @param axis        индекс координаты.
     * @return координата блока.
     * @throws IOException если тег координаты не является целым числом.
     */
    private static int coordinate(ListTag coordinates, int axis) throws IOException {
        int value = SceneNbtCompat.integer(coordinates.get(axis), Integer.MAX_VALUE);
        if (value == Integer.MAX_VALUE) throw new IOException("Invalid structure coordinate " + axis);
        return value;
    }

    /**
     * Читает состояние через NbtUtils с именами полей старого и нового форматов.
     *
     * @param state NBT состояния из палитры.
     * @param index индекс состояния для сообщения об ошибке.
     * @return состояние блока из реестра Minecraft.
     * @throws IOException если запись не содержит идентификатора блока.
     */
    private static BlockState readState(CompoundTag state, int index) throws IOException {
        String name = SceneNbtCompat.text(state, "id");
        if (name.isEmpty()) name = SceneNbtCompat.text(state, "Name");
        if (name.isEmpty()) throw new IOException("Missing block ID in structure palette entry " + index);
        CompoundTag compatible = new CompoundTag();
        compatible.putString("id", name);
        compatible.putString("Name", name);
        Tag properties = state.get("properties");
        if (!(properties instanceof CompoundTag)) properties = state.get("Properties");
        if (properties instanceof CompoundTag) {
            compatible.put("properties", properties);
            compatible.put("Properties", properties);
        }
        return SceneNbtCompat.state(compatible);
    }

}
