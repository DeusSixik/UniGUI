package dev.sixik.isf.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Проверка планирования переноса в сетку крафта (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfCraftTransferSelfTest}.</p>
 */
public final class IsfCraftTransferSelfTest {
    public static void main(String[] args) {
        // Пустой pattern — пустой план.
        check(IsfCraftTransfer.parsePattern(null).isEmpty(), "null pattern");
        check(IsfCraftTransfer.parsePattern(new JsonArray()).isEmpty(), "empty pattern");

        // Один меч: 3 палки/доски, всё есть — 1 крафт, без нехватки.
        JsonArray sword = pattern(
                row(cell("minecraft:oak_planks"), empty(), empty()),
                row(cell("minecraft:oak_planks"), empty(), empty()),
                row(cell("minecraft:stick"), empty(), empty()));
        Map<String, Integer> full = Map.of("minecraft:oak_planks", 10, "minecraft:stick", 10);
        IsfCraftTransfer.TransferPlan single =
                IsfCraftTransfer.plan(IsfCraftTransfer.parsePattern(sword), full, false);
        check(single.crafts() == 1, "single craft");
        check(single.missing().stream().noneMatch(missing -> missing), "nothing missing");
        check(single.cells().size() == 3, "three cells");

        // Нет палок — крафта нет, флаг на клетке палки.
        IsfCraftTransfer.TransferPlan missing = IsfCraftTransfer.plan(
                IsfCraftTransfer.parsePattern(sword), Map.of("minecraft:oak_planks", 10), false);
        check(missing.crafts() == 0, "no craft when missing");
        check(missing.missing().equals(List.of(false, false, true)), "stick cell missing");

        // Альтернативы: берётся первая имеющаяся, иначе первая + нехватка.
        JsonArray pick = pattern(row(cell("minecraft:stone#1", "minecraft:cobblestone#1")));
        IsfCraftTransfer.TransferPlan alt = IsfCraftTransfer.plan(
                IsfCraftTransfer.parsePattern(pick), Map.of("minecraft:cobblestone", 5), false);
        check(alt.cells().get(0).itemId().equals("minecraft:cobblestone"), "available alternative chosen");
        IsfCraftTransfer.TransferPlan altMissing = IsfCraftTransfer.plan(
                IsfCraftTransfer.parsePattern(pick), Map.of(), false);
        check(altMissing.cells().get(0).itemId().equals("minecraft:stone")
                        && altMissing.missing().get(0),
                "first alternative fallback with missing");

        // Shift: общий предмет на 9 клеток (64 камня → 7 крафтов, не 64).
        JsonArray full3x3 = pattern(
                row(cell("minecraft:stone"), cell("minecraft:stone"), cell("minecraft:stone")),
                row(cell("minecraft:stone"), cell("minecraft:stone"), cell("minecraft:stone")),
                row(cell("minecraft:stone"), cell("minecraft:stone"), cell("minecraft:stone")));
        IsfCraftTransfer.TransferPlan maxed = IsfCraftTransfer.plan(
                IsfCraftTransfer.parsePattern(full3x3), Map.of("minecraft:stone", 64), true);
        check(maxed.crafts() == 7, "shared item caps crafts, got " + maxed.crafts());

        // Лимит ячейки 64: need 1 → не больше 64 крафтов.
        IsfCraftTransfer.TransferPlan capped = IsfCraftTransfer.plan(
                IsfCraftTransfer.parsePattern(full3x3), Map.of("minecraft:stone", 1000), true);
        check(capped.crafts() == 64, "slot cap 64");

        // Количество из #count: need 4 → 8 штук хватает на 2 крафта.
        JsonArray torches = pattern(row(cell("minecraft:coal#4")));
        IsfCraftTransfer.TransferPlan two = IsfCraftTransfer.plan(
                IsfCraftTransfer.parsePattern(torches), Map.of("minecraft:coal", 8), true);
        check(two.crafts() == 2 && two.cells().get(0).need() == 4, "count suffix respected");

        // Битые записи игнорируются.
        check(IsfCraftTransfer.parsePattern(new JsonObject()).isEmpty(), "non-array pattern ignored");
        JsonArray badRow = new JsonArray();
        badRow.add(new JsonObject());
        JsonArray badPattern = new JsonArray();
        badPattern.add(badRow);
        check(IsfCraftTransfer.parsePattern(badPattern).isEmpty(), "non-array cells ignored");

        System.out.println("IsfCraftTransferSelfTest passed");
    }

    private static JsonArray pattern(JsonArray... rows) {
        JsonArray array = new JsonArray();
        for (JsonArray row : rows) array.add(row);
        return array;
    }

    private static JsonArray row(JsonArray... cells) {
        JsonArray row = new JsonArray();
        for (JsonArray cell : cells) row.add(cell);
        return row;
    }

    private static JsonArray cell(String... options) {
        JsonArray cell = new JsonArray();
        for (String option : options) cell.add(option);
        return cell;
    }

    private static JsonArray empty() {
        return new JsonArray();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
