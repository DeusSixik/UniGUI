package dev.sixik.unigui.tests;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Создаёт ванильный стек для демонстрации старых версий.
 */
final class SceneDemoItems {
    private SceneDemoItems() {
    }

    /**
     * Создаёт тестовый предмет сцены.
     *
     * @return стек с моделью алмазного меча.
     */
    static ItemStack sword() {
        return new ItemStack(Items.DIAMOND_SWORD);
    }
}
