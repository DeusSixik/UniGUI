package dev.sixik.isf.definition;

import net.minecraft.resources.ResourceLocation;

/** Информация о происхождении импортированного документа, не создающая live-связь с источником. */
public record IsfSourceReference(String category, ResourceLocation sourceId) {
    public IsfSourceReference {
        category = category == null ? "custom" : category;
    }
}
