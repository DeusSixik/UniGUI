package dev.sixik.isf.definition;

import net.minecraft.resources.ResourceLocation;

/** Катализатор recipe type: блок/предмет, на котором выполняется крафт. */
public record IsfCatalystDefinition(ResourceLocation item, int count) {
    public IsfCatalystDefinition {
        if (item == null) throw new IllegalArgumentException("Catalyst item cannot be null");
        if (count < 1) count = 1;
    }
}
