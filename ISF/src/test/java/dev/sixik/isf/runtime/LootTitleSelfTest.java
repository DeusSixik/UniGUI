package dev.sixik.isf.runtime;

/**
 * Проверка заголовка лут-таблиц по умолчанию (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfLootTitleSelfTest}.</p>
 */
public final class LootTitleSelfTest {
    public static void main(String[] args) {
        check(IsfFunctionRegistry.prettifyLastSegment("path/my_table").equals("My Table"),
                "last segment prettified");
        check(IsfFunctionRegistry.prettifyLastSegment("entities/zombie").equals("Zombie"),
                "entity name");
        check(IsfFunctionRegistry.prettifyLastSegment("blocks/nether_fortress/chest").equals("Chest"),
                "deep path takes last");
        check(IsfFunctionRegistry.prettifyLastSegment("single").equals("Single"), "no slashes");
        check(IsfFunctionRegistry.prettifyLastSegment("").isEmpty(), "empty safe");
        check(IsfFunctionRegistry.prettifyLastSegment(null).isEmpty(), "null safe");

        // Маршрутизация заголовка: резолверы-пустышки (реестры вне бутстрапа
        // недоступны), локализация — эхо (ключа нет).
        check(IsfFunctionRegistry.lootTitle("mymod:blocks/gizmo_farm", key -> key, id -> null, id -> null)
                .equals("Gizmo Farm"), "block table falls back to segment");
        check(IsfFunctionRegistry.lootTitle("mymod:entities/grue", key -> key, id -> null, id -> null)
                .equals("Grue"), "entity table falls back to segment");
        check(IsfFunctionRegistry.lootTitle("mymod:chests/dungeon", key -> key, id -> null, id -> null)
                .equals("Dungeon"), "chest table falls back to segment");
        // Имя блока/сущности побеждает сегмент пути.
        check(IsfFunctionRegistry.lootTitle("mymod:blocks/deep/dark/gizmo", key -> key,
                        id -> "Gizmo Block", id -> null)
                .equals("Gizmo Block"), "block source name wins");
        check(IsfFunctionRegistry.lootTitle("mymod:entities/grue", key -> key,
                        id -> null, id -> "Grue Monster")
                .equals("Grue Monster"), "entity name wins");
        // Явная локализация побеждает любой фолбэк.
        check(IsfFunctionRegistry.lootTitle("mymod:blocks/gizmo_farm", key -> "Custom",
                        id -> "Gizmo Block", id -> null)
                .equals("Custom"), "explicit lang key wins");
        check(IsfFunctionRegistry.lootTitle(null, key -> key, id -> null, id -> null)
                .isEmpty(), "null table safe");
        check(IsfFunctionRegistry.lootTitle("??", key -> key, id -> null, id -> null)
                .equals("??"), "broken id verbatim");
        System.out.println("LootTitleSelfTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
