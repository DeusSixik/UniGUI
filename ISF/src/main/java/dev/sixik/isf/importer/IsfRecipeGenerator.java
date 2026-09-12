package dev.sixik.isf.importer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.sixik.isf.IsfMod;
import dev.sixik.isf.api.IsfRecipeTypeSupport;
import dev.sixik.isf.definition.IsfDefinitionJson;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import dev.sixik.isf.definition.IsfSourceReference;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;

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
    private final IsfRecipeTypeSupportRegistry supports;

    public IsfRecipeGenerator() {
        this(IsfMod.runtime().recipeTypes());
    }

    public IsfRecipeGenerator(IsfRecipeTypeSupportRegistry supports) {
        this.supports = java.util.Objects.requireNonNull(supports, "supports");
    }

    public Result generate(MinecraftServer server, IsfImportRequest request) {
        if (server == null) throw new IllegalArgumentException("Server cannot be null");
        IsfImportRequest safeRequest = request == null ? IsfImportRequest.parse("") : request;
        Path packRoot = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR)
                .resolve("isf_generated")
                .toAbsolutePath()
                .normalize();
        Path output = packRoot.resolve("data").resolve("isf").resolve("isf")
                .resolve("recipes").resolve("generated");
        int generated = 0;
        int generatedTypes = 0;
        Set<String> unsupported = new LinkedHashSet<>();
        try {
            Files.createDirectories(output);
            writePackMetadata(packRoot);
            RegistryAccess access = server.registryAccess();

            java.util.Set<ResourceLocation> writtenTypes = new LinkedHashSet<>();
            for (IsfRecipeTypeSupport support : supports.values()) {
                if (!safeRequest.accepts(support.category())) continue;
                // Несколько support'ов могут разделять один recipe type — файл пишем один раз.
                if (!writtenTypes.add(support.definition().id())) continue;
                Path target = definitionPath(packRoot, "recipe_types", support.definition().id());
                writeJson(target, IsfDefinitionJson.writeType(support.definition()));
                generatedTypes++;
            }

            for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
                ResourceLocation recipeId = recipe.getId();
                IsfRecipeTypeSupport support = supports.find(recipe).orElse(null);
                if (support == null) {
                    String category = unsupportedCategory(recipe);
                    if (safeRequest.accepts(category)) unsupported.add(category);
                    continue;
                }
                if (!safeRequest.accepts(support.category())) continue;
                IsfRecipeDefinition definition = toDefinition(recipeId, recipe, access, support);
                Path target = output.resolve(recipeId.getNamespace())
                        .resolve(recipeId.getPath() + ".json").normalize();
                if (!target.startsWith(output)) {
                    throw new IllegalStateException("Recipe id escapes ISF output directory: " + recipeId);
                }
                writeJson(target, IsfDefinitionJson.writeRecipe(definition));
                generated++;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate ISF recipes in " + output, exception);
        }
        return new Result(generated, generatedTypes, output, List.copyOf(unsupported));
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

    private static IsfRecipeDefinition toDefinition(ResourceLocation id,
                                                    Recipe<?> recipe,
                                                    RegistryAccess access,
                                                    IsfRecipeTypeSupport support) {
        Map<String, com.google.gson.JsonElement> extracted = support.extractParameters(recipe, access);
        Map<String, com.google.gson.JsonElement> parameters = new java.util.LinkedHashMap<>(
                extracted == null ? Map.of() : extracted);
        return new IsfRecipeDefinition(ResourceLocation.tryBuild("isf",
                "generated/" + id.getNamespace() + "/" + id.getPath()),
                support.definition().id(), null, parameters,
                support.createTriggers(recipe, access, Map.copyOf(parameters)), null,
                new IsfSourceReference(support.category(), id));
    }

    private static Path definitionPath(Path packRoot, String directory, ResourceLocation id) {
        Path root = packRoot.resolve("data").resolve(id.getNamespace()).resolve("isf").resolve(directory)
                .toAbsolutePath().normalize();
        Path target = root.resolve(id.getPath() + ".json").normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Definition id escapes ISF output directory: " + id);
        }
        return target;
    }

    private static void writeJson(Path target, JsonObject json) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target,
                new GsonBuilder().setPrettyPrinting().create().toJson(json),
                StandardCharsets.UTF_8);
    }

    private static String unsupportedCategory(Recipe<?> recipe) {
        if (recipe.getType() == net.minecraft.world.item.crafting.RecipeType.CRAFTING
                && recipe.isSpecial()) {
            return "crafting_special";
        }
        ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        return type == null ? "unknown" : type.toString();
    }

    public record Result(int generated,
                         int generatedTypes,
                         Path outputDirectory,
                         List<String> unsupportedCategories) {
    }

}
