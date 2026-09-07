package dev.sixik.isf.client;

import dev.sixik.isf.definition.IsfRecipeDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.List;
import java.util.Set;

/** Последний подтверждённый сервером снимок библиотеки локального игрока. */
public final class IsfClientState {
    private static volatile Set<ResourceLocation> unlocked = Set.of();
    private static volatile Set<ResourceLocation> bookmarks = Set.of();
    private static volatile Map<ResourceLocation, ResourceLocation> recipeResults = Map.of();
    private static volatile Map<ResourceLocation, IsfRecipeDefinition> recipes = Map.of();
    private static volatile long version;

    private IsfClientState() {
    }

    public static Set<ResourceLocation> unlocked() {
        return unlocked;
    }

    public static Set<ResourceLocation> bookmarks() {
        return bookmarks;
    }

    public static Map<ResourceLocation, ResourceLocation> recipeResults() {
        return recipeResults;
    }

    public static Map<ResourceLocation, IsfRecipeDefinition> recipes() {
        return recipes;
    }

    public static long version() {
        return version;
    }

    /** Совместимый overload для интеграций, которым не нужна метаинформация рецептов. */
    public static void replace(Set<ResourceLocation> unlockedRecipes, Set<ResourceLocation> bookmarkedItems) {
        replace(unlockedRecipes, bookmarkedItems, Map.of());
    }

    public static void replace(Set<ResourceLocation> unlockedRecipes,
                               Set<ResourceLocation> bookmarkedItems,
                               Map<ResourceLocation, ResourceLocation> resultByRecipe) {
        replace(unlockedRecipes, bookmarkedItems, resultByRecipe, List.of());
    }

    public static void replace(Set<ResourceLocation> unlockedRecipes,
                               Set<ResourceLocation> bookmarkedItems,
                               Map<ResourceLocation, ResourceLocation> resultByRecipe,
                               List<IsfRecipeDefinition> recipeDocuments) {
        unlocked = Set.copyOf(unlockedRecipes == null ? Set.of() : unlockedRecipes);
        bookmarks = Set.copyOf(bookmarkedItems == null ? Set.of() : bookmarkedItems);
        recipeResults = Map.copyOf(resultByRecipe == null ? Map.of() : resultByRecipe);
        java.util.LinkedHashMap<ResourceLocation, IsfRecipeDefinition> indexed = new java.util.LinkedHashMap<>();
        if (recipeDocuments != null) {
            recipeDocuments.forEach(recipe -> indexed.put(recipe.id(), recipe));
        }
        recipes = Map.copyOf(indexed);
        version++;
    }

    public static void clear() {
        replace(Set.of(), Set.of());
    }
}
