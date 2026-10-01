package dev.sixik.isf.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashSet;
import java.util.Set;

/**
 * Проверка сбора id предметов из параметров рецептов (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfCatalogVisibilitySelfTest}.</p>
 */
public final class IsfCatalogVisibilitySelfTest {
    public static void main(String[] args) {
        // Вложенные объекты и массивы обходятся рекурсивно.
        JsonObject root = new JsonObject();
        root.addProperty("result", "minecraft:iron_sword");
        JsonArray ingredients = new JsonArray();
        JsonArray cell = new JsonArray();
        cell.add("minecraft:iron_ingot#9");
        cell.add("minecraft:stick");
        ingredients.add(cell);
        root.add("ingredients", ingredients);
        JsonObject nested = new JsonObject();
        nested.addProperty("table_id", "minecraft:entities/zombie");
        root.add("meta", nested);
        Set<String> ids = new HashSet<>();
        IsfBrowserOverlay.collectMentionedIds(root, ids);
        check(ids.contains("minecraft:iron_sword"), "result collected");
        check(ids.contains("minecraft:iron_ingot#9"), "count suffix kept raw");
        check(ids.contains("minecraft:stick"), "nested array collected");
        check(ids.contains("minecraft:entities/zombie"), "nested object collected");

        // Null-безопасность и нестроковые примитивы.
        IsfBrowserOverlay.collectMentionedIds(null, ids);
        IsfBrowserOverlay.collectMentionedIds(new JsonPrimitive(5), ids);
        IsfBrowserOverlay.collectMentionedIds(com.google.gson.JsonNull.INSTANCE, ids);
        check(ids.contains("5"), "numbers collected verbatim");
        int size = ids.size();
        IsfBrowserOverlay.collectMentionedIds(root, null);
        check(ids.size() == size, "null out safe");

        System.out.println("IsfCatalogVisibilitySelfTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
