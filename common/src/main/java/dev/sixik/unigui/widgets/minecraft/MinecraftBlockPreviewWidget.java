package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftGuiRenderBackend;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class MinecraftBlockPreviewWidget extends MinecraftPreviewWidget {
    private Block block;
    /** Стек создаётся лениво: в 26.x до загрузки мира {@code ItemStack} создать нельзя. */
    private ItemStack stack;

    public MinecraftBlockPreviewWidget(String label, Block block) {
        super(label);
        block(block);
    }

    public Block block() {
        return block;
    }

    public MinecraftBlockPreviewWidget block(Block block) {
        this.block = block;
        stack = null;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    protected void renderMinecraftPreview(MinecraftGuiRenderBackend backend, float x, float y, float size, float opacity) {
        backend.renderItemPreview(stack(), x, y, size, opacity, false);
    }

    @Override
    protected TextureHandle previewTexture(RenderContext context, float size) {
        ItemStack stack = stack();
        if (stack.isEmpty()) return null;
        if (context.backend() instanceof MinecraftGuiRenderBackend backend) {
            return backend.cachedItemPreviewTexture(stack, size);
        }
        return null;
    }

    private ItemStack stack() {
        if (stack != null) return stack;
        if (block == null) return stack = ItemStack.EMPTY;
        if (!MinecraftClientCompat.itemsReady()) return ItemStack.EMPTY;
        return stack = new ItemStack(block);
    }

    @Override
    protected String fallbackText() {
        return "block";
    }
}
