package dev.sixik.isf.client;

import com.google.gson.JsonPrimitive;

/**
 * Проверка клиентского применения NBT ячеек дропа (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfLootNbtClientSelfTest}.</p>
 */
public final class LootNbtClientSelfTest {
    public static void main(String[] args) {
        check(IsfVisualWidgetFactory.readCellTag(
                        new JsonPrimitive("{Potion:\"minecraft:healing\"}")) != null,
                "readCellTag parses valid snbt");
        check(IsfVisualWidgetFactory.readCellTag(
                        new JsonPrimitive("{broken:::}")) == null,
                "readCellTag rejects broken snbt");
        check(IsfVisualWidgetFactory.readCellTag(null) == null, "readCellTag null-safe");
        check(IsfVisualWidgetFactory.readCellTag(new JsonPrimitive(5)) == null,
                "readCellTag non-string safe");
        System.out.println("LootNbtClientSelfTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
