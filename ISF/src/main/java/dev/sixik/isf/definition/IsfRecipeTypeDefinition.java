package dev.sixik.isf.definition;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Переиспользуемый шаблон визуала и параметров рецептов. */
public record IsfRecipeTypeDefinition(
        ResourceLocation id,
        ResourceLocation parent,
        Map<String, IsfParameterDefinition> parameters,
        List<IsfCatalystDefinition> catalysts,
        ResourceLocation icon,
        IsfVisualNode visual
) {
    public IsfRecipeTypeDefinition {
        if (id == null) throw new IllegalArgumentException("Recipe type id cannot be null");
        parameters = parameters == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(parameters));
        catalysts = catalysts == null ? List.of() : List.copyOf(catalysts);
    }

    /** Совместимый конструктор: без катализаторов и иконки. */
    public IsfRecipeTypeDefinition(ResourceLocation id, ResourceLocation parent,
                                   Map<String, IsfParameterDefinition> parameters,
                                   IsfVisualNode visual) {
        this(id, parent, parameters, List.of(), null, visual);
    }

    /** Совместимый конструктор: катализаторы без иконки. */
    public IsfRecipeTypeDefinition(ResourceLocation id, ResourceLocation parent,
                                   Map<String, IsfParameterDefinition> parameters,
                                   List<IsfCatalystDefinition> catalysts,
                                   IsfVisualNode visual) {
        this(id, parent, parameters, catalysts, null, visual);
    }

    public List<IsfCatalystDefinition> catalysts() {
        return catalysts == null ? List.of() : catalysts;
    }

    /** @return новые катализаторы поверх существующих, сохраняя порядок и иконку. */
    public IsfRecipeTypeDefinition withCatalysts(List<IsfCatalystDefinition> additional) {
        if (additional == null || additional.isEmpty()) return this;
        List<IsfCatalystDefinition> merged = new ArrayList<>(catalysts());
        additional.forEach(catalyst -> {
            if (catalyst != null) merged.add(catalyst);
        });
        return new IsfRecipeTypeDefinition(id, parent, parameters, List.copyOf(merged), icon, visual);
    }
}
