package dev.sixik.isf.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/** Неизменяемый набор параметров, доступных вычислению визуальных свойств. */
public record IsfEvaluationContext(Map<String, JsonElement> parameters) {
    public IsfEvaluationContext {
        Map<String, JsonElement> copy = new LinkedHashMap<>();
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                if (key != null) copy.put(key, value == null ? JsonNull.INSTANCE : value.deepCopy());
            });
        }
        parameters = Map.copyOf(copy);
    }

    public JsonElement parameter(String name) {
        JsonElement value = parameters.get(name);
        return value == null ? JsonNull.INSTANCE : value.deepCopy();
    }
}
