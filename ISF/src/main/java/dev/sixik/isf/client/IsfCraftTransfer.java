package dev.sixik.isf.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Перенос ингредиентов рецепта в сетку крафта (аналог JEI transfer).
 *
 * <p>Поддерживается только верстак 3x3 ({@link CraftingMenu}): в других меню кнопка
 * неактивна. Совпадение предметов — по id (без NBT), как слоты меню.</p>
 *
 * <ul>
 *   <li>Обычный клик — выкладка на 1 крафт.</li>
 *   <li>Shift+клик — выкладка на максимум крафтов (по наличию и по 64/ячейка).</li>
 *   <li>Наведение — красная заливка клеток, которых не хватает на 1 крафт.</li>
 * </ul>
 */
final class IsfCraftTransfer {
    /** Слоты CraftingMenu: 0 результат, 1-9 сетка, 10-45 инвентарь игрока. */
    private static final int RESULT_SLOT = 0;
    private static final int GRID_START = 1;
    private static final int GRID_STRIDE = 3;
    private static final int PLAYER_START = 10;
    private static final int PLAYER_END = 46;
    private static final int MAX_STACK = 64;

    private IsfCraftTransfer() {
    }

    /** Альтернатива клетки: id предмета и нужное количество. */
    record Alternative(String id, int count) {
    }

    /** Клетка рецепта с альтернативами (пустые клетки отсутствуют в списке). */
    record CellPlan(int row, int column, List<Alternative> alternatives) {
    }

    /** Выбранная альтернатива клетки для выкладки. */
    record ResolvedCell(int row, int column, String itemId, int need) {
    }

    /** Готовый план: выбранные предметы, флаги нехватки, число крафтов. */
    record TransferPlan(List<ResolvedCell> cells, List<Boolean> missing, int crafts) {
    }

    /**
     * Разбирает pattern 3x3 (строки → клетки → записи {@code "id#count"}).
     * Пустые клетки пропускаются.
     */
    static List<CellPlan> parsePattern(JsonElement pattern) {
        List<CellPlan> cells = new ArrayList<>();
        if (pattern == null || !pattern.isJsonArray()) return List.copyOf(cells);
        int row = 0;
        for (JsonElement rowElement : pattern.getAsJsonArray()) {
            if (!rowElement.isJsonArray()) {
                row++;
                continue;
            }
            int column = 0;
            for (JsonElement cellElement : rowElement.getAsJsonArray()) {
                if (cellElement.isJsonArray() && !cellElement.getAsJsonArray().isEmpty()) {
                    List<Alternative> alternatives = new ArrayList<>();
                    for (JsonElement option : cellElement.getAsJsonArray()) {
                        Alternative alternative = parseAlternative(option);
                        if (alternative != null) alternatives.add(alternative);
                    }
                    if (!alternatives.isEmpty() && row < GRID_STRIDE && column < GRID_STRIDE) {
                        cells.add(new CellPlan(row, column, List.copyOf(alternatives)));
                    }
                }
                column++;
            }
            row++;
        }
        return List.copyOf(cells);
    }

    private static Alternative parseAlternative(JsonElement option) {
        if (option == null || !option.isJsonPrimitive()) return null;
        String raw = option.getAsString();
        int hash = raw.lastIndexOf('#');
        String id = hash < 0 ? raw : raw.substring(0, hash);
        if (id.isBlank() || ResourceLocation.tryParse(id) == null) return null;
        int count = 1;
        if (hash >= 0) {
            try {
                count = Math.max(1, Integer.parseInt(raw.substring(hash + 1).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return new Alternative(id, count);
    }

    /**
     * Строит план выкладки.
     *
     * @param cells клетки рецепта
     * @param available количество каждого id в инвентаре игрока
     * @param maxFill {@code true} — на максимум крафтов, иначе на один
     * @return план; {@code crafts == 0} — не хватает хотя бы на один крафт
     */
    static TransferPlan plan(List<CellPlan> cells, Map<String, Integer> available, boolean maxFill) {
        List<ResolvedCell> resolved = new ArrayList<>();
        List<Boolean> missing = new ArrayList<>();
        for (CellPlan cell : cells) {
            Alternative chosen = chooseAlternative(cell.alternatives(), available);
            int have = available.getOrDefault(chosen.id(), 0);
            resolved.add(new ResolvedCell(cell.row(), cell.column(), chosen.id(), chosen.count()));
            missing.add(have < chosen.count());
        }
        int crafts;
        if (missing.contains(Boolean.TRUE)) {
            crafts = 0;
        } else if (!maxFill || resolved.isEmpty()) {
            crafts = resolved.isEmpty() ? 0 : 1;
        } else {
            crafts = maxCrafts(resolved, available);
        }
        return new TransferPlan(List.copyOf(resolved), List.copyOf(missing), crafts);
    }

    /**
     * Максимум крафтов: минимум по клеткам, по суммарному расходу общих предметов
     * (один id в нескольких клетках) и по вместимости ячеек (64).
     */
    private static int maxCrafts(List<ResolvedCell> cells, Map<String, Integer> available) {
        int crafts = Integer.MAX_VALUE;
        int maxNeed = 1;
        Map<String, Integer> totalNeed = new LinkedHashMap<>();
        for (ResolvedCell cell : cells) {
            maxNeed = Math.max(maxNeed, cell.need());
            totalNeed.merge(cell.itemId(), cell.need(), Integer::sum);
            int have = available.getOrDefault(cell.itemId(), 0);
            crafts = Math.min(crafts, have / cell.need());
        }
        for (Map.Entry<String, Integer> need : totalNeed.entrySet()) {
            int have = available.getOrDefault(need.getKey(), 0);
            crafts = Math.min(crafts, have / Math.max(1, need.getValue()));
        }
        crafts = Math.min(crafts, MAX_STACK / Math.max(1, maxNeed));
        return Math.max(0, crafts);
    }

    /** Первая имеющаяся альтернатива, иначе первая по списку. */
    private static Alternative chooseAlternative(List<Alternative> alternatives,
                                                 Map<String, Integer> available) {
        for (Alternative alternative : alternatives) {
            if (available.getOrDefault(alternative.id(), 0) > 0) return alternative;
        }
        return alternatives.get(0);
    }

    /** Поддерживается ли перенос в открытом экране (верстак 3x3). */
    static boolean isSupportedMenu(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> container
                && container.getMenu() instanceof CraftingMenu;
    }

    /** Количество каждого id в слотах меню (обычно инвентарь игрока). */
    static Map<String, Integer> countInventory(CraftingMenu menu, int fromSlot, int toSlot) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int slot = Math.max(0, fromSlot); slot < Math.min(menu.slots.size(), toSlot); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) continue;
            counts.merge(id.toString(), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    /**
     * Выполняет перенос: забирает результат и сетку квик-мувом, считает план заново
     * и выкладывает. Вызывать в клиентском потоке при открытом меню.
     *
     * @param containerId id меню на момент клика (устаревшее — прерываем)
     * @return {@code true}, если перенос выполнен (окно можно закрывать)
     */
    static boolean executeTransfer(Minecraft minecraft, int containerId, JsonElement pattern,
                                   boolean maxFill) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return false;
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) return false;
        if (!(containerScreen.getMenu() instanceof CraftingMenu menu)) return false;
        if (menu.containerId != containerId) return false;
        Player player = minecraft.player;
        // С занятым курсором математика пикапа ломается — игрок сначала кладёт предмет.
        if (!menu.getCarried().isEmpty()) return false;
        // Сначала проверяем, что хоть один крафт возможен, — инвентарь не трогаем.
        TransferPlan probe = plan(parsePattern(pattern),
                countInventory(menu, PLAYER_START, PLAYER_END), maxFill);
        if (probe.crafts() < 1) return false;

        // Освобождаем результат и сетку квик-мувом, затем считаем по факту.
        if (!menu.getSlot(RESULT_SLOT).getItem().isEmpty()) {
            click(minecraft, menu, RESULT_SLOT, 0, ClickType.QUICK_MOVE, player);
        }
        for (int slot = GRID_START; slot < GRID_START + GRID_STRIDE * GRID_STRIDE; slot++) {
            if (!menu.getSlot(slot).getItem().isEmpty()) {
                click(minecraft, menu, slot, 0, ClickType.QUICK_MOVE, player);
            }
        }

        TransferPlan plan = plan(parsePattern(pattern),
                countInventory(menu, PLAYER_START, PLAYER_END), maxFill);
        if (plan.crafts() < 1) return false;

        for (ResolvedCell cell : plan.cells()) {
            int gridSlot = GRID_START + cell.row() * GRID_STRIDE + cell.column();
            int need = cell.need() * plan.crafts();
            for (int slot = PLAYER_START; slot < PLAYER_END && need > 0; slot++) {
                ItemStack stack = menu.getSlot(slot).getItem();
                if (stack.isEmpty() || !stackId(stack).equals(cell.itemId())) continue;
                int carried = stack.getCount();
                click(minecraft, menu, slot, 0, ClickType.PICKUP, player);
                int place = Math.min(need, carried);
                for (int i = 0; i < place; i++) {
                    click(minecraft, menu, gridSlot, 1, ClickType.PICKUP, player);
                }
                need -= place;
                int rest = carried - place;
                if (rest > 0) {
                    // Возвращаем остаток взятого стака обратно.
                    click(minecraft, menu, slot, 0, ClickType.PICKUP, player);
                }
            }
        }
        return true;
    }

    private static String stackId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static void click(Minecraft minecraft, CraftingMenu menu, int slot, int button,
                              ClickType type, Player player) {
        try {
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, type, player);
        } catch (RuntimeException ignored) {
            // Устаревшее меню/лаг: сервер отклоняет, клиент догонит сам.
        }
    }
}
