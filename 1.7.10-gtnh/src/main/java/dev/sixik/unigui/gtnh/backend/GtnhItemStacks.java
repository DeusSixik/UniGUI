package dev.sixik.unigui.gtnh.backend;

import net.minecraft.item.ItemStack;

/**
 * 1.7.10 has no {@code ItemStack.EMPTY}: an absent stack is {@code null}, and a stack of size zero or without an item
 * is just as empty. The Minecraft widgets (which use {@code ItemStack.EMPTY} / {@code isEmpty()} on 1.12.2 and later)
 * go through this helper so they behave the same way.
 */
public final class GtnhItemStacks {
    /**
     * The empty stack of this platform.
     */
    public static final ItemStack EMPTY = null;

    private GtnhItemStacks() {
    }

    /**
     * @param stack stack or {@code null}
     * @return {@code true} if there is nothing in it
     */
    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.stackSize <= 0;
    }

    /**
     * @param stack stack or {@code null}
     * @return a copy, or {@link #EMPTY} if the stack is empty
     */
    public static ItemStack copyOrEmpty(ItemStack stack) {
        return isEmpty(stack) ? EMPTY : stack.copy();
    }
}