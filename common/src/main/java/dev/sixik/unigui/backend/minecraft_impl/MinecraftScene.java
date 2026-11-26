package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.BlockSpec;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.impl.xml.minecraft.SceneXmlCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.IOException;
import java.io.InputStream;

/**
 * Платформа 3D-сцены для 1.20.1, 1.21.1 и 26.x и типизированный доступ к нативным объектам узлов.
 *
 * <pre>{@code
 * BlockNode furnace = BlockNode.of(Blocks.FURNACE.defaultBlockState());
 * BlockState state = MinecraftScene.state(furnace);
 * BlockEntity entity = MinecraftScene.blockEntity(furnace);
 *
 * StructureNode altar = MinecraftScene.loadStructure("mymod:altar");
 * MinecraftScene.setBlock(altar, new BlockPos(0, 3, 0), Blocks.BEACON.defaultBlockState());
 * }</pre>
 *
 * <p>Строки блоков разбираются ванильным парсером команд, поэтому поддерживают тот же синтаксис, что
 * {@code /setblock}. Метаданные {@code @meta} в этих версиях не существуют и отклоняются.</p>
 */
public final class MinecraftScene implements ScenePlatform {
    /**
     * Направления на два ванильных источника света мира: так игра освещает предметы и сущности в мире.
     * Векторы смотрят к свету, освещённость грани {@code 0.4 + 0.6 * (max(0, n·l0) + max(0, n·l1))}.
     */
    private static final Vector3fc LIGHT_0 = new Vector3f(0.2f, 1, -0.7f).normalize();
    /**
     * Второй источник ванильного света мира.
     */
    private static final Vector3fc LIGHT_1 = new Vector3f(-0.2f, 1, 0.7f).normalize();

    /**
     * Переводит ванильный свет мира в координаты камеры. Нормали предметов и сущностей сцены проходят через
     * {@code view * world}, поэтому направления на свет тоже поворачиваются видом: свет остаётся закреплён в мире
     * сцены и не крутится вместе с камерой.
     *
     * @param view   матрица вида сцены.
     * @param light0 первое направление в координатах камеры.
     * @param light1 второе направление в координатах камеры.
     */
    static void lightsInView(Matrix4fc view, Vector3f light0, Vector3f light1) {
        view.transformDirection(LIGHT_0, light0).normalize();
        view.transformDirection(LIGHT_1, light1).normalize();
    }

    /**
     * Единственный экземпляр платформы.
     */
    public static final MinecraftScene PLATFORM = new MinecraftScene();
    /**
     * Лимит распакованного NBT структуры в байтах.
     */
    private static final long NBT_LIMIT = 64L * 1024 * 1024;

    private MinecraftScene() {
    }

    /**
     * Читает состояние блока узла.
     *
     * @param node узел блока.
     * @return состояние или null, если строка неверна.
     */
    public static BlockState state(BlockNode node) {
        return (BlockState) node.nativeState(PLATFORM);
    }

    /**
     * Читает состояние блока и бросает ошибку неверной строки.
     *
     * @param node узел блока.
     * @return состояние блока.
     * @throws IllegalArgumentException если блок, свойство или значение неизвестны.
     */
    public static BlockState requireState(BlockNode node) {
        return (BlockState) node.requireNativeState(PLATFORM);
    }

    /**
     * Читает block entity узла для настройки данных и анимации.
     *
     * @param node узел блока.
     * @return block entity или null для блока без неё.
     */
    public static BlockEntity blockEntity(BlockNode node) {
        return (BlockEntity) node.blockEntity(PLATFORM);
    }

    /**
     * Читает копию стека узла.
     *
     * @param node узел предмета.
     * @return копия стека или {@link ItemStack#EMPTY}, если предмет недоступен.
     */
    public static ItemStack stack(ItemNode node) {
        return renderStack(node).copy();
    }

    /**
     * Читает сущность узла.
     *
     * @param node узел сущности.
     * @return сущность или null, если превью ждёт мир либо источник — готовое состояние рендера.
     */
    public static Entity entity(EntityNode node) {
        SceneEntity handle = handle(node);
        return handle == null ? null : handle.entity();
    }

    /**
     * Читает блок структуры.
     *
     * @param structure структура этой платформы.
     * @param pos       локальная позиция.
     * @return состояние или воздух для пустой ячейки.
     */
    public static BlockState block(StructureNode structure, BlockPos pos) {
        Object state = structure.block(pos.getX(), pos.getY(), pos.getZ());
        return state == null ? Blocks.AIR.defaultBlockState() : (BlockState) state;
    }

    /**
     * Читает блок с учётом среза слоёв для запекания граней и AO.
     *
     * @param structure структура этой платформы.
     * @param pos       локальная позиция.
     * @return состояние или воздух для пустой либо скрытой ячейки.
     */
    static BlockState visibleBlock(StructureNode structure, BlockPos pos) {
        Object state = structure.visibleBlock(pos.getX(), pos.getY(), pos.getZ());
        return state == null ? Blocks.AIR.defaultBlockState() : (BlockState) state;
    }

    /**
     * Ставит блок структуры. Воздух удаляет запись.
     *
     * @param structure структура этой платформы.
     * @param pos       локальная позиция.
     * @param state     новое состояние.
     * @return та же структура.
     */
    public static StructureNode setBlock(StructureNode structure, BlockPos pos, BlockState state) {
        return structure.setBlock(pos.getX(), pos.getY(), pos.getZ(), state);
    }

    /**
     * Читает block entity структуры для настройки данных. После изменения вызовите
     * {@link StructureNode#invalidateContent()}.
     *
     * @param structure структура этой платформы.
     * @param pos       локальная позиция.
     * @return block entity или null.
     */
    public static BlockEntity blockEntity(StructureNode structure, BlockPos pos) {
        return (BlockEntity) structure.blockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Загружает блоки и данные block entity из сжатого шаблона структуры Minecraft.
     * В многопалитровом шаблоне используется первая палитра.
     *
     * @param source поток с gzip-сжатым NBT. Чтение закрывает поток.
     * @return структура в координатах шаблона.
     * @throws IOException если поток не читается или поля блока не соответствуют формату.
     */
    public static StructureNode readStructure(InputStream source) throws IOException {
        try (source) {
            return readStructure(SceneStructureNbt.readCompressed(source, NBT_LIMIT));
        }
    }

    /**
     * Читает позиции и состояния блоков из уже загруженного шаблона.
     *
     * @param template NBT шаблона Minecraft с полями palette или palettes и blocks.
     * @return структура в координатах шаблона.
     * @throws IOException если запись блока или индекс палитры недействительны.
     */
    public static StructureNode readStructure(CompoundTag template) throws IOException {
        return SceneStructureNbt.read(template);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ищет {@code structure/<path>.nbt} и {@code structures/<path>.nbt} в ресурсах клиента, данных
     * встроенного сервера и на classpath в {@code data/<namespace>/}.</p>
     */
    @Override
    public StructureNode loadStructure(String id) throws IOException {
        var name = SceneXmlCompat.id(id.trim());
        Minecraft client = Minecraft.getInstance();
        for (String directory : new String[]{"structure/", "structures/"}) {
            String path = directory + name.getPath() + ".nbt";
            var file = SceneXmlCompat.file(name, path);
            if (client != null) {
                var resource = client.getResourceManager().getResource(file);
                if (resource.isPresent()) return readStructure(resource.get().open());
                var server = client.getSingleplayerServer();
                if (server != null) {
                    var data = server.getResourceManager().getResource(file);
                    if (data.isPresent()) return readStructure(data.get().open());
                }
            }
            InputStream input = MinecraftScene.class.getClassLoader()
                    .getResourceAsStream("data/" + name.getNamespace() + "/" + path);
            if (input != null) return readStructure(input);
        }
        throw new IOException("Unknown structure: " + name);
    }

    /**
     * Читает собственный стек узла без копирования.
     *
     * @param node узел предмета.
     * @return стек или {@link ItemStack#EMPTY}.
     */
    static ItemStack renderStack(ItemNode node) {
        SceneItem item = (SceneItem) node.nativeItem(PLATFORM);
        return item == null ? ItemStack.EMPTY : item.stack();
    }

    /**
     * Читает обёртку сущности для рендерера.
     *
     * @param node узел сущности.
     * @return обёртка или null.
     */
    static SceneEntity handle(EntityNode node) {
        return (SceneEntity) node.nativeEntity(PLATFORM);
    }

    /**
     * Обновляет состояние совместимого block entity с сохранением его данных и анимации.
     *
     * @param pos      локальная позиция блока.
     * @param state    новое состояние блока.
     * @param previous текущий block entity или null.
     * @return совместимый block entity или null для блока без неё.
     */
    static BlockEntity updateBlockEntity(BlockPos pos, BlockState state, BlockEntity previous) {
        if (previous != null && previous.getType().isValid(state)) {
            previous.setBlockState(state);
            return previous;
        }
        return state.getBlock() instanceof EntityBlock factory ? factory.newBlockEntity(pos, state) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveBlock(BlockSpec spec) {
        if (spec.hasMeta()) {
            throw new IllegalArgumentException("Block metadata is only supported on 1.7.10 and 1.12.2, use [properties]: "
                    + spec);
        }
        try {
            return SceneXmlCompat.state(spec.text());
        } catch (CommandSyntaxException failure) {
            throw new IllegalArgumentException("Invalid block state '" + spec + "': " + failure.getMessage(), failure);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object nativeBlock(Object block, int meta) {
        if (meta > 0) throw new IllegalArgumentException("Block metadata is only supported on 1.7.10 and 1.12.2");
        if (block instanceof BlockState state) return state;
        if (block instanceof Block value) return value.defaultBlockState();
        throw new IllegalArgumentException("Expected BlockState or Block, got " + block.getClass().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAirBlock(Object state) {
        return ((BlockState) state).isAir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object blockEntity(Object state, Object previous, int x, int y, int z) {
        return updateBlockEntity(new BlockPos(x, y, z), (BlockState) state, (BlockEntity) previous);
    }

    /**
     * {@inheritDoc}
     *
     * <p>В 26.x стек нельзя создать до загрузки мира, поэтому до этого момента возвращается null.</p>
     */
    @Override
    public Object resolveItem(String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(SceneXmlCompat.id(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + id));
        if (!MinecraftClientCompat.itemsReady()) return null;
        return new SceneItem(new ItemStack(item));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object nativeItem(Object item) {
        if (item instanceof ItemStack stack) return new SceneItem(stack.copy());
        if (item instanceof ItemLike like) {
            if (!MinecraftClientCompat.itemsReady()) return null;
            return new SceneItem(new ItemStack(like));
        }
        throw new IllegalArgumentException("Expected ItemStack or ItemLike, got " + item.getClass().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb itemBounds(Object item) {
        return ((SceneItem) item).bounds(ItemNode.Display.FIXED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb itemBounds(Object item, ItemNode.Display display) {
        return ((SceneItem) item).bounds(display);
    }

    /**
     * Переводит расположение узла в ванильный контекст модели.
     *
     * @param display расположение предмета в сцене.
     * @return контекст ItemRenderer.
     */
    static ItemDisplayContext context(ItemNode.Display display) {
        return switch (display) {
            case FIXED -> ItemDisplayContext.FIXED;
            case GROUND -> ItemDisplayContext.GROUND;
            case GUI -> ItemDisplayContext.GUI;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveEntity(String id) {
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(SceneXmlCompat.id(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + id));
        return SceneEntity.preview(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object nativeEntity(Object entity) {
        return SceneEntity.of(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb entityBounds(Object entity) {
        return ((SceneEntity) entity).bounds();
    }
}
