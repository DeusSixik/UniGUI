package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.BlockSpec;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.EntityNode;
import net.minecraft.entity.Entity;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.backend.shared.LegacyBlockIds;
import dev.sixik.unigui.backend.shared.LegacyBlockModels;
import dev.sixik.unigui.backend.shared.LegacyChestModel;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import dev.sixik.unigui.backend.shared.LegacySceneVanilla;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockGrass;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.ColorizerGrass;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Objects;

/**
 * 3D scene platform for GTNH 1.7.10: resolves {@link BlockNode} sources to a {@link GtnhBlockState} and bakes
 * box-shaped blocks (standard cubes, glass, slabs, logs) from the block atlas.
 *
 * <p>Block strings use metadata: {@code minecraft:furnace@3}. Block state properties do not exist on this
 * version. {@link dev.sixik.unigui.api.scene.StructureNode} is drawn from these models; structure files are not
 * supported on this version. Items and entities are drawn by the game's own renderers, see
 * {@link GtnhSceneContent}.</p>
 */
public final class GtnhScene implements ScenePlatform, LegacyBlockModels.Baker, LegacySceneVanilla {
    /**
     * The only platform instance.
     */
    public static final GtnhScene PLATFORM = new GtnhScene();
    /**
     * Four corners of each face in Minecraft side order: down, up, north, south, west, east.
     */
    private static final int[][] CORNERS = {
        {0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1}, {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0},
        {1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0}, {0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1},
        {0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1}, {1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0}
    };
    /**
     * Render type of standard boxes, including glass and slabs.
     */
    private static final int STANDARD = 0;
    /**
     * Render type of logs and other pillars; {@link Block#getIcon} already picks the end texture by orientation.
     */
    private static final int PILLAR = 31;
    /**
     * Render type of a cactus with vertical faces offset inward.
     */
    private static final int CACTUS = 13;
    /**
     * Offset of the vertical cactus faces in blocks, matching the vanilla tessellator translation.
     */
    private static final float CACTUS_SIDE_INSET = 1f / 16;
    /**
     * Temperature and rainfall of the plains biome, used for grass that is not in a world.
     */
    private static final double PLAINS_TEMPERATURE = 0.8, PLAINS_RAINFALL = 0.4;
    /**
     * Atlas texture sampled with absolute sprite UVs. Its dimensions are unused by scene materials.
     */
    private static final SimpleTextureHandle ATLAS =
        new SimpleTextureHandle(TextureMap.locationBlocksTexture.toString(), 0, 0);

    private GtnhScene() {
    }

    /**
     * @param node entity node @return native preview, or null until a world is available
     */
    public static Entity entity(EntityNode node) {
        var handle = (GtnhSceneContent.SceneEntity) node.nativeEntity(PLATFORM);
        return handle == null ? null : handle.entity();
    }

    /**
     * Reads the block and metadata of a node.
     *
     * @param node block node.
     * @return the state, or null if its source is invalid.
     */
    public static GtnhBlockState state(BlockNode node) {
        return (GtnhBlockState) node.nativeState(PLATFORM);
    }

    /**
     * Reads the block and metadata of a node and reports an invalid source.
     *
     * @param node block node.
     * @return the state.
     * @throws IllegalArgumentException if the block is unknown.
     */
    public static GtnhBlockState requireState(BlockNode node) {
        return (GtnhBlockState) node.requireNativeState(PLATFORM);
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
     */
    /**
     * {@inheritDoc}
     *
     * <p>Modern ids and the properties {@code axis} of logs, {@code facing} of furnaces and chests and {@code lit}
     * are translated to metadata, see {@link LegacyBlockIds}: {@code minecraft:furnace[facing=south,lit=true]}
     * becomes {@code minecraft:lit_furnace@3}.</p>
     */
    @Override
    public Object resolveBlock(BlockSpec spec) {
        spec = LegacyBlockIds.translate(spec, Block.blockRegistry::containsKey);
        if (!Block.blockRegistry.containsKey(spec.id())) throw new IllegalArgumentException("Unknown block: " + spec);
        Block block = Block.getBlockFromName(spec.id());
        if (block == null) throw new IllegalArgumentException("Unknown block: " + spec);
        return new GtnhBlockState(block, LegacyBlockIds.meta1710(spec));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object nativeBlock(Object block, int meta) {
        if (block instanceof GtnhBlockState state) {
            if (meta >= 0) throw new IllegalArgumentException("GtnhBlockState already carries metadata");
            return state;
        }
        if (block instanceof Block value) return new GtnhBlockState(value, Math.max(0, meta));
        throw new IllegalArgumentException("Expected Block or GtnhBlockState, got " + block.getClass().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAirBlock(Object state) {
        return ((GtnhBlockState) state).block().getMaterial() == net.minecraft.block.material.Material.air;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpaqueCube(Object state) {
        return ((GtnhBlockState) state).block().isOpaqueCube();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int resourceGeneration() {
        return GtnhResourceReloads.generation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LegacyBlockModels.Surface bake(Object nativeState) {
        return bake(nativeState, false);
    }

    /**
     * @param nativeState resolved block and metadata
     * @param lit         whether scene lighting will replace vanilla face shading
     * @return baked surface in block coordinates, or null for unsupported geometry
     */
    LegacyBlockModels.Surface bake(Object nativeState, boolean lit) {
        GtnhBlockState state = (GtnhBlockState) nativeState;
        Block block = state.block();
        int metadata = state.meta();
        String chestTexture = chestTexture(block);
        if (chestTexture != null) return LegacyChestModel.bake(chestTexture, metadata, lit);
        int type = block.getRenderType();
        if (type < 0) return null;
        // Every other shape (stairs, beds, anvils, rails, plants, mod handlers) comes from the game's own renderer.
        if (type != STANDARD && type != PILLAR && type != CACTUS)
            return GtnhBlockShapes.bake(block, metadata, ATLAS, lit);
        // An unplaced furnace has no horizontal metadata. Use the vanilla inventory facing in that case.
        if (block instanceof BlockFurnace && (metadata < 2 || metadata > 5)) metadata = ForgeDirection.SOUTH.ordinal();
        float inset = type == CACTUS ? CACTUS_SIDE_INSET : 0;
        block.setBlockBoundsForItemRender();
        float[] box = {(float) block.getBlockBoundsMinX(), (float) block.getBlockBoundsMinY(),
            (float) block.getBlockBoundsMinZ(), (float) block.getBlockBoundsMaxX(), (float) block.getBlockBoundsMaxY(),
            (float) block.getBlockBoundsMaxZ()};
        LegacySceneQuads mesh = new LegacySceneQuads();
        // Vanilla tints grass only on top and through the side overlay; the dirt stays untinted.
        boolean grass = block instanceof BlockGrass;
        int tint = 0xFF000000 | (grass ? ColorizerGrass.getGrassColor(PLAINS_TEMPERATURE, PLAINS_RAINFALL)
            : block.getRenderColor(metadata));
        for (int side = 0; side < CORNERS.length; side++) {
            IIcon sprite = block.getIcon(side, metadata);
            if (sprite == null) return null;
            face(mesh, side, box, sprite, grass && side != 1 ? 0xFFFFFFFF : tint, lit, inset);
            if (grass && side > 1) {
                IIcon overlay = BlockGrass.getIconSideOverlay();
                if (overlay != null) face(mesh, side, box, overlay, tint, lit, inset);
            }
        }
        Material.AlphaMode alpha = block.getRenderBlockPass() > 0 ? Material.AlphaMode.TRANSLUCENT
            : block.isOpaqueCube() && !grass ? Material.AlphaMode.OPAQUE : Material.AlphaMode.CUTOUT;
        var shading = lit ? Material.Shading.LIT : Material.Shading.UNLIT;
        return new LegacyBlockModels.Surface(mesh.build(), new Material(0xFFFFFFFF, ATLAS, shading, alpha,
            0.1f, Material.Culling.BACK));
    }

    /**
     * @param block block associated with the placed model or the inventory stack
     * @return chest texture basename, or null for a block handled by the ordinary renderer
     */
    static String chestTexture(Block block) {
        if (block == Blocks.chest) return "normal";
        if (block == Blocks.trapped_chest) return "trapped";
        if (block == Blocks.ender_chest) return "ender";
        return null;
    }

    /**
     * Appends a face of the block box with UVs cut from the sprite the way vanilla does for partial blocks.
     *
     * @param mesh   accumulator for complete quads.
     * @param side   Minecraft direction index.
     * @param box    block bounds as min X, Y, Z and max X, Y, Z within the unit cell.
     * @param sprite sprite occupying a subrectangle of the block atlas.
     * @param color  packed ARGB block tint.
     * @param lit    whether the renderer supplies face shading.
     * @param inset  inward offset for vertical faces in blocks, measured along the face normal.
     */
    private static void face(LegacySceneQuads mesh, int side, float[] box, IIcon sprite, int color, boolean lit,
                             float inset) {
        ForgeDirection normal = ForgeDirection.getOrientation(side);
        int shaded = lit ? color : LegacySceneQuads.shade(color, normal.offsetX, normal.offsetY, normal.offsetZ);
        int[] corners = CORNERS[side];
        for (int i = 0; i < corners.length; i += 3) {
            float x = box[corners[i] * 3], y = box[corners[i + 1] * 3 + 1], z = box[corners[i + 2] * 3 + 2];
            // Sprite coordinates in 0..1: top and bottom map X and Z, the sides map the horizontal axis and Y.
            float s = switch (side) {
                case 2 -> 1 - x;
                case 4 -> z;
                case 5 -> 1 - z;
                default -> x;
            };
            float t = side < 2 ? z : 1 - y;
            mesh.vertex(x - normal.offsetX * inset, y, z - normal.offsetZ * inset,
                (float) sprite.getInterpolatedU(s * 16), (float) sprite.getInterpolatedV(t * 16),
                normal.offsetX, normal.offsetY, normal.offsetZ, shaded);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts any registered item id, for example {@code minecraft:diamond_sword}.</p>
     */
    @Override
    public Object resolveItem(String id) {
        return GtnhSceneContent.resolveItem(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts an {@code ItemStack} (copied), an {@code Item} or a {@code Block} with an item.</p>
     */
    @Override
    public Object nativeItem(Object item) {
        return GtnhSceneContent.nativeItem(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb itemBounds(Object item) {
        return GtnhSceneContent.itemBounds(item, dev.sixik.unigui.api.scene.ItemNode.Display.FIXED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb itemBounds(Object item, dev.sixik.unigui.api.scene.ItemNode.Display display) {
        return GtnhSceneContent.itemBounds(item, display);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts a modern id such as {@code minecraft:cave_spider} or an {@code EntityList} name such as
     * {@code CaveSpider}.</p>
     */
    @Override
    public Object resolveEntity(String id) {
        return GtnhSceneContent.resolveEntity(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Accepts an {@code Entity}, an entity {@code Class} or an {@code EntityList} name.</p>
     */
    @Override
    public Object nativeEntity(Object entity) {
        return GtnhSceneContent.nativeEntity(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Aabb entityBounds(Object entity) {
        return GtnhSceneContent.entityBounds(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginVanilla() {
        GtnhSceneContent.beginVanilla();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean drawItem(dev.sixik.unigui.api.scene.ItemNode node, Object item, float partialTick) {
        return GtnhSceneContent.drawItem(item, node.display());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean drawEntity(dev.sixik.unigui.api.scene.EntityNode node, Object entity, float partialTick) {
        return GtnhSceneContent.drawEntity(entity, node, partialTick);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVanilla() {
        GtnhSceneContent.endVanilla();
    }

    /**
     * A 1.7.10 block with its metadata, the native state of a {@link BlockNode} on this version.
     *
     * @param block block providing sprites and render settings.
     * @param meta  metadata from 0 to 15.
     */
    public record GtnhBlockState(Block block, int meta) {
        /**
         * @throws NullPointerException     if the block is null.
         * @throws IllegalArgumentException if the metadata is outside 0..15.
         */
        public GtnhBlockState {
            Objects.requireNonNull(block, "block");
            if (meta < 0 || meta > 15) throw new IllegalArgumentException("Block metadata must be 0..15: " + meta);
        }
    }
}
