package dev.sixik.isf.client;

import dev.sixik.isf.definition.IsfCatalystDefinition;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

/** Последний подтверждённый сервером снимок библиотеки локального игрока. */
public final class IsfClientState {
    private static volatile Set<ResourceLocation> unlocked = Set.of();
    private static volatile Set<ResourceLocation> bookmarks = Set.of();
    private static volatile Map<ResourceLocation, ResourceLocation> recipeResults = Map.of();
    private static volatile Map<ResourceLocation, IsfRecipeDefinition> recipes = Map.of();
    private static volatile Map<ResourceLocation, List<IsfCatalystDefinition>> typeCatalysts = Map.of();
    private static volatile Map<ResourceLocation, ResourceLocation> typeIcons = Map.of();
    private static volatile long version;

    private static long chunkTransferId;
    private static int chunkTotal;
    private static final Map<Integer, List<IsfRecipeDefinition>> chunkBuffer = new java.util.LinkedHashMap<>();

    private IsfClientState() {
    }

    /** Накапливает части LibraryChunk; вызывается на клиенте. */
    public static synchronized void acceptRecipeChunk(long transferId, int index, int total,
                                                      List<IsfRecipeDefinition> recipes) {
        if (transferId != chunkTransferId) {
            chunkTransferId = transferId;
            chunkBuffer.clear();
        }
        chunkTotal = Math.max(1, total);
        if (recipes != null && !recipes.isEmpty()) {
            chunkBuffer.put(index, List.copyOf(recipes));
        } else {
            chunkBuffer.putIfAbsent(index, List.of());
        }
    }

    /** Собирает все части передачи; возвращает пустой список, если часть потеряна. */
    private static synchronized List<IsfRecipeDefinition> drainRecipeChunks(long transferId) {
        if (transferId != chunkTransferId) return List.of();
        List<IsfRecipeDefinition> all = new ArrayList<>();
        for (int index = 0; index < chunkTotal; index++) {
            List<IsfRecipeDefinition> batch = chunkBuffer.get(index);
            if (batch == null) return List.of();
            all.addAll(batch);
        }
        chunkBuffer.clear();
        return all;
    }

    /** Финал передачи библиотеки: склеивает накопленные чанки и применяет состояние. */
    public static void applyLibrarySnapshot(long transferId,
                                            Set<ResourceLocation> unlockedRecipes,
                                            Set<ResourceLocation> bookmarkedItems,
                                            Map<ResourceLocation, ResourceLocation> resultByRecipe,
                                            Map<ResourceLocation, List<IsfCatalystDefinition>> catalystsByType,
                                            Map<ResourceLocation, ResourceLocation> iconsByType) {
        replace(unlockedRecipes, bookmarkedItems, resultByRecipe,
                drainRecipeChunks(transferId), catalystsByType, iconsByType);
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

    /** Катализаторы recipe type: блоки/предметы, на которых выполняется крафт. */
    public static Map<ResourceLocation, List<IsfCatalystDefinition>> typeCatalysts() {
        return typeCatalysts;
    }

    /** Явные иконки вкладок recipe type; без иконки вкладка берёт первый катализатор. */
    public static Map<ResourceLocation, ResourceLocation> typeIcons() {
        return typeIcons;
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
        replace(unlockedRecipes, bookmarkedItems, resultByRecipe, recipeDocuments, Map.of(), Map.of());
    }

    public static void replace(Set<ResourceLocation> unlockedRecipes,
                               Set<ResourceLocation> bookmarkedItems,
                               Map<ResourceLocation, ResourceLocation> resultByRecipe,
                               List<IsfRecipeDefinition> recipeDocuments,
                               Map<ResourceLocation, List<IsfCatalystDefinition>> catalystsByType) {
        replace(unlockedRecipes, bookmarkedItems, resultByRecipe, recipeDocuments, catalystsByType, Map.of());
    }

    public static void replace(Set<ResourceLocation> unlockedRecipes,
                               Set<ResourceLocation> bookmarkedItems,
                               Map<ResourceLocation, ResourceLocation> resultByRecipe,
                               List<IsfRecipeDefinition> recipeDocuments,
                               Map<ResourceLocation, List<IsfCatalystDefinition>> catalystsByType,
                               Map<ResourceLocation, ResourceLocation> iconsByType) {
        unlocked = Set.copyOf(unlockedRecipes == null ? Set.of() : unlockedRecipes);
        bookmarks = Set.copyOf(bookmarkedItems == null ? Set.of() : bookmarkedItems);
        recipeResults = Map.copyOf(resultByRecipe == null ? Map.of() : resultByRecipe);
        java.util.LinkedHashMap<ResourceLocation, IsfRecipeDefinition> indexed = new java.util.LinkedHashMap<>();
        if (recipeDocuments != null) {
            recipeDocuments.forEach(recipe -> indexed.put(recipe.id(), recipe));
        }
        recipes = Map.copyOf(indexed);
        Map<ResourceLocation, List<IsfCatalystDefinition>> safeCatalysts = new LinkedHashMap<>();
        if (catalystsByType != null) {
            catalystsByType.forEach((id, values) -> {
                if (id != null && values != null && !values.isEmpty()) {
                    safeCatalysts.put(id, List.copyOf(values));
                }
            });
        }
        typeCatalysts = Map.copyOf(safeCatalysts);
        Map<ResourceLocation, ResourceLocation> safeIcons = new LinkedHashMap<>();
        if (iconsByType != null) {
            iconsByType.forEach((id, icon) -> {
                if (id != null && icon != null) safeIcons.put(id, icon);
            });
        }
        typeIcons = Map.copyOf(safeIcons);
        version++;
    }

    public static void clear() {
        replace(Set.of(), Set.of());
    }
}
