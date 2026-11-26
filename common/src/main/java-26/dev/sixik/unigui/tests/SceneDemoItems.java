package dev.sixik.unigui.tests;

import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        if (MinecraftClientCompat.itemsReady()) return new ItemStack(Items.DIAMOND_SWORD);
        return new ItemStack(Holder.direct(Items.DIAMOND_SWORD, DataComponentMap.builder()
                .set(DataComponents.ITEM_MODEL, Identifier.withDefaultNamespace("diamond_sword")).build()));
    }
}
