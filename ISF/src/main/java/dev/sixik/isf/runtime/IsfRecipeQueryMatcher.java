package dev.sixik.isf.runtime;

import com.google.gson.JsonElement;
import dev.sixik.isf.definition.IsfTriggerBinding;
import dev.sixik.isf.trigger.IsfTriggerRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** Сопоставляет запросы R/U с доступными визуальными рецептами. */
public final class IsfRecipeQueryMatcher {
    private IsfRecipeQueryMatcher() {
    }

    public static boolean matches(Map<String, JsonElement> parameters,
                                  List<IsfTriggerBinding> triggers,
                                  ResourceLocation itemId,
                                  boolean usages) {
        if (itemId == null) return false;
        List<IsfTriggerBinding> safeTriggers = triggers == null ? List.of() : triggers;
        boolean hasQueryBindings = safeTriggers.stream().anyMatch(binding -> usages
                ? binding.trigger().equals(IsfTriggerRegistry.USE)
                        || binding.trigger().equals(IsfTriggerRegistry.STATION)
                : binding.trigger().equals(IsfTriggerRegistry.CRAFT));
        if (hasQueryBindings) {
            return safeTriggers.stream().anyMatch(binding -> binding.conditions().isEmpty() && (usages
                    ? binding.trigger().equals(IsfTriggerRegistry.USE) && itemId.equals(binding.subject())
                            || binding.trigger().equals(IsfTriggerRegistry.STATION) && itemId.equals(binding.station())
                    : binding.trigger().equals(IsfTriggerRegistry.CRAFT) && itemId.equals(binding.subject())));
        }

        // Старые generated JSON не содержали use/station binding.
        Map<String, JsonElement> safeParameters = parameters == null ? Map.of() : parameters;
        if (!usages) return itemId.equals(readItemId(safeParameters.get("result")));
        JsonElement ingredients = safeParameters.get("ingredients");
        if (ingredients == null || !ingredients.isJsonArray()) return false;
        for (JsonElement ingredient : ingredients.getAsJsonArray()) {
            if (!ingredient.isJsonArray()) continue;
            for (JsonElement alternative : ingredient.getAsJsonArray()) {
                if (itemId.equals(readItemId(alternative))) return true;
            }
        }
        return false;
    }

    /** @return {@code true}, если рецепт привязан к крафтовой станции-катализатору. */
    public static boolean usesStation(List<IsfTriggerBinding> triggers, ResourceLocation station) {
        if (station == null) return false;
        List<IsfTriggerBinding> safeTriggers = triggers == null ? List.of() : triggers;
        return safeTriggers.stream().anyMatch(binding ->
                binding.trigger().equals(IsfTriggerRegistry.STATION)
                        && station.equals(binding.station()));
    }

    private static ResourceLocation readItemId(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) return null;
        try {
            return ResourceLocation.tryParse(value.getAsString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
