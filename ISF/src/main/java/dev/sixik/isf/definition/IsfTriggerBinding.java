package dev.sixik.isf.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Привязка рецепта к расширяемому runtime-триггеру. */
public record IsfTriggerBinding(
        ResourceLocation trigger,
        ResourceLocation subject,
        ResourceLocation station,
        Map<String, JsonElement> conditions
) {
    public IsfTriggerBinding {
        if (trigger == null) throw new IllegalArgumentException("Trigger id cannot be null");
        conditions = conditions == null ? Map.of() : copyJsonMap(conditions);
    }

    private static Map<String, JsonElement> copyJsonMap(Map<String, JsonElement> source) {
        Map<String, JsonElement> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) copy.put(key, value == null ? JsonNull.INSTANCE : value.deepCopy());
        });
        return Map.copyOf(copy);
    }
}
