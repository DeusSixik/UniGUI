package dev.sixik.isf.runtime;

import dev.sixik.isf.persistence.IsfWorldData;
import dev.sixik.isf.importer.IsfRecipeTypeSupportRegistry;
import dev.sixik.isf.trigger.IsfTriggerContext;
import dev.sixik.isf.trigger.IsfTriggerEngine;
import dev.sixik.isf.trigger.IsfTriggerRegistry;
import net.minecraft.resources.ResourceLocation;

/** Владельческий объект runtime-сервисов одного процесса Minecraft. */
public final class IsfRuntime {
    private final IsfDefinitionRegistry definitions = new IsfDefinitionRegistry();
    private final IsfFunctionRegistry functions = new IsfFunctionRegistry();
    private final IsfTriggerRegistry triggers = new IsfTriggerRegistry();
    private final IsfRecipeTypeSupportRegistry recipeTypes = new IsfRecipeTypeSupportRegistry();

    public IsfDefinitionRegistry definitions() {
        return definitions;
    }

    public IsfFunctionRegistry functions() {
        return functions;
    }

    public IsfTriggerRegistry triggers() {
        return triggers;
    }

    public IsfRecipeTypeSupportRegistry recipeTypes() {
        return recipeTypes;
    }

    public IsfTriggerEngine.FireResult fire(ResourceLocation trigger, IsfTriggerContext context) {
        return new IsfTriggerEngine(definitions, triggers, IsfWorldData.get(context.player().server))
                .fire(trigger, context);
    }
}
