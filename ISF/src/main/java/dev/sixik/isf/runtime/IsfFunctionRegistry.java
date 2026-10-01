package dev.sixik.isf.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Реестр операций, которые разрешено использовать в визуальных выражениях. */
public final class IsfFunctionRegistry {
    public static final ResourceLocation ADD = id("add");
    public static final ResourceLocation SUBTRACT = id("subtract");
    public static final ResourceLocation MULTIPLY = id("multiply");
    public static final ResourceLocation DIVIDE = id("divide");
    public static final ResourceLocation CLAMP = id("clamp");
    public static final ResourceLocation PERCENT = id("percent");
    public static final ResourceLocation LOOT_TITLE = id("loot_title");

    private final Map<ResourceLocation, IsfFunction> functions = new LinkedHashMap<>();

    public IsfFunctionRegistry() {
        registerBuiltIns();
    }

    public synchronized void register(ResourceLocation id, IsfFunction function) {
        functions.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(function, "function"));
    }

    public synchronized Optional<IsfFunction> find(ResourceLocation id) {
        return Optional.ofNullable(functions.get(id));
    }

    private void registerBuiltIns() {
        register(ADD, arguments -> number(number(arguments, 0) + number(arguments, 1)));
        register(SUBTRACT, arguments -> number(number(arguments, 0) - number(arguments, 1)));
        register(MULTIPLY, arguments -> number(number(arguments, 0) * number(arguments, 1)));
        register(DIVIDE, arguments -> {
            double divisor = number(arguments, 1);
            return number(divisor == 0.0 ? 0.0 : number(arguments, 0) / divisor);
        });
        register(CLAMP, arguments -> number(Math.max(number(arguments, 1),
                Math.min(number(arguments, 2), number(arguments, 0)))));
        register(PERCENT, arguments -> {
            double maximum = number(arguments, 1);
            return number(maximum == 0.0 ? 0.0 : number(arguments, 0) / maximum * 100.0);
        });
        // Имя таблицы добычи: ключ isf.loot.<ns>.<path>, иначе — последний
        // сегмент пути («entities/zombie» → «Zombie», «path/my_table» → «My Table»).
        // I18n.get(key, fallback) фолбэк НЕ умеет (вернёт сам ключ), поэтому
        // отсутствие ключа определяем сравнением.
        register(LOOT_TITLE, arguments -> {
            String raw = arguments == null || arguments.isEmpty() || arguments.get(0) == null
                    ? "" : arguments.get(0).getAsString();
            ResourceLocation tableId = ResourceLocation.tryParse(raw);
            if (tableId == null) return new JsonPrimitive(raw);
            String key = "isf.loot." + tableId.getNamespace() + "." + tableId.getPath();
            String localized = net.minecraft.client.resources.language.I18n.get(key);
            return new JsonPrimitive(
                    localized.equals(key) ? prettifyLastSegment(tableId.getPath()) : localized);
        });
    }

    /** «path/my_table» → «My Table», «entities/zombie» → «Zombie». */
    static String prettifyLastSegment(String path) {
        if (path == null || path.isEmpty()) return "";
        int slash = path.lastIndexOf('/');
        String last = slash < 0 ? path : path.substring(slash + 1);
        StringBuilder text = new StringBuilder();
        for (String word : last.replace('_', ' ').split(" ")) {
            if (word.isEmpty()) continue;
            if (!text.isEmpty()) text.append(' ');
            text.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return text.isEmpty() ? path : text.toString();
    }

    private static double number(List<JsonElement> arguments, int index) {
        if (arguments == null || index < 0 || index >= arguments.size()) return 0.0;
        JsonElement value = arguments.get(index);
        try {
            return value == null || value.isJsonNull() ? 0.0 : value.getAsDouble();
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    private static JsonPrimitive number(double value) {
        return new JsonPrimitive(Double.isFinite(value) ? value : 0.0);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild("isf", path);
    }
}
