package dev.sixik.isf.runtime;

import com.google.gson.JsonElement;
import dev.sixik.isf.definition.IsfParameterDefinition;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import dev.sixik.isf.definition.IsfRecipeTypeDefinition;
import dev.sixik.isf.definition.IsfTriggerBinding;
import dev.sixik.isf.definition.IsfVisualNode;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Реестр исходных и разрешённых ISF-документов. */
public final class IsfDefinitionRegistry {
    private final Map<ResourceLocation, IsfRecipeTypeDefinition> types = new LinkedHashMap<>();
    private final Map<ResourceLocation, IsfRecipeDefinition> recipes = new LinkedHashMap<>();
    private final Map<ResourceLocation, IsfResolvedRecipe> resolved = new LinkedHashMap<>();

    public synchronized void replace(Collection<IsfRecipeTypeDefinition> newTypes,
                                     Collection<IsfRecipeDefinition> newRecipes) {
        Map<ResourceLocation, IsfRecipeTypeDefinition> previousTypes = new LinkedHashMap<>(types);
        Map<ResourceLocation, IsfRecipeDefinition> previousRecipes = new LinkedHashMap<>(recipes);
        Map<ResourceLocation, IsfResolvedRecipe> previousResolved = new LinkedHashMap<>(resolved);
        types.clear();
        recipes.clear();
        resolved.clear();
        if (newTypes != null) newTypes.forEach(type -> types.put(type.id(), type));
        if (newRecipes != null) newRecipes.forEach(recipe -> recipes.put(recipe.id(), recipe));
        try {
            for (ResourceLocation id : recipes.keySet()) resolveRecipe(id, new ArrayDeque<>());
        } catch (RuntimeException exception) {
            types.clear();
            types.putAll(previousTypes);
            recipes.clear();
            recipes.putAll(previousRecipes);
            resolved.clear();
            resolved.putAll(previousResolved);
            throw exception;
        }
    }

    public synchronized Optional<IsfResolvedRecipe> recipe(ResourceLocation id) {
        return Optional.ofNullable(resolved.get(id));
    }

    public synchronized List<IsfResolvedRecipe> recipes() {
        return List.copyOf(resolved.values());
    }

    public synchronized List<String> validate() {
        List<String> errors = new ArrayList<>();
        for (ResourceLocation id : recipes.keySet()) {
            try {
                resolveRecipe(id, new ArrayDeque<>());
            } catch (IllegalStateException exception) {
                errors.add(exception.getMessage());
            }
        }
        return List.copyOf(errors);
    }

    private IsfResolvedRecipe resolveRecipe(ResourceLocation id, ArrayDeque<ResourceLocation> stack) {
        IsfResolvedRecipe cached = resolved.get(id);
        if (cached != null) return cached;
        IsfRecipeDefinition recipe = recipes.get(id);
        if (recipe == null) throw new IllegalStateException("Unknown ISF recipe: " + id);
        if (stack.contains(id)) throw cycle("recipe", stack, id);
        stack.addLast(id);

        ResolvedType type = resolveType(recipe.recipeType(), new ArrayDeque<>());
        Map<String, JsonElement> values = defaults(type.parameters());
        List<IsfTriggerBinding> triggers = new ArrayList<>();
        IsfVisualNode visual = type.visual();

        if (recipe.parentRecipe() != null) {
            IsfResolvedRecipe parent = resolveRecipe(recipe.parentRecipe(), stack);
            values.putAll(copyJsonMap(parent.parameters()));
            triggers.addAll(parent.triggers());
            visual = IsfVisualMerger.merge(parent.visual(), visual);
        }

        values.putAll(copyJsonMap(recipe.parameters()));
        triggers.addAll(recipe.triggers());
        visual = IsfVisualMerger.merge(visual, recipe.visual());
        validateParameters(recipe.id(), type.parameters(), values);

        IsfResolvedRecipe result = new IsfResolvedRecipe(recipe.id(), recipe.recipeType(),
                Map.copyOf(type.parameters()), Map.copyOf(values), distinctTriggers(triggers),
                visual, recipe.source());
        resolved.put(id, result);
        stack.removeLast();
        return result;
    }

    private ResolvedType resolveType(ResourceLocation id, ArrayDeque<ResourceLocation> stack) {
        IsfRecipeTypeDefinition type = types.get(id);
        if (type == null) throw new IllegalStateException("Unknown ISF recipe type: " + id);
        if (stack.contains(id)) throw cycle("recipe type", stack, id);
        stack.addLast(id);

        Map<String, IsfParameterDefinition> parameters = new LinkedHashMap<>();
        IsfVisualNode visual = null;
        if (type.parent() != null) {
            ResolvedType parent = resolveType(type.parent(), stack);
            parameters.putAll(parent.parameters());
            visual = parent.visual();
        }
        parameters.putAll(type.parameters());
        visual = IsfVisualMerger.merge(visual, type.visual());
        stack.removeLast();
        return new ResolvedType(parameters, visual);
    }

    private static void validateParameters(ResourceLocation recipeId,
                                           Map<String, IsfParameterDefinition> schema,
                                           Map<String, JsonElement> values) {
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            IsfParameterDefinition definition = schema.get(entry.getKey());
            if (definition == null) {
                throw new IllegalStateException("Recipe " + recipeId + " uses unknown parameter '" + entry.getKey() + "'");
            }
            if (!IsfValueValidator.matches(definition.type(), entry.getValue())) {
                throw new IllegalStateException("Recipe " + recipeId + " has invalid value for parameter '"
                        + entry.getKey() + "' (expected " + definition.type() + ")");
            }
        }
    }

    private static Map<String, JsonElement> defaults(Map<String, IsfParameterDefinition> schema) {
        Map<String, JsonElement> values = new LinkedHashMap<>();
        schema.forEach((name, definition) -> values.put(name, definition.defaultValue().deepCopy()));
        return values;
    }

    private static Map<String, JsonElement> copyJsonMap(Map<String, JsonElement> source) {
        Map<String, JsonElement> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, value.deepCopy()));
        return copy;
    }

    private static List<IsfTriggerBinding> distinctTriggers(List<IsfTriggerBinding> triggers) {
        Set<IsfTriggerBinding> distinct = new LinkedHashSet<>(triggers);
        return List.copyOf(distinct);
    }

    private static IllegalStateException cycle(String kind, ArrayDeque<ResourceLocation> stack, ResourceLocation id) {
        return new IllegalStateException("Cyclic ISF " + kind + " inheritance: " + stack + " -> " + id);
    }

    private record ResolvedType(Map<String, IsfParameterDefinition> parameters, IsfVisualNode visual) {
    }
}
