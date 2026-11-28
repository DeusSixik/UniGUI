package dev.sixik.unigui.widgets.minecraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Item data that the demo gallery builds differently on 1.20.1 (NBT) and 1.21.1 (data components).
 */
public final class MinecraftDemoCompat {
    private MinecraftDemoCompat() {
    }

    static ItemStack healingPotion() {
        return PotionContents.createItemStack(Items.POTION, Potions.HEALING);
    }

    /**
     * Enchantments are data-driven since 1.21 and need a loaded world to resolve; the glint override gives the same
     * animated picture without one, which is what the sample is meant to show.
     */
    static ItemStack enchantedSword() {
        return glint(new ItemStack(Items.DIAMOND_SWORD));
    }

    /**
     * Enables vanilla foil on a stack owned by a demo.
     *
     * @param stack the demo stack to update
     * @return the same stack with foil
     */
    public static ItemStack glint(ItemStack stack) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }
}
