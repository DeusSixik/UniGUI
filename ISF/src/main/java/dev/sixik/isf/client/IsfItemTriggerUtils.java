package dev.sixik.isf.client;

import dev.sixik.isf.network.IsfNetwork;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Утилита R/U/A-триггеров: единая точка разрешения предмета под курсором
 * и выполнения действия над ним.
 *
 * <p>Предмет ищется во всех источниках ISF и ванили — визуалы рецептов,
 * катализаторы окна рецептов ({@code catalystColumn}), клетки каталога и
 * закладок ({@code browserPanel}/{@code bookmarkPanel}) и, как последний
 * фолбэк, слот инвентаря под курсором. Любой новый «источник предметов»
 * достаточно добавить в {@link IsfBrowserOverlay#triggerItemAt}.</p>
 */
public final class IsfItemTriggerUtils {
    /** Действие, выполняемое над предметом под курсором. */
    public enum TriggerKind {
        /** R — рецепты, в которых предмет является результатом. */
        CRAFTING,
        /** U — применения предмета (рецепты, где он в ингредиентах). */
        USAGES,
        /** A — переключить закладку. */
        TOGGLE_BOOKMARK
    }

    private IsfItemTriggerUtils() {
    }

    /**
     * Предмет под курсором: источники overlay'а, затем ванильный слот инвентаря.
     *
     * @param overlay   активный browser overlay
     * @param container экран с инвентарём
     * @param mouseX    курсор X в GUI-координатах
     * @param mouseY    курсор Y в GUI-координатах
     * @return id предмета или {@code null}, если под курсором ничего нет
     */
    public static ResourceLocation resolveItemAt(IsfBrowserOverlay overlay,
                                                 AbstractContainerScreen<?> container,
                                                 double mouseX, double mouseY) {
        ResourceLocation itemId = overlay.triggerItemAt(mouseX, mouseY);
        if (itemId != null) return itemId;
        Slot slot = container == null ? null : container.getSlotUnderMouse();
        ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    /**
     * Выполняет триггер над предметом.
     *
     * @param overlay активный browser overlay
     * @param kind    тип действия (R/U/A)
     * @param itemId  предмет, над которым выполняется действие; {@code null} — no-op
     * @return {@code true}, если действие выполнено (событие стоит отменить)
     */
    public static boolean trigger(IsfBrowserOverlay overlay, TriggerKind kind, ResourceLocation itemId) {
        if (overlay == null || itemId == null || kind == null) return false;
        return switch (kind) {
            case CRAFTING -> {
                overlay.showRecipes(itemId, false);
                yield true;
            }
            case USAGES -> {
                overlay.showRecipes(itemId, true);
                yield true;
            }
            case TOGGLE_BOOKMARK -> toggleBookmark(itemId);
        };
    }

    /** Переключает закладку предмета через серверный канал. Воздух (барьер AIR-дропа) игнорируется. */
    public static boolean toggleBookmark(ResourceLocation itemId) {
        if (itemId == null) return false;
        try {
            if (BuiltInRegistries.ITEM.get(itemId) == net.minecraft.world.item.Items.AIR) return false;
        } catch (RuntimeException ignored) {
            return false;
        }
        boolean bookmarked = !IsfClientState.bookmarks().contains(itemId);
        IsfNetwork.toggleBookmark(itemId, bookmarked);
        return true;
    }
}
