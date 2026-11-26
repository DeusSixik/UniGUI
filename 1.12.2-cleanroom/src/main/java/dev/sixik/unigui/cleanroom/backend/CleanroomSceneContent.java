package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.backend.shared.LegacyBlockIds;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
        if (node.hasHead()) applyHead(value, node);
        RenderManager renderer = Minecraft.getMinecraft().getRenderManager();
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GlStateManager.pushMatrix();
        boolean shadows = renderer.isRenderShadow();
        try {
            GlStateManager.color(1, 1, 1, 1);
            // A shadow is projected onto world blocks around the entity's real position, not onto the scene.
            renderer.setRenderShadow(false);
            renderer.renderEntity(value, 0, 0, 0, 0, partialTick, false);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (BROKEN_ENTITIES.add(value.getClass())) {
                LOGGER.error("UniGUI could not draw entity {} in a 3D scene; it is skipped from now on",
                        value.getClass().getName(), failure);
            }
            return false;
        } finally {
            renderer.setRenderShadow(shadows);
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
