package dev.sixik.isf.trigger;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.sixik.isf.definition.IsfDefinitionJson;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import dev.sixik.isf.definition.IsfRecipeTypeDefinition;
import dev.sixik.isf.definition.IsfTriggerDocument;
import dev.sixik.isf.runtime.IsfDefinitionRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Проверка привязок триггеров к рецептам (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfTriggerSelfTest}.</p>
 */
public final class IsfTriggerSelfTest {
    public static void main(String[] args) {
        // JSON-кодек туда-обратно.
        JsonObject json = new JsonObject();
        json.addProperty("trigger", "mymod:my_trigger");
        com.google.gson.JsonArray recipes = new com.google.gson.JsonArray();
        recipes.add("isf:recipe_a");
        recipes.add("isf:recipe_b");
        json.add("recipes", recipes);
        json.addProperty("subject", "minecraft:iron_sword");
        JsonObject conditions = new JsonObject();
        conditions.addProperty("key", "value");
        json.add("conditions", conditions);
        json.addProperty("open", true);
        IsfTriggerDocument parsed = IsfDefinitionJson.parseTriggerDocument(json);
        check(parsed.trigger().toString().equals("mymod:my_trigger"), "trigger id");
        check(parsed.recipes().size() == 2, "recipes list");
        check(parsed.subject().toString().equals("minecraft:iron_sword"), "subject");
        check(parsed.station() == null, "station absent");
        check(parsed.open(), "open flag");
        JsonObject rewritten = IsfDefinitionJson.writeTriggerDocument(parsed);
        IsfTriggerDocument reparsed = IsfDefinitionJson.parseTriggerDocument(rewritten);
        check(reparsed.equals(parsed), "round trip");

        // Минимум полей: только trigger.
        IsfTriggerDocument minimal = IsfDefinitionJson.parseTriggerDocument(
                docOf("mymod:ping", List.of()));
        check(minimal.recipes().isEmpty() && !minimal.open()
                        && minimal.subject() == null && minimal.conditions().isEmpty(),
                "minimal document");

        // Реестр: документы с неизвестными рецептами отбрасываются.
        IsfRecipeTypeDefinition type = new IsfRecipeTypeDefinition(
                id("isf:crafting"), null, Map.of(), List.of(), null, null);
        IsfRecipeDefinition recipeA = new IsfRecipeDefinition(
                id("isf:recipe_a"), id("isf:crafting"), null,
                Map.of(), List.of(), null, null);
        IsfDefinitionRegistry registry = new IsfDefinitionRegistry();
        registry.replace(List.of(type), List.of(recipeA), List.of(
                new IsfTriggerDocument(id("mymod:my_trigger"), List.of(id("isf:recipe_a")), null,
                        null, Map.of(), true),
                new IsfTriggerDocument(id("mymod:ghost"), List.of(id("isf:nope")), null,
                        null, Map.of(), true)));
        check(registry.triggerDocuments().size() == 1, "unknown recipe drops document");
        check(registry.triggerDocuments().get(0).trigger().toString().equals("mymod:my_trigger"),
                "known document kept");

        // Матчинг документов: id/ subject/station/conditions.
        IsfTriggerDocument doc = new IsfTriggerDocument(id("mymod:my_trigger"),
                List.of(id("isf:recipe_a")), id("minecraft:iron_sword"), null,
                Map.of("key", new JsonPrimitive("value")), true);
        check(IsfTriggerEngine.matchesDocument(doc, id("minecraft:iron_sword"), null,
                Map.of("key", new JsonPrimitive("value"), "extra", new JsonPrimitive(1))), "full match");
        check(!IsfTriggerEngine.matchesDocument(doc, id("minecraft:stick"), null,
                Map.of("key", new JsonPrimitive("value"))), "subject mismatch");
        check(!IsfTriggerEngine.matchesDocument(doc, id("minecraft:iron_sword"), null,
                Map.of("key", new JsonPrimitive("other"))), "condition mismatch");
        check(!IsfTriggerEngine.matchesDocument(doc, id("minecraft:iron_sword"), null, Map.of()),
                "missing condition");
        check(!IsfTriggerEngine.matchesDocument(null, id("minecraft:iron_sword"), null, Map.of()),
                "null document");
        // Без фильтров — всегда совпадает.
        IsfTriggerDocument open = new IsfTriggerDocument(id("mymod:open"), List.of(id("isf:recipe_a")),
                null, null, Map.of(), false);
        check(IsfTriggerEngine.matchesDocument(open, null, null, null), "open document matches all");

        // Параметры типов/рецептов в реестре не пострадали.
        check(registry.recipe(id("isf:recipe_a")).isPresent(), "recipe still resolved");

        System.out.println("IsfTriggerSelfTest passed");
    }

    private static JsonObject docOf(String trigger, List<String> recipes) {
        JsonObject json = new JsonObject();
        json.addProperty("trigger", trigger);
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        recipes.forEach(array::add);
        json.add("recipes", array);
        return json;
    }

    private static ResourceLocation id(String value) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) throw new AssertionError("Bad test id: " + value);
        return parsed;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
