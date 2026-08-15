package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

final class MinecraftFastItemCompat {
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private MinecraftFastItemCompat() {
    }

    static void pushModelView(Object modelView) {
        ((PoseStack) modelView).pushPose();
    }

    static void popModelView(Object modelView) {
        ((PoseStack) modelView).popPose();
    }

    static void resetModelView(Object modelView) {
        ((PoseStack) modelView).setIdentity();
    }

    static CompoundTag tagOrNull(ItemStack stack) {
        return stack.getTag();
    }

    static long relevantDataHash(ItemStack stack, CompoundTag tag) {
        return relevantNbtHash(tag);
    }

    private static long relevantNbtHash(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return 0L;
        long hash = FNV_OFFSET;
        hash = mixTag(hash, tag, "CustomModelData");
        hash = mixTag(hash, tag, "Potion");
        hash = mixTag(hash, tag, "CustomPotionColor");
        hash = mixTag(hash, tag, "CustomPotionEffects");
        if (tag.contains("display", Tag.TAG_COMPOUND)) {
            CompoundTag display = tag.getCompound("display");
            hash = mixTag(hash, display, "color");
        }
        return hash == FNV_OFFSET ? 0L : hash;
    }

    private static long mixTag(long hash, CompoundTag tag, String name) {
        if (tag == null || !tag.contains(name)) return hash;
        hash = mixString(hash, name);
        return mixString(hash, String.valueOf(tag.get(name)));
    }

    private static long mixString(long hash, String value) {
        if (value == null) return mixLong(hash, 0L);
        long mixed = hash;
        for (int i = 0; i < value.length(); i++) {
            mixed ^= value.charAt(i);
            mixed *= FNV_PRIME;
        }
        return mixed;
    }

    private static long mixLong(long hash, long value) {
        long mixed = hash;
        for (int i = 0; i < Long.BYTES; i++) {
            mixed ^= (value >>> (i * 8)) & 0xFFL;
            mixed *= FNV_PRIME;
        }
        return mixed;
    }
}
