package dev.sixik.isf.importer;

import dev.sixik.isf.api.IsfRecipeTypeSupport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Реестр подключаемых импортёров игровых recipe type. */
public final class IsfRecipeTypeSupportRegistry {
    private final Map<ResourceLocation, IsfRecipeTypeSupport> supports = new LinkedHashMap<>();

    public synchronized void register(IsfRecipeTypeSupport support) {
        IsfRecipeTypeSupport value = Objects.requireNonNull(support, "support");
        ResourceLocation id = Objects.requireNonNull(value.definition(), "support.definition()").id();
        if (supports.containsKey(id)) {
            throw new IllegalArgumentException("ISF recipe type support is already registered: " + id);
        }
        if (value.category() == null || value.category().isBlank()) {
            throw new IllegalArgumentException("ISF recipe type category cannot be empty: " + id);
        }
        supports.put(id, value);
    }

    public synchronized Optional<IsfRecipeTypeSupport> find(Recipe<?> recipe) {
        if (recipe == null) return Optional.empty();
        return supports.values().stream().filter(support -> support.matches(recipe)).findFirst();
    }

    public synchronized List<IsfRecipeTypeSupport> values() {
        return List.copyOf(supports.values());
    }
}
