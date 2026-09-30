package dev.sixik.isf.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sixik.isf.definition.IsfCatalystDefinition;
import dev.sixik.isf.definition.IsfExpression;
import dev.sixik.isf.definition.IsfParameterDefinition;
import dev.sixik.isf.definition.IsfParameterType;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import dev.sixik.isf.definition.IsfRecipeTypeDefinition;
import dev.sixik.isf.definition.IsfTriggerBinding;
import dev.sixik.isf.definition.IsfVisualNode;
import dev.sixik.isf.trigger.IsfTriggerRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Поддержка Loot Tables: строит визуальные рецепты {@code isf:loot_table}
 * из JSON таблиц добычи блоков, мобов и сундуков.
 *
 * <p>Каждая таблица становится рецептом вида «источник → сетка дропа», где у
 * каждого предмета подписан шанс выпадения и диапазон количества. Триггеры:
 * {@code CRAFT} на каждый предмет дропа (R по предмету показывает таблицы, где
 * он падает) и {@code STATION} на блок-источник (U по блоку/сундуку открывает
 * его таблицу).</p>
 */
public final class LootTableSupports {
    public static final ResourceLocation LOOT_TYPE_ID =
            ResourceLocation.tryParse("isf:loot_table");
    /** Максимум ячеек дропа в одном визуале (5 колонок x 6 строк). */
    private static final int MAX_DROPS = 30;
    /** Максимум предметов из тега в одной ячейке. */
    private static final int MAX_TAG_ITEMS = 12;
    private static final Set<String> SUPPORTED_ROOTS = Set.of("blocks", "entities", "chests");

    private LootTableSupports() {
    }

    /** Определение типа {@code isf:loot_table} (шапка «источник + ID», ниже сетка дропа). */
    public static IsfRecipeTypeDefinition lootTypeDefinition() {
        Map<String, IsfParameterDefinition> schema = new LinkedHashMap<>();
        schema.put("source_item", parameter("source_item", IsfParameterType.ITEM, "minecraft:air"));
        schema.put("table_id", parameter("table_id", IsfParameterType.STRING, "isf:none"));
        schema.put("entity_type", parameter("entity_type", IsfParameterType.STRING, ""));
        schema.put("drops", parameter("drops", IsfParameterType.JSON, new JsonArray()));
        List<IsfCatalystDefinition> catalysts = List.of();
        return new IsfRecipeTypeDefinition(LOOT_TYPE_ID, null, schema, catalysts,
                ResourceLocation.tryParse("minecraft:chest"), lootVisual());
    }

    /**
     * Строит ISF-рецепты из всех таблиц добычи в ресурсах датапаков.
     *
     * @param resources серверные ресурсы (доступ на этапе релоада датапаков)
     * @return по одному рецепту на каждую поддержанную непустую таблицу
     */
    public static List<IsfRecipeDefinition> generate(ResourceManager resources) {
        Map<ResourceLocation, Resource> files =
                resources.listResources("loot_tables", path -> path.getPath().endsWith(".json"));
        List<IsfRecipeDefinition> recipes = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            ResourceLocation tableId = toTableId(entry.getKey());
            if (tableId == null || !isSupportedTable(tableId)) continue;
            JsonObject root = readJson(entry.getValue());
            if (root == null) continue;
            List<Drop> drops = parsePools(root);
            if (drops.isEmpty()) continue;
            recipes.add(buildRecipe(tableId, drops));
        }
        return recipes;
    }

    // ------------------------------------------------------------------
    // Разбор таблицы
    // ------------------------------------------------------------------

    /** Ячейка дропа: варианты предмета (тег), шанс, диапазон количества и условия. */
    private record Drop(List<String> ids, double chance, double countMin, double countMax,
                        List<String> conditions) {
    }

    private static final class DropBuilder {
        final LinkedHashSet<String> ids = new LinkedHashSet<>();
        final LinkedHashSet<String> conditions = new LinkedHashSet<>();
        double chance = 1.0;
        double countMin = 1.0;
        double countMax = 1.0;
    }

    private static List<Drop> parsePools(JsonObject root) {
        if (!root.has("pools") || !root.get("pools").isJsonArray()) return List.of();
        Map<String, DropBuilder> merged = new LinkedHashMap<>();
        JsonArray pools = root.getAsJsonArray("pools");
        for (JsonElement poolElement : pools) {
            if (!poolElement.isJsonObject()) continue;
            JsonObject pool = poolElement.getAsJsonObject();
            if (!pool.has("entries") || !pool.get("entries").isJsonArray()) continue;
            collectEntries(pool.getAsJsonArray("entries"), 1.0, merged);
        }
        List<Drop> drops = new ArrayList<>();
        for (DropBuilder builder : merged.values()) {
            drops.add(new Drop(List.copyOf(builder.ids), builder.chance,
                    builder.countMin, builder.countMax, List.copyOf(builder.conditions)));
            if (drops.size() >= MAX_DROPS) break;
        }
        return List.copyOf(drops);
    }

    /**
     * Обходит дерево entries: контейнеры (alternatives/group/sequence) раскрываются
     * в детей с накопленным шансом, item/tag становятся ячейками дропа.
     */
    private static void collectEntries(JsonArray entries, double baseChance,
                                       Map<String, DropBuilder> merged) {
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            String type = entry.has("type") && entry.get("type").isJsonPrimitive()
                    ? entry.get("type").getAsString() : "";
            double chance = baseChance * chanceMultiplier(entry);
            if (type.endsWith("alternatives") || type.endsWith("group") || type.endsWith("sequence")) {
                if (entry.has("children") && entry.get("children").isJsonArray()) {
                    collectEntries(entry.getAsJsonArray("children"), chance, merged);
                }
                continue;
            }
            if (type.endsWith("loot_table") || type.endsWith("dynamic") || type.endsWith("empty")) {
                continue;
            }
            if (!entry.has("name") || !entry.get("name").isJsonPrimitive()) continue;
            String name = entry.get("name").getAsString();
            DropBuilder builder;
            if (type.endsWith("tag")) {
                List<String> tagItems = expandTag(name);
                if (tagItems.isEmpty()) continue;
                builder = merged.computeIfAbsent("tag:" + name, key -> new DropBuilder());
                builder.ids.addAll(tagItems);
            } else {
                builder = merged.computeIfAbsent(name, key -> new DropBuilder());
                builder.ids.add(name);
            }
            builder.chance = Math.max(builder.chance, chance);
            collectConditionNames(entry, builder.conditions);
            double[] count = entryCount(entry);
            builder.countMin = Math.min(builder.countMin, count[0]);
            builder.countMax = Math.max(builder.countMax, count[1]);
        }
    }

    /**
     * Запоминает человекопонятные условия entry (кроме учтённых в шансах): Silk Touch,
     * killed_by_player, match_tool и т.п. — они раскрываются в тултипе ячейки дропа.
     */
    private static void collectConditionNames(JsonObject entry, LinkedHashSet<String> out) {
        if (!entry.has("conditions") || !entry.get("conditions").isJsonArray()) return;
        for (JsonElement element : entry.getAsJsonArray("conditions")) {
            if (!element.isJsonObject()) continue;
            JsonObject condition = element.getAsJsonObject();
            String name = condition.has("condition") && condition.get("condition").isJsonPrimitive()
                    ? condition.get("condition").getAsString() : "";
            if (name.isEmpty() || name.endsWith("random_chance")
                    || name.endsWith("random_chance_with_looting")
                    || name.endsWith("survives_explosion")) {
                continue;
            }
            int slash = name.lastIndexOf('/');
            out.add(slash >= 0 ? name.substring(slash + 1) : name);
        }
    }

    /**
     * Произведение шансов всех {@code random_chance}/{@code random_chance_with_looting}
     * условий entry (условия соединены AND). Без них — 1.0 (падает всегда).
     */
    private static double chanceMultiplier(JsonObject entry) {
        if (!entry.has("conditions") || !entry.get("conditions").isJsonArray()) return 1.0;
        double chance = 1.0;
        for (JsonElement element : entry.getAsJsonArray("conditions")) {
            if (!element.isJsonObject()) continue;
            JsonObject condition = element.getAsJsonObject();
            String name = condition.has("condition") && condition.get("condition").isJsonPrimitive()
                    ? condition.get("condition").getAsString() : "";
            if (name.endsWith("random_chance") || name.endsWith("random_chance_with_looting")) {
                if (condition.has("chance") && condition.get("chance").isJsonPrimitive()) {
                    try {
                        chance *= Math.max(0.0, Math.min(1.0, condition.get("chance").getAsDouble()));
                    } catch (RuntimeException ignored) {
                    }
                }
            }
        }
        return chance;
    }

    /** Диапазон количества из функции {@code set_count}: {min, max} (uniform). */
    private static double[] entryCount(JsonObject entry) {
        double min = 1.0;
        double max = 1.0;
        if (!entry.has("functions") || !entry.get("functions").isJsonArray()) return new double[]{min, max};
        for (JsonElement element : entry.getAsJsonArray("functions")) {
            if (!element.isJsonObject()) continue;
            JsonObject function = element.getAsJsonObject();
            String name = function.has("function") && function.get("function").isJsonPrimitive()
                    ? function.get("function").getAsString() : "";
            if (!name.endsWith("set_count")) continue;
            JsonElement count = function.get("count");
            if (count == null) continue;
            if (count.isJsonPrimitive()) {
                double value = asDouble(count, 1.0);
                min = Math.max(0.0, value);
                max = min;
            } else if (count.isJsonObject()) {
                JsonObject range = count.getAsJsonObject();
                min = Math.max(0.0, asDouble(range.get("min"), 1.0));
                max = Math.max(min, asDouble(range.get("max"), min));
            }
        }
        return new double[]{min, max};
    }

    /** Разворачивает item tag в список id предметов (с ограничением длины). */
    private static List<String> expandTag(String name) {
        ResourceLocation tagId = ResourceLocation.tryParse(name);
        if (tagId == null) return List.of();
        try {
            return BuiltInRegistries.ITEM
                    .getTag(TagKey.create(Registries.ITEM, tagId))
                    .map(holders -> {
                        List<String> ids = new ArrayList<>();
                        holders.forEach(holder -> {
                            if (ids.size() >= MAX_TAG_ITEMS) return;
                            ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
                            if (id != null) ids.add(id.toString());
                        });
                        return List.copyOf(ids);
                    })
                    .orElseGet(List::of);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // Сборка рецепта
    // ------------------------------------------------------------------

    private static IsfRecipeDefinition buildRecipe(ResourceLocation tableId, List<Drop> drops) {
        String source = sourceItem(tableId);
        String entityType = entityTypeId(tableId);
        JsonArray dropsJson = new JsonArray();
        Set<IsfTriggerBinding> triggers = new LinkedHashSet<>();
        for (Drop drop : drops) {
            JsonObject cell = new JsonObject();
            JsonArray ids = new JsonArray();
            drop.ids().forEach(ids::add);
            cell.add("ids", ids);
            cell.addProperty("chance", drop.chance());
            cell.addProperty("count_min", Math.round(drop.countMin() * 10.0) / 10.0);
            cell.addProperty("count_max", Math.round(drop.countMax() * 10.0) / 10.0);
            if (!drop.conditions().isEmpty()) {
                JsonArray conditions = new JsonArray();
                drop.conditions().forEach(conditions::add);
                cell.add("conditions", conditions);
            }
            dropsJson.add(cell);
            for (String id : drop.ids()) {
                ResourceLocation itemId = ResourceLocation.tryParse(id);
                if (itemId != null) {
                    triggers.add(new IsfTriggerBinding(IsfTriggerRegistry.CRAFT, itemId, null, Map.of()));
                }
            }
        }
        ResourceLocation sourceId = ResourceLocation.tryParse(source);
        if (isBlockTable(tableId) && sourceId != null) {
            // U по блоку в каталоге/инвентаре открывает его таблицу добычи.
            triggers.add(new IsfTriggerBinding(IsfTriggerRegistry.STATION, null, sourceId, Map.of()));
        }

        Map<String, JsonElement> parameters = new LinkedHashMap<>();
        parameters.put("source_item", new com.google.gson.JsonPrimitive(source));
        parameters.put("table_id", new com.google.gson.JsonPrimitive(tableId.toString()));
        if (entityType != null) {
            parameters.put("entity_type", new com.google.gson.JsonPrimitive(entityType));
        }
        parameters.put("drops", dropsJson);
        ResourceLocation recipeId = ResourceLocation.tryBuild("isf",
                "loot/" + tableId.getNamespace() + "/" + tableId.getPath());
        return new IsfRecipeDefinition(recipeId, LOOT_TYPE_ID, null,
                Map.copyOf(parameters), List.copyOf(triggers), null,
                new dev.sixik.isf.definition.IsfSourceReference("loot_table", tableId));
    }

    /**
     * Предмет-источник: у блоков — сам блок, у сундуков — {@code minecraft:chest},
     * у мобов — пустой слот (иконка моба рисуется через {@code entity_type}).
     */
    private static String sourceItem(ResourceLocation tableId) {
        String path = tableId.getPath();
        if (path.startsWith("blocks/")) {
            return tableId.getNamespace() + ":" + path.substring("blocks/".length());
        }
        if (path.startsWith("chests/")) return "minecraft:chest";
        return "minecraft:air";
    }

    /** {@code entities/<entity>} → id сущности для живого рендера в шапке. */
    private static String entityTypeId(ResourceLocation tableId) {
        String path = tableId.getPath();
        if (!path.startsWith("entities/")) return null;
        ResourceLocation entityId = ResourceLocation.tryBuild(tableId.getNamespace(),
                path.substring("entities/".length()));
        if (entityId == null) return null;
        // Не все таблицы entities мапятся на живую сущность (player, armor_stand — ок;
        // неизвестные id не резолвятся и рендер вернётся к fallback-иконке).
        return BuiltInRegistries.ENTITY_TYPE.containsKey(entityId) ? entityId.toString() : null;
    }

    private static boolean isBlockTable(ResourceLocation tableId) {        return tableId.getPath().startsWith("blocks/");
    }

    private static boolean isSupportedTable(ResourceLocation tableId) {
        String path = tableId.getPath();
        int slash = path.indexOf('/');
        return slash > 0 && SUPPORTED_ROOTS.contains(path.substring(0, slash));
    }

    /** {@code loot_tables/<ns>/<path>.json} → {@code <ns>:<path>}. */
    private static ResourceLocation toTableId(ResourceLocation fileId) {
        String path = fileId.getPath();
        String prefix = "loot_tables/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) return null;
        String rest = path.substring(prefix.length(), path.length() - ".json".length());
        return ResourceLocation.tryBuild(fileId.getNamespace(), rest);
    }

    private static JsonObject readJson(Resource resource) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double asDouble(JsonElement element, double fallback) {
        try {
            return element == null || !element.isJsonPrimitive() ? fallback : element.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    // ------------------------------------------------------------------
    // Определение типа (schema + визуал)
    // ------------------------------------------------------------------

    private static IsfParameterDefinition parameter(String name, IsfParameterType type, JsonElement value) {
        return new IsfParameterDefinition(name, type, value, "");
    }

    private static IsfParameterDefinition parameter(String name, IsfParameterType type, String value) {
        return new IsfParameterDefinition(name, type, new com.google.gson.JsonPrimitive(value), "");
    }

    private static IsfVisualNode lootVisual() {
        // Шапка: источник (блок/сундук/живой моб) + локализуемое ID таблицы.
        IsfVisualNode source = new IsfVisualNode("source", id("isf:entity_or_item"), Map.of(
                "item", parameter("source_item"),
                "entity", parameter("entity_type"),
                "slot", literal("isf:textures/jei/atlas/gui/slot.png"),
                "width", literal(18),
                "height", literal(18),
                "alignSelf", literal("center")), List.of());
        IsfVisualNode title = new IsfVisualNode("title", id("unigui:label"), Map.of(
                "text", new IsfExpression.Call("isf:loot_title",
                        List.of(parameter("table_id"))),
                "color", literal("#5A5A5AFF"),
                "alignSelf", literal("center")), List.of());
        IsfVisualNode header = new IsfVisualNode("header", id("unigui:hbox"), Map.of(
                "padding", literal(2),
                "spacing", literal(6),
                "alignItems", literal("center")), List.of(source, title));
        // Тело: сетка дропа (проценты внутри ячеек) в вертикальном скролле,
        // полоса которого скрыта, пока всё влезает.
        IsfVisualNode scroll = new IsfVisualNode("scroll", id("isf:loot_scroll"), Map.of(
                "drops", parameter("drops")), List.of());
        IsfVisualNode body = new IsfVisualNode("body", id("unigui:vbox"), Map.of(
                "padding", literal(4),
                "spacing", literal(4)),
                List.of(header, scroll));
        return new IsfVisualNode("root", id("unigui:box"), Map.of(
                "width", literal(150),
                "height", literal(110),
                "background", literal("#11151DEB"),
                "border", literal("#6A8FAEFF"),
                "radius", literal(3)), List.of(body));
    }

    private static IsfVisualNode arrow() {
        return new IsfVisualNode("arrow", id("isf:texture"), Map.of(
                "texture", literal("isf:textures/jei/atlas/gui/recipe_arrow.png"),
                "width", literal(22),
                "height", literal(16),
                "alignSelf", literal("center")), List.of());
    }

    private static IsfExpression literal(String value) {
        return new IsfExpression.Literal(new com.google.gson.JsonPrimitive(value));
    }

    private static IsfExpression literal(Number value) {
        return new IsfExpression.Literal(new com.google.gson.JsonPrimitive(value));
    }

    private static IsfExpression parameter(String name) {
        return new IsfExpression.Parameter(name);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
