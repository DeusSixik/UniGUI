package dev.sixik.isf.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

/**
 * Проверка чтения маркера тега ячейки ингредиентов (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfCellTagSelfTest}.</p>
 */
public final class IsfCellTagSelfTest {
    public static void main(String[] args) {
        JsonArray cell = new JsonArray();
        cell.add("#minecraft:planks");
        cell.add("minecraft:oak_planks");
        cell.add("minecraft:birch_planks#2");
        check("minecraft:planks".equals(IsfVisualWidgetFactory.cellTag(cell)),
                "marker extracted, count suffix untouched");

        JsonArray plain = new JsonArray();
        plain.add("minecraft:stick");
        check(IsfVisualWidgetFactory.cellTag(plain) == null, "no marker means no tag");
        check(IsfVisualWidgetFactory.cellTag(new JsonArray()) == null, "empty cell safe");
        check(IsfVisualWidgetFactory.cellTag(null) == null, "null safe");

        // Маркер — только запись с ведущим #: "id#count" тегом не считается.
        JsonArray counts = new JsonArray();
        counts.add("minecraft:iron_ingot#9");
        check(IsfVisualWidgetFactory.cellTag(counts) == null, "count suffix is not a marker");

        // Правило совпадения набора с тегом: точное равенство множеств.
        java.util.Map<String, java.util.Set<String>> index = new java.util.LinkedHashMap<>();
        index.put("minecraft:wool", set("minecraft:white_wool", "minecraft:red_wool"));
        index.put("minecraft:planks", set("minecraft:oak_planks", "minecraft:spruce_planks"));
        check("minecraft:wool".equals(IsfVisualWidgetFactory.matchTagIn(
                index, set("minecraft:red_wool", "minecraft:white_wool"))), "exact set matches");
        check(IsfVisualWidgetFactory.matchTagIn(
                index, set("minecraft:white_wool")) == null, "single item never matches");
        check(IsfVisualWidgetFactory.matchTagIn(
                index, set("minecraft:white_wool", "minecraft:stick")) == null,
                "partial overlap is not a tag");
        check(IsfVisualWidgetFactory.matchTagIn(
                index, set("minecraft:white_wool", "minecraft:red_wool", "minecraft:stick")) == null,
                "superset is not a tag");
        check(IsfVisualWidgetFactory.matchTagIn(null, set("a", "b")) == null, "null index safe");
        check(IsfVisualWidgetFactory.matchTagIn(index, null) == null, "null wanted safe");
        System.out.println("IsfCellTagSelfTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static java.util.Set<String> set(String... ids) {
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        for (String id : ids) result.add(id);
        return result;
    }
}
