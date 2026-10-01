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
        System.out.println("LootTitleSelfTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
