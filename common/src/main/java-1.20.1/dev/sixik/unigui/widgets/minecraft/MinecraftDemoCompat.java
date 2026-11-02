package dev.sixik.unigui.widgets.minecraft;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Item data that the demo gallery builds differently on 1.20.1 (NBT) and 1.21.1 (data components).
 */
final class MinecraftDemoCompat {
    private MinecraftDemoCompat() {
    }

    static ItemStack healingPotion() {
        return PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
    }

    static ItemStack enchantedSword() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.SHARPNESS, 3);
        return sword;
    }
}
