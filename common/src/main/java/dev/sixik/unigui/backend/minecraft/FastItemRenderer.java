package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.backend.minecraft.custom_renders.MinecraftRendererPlatformHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minecraft-version-specific fast path for GUI item previews.
 *
 * <p>The class deliberately keeps vanilla {@link ItemRenderer} as the source of truth for the actual
 * item draw. It only adds a stable model/icon cache around items that are safe to snapshot, so custom
 * renderers, animated compass/clock textures, and foil/glint items stay on the live path.</p>
 */
final class FastItemRenderer implements AutoCloseable {
    private static final int RESOLVED_MODEL_CAPACITY = 1024;
    private static final int ICON_TEXTURE_CAPACITY = 512;
    private static final int MAX_BAKES_PER_FRAME = 12;
    private static final int MIN_FLAT_ICON_PIXELS = 32;
    private static final int MAX_FLAT_ICON_PIXELS = 64;
    private static final int GUI_3D_ICON_PIXELS = 128;

    private final Minecraft minecraft;
    private final LinkedHashMap<ItemCacheKey, ResolvedItem> resolvedCache =
            new LinkedHashMap<>(RESOLVED_MODEL_CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<ItemCacheKey, ResolvedItem> eldest) {
                    return size() > RESOLVED_MODEL_CAPACITY;
                }
            };
    private final LinkedHashMap<IconCacheKey, CachedIcon> iconCache =
            new LinkedHashMap<>(ICON_TEXTURE_CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<IconCacheKey, CachedIcon> eldest) {
                    if (size() <= ICON_TEXTURE_CAPACITY) return false;
                    eldest.getValue().close();
                    return true;
                }
            };

    private int bakesThisFrame;

    FastItemRenderer(Minecraft minecraft) {
        this.minecraft = Objects.requireNonNull(minecraft, "minecraft");
    }

    void beginFrame() {
        bakesThisFrame = 0;
    }

    TextureHandle cachedTexture(ItemStack stack, float requestedSize) {
        if (stack == null || stack.isEmpty()) return null;
        ResolvedItem resolved = resolve(stack);
        if (!resolved.cacheable()) return null;
        CachedIcon icon = iconCache.get(new IconCacheKey(ItemCacheKey.from(stack), iconPixels(requestedSize, resolved.model())));
        return icon == null ? null : icon.texture();
    }

    boolean prefersCachedPath(ItemStack stack) {
        return stack != null && !stack.isEmpty() && resolve(stack).cacheable();
    }

    TextureHandle bakeIfBudget(ItemStack stack, float requestedSize, MinecraftRenderTarget restoreTarget) {
        if (stack == null || stack.isEmpty() || bakesThisFrame >= MAX_BAKES_PER_FRAME) return null;

        ResolvedItem resolved = resolve(stack);
        if (!resolved.cacheable()) return null;

        IconCacheKey iconKey = new IconCacheKey(ItemCacheKey.from(stack), iconPixels(requestedSize, resolved.model()));
        CachedIcon cached = iconCache.get(iconKey);
        if (cached != null) return cached.texture();

        bakesThisFrame++;
        CachedIcon baked = bake(iconKey, stack, resolved.model(), restoreTarget);
        iconCache.put(iconKey, baked);
        return baked.texture();
    }

    void clear() {
        resolvedCache.clear();
        for (CachedIcon icon : iconCache.values()) {
            icon.close();
        }
        iconCache.clear();
    }

    @Override
    public void close() {
        clear();
    }

    private ResolvedItem resolve(ItemStack stack) {
        ItemCacheKey key = ItemCacheKey.from(stack);
        ResolvedItem cached = resolvedCache.get(key);
        if (cached != null) return cached;

        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, minecraft.level, minecraft.player, 0);
        ResolvedItem resolved = new ResolvedItem(model, isSafeToCache(stack, model));
        resolvedCache.put(key, resolved);
        return resolved;
    }

    private boolean isSafeToCache(ItemStack stack, BakedModel model) {
        if (model == null || model.isCustomRenderer()) return false;
        if (stack.hasFoil()) return false;
        if (hasAnimatedTexture(stack)) return false;
        return !MinecraftRendererPlatformHook.hasCustomItemRendererImpl(stack);
    }

    private static boolean hasAnimatedTexture(ItemStack stack) {
        return stack.is(ItemTags.COMPASSES) || stack.is(Items.CLOCK);
    }

    private CachedIcon bake(IconCacheKey key, ItemStack sourceStack, BakedModel model, MinecraftRenderTarget restoreTarget) {
        MinecraftRenderTarget target = new MinecraftRenderTarget(
                key.pixels(),
                key.pixels(),
                new RenderTargetOptions(true, true, "item_preview_" + key.pixels()));
        ItemStack stack = sourceStack.copy();
        ScissorState scissor = ScissorState.capture();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableScissor();
        target.bindWrite();
        RenderSystem.backupProjectionMatrix();
        Object modelView = RenderSystem.getModelViewStack();
        MinecraftFastItemCompat.pushModelView(modelView);
        try {
            MinecraftFastItemCompat.resetModelView(modelView);
            RenderSystem.applyModelViewMatrix();
            float depth = Math.max(1000.0f, key.pixels() * 32.0f);
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0.0f, key.pixels(), key.pixels(), 0.0f, -depth, depth),
                    VertexSorting.ORTHOGRAPHIC_Z);
            renderStackToBoundTarget(stack, model, key.pixels());
        } finally {
            MinecraftFastItemCompat.popModelView(modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.unbindWrite();
            if (restoreTarget != null) {
                restoreTarget.bindWritePreserveContents();
            } else {
                minecraft.getMainRenderTarget().bindWrite(true);
            }
            scissor.restore();
        }

        return new CachedIcon(target);
    }

    private void renderStackToBoundTarget(ItemStack stack, BakedModel model, int pixels) {
        boolean flatLighting = !model.usesBlockLight();
        RenderPassState state = RenderPassState.capture();
        GuiGraphics bakeGraphics = new GuiGraphics(minecraft, MinecraftBufferCompat.immediate(256));
        PoseStack pose = bakeGraphics.pose();
        pose.pushPose();
        try {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            if (!flatLighting) {
                Lighting.setupFor3DItems();
            }
            float scale = pixels / 16.0f;
            pose.scale(scale, scale, scale);
            bakeGraphics.renderItem(stack, 0, 0);
            bakeGraphics.flush();
        } finally {
            Lighting.setupFor3DItems();
            state.restore();
            pose.popPose();
        }
    }

    private static int iconPixels(float requestedSize, BakedModel model) {
        if (model != null && (model.isGui3d() || model.usesBlockLight())) {
            return GUI_3D_ICON_PIXELS;
        }
        if (!Float.isFinite(requestedSize) || requestedSize <= MIN_FLAT_ICON_PIXELS) return MIN_FLAT_ICON_PIXELS;
        return Math.min(MAX_FLAT_ICON_PIXELS, requestedSize <= 40.0f ? MIN_FLAT_ICON_PIXELS : MAX_FLAT_ICON_PIXELS);
    }

    private record ResolvedItem(BakedModel model, boolean cacheable) {
    }

    private record IconCacheKey(ItemCacheKey item, int pixels) {
    }

    private record CachedIcon(MinecraftRenderTarget target) implements AutoCloseable {
        private TextureHandle texture() {
            return target.colorTexture();
        }

        @Override
        public void close() {
            target.close();
        }
    }

    private record ScissorState(boolean enabled, int x, int y, int width, int height) {
        private static ScissorState capture() {
            boolean enabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            IntBuffer box = BufferUtils.createIntBuffer(4);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
            return new ScissorState(enabled, box.get(0), box.get(1), box.get(2), box.get(3));
        }

        private void restore() {
            if (enabled) {
                RenderSystem.enableScissor(x, y, width, height);
            } else {
                RenderSystem.disableScissor();
            }
        }
    }

    private record RenderPassState(boolean depthTest, boolean depthMask) {
        private static RenderPassState capture() {
            return new RenderPassState(
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
        }

        private void restore() {
            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            RenderSystem.depthMask(depthMask);
        }
    }

    private record ItemCacheKey(ResourceLocation itemId, int damageValue, int customModelData, long relevantNbtHash) {
        private static ItemCacheKey from(ItemStack stack) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int damage = stack.isDamageableItem() ? stack.getDamageValue() : 0;
            CompoundTag tag = tagOrNull(stack);
            int customModelData = tag != null && tag.contains("CustomModelData", Tag.TAG_INT)
                    ? tag.getInt("CustomModelData")
                    : 0;
            return new ItemCacheKey(id, damage, customModelData, relevantDataHash(stack, tag));
        }

        private static CompoundTag tagOrNull(ItemStack stack) {
            return MinecraftFastItemCompat.tagOrNull(stack);
        }

        private static long relevantDataHash(ItemStack stack, CompoundTag tag) {
            return MinecraftFastItemCompat.relevantDataHash(stack, tag);
        }
    }
}
