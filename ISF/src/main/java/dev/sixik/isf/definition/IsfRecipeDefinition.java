package dev.sixik.isf.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Независимый ISF-рецепт. Он ничего не регистрирует в RecipeManager Minecraft. */
public record IsfRecipeDefinition(
        ResourceLocation id,
        ResourceLocation recipeType,
        ResourceLocation parentRecipe,
        Map<String, JsonElement> parameters,
        List<IsfTriggerBinding> triggers,
        IsfVisualNode visual,
        IsfSourceReference source
) {
    public IsfRecipeDefinition {
        if (id == null) throw new IllegalArgumentException("Recipe id cannot be null");
        if (recipeType == null) throw new IllegalArgumentException("Recipe type id cannot be null");
        parameters = copyJsonMap(parameters);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
    }

    /**
     * @return id выходного предмета из параметра {@code "result"}
     * или {@code null}, если параметра нет/он не предмет.
     */
    public ResourceLocation resultItemId() {
        JsonElement result = parameters.get("result");
        if (result == null || !result.isJsonPrimitive()) return null;
        try {
            return ResourceLocation.tryParse(result.getAsString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Map<String, JsonElement> copyJsonMap(Map<String, JsonElement> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<String, JsonElement> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) copy.put(key, value == null ? JsonNull.INSTANCE : value.deepCopy());
        });
        return Map.copyOf(copy);
    }
}
