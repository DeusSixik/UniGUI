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
import java.util.List;
import java.util.Map;

/** Загружает recipe types и recipes из datapack-каталога {@code isf}. */
public final class IsfDefinitionReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(IsfDefinitionReloadListener.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String TYPE_PREFIX = "recipe_types/";
    private static final String RECIPE_PREFIX = "recipes/";

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
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation resource = entry.getKey();
            String path = resource.getPath();
            if (path.startsWith(TYPE_PREFIX)) {
                types.add(IsfDefinitionJson.parseType(definitionId(resource, TYPE_PREFIX),
                        entry.getValue().getAsJsonObject()));
            } else if (path.startsWith(RECIPE_PREFIX)) {
                recipes.add(IsfDefinitionJson.parseRecipe(definitionId(resource, RECIPE_PREFIX),
                        entry.getValue().getAsJsonObject()));
            }
        }
        registry.replace(types, recipes);
        LOGGER.info("Loaded {} ISF recipe types and {} visual recipes", types.size(), recipes.size());
    }

    private static ResourceLocation definitionId(ResourceLocation resource, String prefix) {
        return ResourceLocation.tryBuild(resource.getNamespace(), resource.getPath().substring(prefix.length()));
    }
}
