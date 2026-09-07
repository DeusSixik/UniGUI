package dev.sixik.isf.runtime;

import com.google.gson.JsonElement;
import dev.sixik.isf.definition.IsfParameterType;
import net.minecraft.resources.ResourceLocation;

/** Проверяет JSON-значения относительно объявленного типа параметра. */
public final class IsfValueValidator {
    private IsfValueValidator() {
    }

    public static boolean matches(IsfParameterType type, JsonElement value) {
        if (type == null || value == null || value.isJsonNull()) return true;
        return switch (type) {
            case NUMBER -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
            case BOOLEAN -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
            case STRING -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
            case RESOURCE_LOCATION, ITEM -> value.isJsonPrimitive()
                    && value.getAsJsonPrimitive().isString()
                    && ResourceLocation.tryParse(value.getAsString()) != null;
            case COLOR -> validColor(value);
            case JSON -> true;
        };
    }

    private static boolean validColor(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return false;
        String color = value.getAsString();
        return color.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?");
    }
}
