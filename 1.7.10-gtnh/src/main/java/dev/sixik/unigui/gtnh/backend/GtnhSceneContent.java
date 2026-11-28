package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.backend.shared.LegacyBlockIds;
import dev.sixik.unigui.backend.shared.LegacyChestModel;
import dev.sixik.unigui.gtnh.mixins.early.SceneTileRendererAccess;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Items and entities of 3D scenes on 1.7.10: resolution of ids and native objects, bounds and drawing through the
 * game's fixed-function renderers.
 *
 * <p>Entity ids follow the modern form ({@code minecraft:zombie}, {@code minecraft:cave_spider}) and are matched
 * against the {@link EntityList} names ({@code Zombie}, {@code CaveSpider}); an {@link EntityList} name itself is
 * accepted too. Mod entities are found as {@code modid:name} for the name {@code modid.Name}.</p>
 */
final class GtnhSceneContent {
    /**
     * Error log for content that cannot be drawn.
     */
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    /**
     * Half thickness of a flat item, one texture pixel in total as in {@link ItemRenderer#renderItemIn2D}.
     */
    private static final float FLAT_HALF_DEPTH = 0.03125f;
    /**
     * Bounds of a block item, centered at the origin.
     */
    private static final Aabb BLOCK_BOUNDS = new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
    /**
     * Bounds of a flat item, centered at the origin.
     */
    private static final Aabb FLAT_BOUNDS = new Aabb(-0.5f, -0.5f, -FLAT_HALF_DEPTH, 0.5f, 0.5f, FLAT_HALF_DEPTH);
    /**
     * Size of a dropped item relative to the frame placement.
     */
    private static final float GROUND_SCALE = 0.5f;
    /**
     * Bounds of an entity that has not been created yet.
     */
    private static final Aabb ENTITY_FALLBACK = new Aabb(-0.5f, 0, -0.5f, 0.5f, 2, 0.5f);
    /**
     * Modern vanilla ids whose 1.7.10 names differ by more than letter case and underscores.
     */
    private static final Map<String, String> RENAMED = Map.ofEntries(
        Map.entry("zombie_pigman", "PigZombie"), Map.entry("magma_cube", "LavaSlime"),
        Map.entry("snow_golem", "SnowMan"), Map.entry("iron_golem", "VillagerGolem"),
        Map.entry("mooshroom", "MushroomCow"), Map.entry("ocelot", "Ozelot"),
        Map.entry("horse", "EntityHorse"), Map.entry("wither", "WitherBoss"),
        Map.entry("item", "Item"), Map.entry("experience_orb", "XPOrb"));
    /**
     * Height where preview entities are placed, above the build limit, so vanilla finds no blocks for a shadow.
     */
    private static final double PREVIEW_Y = 1024;
    /**
     * Vanilla repeating texture for enchanted items.
     */
    private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    /**
     * Items whose drawing failed and is skipped from then on.
     */
    private static final Set<Item> BROKEN_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Entity classes whose drawing failed and is skipped from then on.
     */
    private static final Set<Class<?>> BROKEN_ENTITIES = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Preview tile entity per block state, or null for states without a tile entity renderer.
     */
    private static final Map<GtnhScene.GtnhBlockState, TileEntity> BLOCK_ENTITIES = new HashMap<>();
    /**
     * Block renderer for block items, created on first use.
     */
    private static RenderBlocks renderBlocks;
    /**
     * Closed vanilla model reused for chest stacks.
     */
    private static ModelChest chestModel;

    private GtnhSceneContent() {
    }

    /**
     * Finds an item by registry id.
     *
     * @param id item id, for example {@code minecraft:diamond_sword}.
     * @return stack of one item.
     * @throws IllegalArgumentException if the item is unknown.
     */
    static ItemStack resolveItem(String id) {
        String key = id.indexOf(':') < 0 ? "minecraft:" + id : id;
        LegacyBlockIds.Legacy legacy = LegacyBlockIds.translateItem(key, Item.itemRegistry::containsKey);
        Object item = Item.itemRegistry.getObject(legacy == null ? key : legacy.id());
        if (!(item instanceof Item value)) throw new IllegalArgumentException("Unknown item: " + id);
        return new ItemStack(value, 1, legacy == null ? 0 : legacy.meta());
    }

    /**
     * Copies a native stack or makes a stack of an item or block.
     *
     * @param item {@link ItemStack}, {@link Item}, {@link Block} or an item id.
     * @return independent stack.
     * @throws IllegalArgumentException if the object is not an item or the block has no item.
     */
    static ItemStack nativeItem(Object item) {
        if (item instanceof ItemStack stack) {
            if (stack.getItem() == null) throw new IllegalArgumentException("ItemStack has no item");
            return stack.copy();
        }
        if (item instanceof Item value) return new ItemStack(value);
        if (item instanceof Block block) {
            Item value = Item.getItemFromBlock(block);
            if (value == null) throw new IllegalArgumentException("Block has no item: " + block.getUnlocalizedName());
            return new ItemStack(value);
        }
        if (item instanceof String id) return resolveItem(id);
        throw new IllegalArgumentException("Expected ItemStack, Item or Block, got " + item.getClass().getName());
    }

    /**
     * Bounds of an item model: a cube for blocks drawn in 3D, a thin square otherwise.
     *
     * @param item    stack from {@link #nativeItem} or {@link #resolveItem}.
     * @param display model placement; {@link ItemNode.Display#GROUND} is half the size.
     * @return centered bounds.
     */
    static Aabb itemBounds(Object item, ItemNode.Display display) {
        Aabb bounds = isBlock3d((ItemStack) item) ? BLOCK_BOUNDS : FLAT_BOUNDS;
        if (display != ItemNode.Display.GROUND) return bounds;
        return new Aabb(bounds.minX() * GROUND_SCALE, bounds.minY() * GROUND_SCALE, bounds.minZ() * GROUND_SCALE,
            bounds.maxX() * GROUND_SCALE, bounds.maxY() * GROUND_SCALE, bounds.maxZ() * GROUND_SCALE);
    }

    /**
     * Finds an entity name by id.
     *
     * @param id modern id or {@link EntityList} name.
     * @return entity handle, created when a world is loaded.
     * @throws IllegalArgumentException if no entity has this name.
     */
    static SceneEntity resolveEntity(String id) {
        return new SceneEntity(null, entityName(id));
    }

    /**
     * Wraps a native entity, its class or name.
     *
     * @param entity {@link Entity}, entity {@link Class} or a name.
     * @return entity handle.
     * @throws IllegalArgumentException if the object does not describe a registered entity.
     */
    static SceneEntity nativeEntity(Object entity) {
        if (entity instanceof Entity value) return new SceneEntity(value, null);
        if (entity instanceof String id) return resolveEntity(id);
        if (entity instanceof Class<?> type) {
            String name = EntityList.classToStringMapping.get(type);
            if (name == null) throw new IllegalArgumentException("Entity class is not registered: " + type.getName());
            return new SceneEntity(null, name);
        }
        throw new IllegalArgumentException("Expected Entity, entity Class or name, got " + entity.getClass().getName());
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
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
            GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableStandardItemLighting();
    }

    /**
     * Draws an item centered at the origin: a block item as its inventory block, other items as a flat sprite with
     * one pixel of thickness facing +Z.
     *
     * @param item    stack to draw.
     * @param display model placement: 1.7.10 has no per-placement models, so only the ground size differs.
     * @return false if the item could not be drawn.
     */
    static boolean drawItem(Object item, ItemNode.Display display) {
        ItemStack stack = (ItemStack) item;
        Item value = stack.getItem();
        if (value == null || BROKEN_ITEMS.contains(value)) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GL11.glPushMatrix();
        try {
            if (display == ItemNode.Display.GROUND) GL11.glScalef(GROUND_SCALE, GROUND_SCALE, GROUND_SCALE);
            if (isBlock3d(stack)) {
                Block block = Block.getBlockFromItem(value);
                String chestTexture = GtnhScene.chestTexture(block);
                if (chestTexture != null) {
                    drawChestItem(minecraft, chestTexture);
                    return true;
                }
                minecraft.getTextureManager().bindTexture(minecraft.getTextureManager().getResourceLocation(0));
                if (renderBlocks == null) renderBlocks = new RenderBlocks();
                renderBlocks.useInventoryTint = true;
                renderBlocks.renderBlockAsItem(block, stack.getItemDamage(), 1.0f);
                return true;
            }
            var itemAtlas = minecraft.getTextureManager().getResourceLocation(stack.getItemSpriteNumber());
            // Undo the held-item renderer's mirrored U orientation and center the extrusion.
            GL11.glRotatef(180, 0, 1, 0);
            GL11.glTranslatef(-0.5f, -0.5f, FLAT_HALF_DEPTH);
            int passes = value.requiresMultipleRenderPasses() ? value.getRenderPasses(stack.getItemDamage()) : 1;
            for (int pass = 0; pass < passes; pass++) {
                IIcon icon = value.getIcon(stack, pass);
                if (icon == null) continue;
                minecraft.getTextureManager().bindTexture(itemAtlas);
                int color = value.getColorFromItemStack(stack, pass);
                GL11.glColor4f((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
                ItemRenderer.renderItemIn2D(Tessellator.instance, icon.getMaxU(), icon.getMinV(), icon.getMinU(),
                    icon.getMaxV(), icon.getIconWidth(), icon.getIconHeight(), 2 * FLAT_HALF_DEPTH);
                if (stack.hasEffect(pass)) drawGlint(minecraft, icon.getIconWidth(), icon.getIconHeight());
            }
            GL11.glColor4f(1, 1, 1, 1);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN_ITEMS.add(value)) {
                LOGGER.error("UniGUI could not draw item {} in a 3D scene; it is skipped from now on",
                    Item.itemRegistry.getNameForObject(value), failure);
            }
            return false;
        } finally {
            unwind(depth);
        }
    }

    /**
     * Draws the inventory chest with the client texture manager, which is available before joining a world.
     * The vanilla chest dispatcher only binds the texture after it has been initialized by a world render.
     *
     * @param minecraft client with the active resource textures
     * @param texture   vanilla chest texture basename
     */
    private static void drawChestItem(Minecraft minecraft, String texture) {
        minecraft.getTextureManager().bindTexture(new ResourceLocation(LegacyChestModel.textureId(texture)));
        if (chestModel == null) chestModel = new ModelChest();
        // The inventory rotation follows RenderBlocks. The translation and scale center the TESR coordinates.
        GL11.glRotatef(90, 0, 1, 0);
        GL11.glTranslatef(-0.5f, 0.5f, 0.5f);
        GL11.glScalef(1, -1, -1);
        chestModel.renderAll();
    }

    /**
     * Draws vanilla's two scrolling foil layers. Equal depth clips foil to the sprite's visible pixels.
     *
     * @param minecraft client texture manager owner
     * @param width     atlas sprite width in pixels
     * @param height    atlas sprite height in pixels
     */
    private static void drawGlint(Minecraft minecraft, int width, int height) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
            | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glDepthFunc(GL11.GL_EQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            minecraft.getTextureManager().bindTexture(GLINT);
            GL11.glColor4f(0.38f, 0.19f, 0.608f, 1);
            long time = Minecraft.getSystemTime();
            drawGlintLayer(width, height, (time % 3000) / 3000f, -50);
            drawGlintLayer(width, height, -(time % 4873) / 4873f, 10);
        } finally {
            GL11.glPopAttrib();
        }
    }

    /**
     * Uses the vanilla texture transform {@code S(1/8) * T(8 * scroll,0) * R(angle)} on UVs.
     *
     * @param width  sprite width in pixels
     * @param height sprite height in pixels
     * @param scroll horizontal offset in texture repeats
     * @param angle  rotation in degrees
     */
    private static void drawGlintLayer(int width, int height, float scroll, float angle) {
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        try {
            GL11.glScalef(0.125f, 0.125f, 0.125f);
            GL11.glTranslatef(scroll * 8, 0, 0);
            GL11.glRotatef(angle, 0, 0, 1);
            ItemRenderer.renderItemIn2D(Tessellator.instance, 0, 0, 1, 1, width, height, 2 * FLAT_HALF_DEPTH);
        } finally {
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }
    }

    /**
     * Draws an entity with its feet at the origin, facing +Z.
     *
     * @param entity      entity handle.
     * @param node        entity node with an optional head pose.
     * @param partialTick fraction of the current tick for animations.
     * @return false if the entity does not exist yet or could not be drawn.
     */
    static boolean drawEntity(Object entity, EntityNode node, float partialTick) {
        Entity value = ((SceneEntity) entity).entity();
        if (value == null || BROKEN_ENTITIES.contains(value.getClass()) || RenderManager.instance.renderEngine == null) {
            return false;
        }
        var renderer = RenderManager.instance.getEntityRenderObject(value);
        if (renderer == null) return false;
        var pose = new GtnhEntityPose(value);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        RenderManager manager = RenderManager.instance;
        float viewX = manager.playerViewX, viewY = manager.playerViewY;
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GL11.glPushMatrix();
        try {
            GL11.glColor4f(1, 1, 1, 1);
            // Billboards (fireballs, snowballs, name tags) turn by 180 - viewY around Y and -viewX around X towards
            // the player's camera, unrelated to the scene camera. A fixed view turns them to +Z, where entities face.
            manager.playerViewY = 180;
            manager.playerViewX = 0;
            GtnhSceneEntityLight.drawing = value;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            renderer.doRender(value, 0, 0, 0, 0, pose.apply(value, node, partialTick));
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN_ENTITIES.add(value.getClass())) {
                LOGGER.error("UniGUI could not draw entity {} in a 3D scene; it is skipped from now on",
                    value.getClass().getName(), failure);
            }
            return false;
        } finally {
            unwind(depth);
            pose.restore(value);
            manager.playerViewX = viewX;
            manager.playerViewY = viewY;
            GtnhSceneEntityLight.drawing = null;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            // The hurt overlay enables texturing on the lightmap unit and leaves it on; the lightmap would then tint
            // everything drawn after the scene, like GuiInventory turns it off again.
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    /**
     * Tells whether a block is drawn, fully or in part, by a tile entity renderer: signs, skulls, the end portal,
     * the enchanting table book. Chests are baked by {@link GtnhScene} instead.
     *
     * @param nativeState state resolved by {@link GtnhScene}, or null.
     * @return true if {@link #drawBlockEntity} has something to draw.
     */
    static boolean hasBlockEntity(Object nativeState) {
        return nativeState instanceof GtnhScene.GtnhBlockState state && blockEntity(state) != null;
    }

    /**
     * Creates the preview tile entity of a state once.
     *
     * @param state block and metadata.
     * @return tile entity with a special renderer, or null.
     */
    private static TileEntity blockEntity(GtnhScene.GtnhBlockState state) {
        if (BLOCK_ENTITIES.containsKey(state)) return BLOCK_ENTITIES.get(state);
        Block block = state.block();
        TileEntity created = null;
        if (GtnhScene.chestTexture(block) == null && block.hasTileEntity(state.meta())) {
            try {
                TileEntity value = block.createTileEntity(Minecraft.getMinecraft().theWorld, state.meta());
                if (value != null && TileEntityRendererDispatcher.instance.getSpecialRenderer(value) != null) {
                    created = value;
                }
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("UniGUI could not create the tile entity of {} for a 3D scene",
                    Block.blockRegistry.getNameForObject(block), failure);
            }
        }
        BLOCK_ENTITIES.put(state, created);
        return created;
    }

    /**
     * Draws the tile entity part of a block with the block's corner at the origin, like the baked block geometry.
     * The block and metadata are preset on the tile entity, so renderers do not look them up in a world.
     *
     * @param nativeState block state from {@link GtnhScene}.
     * @param partialTick fraction of the current tick for animations.
     * @return false if the block has no tile entity renderer or drawing failed.
     */
    static boolean drawBlockEntity(Object nativeState, float partialTick) {
        var state = (GtnhScene.GtnhBlockState) nativeState;
        TileEntity tile = blockEntity(state);
        if (tile == null || BROKEN_ENTITIES.contains(tile.getClass())) return false;
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        TileEntitySpecialRenderer renderer = dispatcher.getSpecialRenderer(tile);
        if (renderer == null) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        var fonts = (SceneTileRendererAccess) dispatcher;
        TextureManager textures = dispatcher.field_147553_e;
        FontRenderer font = fonts.unigui$font();
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GL11.glPushMatrix();
        try {
            // World rendering prepares the dispatcher; before the first world frame both are still missing.
            if (textures == null) dispatcher.field_147553_e = minecraft.getTextureManager();
            if (font == null) fonts.unigui$setFont(minecraft.fontRenderer);
            tile.setWorldObj(minecraft.theWorld);
            tile.xCoord = tile.yCoord = tile.zCoord = 0;
            tile.blockType = state.block();
            // Skull metadata 0 is unused; 1 stands on the floor like an unplaced skull.
            tile.blockMetadata = tile instanceof TileEntitySkull && state.meta() == 0 ? 1 : state.meta();
            GL11.glColor4f(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            renderer.renderTileEntityAt(tile, 0, 0, 0, partialTick);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN_ENTITIES.add(tile.getClass())) {
                LOGGER.error("UniGUI could not draw tile entity {} in a 3D scene; it is skipped from now on",
                    tile.getClass().getName(), failure);
            }
            return false;
        } finally {
            dispatcher.field_147553_e = textures;
            fonts.unigui$setFont(font);
            tile.setWorldObj(null);
            unwind(depth);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
    }

    /**
     * Returns the GL state to the UI defaults.
     */
    static void endVanilla() {
        GtnhGlState.afterVanilla();
    }

    /**
     * Whether a stack is drawn as a 3D block like in the inventory.
     *
     * @param stack stack with an item.
     * @return true for block items with a 3D inventory model.
     */
    private static boolean isBlock3d(ItemStack stack) {
        if (stack.getItemSpriteNumber() != 0 || !(stack.getItem() instanceof ItemBlock)) return false;
        Block block = Block.getBlockFromItem(stack.getItem());
        return block != null && RenderBlocks.renderItemIn3d(block.getRenderType());
    }

    /**
     * Pops the matrices a failed draw left on the modelview stack.
     *
     * @param depth stack depth before the draw.
     */
    private static void unwind(int depth) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth) GL11.glPopMatrix();
    }

    /**
     * Finds the {@link EntityList} name for an id.
     *
     * @param id modern id, {@code modid:name} or {@link EntityList} name.
     * @return registered name.
     * @throws IllegalArgumentException if no entity matches.
     */
    static String entityName(String id) {
        Map<String, Class<? extends Entity>> names = EntityList.stringToClassMapping;
        if (names.containsKey(id)) return id;
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = id.substring(colon + 1);
        String wanted;
        if (namespace.equals("minecraft")) {
            String renamed = RENAMED.get(path.toLowerCase(Locale.ROOT));
            if (renamed != null && names.containsKey(renamed)) return renamed;
            wanted = normalize(path);
        } else {
            wanted = normalize(namespace + "." + path);
        }
        for (String name : names.keySet()) {
            if (normalize(name).equals(wanted)) return name;
        }
        throw new IllegalArgumentException("Unknown entity: " + id);
    }

    /**
     * Drops case and underscores, so {@code cave_spider} matches {@code CaveSpider}.
     *
     * @param name id or entity name.
     * @return comparable form.
     */
    private static String normalize(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    /**
     * Entity of a scene node: an existing entity, or a preview created by name in the current world and created
     * again when the world changes.
     */
    static final class SceneEntity {
        /**
         * Existing entity of the user, or null for a preview.
         */
        private final Entity explicit;
        /**
         * {@link EntityList} name of the preview, or null for an existing entity.
         */
        private final String name;
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
         * @param name     preview name or null.
         */
        private SceneEntity(Entity explicit, String name) {
            this.explicit = explicit;
            this.name = name;
        }

        /**
         * Reads the entity, creating the preview in the current world if needed.
         *
         * @return entity, or null without a world or if it cannot be created.
         */
        Entity entity() {
            if (explicit != null) return explicit;
            World current = Minecraft.getMinecraft().theWorld;
            if (current == null) {
                created = null;
                return null;
            }
            if (world.get() == current) return failed ? null : created;
            world = new WeakReference<>(current);
            created = null;
            failed = true;
            try {
                created = EntityList.createEntityByName(name, current);
                if (created != null) {
                    created.setPosition(0, PREVIEW_Y, 0);
                    failed = false;
                }
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("UniGUI could not create entity {} for a 3D scene", name, failure);
            }
            return created;
        }
    }
}
