package dev.sixik.unigui.tests;

import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimPatterns;

/**
 * Создаёт предмет демо и вне мира, когда компоненты реестра ещё не привязаны.
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
        return stack(Items.DIAMOND_SWORD, "diamond_sword");
    }

    /**
     * Создаёт предмет для туториала о воронке.
     *
     * @return стек с моделью железного слитка.
     */
    static ItemStack ironIngot() {
        return stack(Items.IRON_INGOT, "iron_ingot");
    }

    /**
     * Создаёт стек, добавляя компонент модели вручную до привязки реестров клиентского мира.
     *
     * @param item ванильный предмет.
     * @param model путь ванильной модели предмета.
     * @return стек для отрисовки в сцене.
     */
    static ItemStack stack(Item item, String model) {
        if (MinecraftClientCompat.itemsReady()) return new ItemStack(item);
        return new ItemStack(Holder.direct(item, DataComponentMap.builder()
                .set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace(model)).build()));
    }

    /**
     * @param item vanilla item
     * @param model vanilla item model path for previews outside a world
     * @return demo stack displaying enchantment glint
     */
    static ItemStack foil(Item item, String model) {
        ItemStack stack = stack(item, model);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /**
     * @param preview entity whose client world supplies the armor trim registry
     * @return an iron helmet showing both the trim and enchantment glint
     */
    static ItemStack helmet(LivingEntity preview) {
        var registries = preview.level().registryAccess();
        ItemStack helmet = foil(Items.IRON_HELMET, "iron_helmet");
        helmet.set(DataComponents.TRIM, new ArmorTrim(
                registries.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.GOLD),
                registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(TrimPatterns.SENTRY)));
        return helmet;
    }
}
