package dev.sixik.isf.trigger;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/** Данные одного вызова триггера. */
public record IsfTriggerContext(
        ServerPlayer player,
        ResourceLocation subject,
        ResourceLocation station,
        Map<String, JsonElement> attributes
) {
    public IsfTriggerContext {
        if (player == null) throw new IllegalArgumentException("Trigger player cannot be null");
        Map<String, JsonElement> copy = new LinkedHashMap<>();
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (key != null) copy.put(key, value == null ? JsonNull.INSTANCE : value.deepCopy());
            });
        }
        attributes = Map.copyOf(copy);
    }
}
