package dev.sixik.unigui.widgets.minecraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Item data that the demo gallery builds differently on 1.20.1 (NBT) and 1.21.1 (data components).
 */
final class MinecraftDemoCompat {
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
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return sword;
    }
}
