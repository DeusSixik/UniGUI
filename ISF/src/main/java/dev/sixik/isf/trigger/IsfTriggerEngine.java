package dev.sixik.isf.trigger;

import dev.sixik.isf.definition.IsfTriggerBinding;
import dev.sixik.isf.persistence.IsfPlayerLibrary;
import dev.sixik.isf.runtime.IsfDefinitionRegistry;
import dev.sixik.isf.runtime.IsfResolvedRecipe;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Открывает игроку рецепты, подходящие под вызванный trigger. */
public final class IsfTriggerEngine {
    private final IsfDefinitionRegistry definitions;
    private final IsfTriggerRegistry triggers;
    private final IsfPlayerLibrary library;

    public IsfTriggerEngine(IsfDefinitionRegistry definitions,
                            IsfTriggerRegistry triggers,
                            IsfPlayerLibrary library) {
        this.definitions = definitions;
        this.triggers = triggers;
        this.library = library;
    }

    public List<ResourceLocation> fire(ResourceLocation triggerId, IsfTriggerContext context) {
        IsfTriggerType trigger = triggers.find(triggerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ISF trigger: " + triggerId));
        List<ResourceLocation> unlocked = new ArrayList<>();
        for (IsfResolvedRecipe recipe : definitions.recipes()) {
            for (IsfTriggerBinding binding : recipe.triggers()) {
                if (!binding.trigger().equals(triggerId) || !trigger.matches(binding, context)) continue;
                if (library.unlock(context.player().getUUID(), recipe.id())) unlocked.add(recipe.id());
                break;
            }
        }
        return List.copyOf(unlocked);
    }
}
