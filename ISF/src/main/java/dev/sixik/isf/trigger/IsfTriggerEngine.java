package dev.sixik.isf.trigger;

import dev.sixik.isf.definition.IsfTriggerBinding;
import dev.sixik.isf.definition.IsfTriggerDocument;
import dev.sixik.isf.persistence.IsfPlayerLibrary;
import dev.sixik.isf.runtime.IsfDefinitionRegistry;
import dev.sixik.isf.runtime.IsfResolvedRecipe;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Итог вызова: совпавшие рецепты, из них вновь разблокированные, просили ли открыть. */
    public record FireResult(List<ResourceLocation> matched, List<ResourceLocation> unlocked,
                             boolean open) {
    }

    public FireResult fire(ResourceLocation triggerId, IsfTriggerContext context) {
        IsfTriggerType trigger = triggers.find(triggerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ISF trigger: " + triggerId));
        Set<ResourceLocation> matched = new LinkedHashSet<>();
        for (IsfResolvedRecipe recipe : definitions.recipes()) {
            for (IsfTriggerBinding binding : recipe.triggers()) {
                if (!binding.trigger().equals(triggerId) || !trigger.matches(binding, context)) continue;
                matched.add(recipe.id());
                break;
            }
        }
        boolean open = false;
        for (IsfTriggerDocument document : definitions.triggerDocuments()) {
            if (!document.trigger().equals(triggerId)) continue;
            if (!matchesDocument(document, context.subject(), context.station(), context.attributes())) {
                continue;
            }
            matched.addAll(document.recipes());
            if (document.open()) open = true;
        }
        List<ResourceLocation> unlocked = new ArrayList<>();
        for (ResourceLocation recipeId : matched) {
            if (library.unlock(context.player().getUUID(), recipeId)) unlocked.add(recipeId);
        }
        return new FireResult(List.copyOf(matched), List.copyOf(unlocked), open && !matched.isEmpty());
    }

    /**
     * Совпадение документа с вызовом: строгая default-семантика (id триггера уже
     * сверен вызывателем; subject/station — null или равны; conditions — подмножество).
     * Кастомные политики триггеров здесь не применяются: они для биндингов внутри
     * рецептов, а документы — явные и предсказуемые.
     */
    static boolean matchesDocument(IsfTriggerDocument document, ResourceLocation subject,
                                   ResourceLocation station, Map<String, com.google.gson.JsonElement> attributes) {
        if (document == null) return false;
        if (document.subject() != null && !document.subject().equals(subject)) return false;
        if (document.station() != null && !document.station().equals(station)) return false;
        Map<String, com.google.gson.JsonElement> expected = document.conditions();
        Map<String, com.google.gson.JsonElement> actual =
                attributes == null ? Map.of() : attributes;
        for (Map.Entry<String, com.google.gson.JsonElement> condition : expected.entrySet()) {
            if (!condition.getValue().equals(actual.get(condition.getKey()))) return false;
        }
        return true;
    }
}
