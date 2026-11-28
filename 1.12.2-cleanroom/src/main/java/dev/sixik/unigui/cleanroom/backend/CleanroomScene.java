package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.BlockSpec;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.backend.shared.LegacyBlockIds;
import dev.sixik.unigui.backend.shared.LegacyBlockModels;
import dev.sixik.unigui.backend.shared.LegacyChestModel;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import dev.sixik.unigui.backend.shared.LegacySceneVanilla;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.init.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 3D scene platform for Cleanroom 1.12.2: resolves {@link BlockNode} sources to {@link IBlockState} and bakes
 * their models from the block atlas.
 *
 * <p>Block strings accept both forms: {@code minecraft:furnace[facing=south]} and {@code minecraft:wool@14}.
 * Structures load from vanilla templates in {@code assets/<namespace>/structures/<path>.nbt}; their tile entity data
 * is not drawn. Items and entities are drawn by the game's own renderers, see {@link CleanroomSceneContent}.</p>
 */
public final class CleanroomScene implements ScenePlatform, LegacyBlockModels.Baker, LegacySceneVanilla {
    /**
     * The only platform instance.
     */
    public static final CleanroomScene PLATFORM = new CleanroomScene();
    /**
     * Atlas texture sampled with absolute model UVs. Its dimensions are unused by scene materials.
     */
    private static final SimpleTextureHandle ATLAS =
            new SimpleTextureHandle(TextureMap.LOCATION_BLOCKS_TEXTURE.toString(), 0, 0);

    private CleanroomScene() {
    }

    /**
     * Reads the block state of a node.
     *
     * @param node block node.
     * @return the state, or null if its source is invalid.
     */
    public static IBlockState state(BlockNode node) {
        return (IBlockState) node.nativeState(PLATFORM);
    }

    /**
     * Reads the block state of a node and reports an invalid source.
     *
     * @param node block node.
     * @return the state.
     * @throws IllegalArgumentException if the block, property or value is unknown.
     */
    public static IBlockState requireState(BlockNode node) {
        return (IBlockState) node.requireNativeState(PLATFORM);
    }

    /**
     * Returns the model's live source, for example to equip a preview before displaying it.
     *
     * @param node entity node
     * @return native entity, or null if the source is unavailable or its preview needs a world
     */
    public static Entity entity(EntityNode node) {
        return node.nativeEntity(PLATFORM) instanceof CleanroomSceneContent.SceneEntity handle ? handle.entity() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScenePlatform platform() {
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Modern ids are translated first, see {@link LegacyBlockIds}: {@code minecraft:oak_log[axis=x]} becomes
     * {@code minecraft:log[variant=oak,axis=x]}.</p>
     */
    @Override
    public Object resolveBlock(BlockSpec spec) {
        spec = LegacyBlockIds.translate(spec, key -> ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(key)));
        ResourceLocation id = new ResourceLocation(spec.id());
        if (!ForgeRegistries.BLOCKS.containsKey(id)) throw new IllegalArgumentException("Unknown block: " + spec);
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        // A translated id may carry both a base metadata and properties on top of it.
        IBlockState state = spec.hasMeta() ? stateFromMeta(block, spec.meta()) : block.getDefaultState();
        for (Map.Entry<String, String> entry : spec.properties().entrySet()) {
            IProperty<?> property = block.getBlockState().getProperty(entry.getKey());
            if (property == null) {
                throw new IllegalArgumentException("Block " + spec.id() + " has no property '" + entry.getKey() + "'");
            }
            state = withValue(state, property, entry.getValue(), spec);
        }
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object nativeBlock(Object block, int meta) {
        if (block instanceof IBlockState state) {
            if (meta >= 0) throw new IllegalArgumentException("Metadata is only used with a Block, not an IBlockState");
            return state;
        }
        if (block instanceof Block value) return meta < 0 ? value.getDefaultState() : stateFromMeta(value, meta);
        throw new IllegalArgumentException("Expected IBlockState or Block, got " + block.getClass().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAirBlock(Object state) {
        return ((IBlockState) state).getBlock() == Blocks.AIR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(Object state) {
        return ((IBlockState) state).isOpaqueCube();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads a template from the resource packs, then from the classpath.</p>
     */
    @Override
    public StructureNode loadStructure(String id) throws IOException {
        ResourceLocation name = new ResourceLocation(id.trim());
        ResourceLocation file = new ResourceLocation(name.getNamespace(), "structures/" + name.getPath() + ".nbt");
        Minecraft client = Minecraft.getMinecraft();
        if (client != null && client.getResourceManager() != null) {
            try (InputStream input = client.getResourceManager().getResource(file).getInputStream()) {
                return readStructure(input);
            } catch (FileNotFoundException missing) {
                // Fall back to the classpath below.
            }
        }
        try (InputStream input = CleanroomScene.class.getClassLoader()
                .getResourceAsStream("assets/" + file.getNamespace() + "/" + file.getPath())) {
            if (input != null) return readStructure(input);
        }
        throw new IOException("Unknown structure: " + name);
    }

    /**
     * Reads the first palette and the blocks of a gzip-compressed structure template.
     *
     * @param input compressed template.
     * @return structure in template coordinates.
     * @throws IOException if the stream is unreadable or the template is malformed.
     */
    public static StructureNode readStructure(InputStream input) throws IOException {
        NBTTagCompound template = CompressedStreamTools.readCompressed(input);
        NBTTagList paletteTags = template.getTagList("palette", 10);
        if (paletteTags.tagCount() == 0) throw new IOException("Missing structure palette");
        IBlockState[] palette = new IBlockState[paletteTags.tagCount()];
        for (int i = 0; i < palette.length; i++) palette[i] = NBTUtil.readBlockState(paletteTags.getCompoundTagAt(i));
        NBTTagList blocks = template.getTagList("blocks", 10);
        StructureNode structure = new StructureNode(PLATFORM);
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound entry = blocks.getCompoundTagAt(i);
            NBTTagList pos = entry.getTagList("pos", 3);
            int state = entry.getInteger("state");
            if (pos.tagCount() != 3 || state < 0 || state >= palette.length) {
                throw new IOException("Invalid structure block entry " + i);
            }
            try {
                structure.setBlock(pos.getIntAt(0), pos.getIntAt(1), pos.getIntAt(2), palette[state]);
            } catch (IllegalArgumentException invalid) {
                throw new IOException("Invalid structure block entry " + i, invalid);
            }
        }
        return structure;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts any registered item id, for example {@code minecraft:diamond_sword}.</p>
     */
    @Override
    public Object resolveItem(String id) {
        return CleanroomSceneContent.resolveItem(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts an {@code ItemStack} (copied), an {@code Item} or a {@code Block} with an item.</p>
     */
    @Override
    public Object nativeItem(Object item) {
        return CleanroomSceneContent.nativeItem(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb itemBounds(Object item) {
        return CleanroomSceneContent.itemBounds(item, dev.sixik.unigui.api.scene.ItemNode.Display.FIXED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb itemBounds(Object item, dev.sixik.unigui.api.scene.ItemNode.Display display) {
        return CleanroomSceneContent.itemBounds(item, display);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveEntity(String id) {
        return CleanroomSceneContent.resolveEntity(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts an {@code Entity}, an {@code EntityEntry} or an entity {@code Class}.</p>
     */
    @Override
    public Object nativeEntity(Object entity) {
        return CleanroomSceneContent.nativeEntity(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb entityBounds(Object entity) {
        return CleanroomSceneContent.entityBounds(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginVanilla() {
        CleanroomSceneContent.beginVanilla();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean drawItem(dev.sixik.unigui.api.scene.ItemNode node, Object item, float partialTick) {
        return CleanroomSceneContent.drawItem(item, node.display());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean drawEntity(dev.sixik.unigui.api.scene.EntityNode node, Object entity, float partialTick) {
        return CleanroomSceneContent.drawEntity(entity, node, partialTick);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVanilla() {
        CleanroomSceneContent.endVanilla();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int resourceGeneration() {
        return CleanroomResourceReloads.generation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LegacyBlockModels.Surface bake(Object nativeState) {
        return bake(nativeState, false);
    }

    /**
     * @param nativeState resolved Minecraft block state
     * @param lit whether to keep normals and biome tint with scene lighting
     * @return baked surface, or null if there is no block model
     */
    LegacyBlockModels.Surface bake(Object nativeState, boolean lit) {
        IBlockState state = (IBlockState) nativeState;
        Block block = state.getBlock();
        String chest = chestTexture(block);
        if (chest != null) return LegacyChestModel.bake(chest, block.getMetaFromState(state), lit);
        // Banners, beds, shulker boxes and skulls have no block model; their tile entity renderer draws them.
        if (state.getRenderType() != EnumBlockRenderType.MODEL) return null;
        Minecraft minecraft = Minecraft.getMinecraft();
        IBakedModel model = minecraft.getBlockRendererDispatcher().getModelForState(state);
        LegacySceneQuads vertices = new LegacySceneQuads();
        float[] buffer = new float[4];
        append(vertices, state, model.getQuads(state, null, 0), minecraft, buffer, lit);
        for (EnumFacing face : EnumFacing.VALUES) {
            append(vertices, state, model.getQuads(state, face, 0), minecraft, buffer, lit);
        }
        Mesh3D mesh = vertices.build();
        if (mesh == null) return null;
        Material.AlphaMode alpha = switch (block.getRenderLayer()) {
            case SOLID -> Material.AlphaMode.OPAQUE;
            case CUTOUT, CUTOUT_MIPPED -> Material.AlphaMode.CUTOUT;
            case TRANSLUCENT -> Material.AlphaMode.TRANSLUCENT;
        };
        var shading = lit ? Material.Shading.LIT : Material.Shading.UNLIT;
        return new LegacyBlockModels.Surface(mesh, new Material(0xFFFFFFFF, ATLAS, shading, alpha, 0.1f,
                Material.Culling.BACK));
    }

    /**
     * @param block block of a placed model
     * @return chest texture basename for the chests baked by {@link LegacyChestModel}, or null for other blocks
     */
    static String chestTexture(Block block) {
        if (block == Blocks.CHEST) return "normal";
        if (block == Blocks.TRAPPED_CHEST) return "trapped";
        if (block == Blocks.ENDER_CHEST) return "ender";
        return null;
    }

    /**
     * Converts metadata with the block's own mapping.
     *
     * @param block block owning the mapping.
     * @param meta  metadata from 0 to 15.
     * @return the matching state.
     */
    @SuppressWarnings("deprecation")
    private static IBlockState stateFromMeta(Block block, int meta) {
        if (meta > 15) throw new IllegalArgumentException("Block metadata must be 0..15: " + meta);
        return block.getStateFromMeta(meta);
    }

    /**
     * Applies one parsed property value.
     *
     * @param state    current state.
     * @param property property of the block.
     * @param value    property value as written in the string.
     * @param spec     source string for error messages.
     * @param <T>      property value type.
     * @return the updated state.
     */
    private static <T extends Comparable<T>> IBlockState withValue(IBlockState state, IProperty<T> property, String value,
                                                                   BlockSpec spec) {
        var parsed = property.parseValue(value);
        if (!parsed.isPresent()) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for property '" + property.getName()
                    + "' in " + spec);
        }
        return state.withProperty(property, parsed.get());
    }

    /**
     * Unpacks the positions and atlas UVs without assuming a fixed byte stride for modded models.
     *
     * @param out       accumulator for block-local quads.
     * @param state     state providing block tints.
     * @param quads     general or face-specific quads.
     * @param minecraft client providing block tints.
     * @param buffer    scratch space for one vertex element of up to four components.
     * @param lit whether to defer directional shading to the scene
     */
    private static void append(LegacySceneQuads out, IBlockState state, List<BakedQuad> quads, Minecraft minecraft,
                               float[] buffer, boolean lit) {
        for (BakedQuad quad : quads) {
            VertexFormat format = quad.getFormat();
            int position = -1, uv = -1;
            for (int i = 0; i < format.getElementCount(); i++) {
                var element = format.getElement(i);
                switch (element.getUsage()) {
                    case POSITION -> position = i;
                    case UV -> {
                        if (element.getIndex() == 0) uv = i;
                    }
                }
            }
            if (position < 0 || uv < 0) continue;
            int tint = quad.hasTintIndex()
                    ? minecraft.getBlockColors().colorMultiplier(state, null, null, quad.getTintIndex()) : -1;
            EnumFacing face = quad.getFace();
            int color = 0xFF000000 | tint;
            if (!lit && quad.shouldApplyDiffuseLighting()) {
                color = LegacySceneQuads.shade(color, face.getXOffset(), face.getYOffset(), face.getZOffset());
            }
            int[] data = quad.getVertexData();
            for (int i = 0; i < 4; i++) {
                LightUtil.unpack(data, buffer, format, i, position);
                float x = buffer[0], y = buffer[1], z = buffer[2];
                LightUtil.unpack(data, buffer, format, i, uv);
                out.vertex(x, y, z, buffer[0], buffer[1], face.getXOffset(), face.getYOffset(),
                        face.getZOffset(), color);
            }
        }
    }
}
