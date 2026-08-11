package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.backend.minecraft.MinecraftGuiRenderBackend;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class MinecraftBlockPreviewWidget extends MinecraftPreviewWidget {
    private ItemStack stack = ItemStack.EMPTY;

    public MinecraftBlockPreviewWidget(String label, Block block) {
        super(label);
        block(block);
    }

    public Block block() {
        return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
    }

    public MinecraftBlockPreviewWidget block(Block block) {
        stack = block == null ? ItemStack.EMPTY : new ItemStack(block);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    protected void renderMinecraftPreview(MinecraftGuiRenderBackend backend, float x, float y, float size, float opacity) {
        backend.renderItemPreview(stack, x, y, size, opacity, false);
    }

    @Override
    protected TextureHandle previewTexture(RenderContext context, float size) {
        if (stack == null || stack.isEmpty()) return null;
        if (context.backend() instanceof MinecraftGuiRenderBackend backend) {
            return backend.cachedItemPreviewTexture(stack, size);
        }
        return null;
    }

    @Override
    protected String fallbackText() {
        return "block";
    }
}
