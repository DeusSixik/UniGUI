package dev.sixik.unigui.impl.xml.minecraft;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Адаптирует имена ресурсов и поиск состояний блоков для XAML.
 */
public final class SceneXmlCompat {
    private SceneXmlCompat() {
    }

    /**
     * Читает идентификатор ресурса Minecraft.
     *
     * @param value идентификатор с пространством имён или без него.
     * @return проверенный идентификатор.
     */
    public static ResourceLocation id(String value) {
        var id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid resource id: " + value);
        return id;
    }

    /**
     * Строит идентификатор файла структуры в том же пространстве имён.
     *
     * @param source исходный идентификатор ресурса.
     * @param path   путь файла внутри пространства имён.
     * @return идентификатор файла.
     */
    public static ResourceLocation file(ResourceLocation source, String path) {
        return source.withPath(path);
    }

    /**
     * Разбирает состояние в синтаксисе ванильного аргумента блока.
     *
     * @param value состояние блока.
     * @return состояние из реестра.
     * @throws CommandSyntaxException при некорректном состоянии.
     */
    public static BlockState state(String value) throws CommandSyntaxException {
        return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), value, false).blockState();
    }
}
