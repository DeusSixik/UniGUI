package dev.sixik.isf.definition;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Переиспользуемый шаблон визуала и параметров рецептов. */
public record IsfRecipeTypeDefinition(
        ResourceLocation id,
        ResourceLocation parent,
        Map<String, IsfParameterDefinition> parameters,
        IsfVisualNode visual
) {
    public IsfRecipeTypeDefinition {
        if (id == null) throw new IllegalArgumentException("Recipe type id cannot be null");
        parameters = parameters == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(parameters));
    }
}
