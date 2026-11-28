package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.backend.shared.LegacyBlockModels;
import dev.sixik.unigui.backend.shared.LegacySceneQuads;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLadder;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Bakes blocks of every other render type (stairs, beds, anvils, rails, cauldrons, plants, fences, panes, mod
 * shapes from {@code ISimpleBlockRenderingHandler}) by running the game's own {@link RenderBlocks} on a one-block
 * world and collecting the tessellator quads.
 *
 * <p>The block stands at (0,0,0) surrounded by air in the plains biome at full light, so shapes that connect to
 * neighbours appear unconnected, two-block shapes such as beds and doors show the half selected by the metadata,
 * and tinted plants use the plains colour. Vertex colours carry the vanilla face shading; for scene lighting it is
 * divided out again on axis-aligned faces.</p>
 */
final class GtnhBlockShapes {
    /**
     * Error log for shapes that cannot be baked.
     */
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    /**
     * Full sky and block light in the packed lightmap form {@code sky << 20 | block << 4}.
     */
    private static final int FULL_BRIGHT = 15 << 20 | 15 << 4;
    /**
     * Normal components closer to ±1 than this are treated as axis-aligned.
     */
    private static final float AXIS_EPSILON = 1e-4f;
    /**
     * Blocks whose baking failed; they are not tried again until the game restarts.
     */
    private static final Set<Block> BROKEN = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Legacy south facing, the inventory facing of wall-mounted blocks.
     */
    private static final int SOUTH = 3;
    /**
     * Metadata bit of the upper door half.
     */
    private static final int DOOR_TOP = 8;

    private GtnhBlockShapes() {
    }

    /**
     * @param block block with a render type the box baker does not handle
     * @param meta  metadata of the drawn state
     * @param atlas block atlas texture
     * @param lit   whether scene lighting replaces the vanilla face shading
     * @return block-local surface, or null when the renderer draws nothing or fails
     */
    static LegacyBlockModels.Surface bake(Block block, int meta, SimpleTextureHandle atlas, boolean lit) {
        if (BROKEN.contains(block)) return null;
        Mesh3D mesh;
        try {
            // Ladders and the like draw nothing without a wall to face; use the south facing of an unplaced block.
            if (block instanceof BlockLadder && (meta < 2 || meta > 5)) meta = SOUTH;
            var renderer = new RenderBlocks(new OneBlock(block, meta, neighbour(block, meta)));
            Tessellator tessellator = Tessellator.instance;
            LegacySceneQuads quads = GtnhSceneCapture.captureTessellator(() -> {
                tessellator.startDrawingQuads();
                renderer.renderBlockByRenderType(block, 0, 0, 0);
                tessellator.draw();
            });
            mesh = quads == null ? null : quads.build();
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN.add(block)) {
                LOGGER.error("UniGUI could not bake block {} (render type {}) for a 3D scene; it is skipped from now on",
                    Block.blockRegistry.getNameForObject(block), block.getRenderType(), failure);
            }
            return null;
        }
        if (mesh == null) return null;
        var alpha = block.getRenderBlockPass() > 0 ? Material.AlphaMode.TRANSLUCENT : Material.AlphaMode.CUTOUT;
        var shading = lit ? Material.Shading.LIT : Material.Shading.UNLIT;
        return new LegacyBlockModels.Surface(normalize(mesh, lit),
            new Material(0xFFFFFFFF, atlas, shading, alpha, 0.1f, Material.Culling.BACK));
    }

    /**
     * The one extra block some renderers need before they draw anything. It only answers world queries and is not
     * drawn itself.
     *
     * @param block block at the origin
     * @param meta  its metadata
     * @return the neighbour, or null when the block draws on its own
     */
    private static Neighbour neighbour(Block block, int meta) {
        // A door half is drawn only next to its other half, which also carries the hinge and the facing.
        if (block instanceof BlockDoor) {
            return (meta & DOOR_TOP) != 0 ? new Neighbour(-1, block, 0) : new Neighbour(1, block, DOOR_TOP);
        }
        // Fire on a solid floor burns in the cell; on air it only draws flames on flammable neighbours.
        if (block instanceof BlockFire) return new Neighbour(-1, Blocks.netherrack, 0);
        return null;
    }

    /**
     * Normalizes the captured normals, which are cross products of quad edges, snaps axis-aligned ones to exact unit
     * axes so structures can hide faces against opaque neighbours, and divides out the vanilla face shading
     * {@link LegacySceneQuads#shade} applied by {@link RenderBlocks} when the scene lights the block itself.
     *
     * @param mesh captured quads
     * @param lit  whether to remove the vanilla shading
     * @return mesh with unit normals
     */
    private static Mesh3D normalize(Mesh3D mesh, boolean lit) {
        var out = new LegacySceneQuads();
        for (int v = 0; v < mesh.vertexCount(); v++) {
            float nx = mesh.normal(v, 0), ny = mesh.normal(v, 1), nz = mesh.normal(v, 2);
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 0) {
                nx /= length;
                ny /= length;
                nz /= length;
            }
            boolean axis = Math.abs(Math.abs(nx) - 1) < AXIS_EPSILON || Math.abs(Math.abs(ny) - 1) < AXIS_EPSILON
                || Math.abs(Math.abs(nz) - 1) < AXIS_EPSILON;
            if (axis) {
                nx = Math.round(nx);
                ny = Math.round(ny);
                nz = Math.round(nz);
            }
            int color = mesh.colorArgb(v);
            if (lit && axis) color = unshade(color, nx, ny, nz);
            out.vertex(mesh.position(v, 0), mesh.position(v, 1), mesh.position(v, 2), mesh.uv(v, 0), mesh.uv(v, 1),
                nx, ny, nz, color);
        }
        return out.build();
    }

    /**
     * Inverse of {@link LegacySceneQuads#shade} on an axis: bottom 0.5, X 0.6, Z 0.8, top 1.
     *
     * @param argb shaded vertex colour
     * @param nx   unit normal X
     * @param ny   unit normal Y
     * @param nz   unit normal Z
     * @return colour before shading, clamped to 255 per channel
     */
    private static int unshade(int argb, float nx, float ny, float nz) {
        float factor = Math.min(1, nx * nx * 0.6f + nz * nz * 0.8f + ny * ny * (3 + ny) / 4);
        int r = Math.min(255, Math.round(((argb >>> 16) & 0xFF) / factor));
        int g = Math.min(255, Math.round(((argb >>> 8) & 0xFF) / factor));
        int b = Math.min(255, Math.round((argb & 0xFF) / factor));
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    /**
     * A block straight above or below the origin.
     *
     * @param dy    vertical offset from the origin
     * @param block block there
     * @param meta  its metadata
     */
    private record Neighbour(int dy, Block block, int meta) {
    }

    /**
     * A world holding one block at the origin, an optional neighbour and air everywhere else.
     *
     * @param block     block at (0,0,0)
     * @param meta      its metadata
     * @param neighbour supporting block, or null
     */
    private record OneBlock(Block block, int meta, Neighbour neighbour) implements IBlockAccess {
        /**
         * @return whether the coordinates are the origin
         */
        private static boolean origin(int x, int y, int z) {
            return x == 0 && y == 0 && z == 0;
        }

        /**
         * @return whether the coordinates hold the neighbour
         */
        private boolean beside(int x, int y, int z) {
            return neighbour != null && x == 0 && z == 0 && y == neighbour.dy;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            if (origin(x, y, z)) return block;
            return beside(x, y, z) ? neighbour.block : Blocks.air;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return null;
        }

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int minBlockLight) {
            return FULL_BRIGHT;
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            if (origin(x, y, z)) return meta;
            return beside(x, y, z) ? neighbour.meta : 0;
        }

        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int direction) {
            return 0;
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return getBlock(x, y, z).getMaterial() == net.minecraft.block.material.Material.air;
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return BiomeGenBase.plains;
        }

        @Override
        public int getHeight() {
            return 256;
        }

        @Override
        public boolean extendedLevelsInChunkCache() {
            return false;
        }

        @Override
        public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
            Block value = getBlock(x, y, z);
            return value != Blocks.air && value.isSideSolid(this, x, y, z, side);
        }
    }
}
