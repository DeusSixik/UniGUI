package dev.sixik.isf.command;

import java.util.List;
import java.util.Set;

/**
 * Проверка автодополнения селекторов generateRecipes (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfCommandsSuggestSelfTest}.</p>
 */
public final class IsfCommandsSuggestSelfTest {
    private static final Set<String> CATEGORIES = Set.of("crafting_shaped", "loot");

    public static void main(String[] args) {
        // Пустой ввод: все категории + отрицания.
        check(IsfCommands.suggestCategoryTokens("", CATEGORIES).equals(List.of(
                        "crafting_shaped", "-crafting_shaped", "loot", "-loot")),
                "empty input suggests all");

        // Префикс добивает текущий токен.
        check(IsfCommands.suggestCategoryTokens("cr", CATEGORIES).equals(List.of("crafting_shaped")),
                "prefix completes token");
        check(IsfCommands.suggestCategoryTokens("LO", CATEGORIES).equals(List.of("loot")),
                "case-insensitive");

        // Минус — только отрицания.
        check(IsfCommands.suggestCategoryTokens("-", CATEGORIES).equals(List.of("-crafting_shaped", "-loot")),
                "dash suggests negations");

        // После запятой подсказываем хвост, уже введённое прячем (кроме отрицания).
        check(IsfCommands.suggestCategoryTokens("crafting_shaped,", CATEGORIES).equals(List.of(
                        "crafting_shaped,-crafting_shaped",
                        "crafting_shaped,loot", "crafting_shaped,-loot")),
                "after comma with head");
        check(IsfCommands.suggestCategoryTokens("crafting_shaped,lo", CATEGORIES)
                        .equals(List.of("crafting_shaped,loot")),
                "token after comma");
        check(IsfCommands.suggestCategoryTokens("crafting_shaped,-", CATEGORIES).equals(List.of(
                        "crafting_shaped,-crafting_shaped", "crafting_shaped,-loot")),
                "negation after comma");

        // Полностью введённый токен больше не подсказывается (дальше запятая/enter).
        check(IsfCommands.suggestCategoryTokens("crafting_shaped", CATEGORIES).isEmpty(),
                "completed token needs no suggestion");
        check(IsfCommands.suggestCategoryTokens("-loot", CATEGORIES).isEmpty(),
                "completed negation needs no suggestion");

        // Null и мусор безопасны.
        check(IsfCommands.suggestCategoryTokens(null, CATEGORIES).equals(List.of(
                        "crafting_shaped", "-crafting_shaped", "loot", "-loot")),
                "null input");
        check(IsfCommands.suggestCategoryTokens("", null).isEmpty(), "null categories");
        check(IsfCommands.suggestCategoryTokens("", Set.of("  ", "loot")).equals(List.of("loot", "-loot")),
                "blank categories skipped");

        System.out.println("IsfCommandsSuggestSelfTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
