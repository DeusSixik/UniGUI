package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.gtnh.backend.GtnhGuiRenderBackend;
import dev.sixik.unigui.gtnh.backend.GtnhItemStacks;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Shows a block through the item pipeline (its inventory model). A block without an item, such as a water block,
 * shows nothing.
 */
public final class MinecraftBlockPreviewWidget extends MinecraftPreviewWidget {
    private ItemStack stack = GtnhItemStacks.EMPTY;

    public MinecraftBlockPreviewWidget(String label, Block block) {
        super(label);
        block(block);
    }

    /**
     * @return the block shown, or {@code null} if none
     */
    public Block block() {
        return GtnhItemStacks.isEmpty(stack) ? null : Block.getBlockFromItem(stack.getItem());
    }

    public MinecraftBlockPreviewWidget block(Block block) {
        return block(block, 0);
    }

    /**
     * @param block    block to show, {@code null} clears the preview
     * @param metadata block metadata in 1.7.10 terms, for example the wool color
     * @return this widget
     */
    public MinecraftBlockPreviewWidget block(Block block, int metadata) {
        Item item = block == null ? null : Item.getItemFromBlock(block);
        stack = item == null ? GtnhItemStacks.EMPTY : new ItemStack(item, 1, Math.max(0, metadata));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    protected void renderMinecraftPreview(GtnhGuiRenderBackend backend, float x, float y, float size, float opacity) {
        backend.renderItemPreview(stack, x, y, size, opacity, false);
    }

    @Override
    protected TextureHandle previewTexture(RenderContext context, float size) {
        if (GtnhItemStacks.isEmpty(stack)) return null;
        if (context.backend() instanceof GtnhGuiRenderBackend backend) {
            return backend.cachedItemPreviewTexture(stack, size);
        }
        return null;
    }

    @Override
    protected String fallbackText() {
        return "block";
    }
}