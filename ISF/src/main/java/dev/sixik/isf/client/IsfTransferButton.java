package dev.sixik.isf.client;

import com.google.gson.JsonElement;
import dev.sixik.isf.IsfMod;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftTextureHandle;
import dev.sixik.unigui.widgets.interaction.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;

import java.util.List;

/**
 * Кнопка «+» переноса ингредиентов в сетку крафта (аналог JEI transfer).
 *
 * <p>Создаётся из ноды {@code isf:transfer_button} визуала типа (сейчас — только
 * крафт): поддержка тем самым объявлена при регистрации RecipeType, а логика
 * лежит рядом. Обычный клик — выкладка на 1 крафт, Shift+клик — на максимум.
 * Наведение подсвечивает красным клетки, которых не хватает.</p>
 */
final class IsfTransferButton extends Button {
    static final String KIND_CRAFTING_GRID = "crafting_grid";
    private static final int ICON_PIXELS = 8;

    private final String kind;
    private final JsonElement pattern;
    private List<IsfItemButton> cells = List.of();
    private Runnable onTransferred;

    IsfTransferButton(String kind, JsonElement pattern) {
        this.kind = kind == null || kind.isBlank() ? KIND_CRAFTING_GRID : kind;
        this.pattern = pattern;
        textPadding(0.0f, 0.0f);
        layout(style -> style.size(ICON_PIXELS, ICON_PIXELS).flexNone());
        refreshState();
        on(PointerEnteredEvent.TYPE, event -> showMissing(true));
        on(PointerExitedEvent.TYPE, event -> showMissing(false));
        onClick(event -> {
            if (transfer(Screen.hasShiftDown()) && onTransferred != null) onTransferred.run();
        });
    }

    /** Вызывается после выполненного переноса (окно рецептов закрывается). */
    void onTransferred(Runnable handler) {
        onTransferred = handler;
    }

    /** Клетки сетки ингредиентов в порядке строк (линкует фабрика после сборки). */
    void bindCells(List<IsfItemButton> ingredientCells) {
        cells = ingredientCells == null ? List.of() : List.copyOf(ingredientCells);
    }

    /** Переключает текстуру по поддержке текущего экрана. */
    void refreshState() {
        renderer(new NineSliceButtonRenderer(
                new MinecraftTextureHandle(supportedTexture(), ICON_PIXELS, ICON_PIXELS,
                        TextureOptions.nearest()),
                4.0f));
    }

    private ResourceLocation supportedTexture() {
        return ResourceLocation.tryBuild(IsfMod.MOD_ID, supportedMenu()
                ? "textures/jei/atlas/gui/enabled_plus_button.png"
                : "textures/jei/atlas/gui/disabled_plus_button.png");
    }

    private static boolean supportedMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && IsfCraftTransfer.isSupportedMenu(minecraft.screen);
    }

    private void showMissing(boolean visible) {
        if (!visible) {
            clearMissing();
            return;
        }
        if (!KIND_CRAFTING_GRID.equals(kind) || !supportedMenu()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !(minecraft.screen instanceof
                net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen)
                || !(containerScreen.getMenu() instanceof CraftingMenu menu)) {
            return;
        }
        IsfCraftTransfer.TransferPlan plan = IsfCraftTransfer.plan(
                IsfCraftTransfer.parsePattern(pattern),
                IsfCraftTransfer.countInventory(menu, 10, 46), false);
        // Клетки идут в том же построчном порядке, что и план (пустые пропущены
        // обеими сторонами); лишнее гасим.
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setTransferMissing(i < plan.missing().size() && plan.missing().get(i));
        }
    }

    private void clearMissing() {
        for (IsfItemButton cell : cells) cell.setTransferMissing(false);
    }

    private boolean transfer(boolean maxFill) {
        refreshState();
        if (!KIND_CRAFTING_GRID.equals(kind) || !supportedMenu()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !(minecraft.screen instanceof
                net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen)
                || !(containerScreen.getMenu() instanceof CraftingMenu menu)) {
            return false;
        }
        return IsfCraftTransfer.executeTransfer(minecraft, menu.containerId, pattern, maxFill);
    }
}
