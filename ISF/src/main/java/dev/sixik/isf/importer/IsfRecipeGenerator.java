package dev.sixik.isf.importer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import dev.sixik.isf.definition.IsfDefinitionJson;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import dev.sixik.isf.definition.IsfSourceReference;
import dev.sixik.isf.definition.IsfTriggerBinding;
import dev.sixik.isf.definition.IsfVisualNode;
import dev.sixik.isf.trigger.IsfTriggerRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Импортирует поддерживаемые Minecraft recipes в независимые ISF JSON-документы. */
public final class IsfRecipeGenerator {
    public Result generate(MinecraftServer server, IsfImportRequest request) {
        if (server == null) throw new IllegalArgumentException("Server cannot be null");
        IsfImportRequest safeRequest = request == null ? IsfImportRequest.parse("") : request;
        Path packRoot = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR)
                .resolve("isf_generated");
        Path output = packRoot.resolve("data").resolve("isf").resolve("isf")
                .resolve("recipes").resolve("generated");
        int generated = 0;
        Set<String> unsupported = new LinkedHashSet<>();
        try {
            Files.createDirectories(output);
            writePackMetadata(packRoot);
            RegistryAccess access = server.registryAccess();
            for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
                ResourceLocation recipeId = recipe.getId();
                String category = category(recipe);
                if (!safeRequest.accepts(category)) continue;
                if (!isSupported(recipe)) {
                    unsupported.add(category);
                    continue;
                }
                IsfRecipeDefinition definition = toDefinition(recipeId, recipe, access);
                Path target = output.resolve(recipeId.getNamespace())
                        .resolve(recipeId.getPath() + ".json").normalize();
                if (!target.startsWith(output)) {
                    throw new IllegalStateException("Recipe id escapes ISF output directory: " + recipeId);
                }
                Files.createDirectories(target.getParent());
                Files.writeString(target,
                        new GsonBuilder().setPrettyPrinting().create().toJson(IsfDefinitionJson.writeRecipe(definition)),
                        StandardCharsets.UTF_8);
                generated++;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate ISF recipes in " + output, exception);
        }
        return new Result(generated, output, List.copyOf(unsupported));
    }

    private static void writePackMetadata(Path packRoot) throws IOException {
        Path metadata = packRoot.resolve("pack.mcmeta");
        if (Files.exists(metadata)) return;
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 15);
        pack.addProperty("description", "Generated independent ISF visual recipes");
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        Files.createDirectories(packRoot);
        Files.writeString(metadata,
                new GsonBuilder().setPrettyPrinting().create().toJson(root),
                StandardCharsets.UTF_8);
    }

    private static IsfRecipeDefinition toDefinition(ResourceLocation id, Recipe<?> recipe, RegistryAccess access) {
        net.minecraft.world.item.ItemStack resultStack = recipe.getResultItem(access);
        ResourceLocation result = BuiltInRegistries.ITEM.getKey(resultStack.getItem());
        String category = category(recipe);
        Map<String, com.google.gson.JsonElement> parameters = new java.util.LinkedHashMap<>();
        parameters.put("result", new JsonPrimitive(result.toString()));
        parameters.put("result_count", new JsonPrimitive(resultStack.getCount()));
        parameters.put("ingredients", ingredients(recipe));
        if (recipe instanceof ShapedRecipe shaped) {
            parameters.put("width", new JsonPrimitive(shaped.getWidth()));
            parameters.put("height", new JsonPrimitive(shaped.getHeight()));
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            parameters.put("cooking_time", new JsonPrimitive(cooking.getCookingTime()));
            parameters.put("experience", new JsonPrimitive(cooking.getExperience()));
        }
        IsfTriggerBinding trigger = new IsfTriggerBinding(IsfTriggerRegistry.CRAFT, result, null, Map.of());
        return new IsfRecipeDefinition(ResourceLocation.tryBuild("isf",
                "generated/" + id.getNamespace() + "/" + id.getPath()),
                ResourceLocation.tryBuild("isf", category), null, parameters, List.of(trigger), null,
                new IsfSourceReference(category, id));
    }

    private static JsonArray ingredients(Recipe<?> recipe) {
        JsonArray ingredients = new JsonArray();
        for (Ingredient ingredient : recipe.getIngredients()) {
            JsonArray alternatives = new JsonArray();
            for (net.minecraft.world.item.ItemStack stack : ingredient.getItems()) {
                alternatives.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
            ingredients.add(alternatives);
        }
        return ingredients;
    }

    private static boolean isSupported(Recipe<?> recipe) {
        return recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe || recipe instanceof AbstractCookingRecipe;
    }

    private static String category(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe) return "crafting_shaped";
        if (recipe instanceof ShapelessRecipe) return "crafting_shapeless";
        if (recipe instanceof AbstractCookingRecipe cooking) {
            RecipeType<?> type = cooking.getType();
            if (type == RecipeType.SMELTING) return "smelting";
            if (type == RecipeType.BLASTING) return "blasting";
            if (type == RecipeType.SMOKING) return "smoking";
            if (type == RecipeType.CAMPFIRE_COOKING) return "campfire_cooking";
        }
        return "unsupported";
    }

    public record Result(int generated, Path outputDirectory, List<String> unsupportedCategories) {
    }

}
