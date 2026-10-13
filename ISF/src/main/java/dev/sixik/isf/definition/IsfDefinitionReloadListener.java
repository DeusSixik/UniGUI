package dev.sixik.isf.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.sixik.isf.runtime.IsfDefinitionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Загружает recipe types и recipes из datapack-каталога {@code isf}. */
public final class IsfDefinitionReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IsfDefinitionReloadListener.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String TYPE_PREFIX = "recipe_types/";
    private static final String RECIPE_PREFIX = "recipes/";
    private static final String TRIGGER_PREFIX = "trigger_bindings/";

    private final IsfDefinitionRegistry registry;

    public IsfDefinitionReloadListener(IsfDefinitionRegistry registry) {
        super(GSON, "isf");
        this.registry = registry;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<IsfRecipeTypeDefinition> types = new ArrayList<>();
        List<IsfRecipeDefinition> recipes = new ArrayList<>();
        List<IsfTriggerDocument> triggerDocuments = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation resource = entry.getKey();
            String path = resource.getPath();
            if (path.startsWith(TYPE_PREFIX)) {
                types.add(IsfDefinitionJson.parseType(definitionId(resource, TYPE_PREFIX),
                        entry.getValue().getAsJsonObject()));
            } else if (path.startsWith(RECIPE_PREFIX)) {
                recipes.add(IsfDefinitionJson.parseRecipe(definitionId(resource, RECIPE_PREFIX),
                        entry.getValue().getAsJsonObject()));
            } else if (path.startsWith(TRIGGER_PREFIX)) {
                triggerDocuments.add(IsfDefinitionJson.parseTriggerDocument(
                        entry.getValue().getAsJsonObject()));
            }
        }
        // Группировка по выходному предмету: рецепты с одним результатом
        // (например, все рецепты железного слитка) идут в реестре, а значит
        // и в синхронизированной библиотеке/окне рецептов, друг за другом.
        // Сортировка стабильная: рецепты без "result" сохраняют свой порядок в конце.
        recipes.sort(Comparator
                .comparing(IsfRecipeDefinition::resultItemId,
                        java.util.Comparator.nullsLast(java.util.Comparator.comparing(ResourceLocation::toString)))
                .thenComparing(recipe -> recipe.id().toString()));
        // Порядок типов детерминирован (по id): от него зависит порядок вкладок
        // в окне рецептов, а порядок обхода ресурсов датапаков не гарантирован.
        types.sort(Comparator.comparing(type -> type.id().toString()));
        // Loot tables живут ТОЛЬКО в файлах (см. /isf generateRecipes loot):
        // автогенерации в памяти нет — ненужное вырезается удалением файла.
        // Таблицы из loot_disabled.json выкидываются, даже если файл рецепта есть
        // (команда /isf loot disable).
        java.util.Set<ResourceLocation> disabledTables = new java.util.LinkedHashSet<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            if (entry.getKey().getPath().equals("isf/loot_disabled.json")) {
                disabledTables.addAll(
                        dev.sixik.isf.importer.LootTableSupports.parseDisabledTables(entry.getValue()));
            }
        }
        if (!disabledTables.isEmpty()) {
            recipes.removeIf(recipe -> {
                ResourceLocation tableId =
                        dev.sixik.isf.importer.LootTableSupports.lootTableId(recipe);
                return tableId != null && disabledTables.contains(tableId);
            });
        }
        registry.replace(types, recipes, triggerDocuments);
        LOGGER.info("Loaded {} ISF recipe types, {} visual recipes and {} trigger bindings",
                types.size(), recipes.size(), triggerDocuments.size());
    }

    private static ResourceLocation definitionId(ResourceLocation resource, String prefix) {
        return ResourceLocation.tryBuild(resource.getNamespace(), resource.getPath().substring(prefix.length()));
    }
}
