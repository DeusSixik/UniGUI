package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.MinecraftForgeClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Item icons rendered once into small textures, then drawn as plain textured quads.
 *
 * <p>Drawing a vanilla item costs a state change, a lit 3D pass for blocks and several draw calls; a list of
 * hundreds of items (a picker grid) would be unusable that way. An icon is rendered through the vanilla item renderer
 * into its own framebuffer and reused until the resources reload. Only items whose picture cannot change are cached:
 * not enchanted (animated glint), not an animated icon (clock, compass, magma) and not drawn by the
 * {@code IItemRenderer} of a mod. Everything else stays on the live path in {@link GtnhVanillaDraw}.</p>
 *
 * <p>The cache is shared by all screens and layers; it is bounded and evicts the least recently used icon. Render
 * thread only.</p>
 */
final class GtnhItemIcons {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final int MODEL_CAPACITY = 1024;
    private static final int ICON_CAPACITY = 512;
    /**
     * Icons baked per frame; a full grid of new items spreads over several frames instead of one long stall.
     */
    private static final int MAX_BAKES_PER_FRAME = 12;
    private static final int MIN_PIXELS = 16;
    private static final int MIN_PIXELS_3D = 32;
    private static final int MAX_PIXELS = 256;
    private static final GtnhItemIcons SHARED = new GtnhItemIcons();

    /**
     * What identifies the picture of an item: the model never depends on count or on durability.
     */
    private record ItemKey(Item item, int metadata, NBTTagCompound tag) {
    }

    private record IconKey(ItemKey item, int pixels) {
    }

    /**
     * Result of inspecting an item's model once.
     */
    private record Resolved(boolean cacheable, boolean gui3d) {
    }

    private final Map<ItemKey, Resolved> resolved = new LinkedHashMap<>(MODEL_CAPACITY, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ItemKey, Resolved> eldest) {
            return size() > MODEL_CAPACITY;
        }
    };
    private final Map<IconKey, GtnhRenderTarget> icons = new LinkedHashMap<>(ICON_CAPACITY, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<IconKey, GtnhRenderTarget> eldest) {
            if (size() <= ICON_CAPACITY) return false;
            eldest.getValue().close();
            return true;
        }
    };
    /**
     * Created on the first bake: LWJGL buffers need its native library, which unit tests do not load.
     */
    private IntBuffer scissor;
    private int bakesThisFrame;
    private int generation = -1;

    private GtnhItemIcons() {
    }

    static GtnhItemIcons shared() {
        return SHARED;
    }

    /**
     * Resets the per-frame bake budget.
     */
    void beginFrame() {
        bakesThisFrame = 0;
    }

    /**
     * Edge length, in pixels, of the icon texture for an item drawn at a given on-screen size.
     *
     * @param devicePixels on-screen edge of the item in physical pixels
     * @param gui3d        {@code true} for block-like models, which need more room for their depth
     * @return a multiple of 16 between 16 (32 for 3D models) and 256
     */
    static int iconPixels(float devicePixels, boolean gui3d) {
        float wanted = Float.isFinite(devicePixels) ? devicePixels : MIN_PIXELS;
        int rounded = (int) Math.ceil(wanted / 16.0f) * 16;
        return Math.max(gui3d ? MIN_PIXELS_3D : MIN_PIXELS, Math.min(MAX_PIXELS, rounded));
    }

    /**
     * @param stack stack to look up
     * @return {@code true} if the item has a stable picture that can be cached
     */
    boolean cacheable(ItemStack stack) {
        return !GtnhItemStacks.isEmpty(stack) && resolve(stack).cacheable();
    }

    /**
     * @param stack        stack to look up
     * @param devicePixels on-screen edge of the item in physical pixels
     * @return the icon if it is already baked, otherwise {@code null}
     */
    TextureHandle cached(ItemStack stack, float devicePixels) {
        if (GtnhItemStacks.isEmpty(stack)) return null;
        syncWithResources();
        Resolved info = resolve(stack);
        if (!info.cacheable()) return null;
        GtnhRenderTarget icon = icons.get(new IconKey(key(stack), iconPixels(devicePixels, info.gui3d())));
        return icon == null || icon.isDisposed() ? null : icon.colorTexture();
    }

    /**
     * Returns the icon, baking it first if the frame still has budget.
     *
     * @param stack        stack to look up
     * @param devicePixels on-screen edge of the item in physical pixels
     * @return the icon, or {@code null} if the item is not cacheable, the budget is spent, or baking failed
     */
    TextureHandle bakeIfBudget(ItemStack stack, float devicePixels) {
        TextureHandle existing = cached(stack, devicePixels);
        if (existing != null) return existing;
        if (GtnhItemStacks.isEmpty(stack) || bakesThisFrame >= MAX_BAKES_PER_FRAME) return null;
        Resolved info = resolve(stack);
        if (!info.cacheable() || !OpenGlHelper.isFramebufferEnabled()) return null;
        IconKey key = new IconKey(key(stack), iconPixels(devicePixels, info.gui3d()));
        bakesThisFrame++;
        GtnhRenderTarget baked = bake(stack, key.pixels());
        if (baked == null) {
            // Draw it live from now on instead of trying (and allocating a framebuffer) every frame.
            resolved.put(key.item(), new Resolved(false, info.gui3d()));
            return null;
        }
        icons.put(key, baked);
        return baked.colorTexture();
    }

    /**
     * Drops every icon and every inspected model.
     */
    void clear() {
        resolved.clear();
        for (GtnhRenderTarget icon : icons.values()) icon.close();
        icons.clear();
    }

    /**
     * @return the number of baked icons
     */
    int size() {
        return icons.size();
    }

    private void syncWithResources() {
        int current = GtnhResourceReloads.generation();
        if (current == generation) return;
        // Texture atlas stitching changes on reload: models and baked pictures are stale.
        clear();
        generation = current;
    }

    private Resolved resolve(ItemStack stack) {
        syncWithResources();
        ItemKey key = key(stack);
        Resolved known = resolved.get(key);
        if (known != null) return known;
        Resolved fresh = inspect(stack);
        resolved.put(key, fresh);
        return fresh;
    }

    private static ItemKey key(ItemStack stack) {
        // Damage is drawn as a separate bar, and tools of every damage share a picture.
        int metadata = stack.isItemStackDamageable() ? 0 : stack.getItemDamage();
        NBTTagCompound tag = stack.getTagCompound();
        return new ItemKey(stack.getItem(), metadata, tag == null || tag.hasNoTags() ? null : (NBTTagCompound) tag.copy());
    }

    /**
     * Decides, once per item variant, whether its picture is stable (cacheable) and whether it is a 3D block model.
     */
    private static Resolved inspect(ItemStack stack) {
        try {
            Item item = stack.getItem();
            boolean block = item instanceof ItemBlock;
            boolean gui3d = block && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(item).getRenderType());
            boolean safe = !stack.hasEffect()
                && item != Items.clock && item != Items.compass
                // A mod that draws inventory icons itself may animate or depend on anything.
                && MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.INVENTORY) == null
                && !hasAnimatedIcon(stack);
            return new Resolved(safe, gui3d);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.warn("UniGUI could not inspect {}; it is drawn live", Item.itemRegistry.getNameForObject(stack.getItem()),
                e);
            return new Resolved(false, false);
        }
    }

    /**
     * An icon of the block or item atlas with animation metadata (magma, fire, portals) changes between frames.
     */
    private static boolean hasAnimatedIcon(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ItemBlock) {
            Block block = Block.getBlockFromItem(item);
            int metadata = stack.getItemDamage();
            for (int side = 0; side < 6; side++) {
                if (animated(block.getIcon(side, metadata))) return true;
            }
            return false;
        }
        int passes = item.requiresMultipleRenderPasses() ? item.getRenderPasses(stack.getItemDamage()) : 1;
        for (int pass = 0; pass < passes; pass++) {
            if (animated(item.getIcon(stack, pass))) return true;
        }
        return animated(stack.getIconIndex());
    }

    private static boolean animated(IIcon icon) {
        return icon instanceof TextureAtlasSprite sprite && sprite.hasAnimationMetadata();
    }

    /**
     * Renders the stack into a new square target, drawing at a top-left origin so the picture is upright like any
     * other UI image. The framebuffer, viewport, scissor, matrices and GL state of the caller are preserved.
     */
    private GtnhRenderTarget bake(ItemStack source, int pixels) {
        GtnhRenderTarget target = null;
        GtnhRenderTarget.Guard guard = null;
        boolean scissorWas = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissor == null) scissor = BufferUtils.createIntBuffer(16);
        scissor.clear();
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
        int[] box = {scissor.get(0), scissor.get(1), scissor.get(2), scissor.get(3)};
        boolean ok = false;
        try {
            target = new GtnhRenderTarget(pixels, pixels, new RenderTargetOptions(true, true, "item_icon"));
            guard = GtnhRenderTarget.Guard.capture();
            GtnhGlState.disableScissor();
            target.bindWrite();
            float depth = Math.max(1000.0f, pixels * 32.0f);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0, pixels, pixels, 0.0, -depth, depth);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            try {
                ItemStack one = source.copy();
                one.stackSize = 1;
                ok = GtnhVanillaDraw.item(one, 0.0f, 0.0f, pixels, false);
            } finally {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.warn("UniGUI could not bake the icon of {}", Item.itemRegistry.getNameForObject(source.getItem()), e);
        } finally {
            if (guard != null) guard.restore();
            if (scissorWas) GtnhGlState.scissor(box);
            else GtnhGlState.disableScissor();
        }
        if (!ok && target != null) {
            target.close();
            return null;
        }
        return target;
    }
}
