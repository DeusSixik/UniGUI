package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class MinecraftItemPreviewWidget extends MinecraftPreviewWidget {
    private ItemStack stack = ItemStack.EMPTY;
    private boolean decorations = true;

    public MinecraftItemPreviewWidget(String label, ItemLike item) {
        this(label, item == null ? ItemStack.EMPTY : new ItemStack(item));
    }

    public MinecraftItemPreviewWidget(String label, ItemStack stack) {
        super(label);
        stack(stack);
    }

    public ItemStack stack() {
        return stack;
    }

    public MinecraftItemPreviewWidget stack(ItemStack stack) {
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean decorations() {
        return decorations;
    }

    public MinecraftItemPreviewWidget decorations(boolean decorations) {
        if (this.decorations == decorations) return this;
        this.decorations = decorations;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    protected void renderMinecraftPreview(MinecraftGuiRenderBackend backend, float x, float y, float size, float opacity) {
        backend.renderItemPreview(stack, x, y, size, opacity, decorations);
    }

    @Override
    protected String fallbackText() {
        return "item";
    }
}
