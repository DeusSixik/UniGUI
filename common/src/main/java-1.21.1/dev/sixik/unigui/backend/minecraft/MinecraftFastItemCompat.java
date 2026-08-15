package dev.sixik.unigui.backend.minecraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fStack;

final class MinecraftFastItemCompat {
    private MinecraftFastItemCompat() {
    }

    static void pushModelView(Object modelView) {
        ((Matrix4fStack) modelView).pushMatrix();
    }

    static void popModelView(Object modelView) {
        ((Matrix4fStack) modelView).popMatrix();
    }

    static void resetModelView(Object modelView) {
        ((Matrix4fStack) modelView).identity();
    }

    static CompoundTag tagOrNull(ItemStack stack) {
        return null;
    }

    static long relevantDataHash(ItemStack stack, CompoundTag tag) {
        return stack.getComponents().hashCode();
    }
}
