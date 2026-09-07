package dev.sixik.isf.api;

import com.google.gson.JsonElement;
import dev.sixik.isf.IsfMod;
import dev.sixik.isf.trigger.IsfTriggerContext;
import dev.sixik.isf.trigger.IsfTriggerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

/** Публичная точка интеграции ISF для других модов. */
public final class IsfApi {
    private IsfApi() {
    }

    public static void registerTrigger(ResourceLocation id, IsfTriggerType trigger) {
        IsfMod.runtime().triggers().register(id, trigger);
    }

    public static List<ResourceLocation> fireTrigger(ServerPlayer player,
                                                     ResourceLocation trigger,
                                                     ResourceLocation subject) {
        return fireTrigger(player, trigger, subject, null, Map.of());
    }

    public static List<ResourceLocation> fireTrigger(ServerPlayer player,
                                                     ResourceLocation trigger,
                                                     ResourceLocation subject,
                                                     ResourceLocation station,
                                                     Map<String, JsonElement> attributes) {
        return IsfMod.runtime().fire(trigger,
                new IsfTriggerContext(player, subject, station, attributes));
    }
}
