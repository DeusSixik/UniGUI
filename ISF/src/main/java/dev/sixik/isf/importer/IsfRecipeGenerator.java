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
import net.minecraft.world.level.storage.LevelResource;

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
        Path packRoot = generatedPackRoot(server);
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

            // Сначала собираем и сортируем по выходному предмету: рецепты с одним
            // результатом (например, железный слиток) пишутся файлами подряд.
            List<IsfRecipeDefinition> definitions = new ArrayList<>();
            for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
                ResourceLocation recipeId = recipe.getId();
                IsfRecipeTypeSupport support = supports.find(recipe).orElse(null);
                if (support == null) {
                    String category = unsupportedCategory(recipe);
                    if (safeRequest.accepts(category)) unsupported.add(category);
                    continue;
                }
                if (!safeRequest.accepts(support.category())) continue;
                definitions.add(toDefinition(recipeId, recipe, access, support));
            }
            definitions.sort(java.util.Comparator
                    .comparing(IsfRecipeDefinition::resultItemId,
                            java.util.Comparator.nullsLast(java.util.Comparator.comparing(ResourceLocation::toString)))
                    .thenComparing(recipe -> recipe.id().toString()));
            for (IsfRecipeDefinition definition : definitions) {
                ResourceLocation source = definition.source() == null ? null : definition.source().sourceId();
                if (source == null || definition.recipeType() == null) continue;
                Path target = generatedRecipePath(output, definition.recipeType(), source);
                writeJson(target, IsfDefinitionJson.writeRecipe(definition));
                generated++;
            }
            if (safeRequest.accepts("loot")) {
                Path typeTarget = definitionPath(packRoot, "recipe_types",
                        LootTableSupports.LOOT_TYPE_ID);
                // Существующие файлы не трогаем: админ мог их править; обновление —
                // удалить файл и сдампить заново. Отключённые не дампим вообще.
                if (java.nio.file.Files.notExists(typeTarget)) {
                    writeJson(typeTarget, IsfDefinitionJson.writeType(LootTableSupports.lootTypeDefinition()));
                    generatedTypes++;
                }
                generated += writeLootRecipes(packRoot, server);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate ISF recipes in " + output, exception);
        }
        return new Result(generated, generatedTypes, output, List.copyOf(unsupported));
    }

    /** Корень генерируемого пакета {@code datapacks/isf_generated}. */
    public static Path generatedPackRoot(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("Server cannot be null");
        return server.getWorldPath(LevelResource.DATAPACK_DIR)
                .resolve("isf_generated")
                .toAbsolutePath()
                .normalize();
    }

    /** Файл отключённых лут-таблиц: {@code data/isf/isf/loot_disabled.json} пакета. */
    public static Path lootDisabledFile(MinecraftServer server) {
        return generatedPackRoot(server).resolve("data").resolve("isf").resolve("isf")
                .resolve("loot_disabled.json");
    }

    /**
     * Дампит лут-таблицы в JSON-файлы (категория {@code loot}).
     * Путь файла зеркалит id рецепта ({@code recipes/loot/<ns>/<path>.json}),
     * релоад грузит только файлы — автогенерации в памяти нет.
     * Существующие файлы не перезаписываются (правки админа), отключённые через
     * {@code loot_disabled.json} таблицы пропускаются.
     *
     * @return число записанных рецептов
     */
    private static int writeLootRecipes(Path packRoot, MinecraftServer server) throws IOException {
        Path lootRoot = packRoot.resolve("data").resolve("isf").resolve("isf")
                .resolve("recipes").resolve("loot").toAbsolutePath().normalize();
        java.util.Set<ResourceLocation> disabled =
                LootTableSupports.loadDisabledTables(server.getResourceManager());
        List<IsfRecipeDefinition> loot =
                new ArrayList<>(LootTableSupports.generate(server.getResourceManager()));
        loot.sort(java.util.Comparator.comparing(recipe -> recipe.id().toString()));
        int count = 0;
        for (IsfRecipeDefinition definition : loot) {
            ResourceLocation tableId = LootTableSupports.lootTableId(definition);
            if (tableId == null || disabled.contains(tableId)) continue;
            Path target = lootRoot.resolve(tableId.getNamespace())
                    .resolve(tableId.getPath() + ".json").normalize();
            if (!target.startsWith(lootRoot)) {
                throw new IllegalStateException("Loot table id escapes ISF output directory: " + tableId);
            }
            if (java.nio.file.Files.exists(target)) continue;
            writeJson(target, IsfDefinitionJson.writeRecipe(definition));
            count++;
        }
        return count;
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

    /**
     * Путь сгенерированного рецепта: {@code <root>/<typeNs>/<typePath>/<srcNs>/<srcPath>.json}.
     * Раскладка по типам — только для удобства разработчиков: при парсинге папки
     * не важны (id берётся из пути целиком, тип — из содержимого JSON).
     */
    static Path generatedRecipePath(Path outputRoot, ResourceLocation typeId, ResourceLocation sourceId) {
        java.util.Objects.requireNonNull(outputRoot, "outputRoot");
        java.util.Objects.requireNonNull(typeId, "typeId");
        java.util.Objects.requireNonNull(sourceId, "sourceId");
        Path root = outputRoot.toAbsolutePath().normalize();
        Path target = root.resolve(typeId.getNamespace()).resolve(typeId.getPath())
                .resolve(sourceId.getNamespace()).resolve(sourceId.getPath() + ".json").normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Recipe id escapes ISF output directory: " + sourceId);
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
