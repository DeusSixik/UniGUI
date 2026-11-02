package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.gtnh.backend.GtnhGuiRenderBackend;
import dev.sixik.unigui.gtnh.backend.GtnhItemStacks;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Shows an item stack drawn by the vanilla item renderer, with optional count and durability bar.
 */
public final class MinecraftItemPreviewWidget extends MinecraftPreviewWidget {
    private ItemStack stack = GtnhItemStacks.EMPTY;
    private boolean decorations = true;

    public MinecraftItemPreviewWidget(String label, Item item) {
        this(label, item == null ? GtnhItemStacks.EMPTY : new ItemStack(item));
    }

    public MinecraftItemPreviewWidget(String label, Block block) {
        this(label, block == null ? GtnhItemStacks.EMPTY : new ItemStack(block));
    }

    public MinecraftItemPreviewWidget(String label, ItemStack stack) {
        super(label);
        stack(stack);
    }

    public ItemStack stack() {
        return stack;
    }

    public MinecraftItemPreviewWidget stack(ItemStack stack) {
        this.stack = GtnhItemStacks.copyOrEmpty(stack);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean decorations() {
        return decorations;
    }

    public MinecraftItemTooltip vanillaTooltip() {
        return new MinecraftItemTooltip(this, this::stack);
    }

    public MinecraftItemPreviewWidget addVanillaTooltip(OverlayLayer overlayLayer) {
        if (overlayLayer != null) {
            overlayLayer.addOverlay(vanillaTooltip());
        }
        return this;
    }

    public MinecraftItemPreviewWidget decorations(boolean decorations) {
        if (this.decorations == decorations) return this;
        this.decorations = decorations;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    protected void renderMinecraftPreview(GtnhGuiRenderBackend backend, float x, float y, float size, float opacity) {
        backend.renderItemPreview(stack, x, y, size, opacity, decorations);
    }

    @Override
    protected TextureHandle previewTexture(RenderContext context, float size) {
        if (decorations || GtnhItemStacks.isEmpty(stack)) return null;
        if (context.backend() instanceof GtnhGuiRenderBackend backend) {
            return backend.cachedItemPreviewTexture(stack, size);
        }
        return null;
    }

    @Override
    protected String fallbackText() {
        return "item";
    }
}