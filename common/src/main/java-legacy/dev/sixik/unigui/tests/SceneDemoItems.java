package dev.sixik.unigui.tests;

import net.minecraft.world.item.ItemStack;
import dev.sixik.unigui.widgets.minecraft.MinecraftDemoCompat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;

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

    /**
     * Создаёт предмет для туториала о воронке.
     *
     * @return стек с моделью железного слитка.
     */
    static ItemStack ironIngot() {
        return new ItemStack(Items.IRON_INGOT);
    }

    /**
     * Создаёт стек для предпросмотра, модель разрешается по предмету.
     *
     * @param item ванильный предмет.
     * @param model путь модели для совместимости с загрузкой в 26.x.
     * @return стек для отрисовки в сцене.
     */
    static ItemStack stack(Item item, String model) {
        return new ItemStack(item);
    }

    /**
     * @param item vanilla item
     * @param model vanilla item model path
     * @return demo stack
     */
    static ItemStack foil(Item item, String model) {
        return MinecraftDemoCompat.glint(stack(item, model));
    }

    /**
     * @param preview entity that wears the demo helmet
     * @return iron helmet
     */
    static ItemStack helmet(LivingEntity preview) {
        return new ItemStack(Items.IRON_HELMET);
    }
}
