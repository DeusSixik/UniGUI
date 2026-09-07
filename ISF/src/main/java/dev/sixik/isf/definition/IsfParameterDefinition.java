package dev.sixik.isf.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** Описание входного параметра recipe type. */
public record IsfParameterDefinition(
        String name,
        IsfParameterType type,
        JsonElement defaultValue,
        String description
) {
    public IsfParameterDefinition {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Parameter name cannot be empty");
        type = type == null ? IsfParameterType.STRING : type;
        defaultValue = defaultValue == null ? JsonNull.INSTANCE : defaultValue.deepCopy();
        description = description == null ? "" : description;
    }
}
