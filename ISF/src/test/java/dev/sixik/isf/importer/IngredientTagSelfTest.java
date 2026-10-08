package dev.sixik.isf.importer;

import com.google.gson.JsonParser;

/**
 * Проверка извлечения тега одиночного tag-ингредиента (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfIngredientTagSelfTest}.</p>
 */
public final class IngredientTagSelfTest {
    public static void main(String[] args) {
        check("minecraft:planks".equals(VanillaRecipeTypeSupports.singleTag(
                JsonParser.parseString("{\"tag\":\"minecraft:planks\"}"))), "tag extracted");
        check(VanillaRecipeTypeSupports.singleTag(
                JsonParser.parseString("{\"item\":\"minecraft:stick\"}")) == null,
                "item has no tag");
        check(VanillaRecipeTypeSupports.singleTag(JsonParser.parseString(
                "[{\"tag\":\"minecraft:planks\"},{\"item\":\"minecraft:stick\"}]")) == null,
                "choice array has no single tag");
        check(VanillaRecipeTypeSupports.singleTag(
                JsonParser.parseString("{\"tag\":\"\"}")) == null, "blank tag ignored");
        check(VanillaRecipeTypeSupports.singleTag(
                JsonParser.parseString("{\"tag\":5}")) == null, "non-string tag ignored");
        check(VanillaRecipeTypeSupports.singleTag(null) == null, "null safe");
        System.out.println("IngredientTagSelfTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
