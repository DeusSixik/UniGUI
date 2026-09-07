package dev.sixik.isf.runtime;

import com.google.gson.JsonElement;

import java.util.List;

/** Безопасная именованная функция expression runtime. */
@FunctionalInterface
public interface IsfFunction {
    JsonElement apply(List<JsonElement> arguments);
}
