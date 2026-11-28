package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.backend.shared.LegacyBlockIds;
import dev.sixik.unigui.cleanroom.mixin.SceneTileEntityAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Items and entities of 3D scenes on 1.12.2: resolution of ids and native objects, bounds and drawing through the
 * game's renderers.
 *
 * <p>Items use the {@link ItemCameraTransforms.TransformType#FIXED} transform of their model, the one of item frames,
 * centered at the origin. Entities stand with their feet at the origin and face +Z.</p>
 */
final class CleanroomSceneContent {
    /**
     * Error log for content that cannot be drawn.
     */
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    /**
     * Bounds of an item model, a unit cube around the origin.
     */
    private static final Aabb ITEM_BOUNDS = new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
    /**
     * Bounds of a flat item, one texture pixel thick.
     */
    private static final Aabb FLAT_BOUNDS = new Aabb(-0.5f, -0.5f, -0.03125f, 0.5f, 0.5f, 0.03125f);
    /**
     * Bounds of an entity that has not been created yet.
     */
    private static final Aabb ENTITY_FALLBACK = new Aabb(-0.5f, 0, -0.5f, 0.5f, 2, 0.5f);
    /**
     * Items whose drawing failed and is skipped from then on.
     */
    private static final Set<Item> BROKEN_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Entity classes whose drawing failed and is skipped from then on.
     */
    private static final Set<Class<?>> BROKEN_ENTITIES = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Preview tile entity per canonical block state, or null for states without a tile entity renderer.
     */
    private static final Map<IBlockState, TileEntity> BLOCK_ENTITIES = new IdentityHashMap<>();

    private CleanroomSceneContent() {
    }

    /**
     * Finds an item by registry id.
     *
     * @param id item id, for example {@code minecraft:diamond_sword}.
     * @return stack of one item.
     * @throws IllegalArgumentException if the item is unknown.
     */
    static ItemStack resolveItem(String id) {
        ResourceLocation key = new ResourceLocation(id);
        LegacyBlockIds.Legacy legacy = LegacyBlockIds.translateItem(key.toString(),
                name -> ForgeRegistries.ITEMS.containsKey(new ResourceLocation(name)));
        if (legacy != null) key = new ResourceLocation(legacy.id());
        Item item = ForgeRegistries.ITEMS.containsKey(key) ? ForgeRegistries.ITEMS.getValue(key) : null;
        if (item == null) throw new IllegalArgumentException("Unknown item: " + id);
        return new ItemStack(item, 1, legacy == null ? 0 : legacy.meta());
    }

    /**
     * Copies a native stack or makes a stack of an item or block.
     *
     * @param item {@link ItemStack}, {@link Item}, {@link Block} or an item id.
     * @return independent stack.
     * @throws IllegalArgumentException if the object is not an item or the block has no item.
     */
    static ItemStack nativeItem(Object item) {
        if (item instanceof ItemStack stack) return stack.copy();
        if (item instanceof Item value) return new ItemStack(value);
        if (item instanceof Block block) {
            ItemStack stack = new ItemStack(block);
            if (stack.isEmpty()) throw new IllegalArgumentException("Block has no item: " + block.getRegistryName());
            return stack;
        }
        if (item instanceof String id) return resolveItem(id);
        throw new IllegalArgumentException("Expected ItemStack, Item or Block, got " + item.getClass().getName());
    }

    /**
     * Bounds of an item model: a unit cube for blocks and for the isometric inventory view, a thin square for
     * other items in a frame or on the ground.
     *
     * @param item    stack from {@link #nativeItem} or {@link #resolveItem}.
     * @param display model placement.
     * @return centered bounds, or null for an empty stack.
     */
    static Aabb itemBounds(Object item, ItemNode.Display display) {
        ItemStack stack = (ItemStack) item;
        if (stack.isEmpty()) return null;
        return display == ItemNode.Display.GUI || stack.getItem() instanceof ItemBlock ? ITEM_BOUNDS : FLAT_BOUNDS;
    }

    /**
     * @param display placement of a scene item. @return the vanilla camera transform of that placement.
     */
    private static ItemCameraTransforms.TransformType transform(ItemNode.Display display) {
        return switch (display) {
            case FIXED -> ItemCameraTransforms.TransformType.FIXED;
            case GROUND -> ItemCameraTransforms.TransformType.GROUND;
            case GUI -> ItemCameraTransforms.TransformType.GUI;
        };
    }

    /**
     * Finds an entity type by registry id.
     *
     * @param id entity id, for example {@code minecraft:zombie}.
     * @return entity handle, created when a world is loaded.
     * @throws IllegalArgumentException if the type is unknown.
     */
    static SceneEntity resolveEntity(String id) {
        ResourceLocation key = new ResourceLocation(id);
        EntityEntry entry = ForgeRegistries.ENTITIES.containsKey(key) ? ForgeRegistries.ENTITIES.getValue(key) : null;
        if (entry == null) throw new IllegalArgumentException("Unknown entity: " + id);
        return new SceneEntity(null, entry);
    }

    /**
     * Wraps a native entity or entity type.
     *
     * @param entity {@link Entity}, {@link EntityEntry}, entity {@link Class} or an id.
     * @return entity handle.
     * @throws IllegalArgumentException if the object does not describe a registered entity.
     */
    static SceneEntity nativeEntity(Object entity) {
        if (entity instanceof Entity value) return new SceneEntity(value, null);
        if (entity instanceof EntityEntry entry) return new SceneEntity(null, entry);
        if (entity instanceof String id) return resolveEntity(id);
        if (entity instanceof Class<?> type && Entity.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            EntityEntry entry = EntityRegistry.getEntry((Class<? extends Entity>) type);
            if (entry == null) throw new IllegalArgumentException("Entity class is not registered: " + type.getName());
            return new SceneEntity(null, entry);
        }
        throw new IllegalArgumentException("Expected Entity, EntityEntry or entity Class, got "
                + entity.getClass().getName());
    }

    /**
     * Bounds of an entity standing at the origin.
     *
     * @param entity handle from {@link #resolveEntity} or {@link #nativeEntity}.
     * @return bounding box of the entity, or a two-block column until it exists.
     */
    static Aabb entityBounds(Object entity) {
        Entity value = ((SceneEntity) entity).entity();
        if (value == null) return ENTITY_FALLBACK;
        float half = value.width / 2;
        return new Aabb(-half, 0, -half, half, value.height, half);
    }

    /**
     * Turns lighting on for vanilla draws, with the light directions fixed in the scene.
     */
    static void beginVanilla() {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
    }

    /**
     * Draws an item with the camera transform of its model for the placement, centered at the origin.
     *
     * @param item    stack to draw.
     * @param display model placement.
     * @return false if the item could not be drawn.
     */
    static boolean drawItem(Object item, ItemNode.Display display) {
        ItemStack stack = (ItemStack) item;
        if (stack.isEmpty() || BROKEN_ITEMS.contains(stack.getItem())) return false;
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.color(1, 1, 1, 1);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, transform(display));
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN_ITEMS.add(stack.getItem())) {
                LOGGER.error("UniGUI could not draw item {} in a 3D scene; it is skipped from now on",
                        stack.getItem().getRegistryName(), failure);
            }
            return false;
        } finally {
            unwind(depth);
        }
    }

    /**
     * Draws an entity with its feet at the origin, facing +Z, without a shadow.
     *
     * @param entity      entity handle.
     * @param node        entity node with an optional head pose.
     * @param partialTick fraction of the current tick for animations.
     * @return false if the entity does not exist yet or could not be drawn.
     */
    static boolean drawEntity(Object entity, EntityNode node, float partialTick) {
        Entity value = ((SceneEntity) entity).entity();
        if (value == null || BROKEN_ENTITIES.contains(value.getClass())) return false;
        var pose = new CleanroomEntityPose(value);
        Minecraft minecraft = Minecraft.getMinecraft();
        RenderManager renderer = minecraft.getRenderManager();
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GlStateManager.pushMatrix();
        boolean shadows = renderer.isRenderShadow();
        float viewX = renderer.playerViewX, viewY = renderer.playerViewY;
        GameSettings options = renderer.options;
        try {
            GlStateManager.color(1, 1, 1, 1);
            // A shadow is projected onto world blocks around the entity's real position, not onto the scene.
            renderer.setRenderShadow(false);
            // Billboards (fireballs, snowballs, name tags) turn by 180 - viewY around Y and -viewX around X towards
            // the player's camera, which has no relation to the scene camera; the single-sided quad then faced away
            // or edge-on. A fixed view turns them to +Z, the direction entities face in a scene.
            renderer.playerViewY = 180;
            renderer.playerViewX = 0;
            if (options == null) renderer.options = minecraft.gameSettings;
            renderer.renderEntity(value, 0, 0, 0, 0, pose.apply(value, node, partialTick), false);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN_ENTITIES.add(value.getClass())) {
                LOGGER.error("UniGUI could not draw entity {} in a 3D scene; it is skipped from now on",
                        value.getClass().getName(), failure);
            }
            return false;
        } finally {
            pose.restore(value);
            renderer.setRenderShadow(shadows);
            renderer.playerViewX = viewX;
            renderer.playerViewY = viewY;
            renderer.options = options;
            unwind(depth);
            // The hurt overlay enables texturing on the lightmap unit and leaves it on; the lightmap would then tint
            // everything drawn after the scene, like GuiInventory.drawEntityOnScreen turns it off again.
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
        }
    }

    /**
     * Tells whether a block state is drawn, fully or in part, by a tile entity renderer: banners, beds, shulker
     * boxes, skulls, signs, the enchanting table book. Chests are baked by {@link CleanroomScene} instead.
     *
     * @param nativeState state resolved by {@link CleanroomScene}, or null.
     * @return true if {@link #drawBlockEntity} has something to draw.
     */
    static boolean hasBlockEntity(Object nativeState) {
        return nativeState instanceof IBlockState state && blockEntity(state) != null;
    }

    /**
     * Creates the preview tile entity of a state once.
     *
     * @param state block state.
     * @return tile entity with a special renderer, or null.
     */
    private static TileEntity blockEntity(IBlockState state) {
        if (BLOCK_ENTITIES.containsKey(state)) return BLOCK_ENTITIES.get(state);
        Block block = state.getBlock();
        TileEntity created = null;
        if (CleanroomScene.chestTexture(block) == null && block.hasTileEntity(state)) {
            try {
                TileEntity value = block.createTileEntity(Minecraft.getMinecraft().world, state);
                if (value != null && TileEntityRendererDispatcher.instance.getRenderer(value) != null) created = value;
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("UniGUI could not create the tile entity of {} for a 3D scene", state, failure);
            }
        }
        BLOCK_ENTITIES.put(state, created);
        return created;
    }

    /**
     * Draws the tile entity part of a block at the origin cell, with the block's corner at the origin like the baked
     * block geometry. The tile entity is placed at {@link BlockPos#ORIGIN} of the current world, if any, with its
     * block and metadata preset, so renderers read the previewed state instead of the world.
     *
     * @param nativeState block state from {@link CleanroomScene}.
     * @param partialTick fraction of the current tick for animations.
     * @return false if the block has no tile entity renderer or drawing failed.
     */
    static boolean drawBlockEntity(Object nativeState, float partialTick) {
        IBlockState state = (IBlockState) nativeState;
        TileEntity tile = blockEntity(state);
        if (tile == null || BROKEN_ENTITIES.contains(tile.getClass())) return false;
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer<TileEntity> renderer = dispatcher.getRenderer(tile);
        if (renderer == null) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        TextureManager textures = dispatcher.renderEngine;
        FontRenderer font = dispatcher.fontRenderer;
        World dispatcherWorld = dispatcher.world;
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GlStateManager.pushMatrix();
        try {
            // The dispatcher is prepared by world rendering; outside a world its texture manager is still missing.
            if (textures == null) dispatcher.renderEngine = minecraft.getTextureManager();
            if (font == null) dispatcher.fontRenderer = minecraft.fontRenderer;
            World world = minecraft.world;
            if (dispatcherWorld == null) dispatcher.world = world;
            // Banner and bed renderers use the metadata only when the tile entity has a world.
            tile.setWorld(world);
            tile.setPos(BlockPos.ORIGIN);
            SceneTileEntityAccess access = (SceneTileEntityAccess) tile;
            access.unigui$setBlockType(state.getBlock());
            access.unigui$setBlockMetadata(state.getBlock().getMetaFromState(state));
            GlStateManager.color(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            renderer.render(tile, 0, 0, 0, partialTick, -1, 1);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN_ENTITIES.add(tile.getClass())) {
                LOGGER.error("UniGUI could not draw tile entity {} in a 3D scene; it is skipped from now on",
                        tile.getClass().getName(), failure);
            }
            return false;
        } finally {
            dispatcher.renderEngine = textures;
            dispatcher.fontRenderer = font;
            dispatcher.world = dispatcherWorld;
            tile.setWorld(null);
            unwind(depth);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.enableCull();
        }
    }

    /**
     * Returns the GL state to the UI defaults.
     */
    static void endVanilla() {
        CleanroomGlState.afterVanilla();
    }

    /**
     * Pops the matrices a failed draw left on the modelview stack.
     *
     * @param depth stack depth before the draw.
     */
    private static void unwind(int depth) {
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth) GlStateManager.popMatrix();
    }

    /**
     * Entity of a scene node: an existing entity, or a preview created from its registry entry in the current
     * world and created again when the world changes.
     */
    static final class SceneEntity {
        /**
         * Existing entity of the user, or null for a preview.
         */
        private final Entity explicit;
        /**
         * Registry entry of the preview, or null for an existing entity.
         */
        private final EntityEntry entry;
        /**
         * Preview created in {@link #world}.
         */
        private Entity created;
        /**
         * World of the preview attempt.
         */
        private WeakReference<World> world = new WeakReference<>(null);
        /**
         * Whether creating the preview in {@link #world} failed.
         */
        private boolean failed;

        /**
         * @param explicit existing entity or null.
         * @param entry    preview type or null.
         */
        private SceneEntity(Entity explicit, EntityEntry entry) {
            this.explicit = explicit;
            this.entry = explicit == null ? Objects.requireNonNull(entry, "entry") : null;
        }

        /**
         * Reads the entity, creating the preview in the current world if needed.
         *
         * @return entity, or null without a world or if it cannot be created.
         */
        Entity entity() {
            if (explicit != null) return explicit;
            World current = Minecraft.getMinecraft().world;
            if (current == null) {
                created = null;
                return null;
            }
            if (world.get() == current) return failed ? null : created;
            world = new WeakReference<>(current);
            created = null;
            failed = true;
            try {
                created = entry.newInstance(current);
                failed = created == null;
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("UniGUI could not create entity {} for a 3D scene", entry.getRegistryName(), failure);
            }
            return created;
        }
    }
}
