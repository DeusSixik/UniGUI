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

    /** Ячейка дропа: варианты предмета (тег), шанс, диапазон количества, условия и NBT (SNBT). */
    record Drop(List<String> ids, double chance, double countMin, double countMax,
                List<String> conditions, String nbt) {
    }

    private static final class DropBuilder {
        final LinkedHashSet<String> ids = new LinkedHashSet<>();
        final LinkedHashSet<String> conditions = new LinkedHashSet<>();
        /** Вероятность НЕ получить дроп ни разу (произведение по всем вхождениям). */
        double miss = 1.0;
        double countMin = Double.POSITIVE_INFINITY;
        double countMax = 1.0;
        /** SNBT-тег предмета (зелья, чары, имена): "" — обычный предмет без NBT. */
        String nbt = "";

        double chance() {
            return Math.max(0.0, Math.min(1.0, 1.0 - miss));
        }
    }

    /** Синтетическое условие для тултипа: точные чары неизвестны (случайный ролл). */
    static final String RANDOM_ENCHANT_CONDITION = "isf:random_enchant";
    /** Маркерный чар для foil-блика случайно зачарованного дропа (см. itemNbtSnbt). */
    private static final String RANDOM_ENCHANT_ID = "isf:random";
    private static final String BOOK_ID = "minecraft:book";
    private static final String ENCHANTED_BOOK_ID = "minecraft:enchanted_book";

    /** Распределение числа бросков пула: константа / равномерное целое / биномиальное. */
    private enum RollKind {
        CONSTANT, UNIFORM_INT, BINOMIAL
    }

    private record RollSpec(RollKind kind, double a, double b) {
        static RollSpec constant(double value) {
            return new RollSpec(RollKind.CONSTANT, value, 0.0);
        }
    }

    static List<Drop> parsePools(JsonObject root) {
        if (!root.has("pools") || !root.get("pools").isJsonArray()) return List.of();
        Map<String, DropBuilder> merged = new LinkedHashMap<>();
        JsonArray pools = root.getAsJsonArray("pools");
        for (JsonElement poolElement : pools) {
            if (!poolElement.isJsonObject()) continue;
            collectPool(poolElement.getAsJsonObject(), merged);
        }
        List<Drop> drops = new ArrayList<>();
        for (DropBuilder builder : merged.values()) {
            double countMin = Double.isFinite(builder.countMin) ? builder.countMin : builder.countMax;
            drops.add(new Drop(List.copyOf(builder.ids), builder.chance(),
                    countMin, builder.countMax, List.copyOf(builder.conditions), builder.nbt));
            if (drops.size() >= MAX_DROPS) break;
        }
        return List.copyOf(drops);
    }

    /**
     * Один пул таблицы: условия пула умножают шанс всех его entry, роллы бросаются
     * {@code rolls + bonus_rolls} раз, за каждый бросок одно entry выбирается по весу.
     * Вес пустых/динамических/вложенных entry тоже участвует (они съедают роллы).
     */
    private static void collectPool(JsonObject pool, Map<String, DropBuilder> merged) {
        if (!pool.has("entries") || !pool.get("entries").isJsonArray()) return;
        JsonArray entries = pool.getAsJsonArray("entries");
        double poolChance = allConditionsChance(pool);
        if (poolChance <= 0.0) return;
        RollSpec rolls = poolRolls(pool);
        double totalWeight = 0.0;
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            totalWeight += entryWeight(element.getAsJsonObject());
        }
        if (totalWeight <= 0.0) return;
        // Геймплейные условия пула (killed_by_player и т.п.) наследуют все его дропы.
        LinkedHashSet<String> poolConditions = new LinkedHashSet<>();
        collectConditionNames(pool, poolConditions);
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            resolveEntry(entry, poolChance * entryWeight(entry) / totalWeight, rolls,
                    poolConditions, merged);
        }
    }

    /**
     * Раскрытие entry при базовой однобросковой вероятности {@code base}
     * (в {@code base} уже входят шанс пула и доля веса; условия самого entry — нет).
     * Веса вложенных entry ванилла не разыгрывает, только веса прямых детей пула.
     */
    private static void resolveEntry(JsonObject entry, double base, RollSpec rolls,
                                     LinkedHashSet<String> inheritedConditions,
                                     Map<String, DropBuilder> merged) {
        if (base <= 0.0) return;
        String type = entryType(entry);
        if (type.endsWith("alternatives")) {
            // Первый проходящий ребёнок забирает ролл:
            // P(child_i) = base * q_i * Π_{j<i}(1 - q_j).
            double skip = 1.0;
            if (entry.has("children") && entry.get("children").isJsonArray()) {
                for (JsonElement child : entry.getAsJsonArray("children")) {
                    if (child.isJsonObject()) {
                        resolveEntry(child.getAsJsonObject(), base * skip, rolls,
                                inheritedConditions, merged);
                    }
                    skip *= 1.0 - childPassChance(child);
                }
            }
            return;
        }
        if (type.endsWith("group") || type.endsWith("sequence")) {
            // Все дети бросаются (условия контейнера — на всех).
            double inner = base * allConditionsChance(entry);
            if (inner <= 0.0) return;
            if (entry.has("children") && entry.get("children").isJsonArray()) {
                for (JsonElement child : entry.getAsJsonArray("children")) {
                    if (child.isJsonObject()) {
                        resolveEntry(child.getAsJsonObject(), inner, rolls,
                                inheritedConditions, merged);
                    }
                }
            }
            return;
        }
        if (type.endsWith("loot_table") || type.endsWith("dynamic") || type.endsWith("empty")) {
            return;
        }
        if (!entry.has("name") || !entry.get("name").isJsonPrimitive()) return;
        double single = base * allConditionsChance(entry);
        if (single <= 0.0) return;
        double probability = rollProbability(rolls, single);
        if (probability <= 0.0) return;
        String name = entry.get("name").getAsString();
        // Ванилла заменяет обычную книгу на зачарованную при любом зачаровании
        // (enchant_randomly/enchant_with_levels/set_enchantments): показываем результат.
        String displayName = BOOK_ID.equals(name) && hasBookConvertingFunction(entry)
                ? ENCHANTED_BOOK_ID : name;
        String nbt = itemNbtSnbt(entry, displayName);
        DropBuilder builder;
        if (type.endsWith("tag")) {
            List<String> tagItems = expandTag(name);
            if (tagItems.isEmpty()) return;
            // Один и тот же id с разным NBT (два зелья) — разные ячейки.
            builder = merged.computeIfAbsent("tag:" + name + "\u0000" + nbt, key -> new DropBuilder());
            builder.ids.addAll(tagItems);
        } else {
            builder = merged.computeIfAbsent(displayName + "\u0000" + nbt, key -> new DropBuilder());
            builder.ids.add(displayName);
        }
        // Одно и то же может падать из нескольких пулов/вхождений: комбинируем
        // как независимые попытки (вероятность промаха перемножается).
        builder.miss *= 1.0 - probability;
        builder.nbt = nbt;
        builder.conditions.addAll(inheritedConditions);
        collectConditionNames(entry, builder.conditions);
        if (hasUnpinnedRandomEnchant(entry)) builder.conditions.add(RANDOM_ENCHANT_CONDITION);
        double[] count = entryCount(entry);
        builder.countMin = Math.min(builder.countMin, count[0]);
        builder.countMax = Math.max(builder.countMax, count[1]);
    }

    private static String entryType(JsonObject entry) {
        return entry.has("type") && entry.get("type").isJsonPrimitive()
                ? entry.get("type").getAsString() : "";
    }

    /** Шанс прохождения ребёнка alternatives (для фактора «предыдущие не прошли»). */
    private static double childPassChance(JsonElement child) {
        return child != null && child.isJsonObject() ? allConditionsChance(child.getAsJsonObject()) : 0.0;
    }

    /**
     * P(все условия держателя проходят), условия соединены AND.
     * Случайные условия — по числам, boolean геймплейные (silk touch,
     * killed_by_player...) считаются за 1.0 и уходят в тултип.
     */
    static double allConditionsChance(JsonObject holder) {
        if (holder == null || !holder.has("conditions") || !holder.get("conditions").isJsonArray()) return 1.0;
        double chance = 1.0;
        for (JsonElement element : holder.getAsJsonArray("conditions")) {
            if (!element.isJsonObject()) continue;
            chance *= conditionChance(element.getAsJsonObject());
        }
        return chance;
    }

    /** P(одно условие проходит), рекурсивно через alternative/inverted/all_of. */
    static double conditionChance(JsonObject condition) {
        String name = condition.has("condition") && condition.get("condition").isJsonPrimitive()
                ? condition.get("condition").getAsString() : "";
        if (name.endsWith("random_chance") || name.endsWith("random_chance_with_looting")) {
            // random_chance_with_looting: базовый шанс без лутинга.
            return chanceField(condition);
        }
        if (name.endsWith("alternative") || name.endsWith("any_of")) {
            double miss = 1.0;
            boolean any = false;
            for (JsonElement term : conditionTerms(condition)) {
                if (!term.isJsonObject()) continue;
                any = true;
                miss *= 1.0 - conditionChance(term.getAsJsonObject());
            }
            return any ? 1.0 - miss : 1.0;
        }
        if (name.endsWith("inverted")) {
            JsonElement term = condition.get("term");
            return term != null && term.isJsonObject()
                    ? 1.0 - conditionChance(term.getAsJsonObject()) : 1.0;
        }
        if (name.endsWith("all_of")) {
            double chance = 1.0;
            for (JsonElement term : conditionTerms(condition)) {
                if (!term.isJsonObject()) continue;
                chance *= conditionChance(term.getAsJsonObject());
            }
            return chance;
        }
        return 1.0;
    }

    private static List<JsonElement> conditionTerms(JsonObject condition) {
        if (condition.has("terms") && condition.get("terms").isJsonArray()) {
            List<JsonElement> terms = new ArrayList<>();
            condition.getAsJsonArray("terms").forEach(terms::add);
            return terms;
        }
        return List.of();
    }

    private static double chanceField(JsonObject condition) {
        if (condition.has("chance") && condition.get("chance").isJsonPrimitive()) {
            try {
                double value = condition.get("chance").getAsDouble();
                return Math.max(0.0, Math.min(1.0, value));
            } catch (RuntimeException ignored) {
            }
        }
        return 1.0;
    }

    /** Число бросков пула: {@code rolls + bonus_rolls} (constant/uniform/binomial). */
    static RollSpec poolRolls(JsonObject pool) {
        RollSpec base = parseRolls(pool.get("rolls"), 1.0);
        double bonus = expectedValue(pool.get("bonus_rolls"), 0.0);
        if (bonus <= 0.0) return base;
        return switch (base.kind()) {
            case CONSTANT -> RollSpec.constant(base.a() + bonus);
            // Постоянный бонус сдвигает равномерное распределение целиком.
            case UNIFORM_INT -> new RollSpec(RollKind.UNIFORM_INT, base.a() + bonus, base.b() + bonus);
            // Биномиальные роллы + бонус: приближаем матожиданием как константу.
            case BINOMIAL -> RollSpec.constant(Math.round(base.a() * clamp01(base.b())) + bonus);
        };
    }

    /** Разбор number-provider'а: число | {value} | {min,max} | {n,p}. */
    static RollSpec parseRolls(JsonElement element, double fallback) {
        if (element == null || element.isJsonNull()) return RollSpec.constant(fallback);
        if (element.isJsonPrimitive()) return RollSpec.constant(asDouble(element, fallback));
        if (!element.isJsonObject()) return RollSpec.constant(fallback);
        JsonObject range = element.getAsJsonObject();
        if (range.has("value")) return RollSpec.constant(asDouble(range.get("value"), fallback));
        if (range.has("n") && range.has("p")) {
            return new RollSpec(RollKind.BINOMIAL, asDouble(range.get("n"), fallback),
                    asDouble(range.get("p"), 0.0));
        }
        if (range.has("min") && range.has("max")) {
            return new RollSpec(RollKind.UNIFORM_INT,
                    asDouble(range.get("min"), fallback), asDouble(range.get("max"), fallback));
        }
        return RollSpec.constant(fallback);
    }

    /** Матожидание number-provider'а (для bonus_rolls и прочих аддитивных величин). */
    private static double expectedValue(JsonElement element, double fallback) {
        if (element == null || element.isJsonNull()) return fallback;
        if (element.isJsonPrimitive()) return asDouble(element, fallback);
        if (!element.isJsonObject()) return fallback;
        JsonObject range = element.getAsJsonObject();
        if (range.has("value")) return asDouble(range.get("value"), fallback);
        if (range.has("n") && range.has("p")) {
            return asDouble(range.get("n"), 0.0) * asDouble(range.get("p"), 0.0);
        }
        if (range.has("min") && range.has("max")) {
            return (asDouble(range.get("min"), fallback) + asDouble(range.get("max"), fallback)) / 2.0;
        }
        return fallback;
    }

    /** P(выпасть хотя бы раз) при однобросковой вероятности {@code single}. */
    static double rollProbability(RollSpec rolls, double single) {
        double chance = clamp01(single);
        return switch (rolls.kind()) {
            case CONSTANT -> 1.0 - Math.pow(1.0 - chance, Math.max(0.0, Math.round(rolls.a())));
            case UNIFORM_INT -> {
                int low = (int) Math.max(0, Math.round(Math.min(rolls.a(), rolls.b())));
                int high = (int) Math.max(0, Math.round(Math.max(rolls.a(), rolls.b())));
                if (high - low > 4096) {
                    // Параноидальная крышка для абсурдных диапазонов: по среднему.
                    yield 1.0 - Math.pow(1.0 - chance, (low + high) / 2.0);
                }
                double sum = 0.0;
                for (int count = low; count <= high; count++) {
                    sum += 1.0 - Math.pow(1.0 - chance, count);
                }
                yield sum / (high - low + 1);
            }
            // Биномиальные роллы сворачиваются точно: 1 - (1 - p*s)^n.
            case BINOMIAL -> {
                int count = (int) Math.max(0, Math.round(rolls.a()));
                yield 1.0 - Math.pow(1.0 - clamp01(rolls.b()) * chance, count);
            }
        };
    }

    /** Вес entry в розыгрыше пула (по умолчанию 1). */
    static double entryWeight(JsonObject entry) {
        if (entry.has("weight") && entry.get("weight").isJsonPrimitive()) {
            try {
                return Math.max(0.0, entry.get("weight").getAsDouble());
            } catch (RuntimeException ignored) {
            }
        }
        return 1.0;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Запоминает человекопонятные условия держателя (entry или пула, кроме учтённых
     * в шансах числовых): Silk Touch, killed_by_player, match_tool и т.п. — они
     * раскрываются в тултипе ячейки дропа. survives_explosion тоже показываем:
     * шанс считается до взрыва.
     */
    private static void collectConditionNames(JsonObject holder, LinkedHashSet<String> out) {
        if (!holder.has("conditions") || !holder.get("conditions").isJsonArray()) return;
        for (JsonElement element : holder.getAsJsonArray("conditions")) {
            if (!element.isJsonObject()) continue;
            JsonObject condition = element.getAsJsonObject();
            String name = condition.has("condition") && condition.get("condition").isJsonPrimitive()
                    ? condition.get("condition").getAsString() : "";
            if (name.isEmpty() || name.endsWith("random_chance")
                    || name.endsWith("random_chance_with_looting")) {
                continue;
            }
            int slash = name.lastIndexOf('/');
            out.add(slash >= 0 ? name.substring(slash + 1) : name);
        }
    }

        /**
     * SNBT-тег предмета из функций entry (порядок функций соблюдается, как в ванилле):
     * {@code set_nbt} (точный тег — зелья, имена, атрибуты), {@code set_potion} (id зелья),
     * {@code set_enchantments} (точные чары с уровнями).
     *
     * <p>{@code enchant_randomly} с явным списком из одного чара детерминирован по имени
     * (уровень случаен — пишем 1): такой чар пишется как настоящий. В остальных
     * случайных случаях ({@code enchant_with_levels}, список из нескольких/без списка)
     * точные чары неизвестны — подмешивается маркер {@code isf:random} ради foil-блика.
     * Неизвестные id ванилла молча пропускает в тултипе (ifPresent), блик остаётся,
     * а строка «случайно зачаровано» добавляется отдельно через
     * {@link #RANDOM_ENCHANT_CONDITION}. Подделывать реальные чары/уровни нельзя —
     * тултип тогда врал бы.</p>
     *
     * @param displayId отображаемый id (книга после конверсии — уже enchanted_book)
     * @return SNBT без обёртки или "" (обычный предмет без NBT)
     */
    static String itemNbtSnbt(JsonObject entry, String displayId) {
        if (!entry.has("functions") || !entry.get("functions").isJsonArray()) return "";
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        boolean touched = false;
        boolean unpinnedRandom = false;
        LinkedHashSet<String> pinned = new LinkedHashSet<>();
        boolean isBook = ENCHANTED_BOOK_ID.equals(displayId);
        for (JsonElement element : entry.getAsJsonArray("functions")) {
            if (!element.isJsonObject()) continue;
            JsonObject function = element.getAsJsonObject();
            String name = function.has("function") && function.get("function").isJsonPrimitive()
                    ? function.get("function").getAsString() : "";
            if (name.endsWith("set_nbt")) {
                net.minecraft.nbt.CompoundTag parsed = parseSnbtTag(function.get("tag"));
                if (parsed != null) {
                    tag.merge(parsed);
                    touched = true;
                }
            } else if (name.endsWith("set_potion")) {
                JsonElement id = function.get("id");
                if (id != null && id.isJsonPrimitive() && !id.getAsString().isBlank()) {
                    tag.putString("Potion", id.getAsString());
                    touched = true;
                }
            } else if (name.endsWith("set_enchantments")) {
                if (applySetEnchantments(tag, function, isBook)) touched = true;
            } else if (name.endsWith("enchant_randomly")) {
                List<String> listed = listedRandomEnchants(function);
                if (listed.size() == 1) {
                    pinned.add(listed.get(0));
                } else {
                    unpinnedRandom = true;
                }
            } else if (name.endsWith("enchant_with_levels")) {
                unpinnedRandom = true;
            }
        }
        if (!pinned.isEmpty()) {
            net.minecraft.nbt.ListTag list = enchantList(tag, isBook);
            for (String id : pinned) addEnchantEntry(list, id, 1);
            tag.put(isBook ? "StoredEnchantments" : "Enchantments", list);
            touched = true;
        }
        if (unpinnedRandom && !hasAnyEnchantments(tag)) {
            putEnchantMarker(tag, isBook);
            touched = true;
        }
        return touched ? tag.getAsString() : "";
    }

    /** Явный список чар из {@code enchant_randomly}: [{enchantment: id}, ...]. */
    private static List<String> listedRandomEnchants(JsonObject function) {
        if (!function.has("enchantments") || !function.get("enchantments").isJsonArray()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (JsonElement element : function.getAsJsonArray("enchantments")) {
            if (!element.isJsonObject()) continue;
            JsonElement id = element.getAsJsonObject().get("enchantment");
            if (id != null && id.isJsonPrimitive() && !id.getAsString().isBlank()) {
                ids.add(id.getAsString());
            }
        }
        return List.copyOf(ids);
    }

    /**
     * Есть ли у entry недетерминированная часть случайных чар (список из
     * нескольких/без списка, {@code enchant_with_levels}) — нужна строка в тултипе.
     */
    private static boolean hasUnpinnedRandomEnchant(JsonObject entry) {
        if (!entry.has("functions") || !entry.get("functions").isJsonArray()) return false;
        for (JsonElement element : entry.getAsJsonArray("functions")) {
            if (!element.isJsonObject()) continue;
            JsonObject function = element.getAsJsonObject();
            String name = function.has("function") && function.get("function").isJsonPrimitive()
                    ? function.get("function").getAsString() : "";
            if (name.endsWith("enchant_with_levels")) return true;
            if (name.endsWith("enchant_randomly") && listedRandomEnchants(function).size() != 1) {
                return true;
            }
        }
        return false;
    }

    /** Конвертирует ли хоть одна функция обычную книгу в зачарованную (как ванилла). */
    private static boolean hasBookConvertingFunction(JsonObject entry) {
        if (!entry.has("functions") || !entry.get("functions").isJsonArray()) return false;
        for (JsonElement element : entry.getAsJsonArray("functions")) {
            if (!element.isJsonObject()) continue;
            JsonObject function = element.getAsJsonObject();
            String name = function.has("function") && function.get("function").isJsonPrimitive()
                    ? function.get("function").getAsString() : "";
            if (name.endsWith("enchant_randomly") || name.endsWith("enchant_with_levels")
                    || name.endsWith("set_enchantments")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyEnchantments(net.minecraft.nbt.CompoundTag tag) {
        return tag.contains("Enchantments") || tag.contains("StoredEnchantments");
    }

    /** Маркер случайных чар: foil-блик есть, в тултипе неизвестный id пропускается. */
    private static void putEnchantMarker(net.minecraft.nbt.CompoundTag tag, boolean isBook) {
        net.minecraft.nbt.CompoundTag marker = new net.minecraft.nbt.CompoundTag();
        marker.putString("id", RANDOM_ENCHANT_ID);
        marker.putShort("lvl", (short) 1);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        list.add(marker);
        tag.put(isBook ? "StoredEnchantments" : "Enchantments", list);
    }

    /**
     * Точные чары из {@code set_enchantments}: {id: уровень|провайдер}.
     * Уровни-провайдеры сворачиваются матожиданием (точные числа — как есть).
     * {@code add: false} заменяет список, {@code add: true} дописывает по id.
     */
    private static boolean applySetEnchantments(net.minecraft.nbt.CompoundTag tag, JsonObject function,
                                                boolean isBook) {
        JsonElement map = function.get("enchantments");
        if (map == null || !map.isJsonObject()) return false;
        boolean add = function.has("add") && function.get("add").isJsonPrimitive()
                && function.get("add").getAsBoolean();
        String key = isBook ? "StoredEnchantments" : "Enchantments";
        net.minecraft.nbt.ListTag list = add ? enchantList(tag, isBook)
                : new net.minecraft.nbt.ListTag();
        for (Map.Entry<String, JsonElement> enchant : map.getAsJsonObject().entrySet()) {
            if (enchant.getKey() == null || enchant.getKey().isBlank()) continue;
            int level = (int) Math.round(expectedValue(enchant.getValue(), 1.0));
            if (level <= 0) continue;
            addEnchantEntry(list, enchant.getKey(), level);
        }
        tag.put(key, list);
        return true;
    }

    /** Живой список чар тега (создаёт при отсутствии). */
    private static net.minecraft.nbt.ListTag enchantList(net.minecraft.nbt.CompoundTag tag, boolean isBook) {
        String key = isBook ? "StoredEnchantments" : "Enchantments";
        if (tag.contains(key, 9)) return tag.getList(key, 10);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        tag.put(key, list);
        return list;
    }

    /** Добавляет чар в список с дедупликацией по id (как merge ваниллы). */
    private static void addEnchantEntry(net.minecraft.nbt.ListTag list, String id, int level) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (id.equals(list.getCompound(i).getString("id"))) list.remove(i);
        }
        net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
        entry.putString("id", id);
        entry.putShort("lvl", (short) Math.min(level, Short.MAX_VALUE));
        list.add(entry);
    }

    /** Разбор SNBT из {@code set_nbt}: битый тег игнорируется (таблица ваниллы бы упала). */
    private static net.minecraft.nbt.CompoundTag parseSnbtTag(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return net.minecraft.nbt.TagParser.parseTag(element.getAsString());
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException | RuntimeException ignored) {
            return null;
        }
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
            if (!drop.nbt().isEmpty()) cell.addProperty("nbt", drop.nbt());
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
                "entity_size", literal(36),
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
