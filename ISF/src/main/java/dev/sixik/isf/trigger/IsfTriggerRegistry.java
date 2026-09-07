package dev.sixik.isf.trigger;

import com.google.gson.JsonElement;
import dev.sixik.isf.definition.IsfTriggerBinding;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Расширяемый реестр типов триггеров. */
public final class IsfTriggerRegistry {
    public static final ResourceLocation CRAFT = id("craft");
    public static final ResourceLocation USE = id("use");
    public static final ResourceLocation STATION = id("station");

    private final Map<ResourceLocation, IsfTriggerType> types = new LinkedHashMap<>();

    public IsfTriggerRegistry() {
        register(CRAFT, IsfTriggerRegistry::defaultMatch);
        register(USE, IsfTriggerRegistry::defaultMatch);
        register(STATION, IsfTriggerRegistry::defaultMatch);
    }

    public synchronized void register(ResourceLocation id, IsfTriggerType type) {
        types.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(type, "type"));
    }

    public synchronized Optional<IsfTriggerType> find(ResourceLocation id) {
        return Optional.ofNullable(types.get(id));
    }

    private static boolean defaultMatch(IsfTriggerBinding binding, IsfTriggerContext context) {
        if (binding.subject() != null && !binding.subject().equals(context.subject())) return false;
        if (binding.station() != null && !binding.station().equals(context.station())) return false;
        for (Map.Entry<String, JsonElement> condition : binding.conditions().entrySet()) {
            if (!condition.getValue().equals(context.attributes().get(condition.getKey()))) return false;
        }
        return true;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild("isf", path);
    }
}
