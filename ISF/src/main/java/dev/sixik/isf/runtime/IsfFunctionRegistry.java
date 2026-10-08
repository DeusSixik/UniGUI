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
        // Имя таблицы добычи: явный lang-ключ isf.loot.<ns>.<path>, иначе —
        // имя блока-источника / сущности, иначе — последний сегмент пути.
        // I18n.get(key, fallback) фолбэк НЕ умеет (вернёт сам ключ), поэтому
        // отсутствие ключа определяем сравнением.
        register(LOOT_TITLE, arguments -> new JsonPrimitive(lootTitle(
                arguments == null || arguments.isEmpty() || arguments.get(0) == null
                        ? "" : arguments.get(0).getAsString(),
                net.minecraft.client.resources.language.I18n::get)));
    }

    /**
     * Заголовок окна добычи по id таблицы.
     *
     * <ol>
     *   <li>Явная локализация {@code isf.loot.<ns>.<path>} (имена от админа).</li>
     *   <li>Таблицы блоков ({@code <ns>:blocks/<block>}) — отображаемое имя
     *   блока-предмета вместо id из реестра.</li>
     *   <li>Таблицы сущностей ({@code <ns>:entities/<entity>}) — имя сущности.</li>
     *   <li>Остальное (сундуки и т.п.) — последний сегмент пути таблицы.</li>
     * </ol>
     *
     * @param raw идентификатор таблицы добычи
     * @param localizer резолвер lang-ключей ({@code null} — без локализации);
     *                  в игре передаётся клиентский I18n, в тестах — заглушка
     */
    static String lootTitle(String raw, java.util.function.Function<String, String> localizer) {
        return lootTitle(raw, localizer,
                IsfFunctionRegistry::itemDisplayName, IsfFunctionRegistry::entityDisplayName);
    }

    /**
     * То же, но резолверы имён подменяемы: в игре — по реестру, в тестах —
     * заглушки (реестры вне бутстрапа недоступны).
     */
    static String lootTitle(String raw,
                            java.util.function.Function<String, String> localizer,
                            java.util.function.Function<String, String> itemNames,
                            java.util.function.Function<String, String> entityNames) {
        ResourceLocation tableId = ResourceLocation.tryParse(raw == null ? "" : raw);
        if (tableId == null) return raw == null ? "" : raw;
        String key = "isf.loot." + tableId.getNamespace() + "." + tableId.getPath();
        try {
            String localized = localizer == null ? null : localizer.apply(key);
            if (localized != null && !localized.equals(key)) return localized;
        } catch (RuntimeException ignored) {
        }
        String path = tableId.getPath();
        if (path.startsWith("blocks/")) {
            String source = resolveName(
                    tableId.getNamespace() + ":" + path.substring("blocks/".length()), itemNames);
            if (source != null) return source;
        } else if (path.startsWith("entities/")) {
            String entity = resolveName(
                    tableId.getNamespace() + ":" + path.substring("entities/".length()), entityNames);
            if (entity != null) return entity;
        }
        return prettifyLastSegment(path);
    }

    private static String resolveName(String raw,
                                      java.util.function.Function<String, String> resolver) {
        try {
            String name = resolver == null ? null : resolver.apply(raw);
            return name == null || name.isBlank() ? null : name;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Отображаемое имя предмета или {@code null} (воздух/неизвестный id). */
    private static String itemDisplayName(String raw) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null) return null;
            net.minecraft.world.item.Item item =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            if (item == null || item == net.minecraft.world.item.Items.AIR) return null;
            String name = new net.minecraft.world.item.ItemStack(item).getHoverName().getString();
            return name == null || name.isBlank() ? null : name;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Имя типа сущности или {@code null} (неизвестный id). */
    private static String entityDisplayName(String raw) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null) return null;
            net.minecraft.world.entity.EntityType<?> type =
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type == null) return null;
            String name = type.getDescription().getString();
            return name == null || name.isBlank() ? null : name;
        } catch (RuntimeException ignored) {
            return null;
        }
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
