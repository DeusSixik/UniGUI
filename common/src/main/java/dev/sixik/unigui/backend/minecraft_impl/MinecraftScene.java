package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
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
 * Scene platform for Minecraft 1.20.1, 1.21.1 and 26.x with typed access to native content.
 *
 * <pre>{@code
 * BlockNode furnace = BlockNode.of(Blocks.FURNACE.defaultBlockState());
 * BlockState state = MinecraftScene.state(furnace);
 * BlockEntity entity = MinecraftScene.blockEntity(furnace);
 *
 * StructureNode altar = MinecraftScene.PLATFORM.loadStructure("mymod:altar");
 * MinecraftScene.setBlock(altar, new BlockPos(0, 3, 0), Blocks.BEACON.defaultBlockState());
 * }</pre>
 * <p>Block specifications use the vanilla command parser and follow {@code /setblock} state syntax.
 * These versions reject legacy {@code @meta}.</p>
 */
@WikiPage(
        title = "MinecraftScene", slug = "minecraft-scene",
        category = "API/3D Scene", order = 210,
        summary = "Typed access to native scene content on modern Minecraft.")
@WikiSection(title = "Typed Minecraft helpers", order = 10, markdown = """
        Use the static accessors to inspect the objects resolved by portable nodes. `readStructure` accepts a
        gzip-compressed template stream or a decoded `CompoundTag`. Resource lookups accept a namespaced ID without
        `structure/` or `.nbt`.
        """)
@WikiExample(title = "Read the native block state", code = """
        BlockNode furnace = BlockNode.of(Blocks.FURNACE.defaultBlockState());
        BlockState state = MinecraftScene.state(furnace);
        """)
@WikiLink(label = "ScenePlatform", target = "page:scene-platform")
@WikiLink(label = "BlockNode", target = "page:block-node")
@WikiLink(label = "ItemNode", target = "page:item-node")
@WikiLink(label = "EntityNode", target = "page:entity-node")
@WikiLink(label = "StructureNode", target = "page:structure-node")
@AiDoc(
        summary = "Typed access to native scene content on modern Minecraft.",
        purpose = "Use BlockState, BlockEntity, ItemStack, Entity and structure NBT from Java code targeting modern Minecraft.",
        criticalRules = {
                "Use `MinecraftScene.PLATFORM` to access the installed modern adapter's structure loading API.",
                "`state` tolerates invalid content. `requireState` propagates invalid state errors.",
                "`stack` returns a copy. Call `invalidateContent()` after changing a returned block entity used in an on-change viewport."
        },
        related = {"ScenePlatform", "BlockNode", "ItemNode", "EntityNode", "StructureNode"},
        examples = @AiDoc.Example(title = "Read the native block state", code = """
                BlockNode furnace = BlockNode.of(Blocks.FURNACE.defaultBlockState());
                BlockState state = MinecraftScene.state(furnace);
                """))
public final class MinecraftScene implements ScenePlatform {
    /**
     * Direction toward the first of two vanilla world lights. For a unit normal n and directions l0 and l1,
     * illumination is {@code 0.4 + 0.6 * (max(0, n·l0) + max(0, n·l1))}.
     */
    private static final Vector3fc LIGHT_0 = new Vector3f(0.2f, 1, -0.7f).normalize();
    /**
     * Direction toward the second vanilla world light.
     */
    private static final Vector3fc LIGHT_1 = new Vector3f(-0.2f, 1, 0.7f).normalize();

    /**
     * Transforms the vanilla world lights into camera space. Item and entity normals go through
     * {@code view * world}, so applying the view rotation to the light directions keeps the lights fixed in world space.
     *
     * @param view scene view matrix.
     * @param light0 output vector for the first camera-space direction.
     * @param light1 output vector for the second camera-space direction.
     */
    static void lightsInView(Matrix4fc view, Vector3f light0, Vector3f light1) {
        view.transformDirection(LIGHT_0, light0).normalize();
        view.transformDirection(LIGHT_1, light1).normalize();
    }

    /**
     * Shared platform instance.
     */
    public static final MinecraftScene PLATFORM = new MinecraftScene();
    /**
     * Limit for decompressed structure NBT in bytes.
     */
    private static final long NBT_LIMIT = 64L * 1024 * 1024;

    private MinecraftScene() {
    }

    /**
     * Reads the node's block state.
     *
     * @param node block node.
     * @return block state, or null if unavailable or invalid.
     */
    @WikiMethod(section = "Native content", sectionOrder = 10, order = 10,
            summary = "Reads the node's block state.")
    @WikiReturn("block state, or null if unavailable or invalid")
    @AiDoc(section = "Native content", sectionOrder = 10, order = 10,
            summary = "Reads the node's block state.")
    public static BlockState state(
            @WikiParameter(description = "block node.") BlockNode node) {
        return (BlockState) node.nativeState(PLATFORM);
    }

    /**
     * Reads the block state and propagates resolution errors.
     *
     * @param node block node.
     * @return block state, or null if temporarily unavailable.
     * @throws IllegalArgumentException if a block, property or value is unknown.
     */
    @WikiMethod(section = "Native content", sectionOrder = 10, order = 20,
            summary = "Reads the block state and propagates resolution errors.")
    @WikiReturn("block state, or null if temporarily unavailable")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a block, property or value is unknown.")
    @AiDoc(section = "Native content", sectionOrder = 10, order = 20,
            summary = "Reads the block state and propagates resolution errors.")
    public static BlockState requireState(
            @WikiParameter(description = "block node.") BlockNode node) {
        return (BlockState) node.requireNativeState(PLATFORM);
    }

    /**
     * Reads the block entity for data or animation changes.
     *
     * @param node block node.
     * @return block entity, or null if absent.
     */
    @WikiMethod(section = "Native content", sectionOrder = 10, order = 30,
            summary = "Reads the block entity for data or animation changes.")
    @WikiReturn("block entity, or null if absent")
    @AiDoc(section = "Native content", sectionOrder = 10, order = 30,
            summary = "Reads the block entity for data or animation changes.")
    public static BlockEntity blockEntity(
            @WikiParameter(description = "block node.") BlockNode node) {
        return (BlockEntity) node.blockEntity(PLATFORM);
    }

    /**
     * Copies the node's resolved stack.
     *
     * @param node item node.
     * @return stack copy, or {@link ItemStack#EMPTY} if the item is unavailable.
     */
    @WikiMethod(section = "Native content", sectionOrder = 10, order = 40,
            summary = "Copies the node's resolved stack.")
    @WikiReturn("stack copy, or `ItemStack#EMPTY` if the item is unavailable")
    @AiDoc(section = "Native content", sectionOrder = 10, order = 40,
            summary = "Copies the node's resolved stack.")
    public static ItemStack stack(
            @WikiParameter(description = "item node.") ItemNode node) {
        return renderStack(node).copy();
    }

    /**
     * Reads the node's entity.
     *
     * @param node entity node.
     * @return entity, or null while waiting for a world or when the source is a render state.
     */
    @WikiMethod(section = "Native content", sectionOrder = 10, order = 50,
            summary = "Reads the node's entity.")
    @WikiReturn("entity, or null while waiting for a world or when the source is a render state")
    @AiDoc(section = "Native content", sectionOrder = 10, order = 50,
            summary = "Reads the node's entity.")
    public static Entity entity(
            @WikiParameter(description = "entity node.") EntityNode node) {
        SceneEntity handle = handle(node);
        return handle == null ? null : handle.entity();
    }

    /**
     * Reads a structure's block state.
     *
     * @param structure structure bound to this platform.
     * @param pos local block position.
     * @return block state, or air for an empty cell.
     */
    @WikiMethod(section = "Structures", sectionOrder = 20, order = 10,
            summary = "Reads a structure's block state.")
    @WikiReturn("block state, or air for an empty cell")
    @AiDoc(section = "Structures", sectionOrder = 20, order = 10,
            summary = "Reads a structure's block state.")
    public static BlockState block(
            @WikiParameter(description = "structure bound to this platform.") StructureNode structure,
            @WikiParameter(description = "local block position.") BlockPos pos) {
        Object state = structure.block(pos.getX(), pos.getY(), pos.getZ());
        return state == null ? Blocks.AIR.defaultBlockState() : (BlockState) state;
    }

    /**
     * Reads a structure's block state with the layer cutoff applied, for face baking and ambient occlusion.
     *
     * @param structure structure bound to this platform.
     * @param pos local block position.
     * @return block state, or air for an empty or hidden cell.
     */
    static BlockState visibleBlock(StructureNode structure, BlockPos pos) {
        Object state = structure.visibleBlock(pos.getX(), pos.getY(), pos.getZ());
        return state == null ? Blocks.AIR.defaultBlockState() : (BlockState) state;
    }

    /**
     * Sets a cell. Air removes it.
     *
     * @param structure structure bound to this platform.
     * @param pos local block position.
     * @param state new block state.
     * @return the supplied structure.
     */
    @WikiMethod(section = "Structures", sectionOrder = 20, order = 20,
            summary = "Sets a cell. Air removes it.")
    @WikiReturn("the supplied structure")
    @AiDoc(section = "Structures", sectionOrder = 20, order = 20,
            summary = "Sets a cell. Air removes it.")
    public static StructureNode setBlock(
            @WikiParameter(description = "structure bound to this platform.") StructureNode structure,
            @WikiParameter(description = "local block position.") BlockPos pos,
            @WikiParameter(description = "new block state.") BlockState state) {
        return structure.setBlock(pos.getX(), pos.getY(), pos.getZ(), state);
    }

    /**
     * Reads a structure block entity for data changes. Call {@link StructureNode#invalidateContent()} afterward.
     *
     * @param structure structure bound to this platform.
     * @param pos local block position.
     * @return block entity, or null if absent.
     */
    @WikiMethod(section = "Native content", sectionOrder = 10, order = 30,
            summary = "Reads a structure block entity for data changes. Call `StructureNode#invalidateContent()` afterward.")
    @WikiReturn("block entity, or null if absent")
    @AiDoc(section = "Native content", sectionOrder = 10, order = 30,
            summary = "Reads a structure block entity for data changes. Call `StructureNode#invalidateContent()` afterward.")
    public static BlockEntity blockEntity(
            @WikiParameter(description = "structure bound to this platform.") StructureNode structure,
            @WikiParameter(description = "local block position.") BlockPos pos) {
        return (BlockEntity) structure.blockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Loads blocks and block entity data from a compressed Minecraft template. Uses the first palette
     * when the template has multiple palettes.
     *
     * @param source stream with gzip-compressed NBT, closed by this method.
     * @return structure in template coordinates.
     * @throws IOException if the input cannot be read or a block entry is invalid.
     */
    @WikiMethod(section = "Structures", sectionOrder = 20, order = 30,
            summary = "Loads blocks and block entity data from a compressed Minecraft template. Uses the first palette when the template has multiple palettes.")
    @WikiReturn("structure in template coordinates")
    @WikiThrows(type = IOException.class, description = "if the input cannot be read or a block entry is invalid.")
    @AiDoc(section = "Structures", sectionOrder = 20, order = 30,
            summary = "Loads blocks and block entity data from a compressed Minecraft template. Uses the first palette when the template has multiple palettes.")
    public static StructureNode readStructure(
            @WikiParameter(description = "stream with gzip-compressed NBT, closed by this method.") InputStream source) throws IOException {
        try (source) {
            return readStructure(SceneStructureNbt.readCompressed(source, NBT_LIMIT));
        }
    }

    /**
     * Loads blocks from an already decoded Minecraft template.
     *
     * @param template template NBT containing palette or palettes and blocks.
     * @return structure in template coordinates.
     * @throws IOException if a block entry or palette index is invalid.
     */
    @WikiMethod(section = "Structures", sectionOrder = 20, order = 30,
            summary = "Loads blocks from an already decoded Minecraft template.")
    @WikiReturn("structure in template coordinates")
    @WikiThrows(type = IOException.class, description = "if a block entry or palette index is invalid.")
    @AiDoc(section = "Structures", sectionOrder = 20, order = 30,
            summary = "Loads blocks from an already decoded Minecraft template.")
    public static StructureNode readStructure(
            @WikiParameter(description = "template NBT containing palette or palettes and blocks.") CompoundTag template) throws IOException {
        return SceneStructureNbt.read(template);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Searches {@code structure/<path>.nbt} and {@code structures/<path>.nbt} in client resources,
     * integrated server data and under {@code data/<namespace>/} on the classpath.</p>
     */
    @WikiMethod(section = "Structures", sectionOrder = 20, order = 40,
            summary = "Loads a Minecraft structure resource by namespaced ID using client, server or classpath resources.")
    @AiDoc(section = "Structures", sectionOrder = 20, order = 40,
            summary = "Loads a Minecraft structure resource by namespaced ID using client, server or classpath resources.")
    @Override
    public StructureNode loadStructure(
            @WikiParameter(description = "Namespaced structure resource ID without the directory or file extension.") String id) throws IOException {
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
     * Reads the resolved stack without copying it.
     *
     * @param node item node.
     * @return resolved stack, or {@link ItemStack#EMPTY}.
     */
    static ItemStack renderStack(ItemNode node) {
        SceneItem item = (SceneItem) node.nativeItem(PLATFORM);
        return item == null ? ItemStack.EMPTY : item.stack();
    }

    /**
     * Reads the renderer's entity wrapper.
     *
     * @param node entity node.
     * @return wrapper, or null.
     */
    static SceneEntity handle(EntityNode node) {
        return (SceneEntity) node.nativeEntity(PLATFORM);
    }

    /**
     * Updates a compatible block entity and retains its data and animation.
     *
     * @param pos local block position.
     * @param state new block state.
     * @param previous current block entity, or null.
     * @return compatible block entity, or null if absent.
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
     * <p>On 26.x, item resolution can return null before the required game data is loaded.</p>
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
     * Maps the item's placement to the vanilla model context.
     *
     * @param display item placement in the scene.
     * @return vanilla item display context.
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
