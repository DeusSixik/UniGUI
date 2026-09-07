package dev.sixik.isf.runtime;

import com.google.gson.JsonElement;
import dev.sixik.isf.definition.IsfParameterDefinition;
import dev.sixik.isf.definition.IsfSourceReference;
import dev.sixik.isf.definition.IsfTriggerBinding;
import dev.sixik.isf.definition.IsfVisualNode;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** Полностью разрешённый документ, готовый для evaluation и отображения. */
public record IsfResolvedRecipe(
        ResourceLocation id,
        ResourceLocation recipeType,
        Map<String, IsfParameterDefinition> parameterSchema,
        Map<String, JsonElement> parameters,
        List<IsfTriggerBinding> triggers,
        IsfVisualNode visual,
        IsfSourceReference source
) {
}
