package dev.sixik.isf.importer;

import dev.sixik.isf.api.IsfRecipeTypeSupport;
import net.minecraft.world.item.crafting.Recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Реестр подключаемых импортёров игровых recipe type. */
public final class IsfRecipeTypeSupportRegistry {
    /**
     * Ключ — category: несколько support'ов могут разделять один recipe type
     * (например, shaped и shapeless крафты живут в общем {@code isf:crafting}).
     */
    private final Map<String, IsfRecipeTypeSupport> supports = new LinkedHashMap<>();

    public synchronized void register(IsfRecipeTypeSupport support) {
        IsfRecipeTypeSupport value = Objects.requireNonNull(support, "support");
        if (value.category() == null || value.category().isBlank()) {
            throw new IllegalArgumentException("ISF recipe type support category cannot be empty");
        }
        Objects.requireNonNull(value.definition(), "support.definition()");
        String key = value.category();
        if (supports.containsKey(key)) {
            throw new IllegalArgumentException("ISF recipe type support is already registered: " + key);
        }
        supports.put(key, value);
    }

    public synchronized Optional<IsfRecipeTypeSupport> find(Recipe<?> recipe) {
        if (recipe == null) return Optional.empty();
        return supports.values().stream().filter(support -> support.matches(recipe)).findFirst();
    }

    public synchronized List<IsfRecipeTypeSupport> values() {
        return List.copyOf(supports.values());
    }
}
