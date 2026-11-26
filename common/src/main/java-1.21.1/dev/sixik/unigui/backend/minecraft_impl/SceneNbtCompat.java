package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;

/**
 * Адаптирует чтение NBT для Minecraft 1.21.1.
 */
final class SceneNbtCompat {
    private SceneNbtCompat() {
    }

    /**
     * Распаковывает source с ограничением limit в байтах.
     */
    static CompoundTag readCompressed(InputStream source, long limit) throws IOException {
        return NbtIo.readCompressed(source, NbtAccounter.create(limit));
    }

    /**
     * Читает число из тега или возвращает резервное значение.
     */
    static int integer(Tag tag, int fallback) {
        return tag instanceof NumericTag number ? number.getAsInt() : fallback;
    }

    /**
     * Читает текст по ключу или возвращает пустую строку.
     */
    static String text(CompoundTag tag, String key) {
        return tag.getString(key);
    }

    /**
     * Читает состояние блока из палитры.
     */
    static BlockState state(CompoundTag tag) {
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
    }

    /**
     * Декодирует объект блока из данных шаблона.
     */
    static BlockEntity blockEntity(BlockPos pos, BlockState state, CompoundTag tag) {
        Minecraft client = Minecraft.getInstance();
        var level = client == null ? null : client.level;
        var registries = level == null ? RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
                : level.registryAccess();
        return BlockEntity.loadStatic(pos, state, tag, registries);
    }
}
