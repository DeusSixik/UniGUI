package dev.sixik.isf.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Привязка триггера к конкретным рецептам по их id.
 *
 * <p>В отличие от {@link IsfTriggerBinding} (живёт внутри рецепта), документ лежит
 * отдельным JSON-файлом и позволяет стороннему моду/админу открыть нужный рецепт
 * по нужному действию, не трогая сам рецепт. Файлы:
 * {@code data/<ns>/isf/trigger_bindings/<name>.json}.</p>
 */
public record IsfTriggerDocument(
        ResourceLocation trigger,
        List<ResourceLocation> recipes,
        ResourceLocation subject,
        ResourceLocation station,
        Map<String, JsonElement> conditions,
        boolean open
) {
    public IsfTriggerDocument {
        if (trigger == null) throw new IllegalArgumentException("Trigger id cannot be null");
        List<ResourceLocation> safeRecipes = new ArrayList<>();
        if (recipes != null) {
            recipes.forEach(id -> {
                if (id != null && !safeRecipes.contains(id)) safeRecipes.add(id);
            });
        }
        recipes = List.copyOf(safeRecipes);
        conditions = conditions == null ? Map.of() : copyJsonMap(conditions);
    }

    private static Map<String, JsonElement> copyJsonMap(Map<String, JsonElement> source) {
        Map<String, JsonElement> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) copy.put(key, value == null ? JsonNull.INSTANCE : value.deepCopy());
        });
        return Map.copyOf(copy);
    }
}
