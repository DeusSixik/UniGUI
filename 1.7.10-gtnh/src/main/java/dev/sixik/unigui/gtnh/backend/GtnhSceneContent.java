package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.backend.shared.LegacyBlockIds;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.lang.ref.WeakReference;
import java.util.Collections;
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
     * Items whose drawing failed and is skipped from then on.
     */
    private static final Set<Item> BROKEN_ITEMS = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Entity classes whose drawing failed and is skipped from then on.
     */
    private static final Set<Class<?>> BROKEN_ENTITIES = Collections.newSetFromMap(new IdentityHashMap<>());
    /**
     * Block renderer for block items, created on first use.
     */
    private static RenderBlocks renderBlocks;

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
                minecraft.getTextureManager().bindTexture(minecraft.getTextureManager().getResourceLocation(0));
                if (renderBlocks == null) renderBlocks = new RenderBlocks();
                renderBlocks.useInventoryTint = true;
                renderBlocks.renderBlockAsItem(Block.getBlockFromItem(value), stack.getItemDamage(), 1.0f);
                return true;
            }
            minecraft.getTextureManager().bindTexture(
                minecraft.getTextureManager().getResourceLocation(stack.getItemSpriteNumber()));
            // renderItemIn2D builds the sprite in [0, 1] with its front at z = 0 read from -Z; turn it to +Z.
            GL11.glRotatef(180, 0, 1, 0);
            GL11.glTranslatef(-0.5f, -0.5f, FLAT_HALF_DEPTH);
            int passes = value.requiresMultipleRenderPasses() ? value.getRenderPasses(stack.getItemDamage()) : 1;
            for (int pass = 0; pass < passes; pass++) {
                IIcon icon = value.getIcon(stack, pass);
                if (icon == null) continue;
                int color = value.getColorFromItemStack(stack, pass);
                GL11.glColor4f((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, 1);
                ItemRenderer.renderItemIn2D(Tessellator.instance, icon.getMaxU(), icon.getMinV(), icon.getMinU(),
                    icon.getMaxV(), icon.getIconWidth(), icon.getIconHeight(), 0.0625f);
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
        if (node.hasHead()) applyHead(value, node);
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GL11.glPushMatrix();
        try {
            GL11.glColor4f(1, 1, 1, 1);
            RenderManager.instance.renderEntityWithPosYaw(value, 0, 0, 0, 0, partialTick);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN_ENTITIES.add(value.getClass())) {
                LOGGER.error("UniGUI could not draw entity {} in a 3D scene; it is skipped from now on",
                    value.getClass().getName(), failure);
            }
            return false;
        } finally {
            unwind(depth);
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
     * Turns the head relative to the body. Minecraft stores the absolute head yaw in degrees; the previous-tick
     * values are set too, so interpolation does not swing the head.
     *
     * @param entity entity to pose.
     * @param node   node with the head angles in radians.
     */
    private static void applyHead(Entity entity, EntityNode node) {
        float pitch = (float) Math.toDegrees(node.headPitch());
        entity.rotationPitch = pitch;
        entity.prevRotationPitch = pitch;
        if (entity instanceof EntityLivingBase living) {
            float yaw = living.renderYawOffset + (float) Math.toDegrees(node.headYaw());
            living.rotationYawHead = yaw;
            living.prevRotationYawHead = yaw;
            living.prevRenderYawOffset = living.renderYawOffset;
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
