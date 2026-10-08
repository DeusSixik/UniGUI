package dev.sixik.isf.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Проверка математики шансов лут-таблиц без запуска Minecraft.
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfLootChanceSelfTest}.</p>
 */
public final class LootChanceSelfTest {
    private static final double EPSILON = 1e-9;

    public static void main(String[] args) {
        new LootChanceSelfTest().run();
        System.out.println("LootChanceSelfTest passed");
    }

    private void run() {
        // Веса: 1 к 3 при одном ролле.
        checkChance(table(pool(1, entry("a", 1), entry("b", 3))), "a", 0.25, "weights 1:3 (a)");
        checkChance(table(pool(1, entry("a", 1), entry("b", 3))), "b", 0.75, "weights 1:3 (b)");

        // random_chance умножается на долю веса (железо зомби: 1/2 * 0.01).
        checkChance(table(pool(1, entry("flesh", 1), chanceEntry("iron", 1, 0.01))), "iron", 0.005,
                "weight share times random_chance");

        // Роллы диапазоном: крипер (0-2 пороха), эндермен (0-1 жемчуг).
        checkChance(table(pool(rangeObject(0, 2), entry("gunpowder", 1))), "gunpowder", 2.0 / 3.0,
                "uniform rolls 0-2");
        checkChance(table(pool(rangeObject(0, 1), entry("pearl", 1))), "pearl", 0.5, "uniform rolls 0-1");

        // Условие пула умножает все entry.
        checkChance(table(poolWithCondition(1, chanceCond(0.5), entry("bone", 1))), "bone", 0.5,
                "pool-level random_chance");

        // Alternatives: первый проходящий забирает ролл.
        JsonObject alternatives = composite("minecraft:alternatives",
                chanceEntry("egg_a", 1, 0.5), entry("egg_b", 1));
        checkChance(table(singleEntryPool(1, alternatives)), "egg_a", 0.5, "alternatives first");
        checkChance(table(singleEntryPool(1, alternatives)), "egg_b", 0.5, "alternatives second");

        // Пустое entry съедает роллы своим весом.
        checkChance(table(pool(1, entry("diamond", 1), emptyEntry(3))), "diamond", 0.25,
                "empty entry dilutes weight");

        // Инверсия шанса.
        JsonObject inverted = new JsonObject();
        inverted.addProperty("condition", "minecraft:inverted");
        JsonObject inner = new JsonObject();
        inner.addProperty("condition", "minecraft:random_chance");
        inner.addProperty("chance", 0.25);
        inverted.add("term", inner);
        checkChance(table(poolWithCondition(1, inverted, entry("stick", 1))), "stick", 0.75,
                "inverted random_chance");

        // Биномиальные роллы {n:3, p:0.5} при гарантии выбора: 1 - 0.5^3.
        checkChance(table(pool(binomial(3, 0.5), entry("coal", 1))), "coal", 0.875,
                "binomial rolls");

        // Один и тот же предмет из двух пулов комбинируется как независимые попытки.
        JsonObject twoPools = new JsonObject();
        JsonArray pools = new JsonArray();
        pools.add(poolWithCondition(1, chanceCond(0.5), entry("apple", 1)));
        pools.add(poolWithCondition(1, chanceCond(0.5), entry("apple", 1)));
        twoPools.add("pools", pools);
        checkChance(twoPools, "apple", 0.75, "merge across pools");

        // Группа: все дети бросаются полностью.
        JsonObject group = composite("minecraft:group", entry("x", 1), entry("y", 1));
        checkChance(table(singleEntryPool(1, group)), "x", 1.0, "group first");
        checkChance(table(singleEntryPool(1, group)), "y", 1.0, "group second");

        // random_chance_with_looting считается по базовому шансу.
        checkChance(table(pool(1, lootingEntry("skull", 1, 0.2))), "skull", 0.2,
                "random_chance_with_looting base");

        // Геймплейные условия шанс не режут, но запоминаются для тултипа.
        JsonObject killed = new JsonObject();
        killed.addProperty("condition", "minecraft:killed_by_player");
        List<LootTableSupports.Drop> drops = LootTableSupports.parsePools(
                table(poolWithCondition(1, killed, entry("eye", 1))));
        checkDouble(dropChance(drops, "eye"), 1.0, "killed_by_player keeps chance");
        check(dropConditions(drops, "eye").contains("minecraft:killed_by_player"),
                "killed_by_player recorded for tooltip");

        // Ветка tag-entry пишет свой id в поле "tag" ячейки (тултип «Принимает»).
        JsonObject tagCell = LootTableSupports.dropCell(new LootTableSupports.Drop(
                List.of("minecraft:oak_planks", "minecraft:spruce_planks"), 0.5, 1.0, 2.0,
                List.of(), "", "minecraft:planks"));
        check(tagCell.has("tag")
                        && tagCell.get("tag").getAsString().equals("minecraft:planks"),
                "tag recorded in drop cell");
        JsonObject plainCell = LootTableSupports.dropCell(new LootTableSupports.Drop(
                List.of("minecraft:stick"), 1.0, 1.0, 1.0, List.of(), "", null));
        check(!plainCell.has("tag"), "plain drop has no tag");

        // Низкоуровневые единицы.
        checkDouble(LootTableSupports.rollProbability(
                LootTableSupports.parseRolls(rangeObject(0, 2), 1.0), 1.0), 2.0 / 3.0,
                "rollProbability uniform");
        checkDouble(LootTableSupports.entryWeight(entry("w", 5)), 5.0, "entryWeight");
        checkDouble(LootTableSupports.conditionChance(chanceCond(0.3)), 0.3, "conditionChance");

        runNbt();
        runMatchTool();
    }

    private void runMatchTool() {
        // Шёлк в предикате чар — точное условие.
        check(LootTableSupports.matchToolNames(matchTool(silkPredicate(1))).equals(List.of("silk_touch")),
                "match_tool silk levels number");
        JsonObject silkRange = new JsonObject();
        silkRange.addProperty("enchantment", "minecraft:silk_touch");
        JsonObject levels = new JsonObject();
        levels.addProperty("min", 1);
        levels.addProperty("max", 1);
        silkRange.add("levels", levels);
        check(LootTableSupports.matchToolNames(matchTool(silkPredicate(silkRange)))
                        .equals(List.of("silk_touch")),
                "match_tool silk levels range");

        // Ножницы предметом — точное условие (строкой и массивом).
        check(LootTableSupports.matchToolNames(matchTool(itemsPredicate("minecraft:shears")))
                        .equals(List.of("shears")),
                "match_tool shears string");
        check(LootTableSupports.matchToolNames(matchTool(itemsPredicate(
                        "minecraft:stone_pickaxe", "minecraft:shears")))
                        .equals(List.of("shears")),
                "match_tool shears array");

        // Чужой предикат и отсутствие предиката — общее условие.
        check(LootTableSupports.matchToolNames(matchTool(fortunePredicate()))
                        .equals(List.of("match_tool")),
                "match_tool other enchantment");
        JsonObject bare = new JsonObject();
        bare.addProperty("condition", "minecraft:match_tool");
        check(LootTableSupports.matchToolNames(bare).equals(List.of("match_tool")),
                "match_tool without predicate");

        // Сквозной кейс железной руды: руда только с шёлком, сырое железо всегда.
        JsonObject orePool = poolWithCondition(1, matchTool(silkPredicate(1)), entry("minecraft:iron_ore", 1));
        JsonObject rawPool = pool(1, entry("minecraft:raw_iron", 1));
        JsonObject root = new JsonObject();
        JsonArray pools = new JsonArray();
        pools.add(orePool);
        pools.add(rawPool);
        root.add("pools", pools);
        List<LootTableSupports.Drop> drops = LootTableSupports.parsePools(root);
        checkDouble(dropChance(drops, "minecraft:iron_ore"), 1.0, "ore pool chance");
        checkDouble(dropChance(drops, "minecraft:raw_iron"), 1.0, "raw pool chance");
        check(dropConditions(drops, "minecraft:iron_ore").contains("silk_touch"),
                "ore gated by silk_touch");
        check(!dropConditions(drops, "minecraft:raw_iron").contains("silk_touch"),
                "raw iron unconditional");

        // Настоящая структура руды: один пул, alternatives [шёлк-руда, сырьё].
        // Boolean-гейт НЕ должен гасить вторую ветку (сырьё пропадало вообще).
        JsonObject silkOre = entry("minecraft:iron_ore", 1);
        JsonArray silkConds = new JsonArray();
        silkConds.add(matchTool(silkPredicate(1)));
        silkOre.add("conditions", silkConds);
        JsonObject oreAlternatives = composite("minecraft:alternatives",
                silkOre, entry("minecraft:raw_iron", 1));
        List<LootTableSupports.Drop> altDrops = LootTableSupports.parsePools(
                table(singleEntryPool(1, oreAlternatives)));
        checkDouble(dropChance(altDrops, "minecraft:iron_ore"), 1.0, "alternatives ore branch");
        checkDouble(dropChance(altDrops, "minecraft:raw_iron"), 1.0, "alternatives raw branch kept");
        check(dropConditions(altDrops, "minecraft:iron_ore").contains("silk_touch"),
                "alternatives ore silk tooltip");

        // Смешанный гейт (шёлк + случайность): вторая ветка режется только роллом.
        JsonObject mixedFirst = chanceEntry("minecraft:gold_ore", 1, 0.5);
        JsonArray mixedConds = mixedFirst.getAsJsonArray("conditions");
        mixedConds.add(matchTool(silkPredicate(1)));
        JsonObject goldAlternatives = composite("minecraft:alternatives",
                mixedFirst, entry("minecraft:raw_gold", 1));
        List<LootTableSupports.Drop> mixedDrops = LootTableSupports.parsePools(
                table(singleEntryPool(1, goldAlternatives)));
        checkDouble(dropChance(mixedDrops, "minecraft:raw_gold"), 0.5, "mixed gate discount");

        runGenerationControl();
    }

    private void runGenerationControl() {
        // Id рецепта зеркалит таблицу.
        check(LootTableSupports.lootRecipeId(id("minecraft:entities/zombie")).toString()
                        .equals("isf:loot/minecraft/entities/zombie"),
                "loot recipe id mirrors table");

        // Разбор loot_disabled.json: валидный, пустой, битый.
        JsonObject disabledFile = new JsonObject();
        JsonArray disabledList = new JsonArray();
        disabledList.add("minecraft:entities/zombie");
        disabledList.add("not an id!!!");
        disabledList.add(new JsonObject());
        disabledFile.add("disabled", disabledList);
        check(LootTableSupports.parseDisabledTables(disabledFile)
                        .equals(java.util.Set.of(id("minecraft:entities/zombie"))),
                "disabled tables parsed, garbage skipped");
        check(LootTableSupports.parseDisabledTables(new JsonObject()).isEmpty(),
                "missing disabled key means empty");
        check(LootTableSupports.parseDisabledTables(null).isEmpty(), "null json means empty");
        check(LootTableSupports.parseDisabledTables(new JsonPrimitive("nope")).isEmpty(),
                "non-object json means empty");

        // lootTableId вытаскивает таблицу из source рецепта (для фильтра disabled).
        dev.sixik.isf.definition.IsfRecipeDefinition zombie = new dev.sixik.isf.definition.IsfRecipeDefinition(
                LootTableSupports.lootRecipeId(id("minecraft:entities/zombie")),
                LootTableSupports.LOOT_TYPE_ID,
                null,
                java.util.Map.of(),
                java.util.List.of(),
                null,
                new dev.sixik.isf.definition.IsfSourceReference("loot_table",
                        id("minecraft:entities/zombie")));
        check(id("minecraft:entities/zombie").equals(LootTableSupports.lootTableId(zombie)),
                "loot table id from recipe source");
        check(LootTableSupports.lootTableId(null) == null, "loot table id null-safe");
    }

    private static ResourceLocation id(String value) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) throw new AssertionError("Bad test id: " + value);
        return parsed;
    }

    private static JsonObject matchTool(JsonObject predicate) {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:match_tool");
        if (predicate != null) condition.add("predicate", predicate);
        return condition;
    }

    private static JsonObject silkPredicate(int levels) {
        JsonObject enchant = new JsonObject();
        enchant.addProperty("enchantment", "minecraft:silk_touch");
        enchant.addProperty("levels", levels);
        return enchantmentsPredicate(enchant);
    }

    private static JsonObject silkPredicate(JsonObject levels) {
        JsonObject enchant = new JsonObject();
        enchant.addProperty("enchantment", "minecraft:silk_touch");
        enchant.add("levels", levels);
        return enchantmentsPredicate(enchant);
    }

    private static JsonObject fortunePredicate() {
        JsonObject enchant = new JsonObject();
        enchant.addProperty("enchantment", "minecraft:fortune");
        enchant.addProperty("levels", 3);
        return enchantmentsPredicate(enchant);
    }

    private static JsonObject enchantmentsPredicate(JsonObject... enchants) {
        JsonObject predicate = new JsonObject();
        JsonArray array = new JsonArray();
        for (JsonObject enchant : enchants) array.add(enchant);
        predicate.add("enchantments", array);
        return predicate;
    }

    private static JsonObject itemsPredicate(String... ids) {
        JsonObject predicate = new JsonObject();
        if (ids.length == 1) {
            predicate.addProperty("items", ids[0]);
        } else {
            JsonArray array = new JsonArray();
            for (String id : ids) array.add(id);
            predicate.add("items", array);
        }
        return predicate;
    }

    private void runNbt() {
        // set_nbt проходит как есть (зелья: цвет/название/эффекты подтянет ванилла).
        JsonObject potion = withFunctions(entry("minecraft:potion", 1),
                setNbt("{Potion:\"minecraft:strong_healing\"}"));
        net.minecraft.nbt.CompoundTag potionTag = parseSnbt(
                LootTableSupports.itemNbtSnbt(potion, "minecraft:potion"));
        check(potionTag != null && potionTag.getString("Potion").equals("minecraft:strong_healing"),
                "set_nbt potion passthrough");

        // set_potion сворачивается в тот же Potion-тег.
        JsonObject poison = withFunctions(entry("minecraft:potion", 1), setPotion("minecraft:poison"));
        net.minecraft.nbt.CompoundTag poisonTag = parseSnbt(
                LootTableSupports.itemNbtSnbt(poison, "minecraft:potion"));
        check(poisonTag != null && poisonTag.getString("Potion").equals("minecraft:poison"),
                "set_potion id");

        // Точные чары из set_enchantments (id и уровень как в таблице).
        JsonObject sword = withFunctions(entry("minecraft:diamond_sword", 1),
                setEnchantments(false, "minecraft:sharpness", 3));
        net.minecraft.nbt.CompoundTag swordTag = parseSnbt(
                LootTableSupports.itemNbtSnbt(sword, "minecraft:diamond_sword"));
        check(swordTag != null && hasEnchant(swordTag, "Enchantments", "minecraft:sharpness", 3),
                "set_enchantments exact");

        // add: true дописывает к уже имеющимся (из set_nbt), а не затирает.
        JsonObject merged = withFunctions(entry("minecraft:bow", 1),
                setNbt("{Enchantments:[{id:\"minecraft:power\",lvl:2s}]}"),
                setEnchantments(true, "minecraft:punch", 1));
        net.minecraft.nbt.CompoundTag mergedTag = parseSnbt(
                LootTableSupports.itemNbtSnbt(merged, "minecraft:bow"));
        check(mergedTag != null && hasEnchant(mergedTag, "Enchantments", "minecraft:power", 2)
                        && hasEnchant(mergedTag, "Enchantments", "minecraft:punch", 1),
                "set_enchantments add merges");

        // Случайные чары: маркер ради блика (снаряжение — Enchantments, книга — Stored).
        JsonObject rod = withFunctions(entry("minecraft:fishing_rod", 1), randomEnchant());
        net.minecraft.nbt.CompoundTag rodTag = parseSnbt(
                LootTableSupports.itemNbtSnbt(rod, "minecraft:fishing_rod"));
        check(rodTag != null && hasEnchant(rodTag, "Enchantments", "isf:random", 1),
                "enchant_randomly marker on gear");
        JsonObject book = withFunctions(entry("minecraft:enchanted_book", 1), randomEnchant());
        net.minecraft.nbt.CompoundTag bookTag = parseSnbt(
                LootTableSupports.itemNbtSnbt(book, "minecraft:enchanted_book"));
        check(bookTag != null && hasEnchant(bookTag, "StoredEnchantments", "isf:random", 1),
                "enchant_randomly marker on book");

        // Без функций — пусто (обычный предмет).
        check(LootTableSupports.itemNbtSnbt(entry("minecraft:stick", 1), "minecraft:stick").isEmpty(),
                "no functions means no nbt");

        // Битый SNBT игнорируется, остальное синтезируется.
        JsonObject broken = withFunctions(entry("minecraft:stick", 1),
                setNbt("{broken:::}"), setPotion("minecraft:healing"));
        net.minecraft.nbt.CompoundTag brokenTag = parseSnbt(
                LootTableSupports.itemNbtSnbt(broken, "minecraft:stick"));
        check(brokenTag != null && brokenTag.getString("Potion").equals("minecraft:healing"),
                "broken set_nbt ignored, set_potion kept");

        // Один id с разным NBT — разные ячейки (два разных зелья не сливаются).
        JsonObject twoPotions = new JsonObject();
        JsonArray pools = new JsonArray();
        JsonArray entries = new JsonArray();
        entries.add(withFunctions(entry("minecraft:potion", 1), setPotion("minecraft:healing")));
        entries.add(withFunctions(entry("minecraft:potion", 1), setPotion("minecraft:harming")));
        JsonObject pool = new JsonObject();
        pool.add("rolls", new JsonPrimitive(1));
        pool.add("entries", entries);
        pools.add(pool);
        twoPotions.add("pools", pools);
        List<LootTableSupports.Drop> potionDrops = LootTableSupports.parsePools(twoPotions);
        check(potionDrops.size() == 2, "same id different nbt splits cells");
        check(!potionDrops.get(0).nbt().equals(potionDrops.get(1).nbt()),
                "potion cells keep distinct nbt");

        // Обычная книга со случайными чарами превращается в зачарованную (как ванилла).
        List<LootTableSupports.Drop> bookDrops = LootTableSupports.parsePools(
                table(pool(1, withFunctions(entry("minecraft:book", 1), randomEnchant()))));
        check(bookDrops.size() == 1 && bookDrops.get(0).ids().contains("minecraft:enchanted_book"),
                "book converts to enchanted_book");
        check(hasEnchant(parseSnbt(bookDrops.get(0).nbt()), "StoredEnchantments", "isf:random", 1),
                "converted book keeps marker in stored");
        check(bookDrops.get(0).conditions().contains("isf:random_enchant"),
                "converted book keeps caption");

        // Обычная книга без функций остаётся книгой.
        List<LootTableSupports.Drop> plainBook = LootTableSupports.parsePools(
                table(pool(1, entry("minecraft:book", 1))));
        check(plainBook.size() == 1 && plainBook.get(0).ids().contains("minecraft:book")
                        && plainBook.get(0).nbt().isEmpty(),
                "plain book untouched");

        // Явный список из одного чара — настоящий чар без подписи.
        JsonObject pinned = new JsonObject();
        pinned.addProperty("function", "minecraft:enchant_randomly");
        JsonArray pinnedList = new JsonArray();
        JsonObject pinnedEntry = new JsonObject();
        pinnedEntry.addProperty("enchantment", "minecraft:sharpness");
        pinnedList.add(pinnedEntry);
        pinned.add("enchantments", pinnedList);
        List<LootTableSupports.Drop> pinnedDrops = LootTableSupports.parsePools(
                table(pool(1, withFunctions(entry("minecraft:diamond_sword", 1), pinned))));
        check(pinnedDrops.size() == 1
                        && hasEnchant(parseSnbt(pinnedDrops.get(0).nbt()),
                                "Enchantments", "minecraft:sharpness", 1),
                "single listed enchant applied");
        check(!pinnedDrops.get(0).conditions().contains("isf:random_enchant"),
                "single listed enchant needs no caption");

        // Список из нескольких — всё ещё ролл: маркер + подпись.
        JsonObject multi = new JsonObject();
        multi.addProperty("function", "minecraft:enchant_randomly");
        JsonArray multiList = new JsonArray();
        JsonObject first = new JsonObject();
        first.addProperty("enchantment", "minecraft:sharpness");
        JsonObject second = new JsonObject();
        second.addProperty("enchantment", "minecraft:smite");
        multiList.add(first);
        multiList.add(second);
        multi.add("enchantments", multiList);
        List<LootTableSupports.Drop> multiDrops = LootTableSupports.parsePools(
                table(pool(1, withFunctions(entry("minecraft:diamond_sword", 1), multi))));
        check(multiDrops.size() == 1
                        && hasEnchant(parseSnbt(multiDrops.get(0).nbt()),
                                "Enchantments", "isf:random", 1),
                "multi listed enchants keep marker");
        check(multiDrops.get(0).conditions().contains("isf:random_enchant"),
                "multi listed enchants keep caption");
    }

    private static net.minecraft.nbt.CompoundTag parseSnbt(String snbt) {
        if (snbt == null || snbt.isEmpty()) return null;
        try {
            return net.minecraft.nbt.TagParser.parseTag(snbt);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean hasEnchant(net.minecraft.nbt.CompoundTag tag, String key,
                                      String id, int level) {
        if (!tag.contains(key, 9)) return false;
        net.minecraft.nbt.ListTag list = tag.getList(key, 10);
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.nbt.CompoundTag enchant = list.getCompound(i);
            if (id.equals(enchant.getString("id")) && enchant.getShort("lvl") == level) return true;
        }
        return false;
    }

    private static JsonObject withFunctions(JsonObject entry, JsonObject... functions) {
        JsonArray array = new JsonArray();
        for (JsonObject function : functions) array.add(function);
        entry.add("functions", array);
        return entry;
    }

    private static JsonObject setNbt(String snbt) {
        JsonObject function = new JsonObject();
        function.addProperty("function", "minecraft:set_nbt");
        function.addProperty("tag", snbt);
        return function;
    }

    private static JsonObject setPotion(String id) {
        JsonObject function = new JsonObject();
        function.addProperty("function", "minecraft:set_potion");
        function.addProperty("id", id);
        return function;
    }

    private static JsonObject setEnchantments(boolean add, String id, int level) {
        JsonObject levels = new JsonObject();
        levels.addProperty(id, level);
        JsonObject function = new JsonObject();
        function.addProperty("function", "minecraft:set_enchantments");
        function.add("enchantments", levels);
        function.addProperty("add", add);
        return function;
    }

    private static JsonObject randomEnchant() {
        JsonObject function = new JsonObject();
        function.addProperty("function", "minecraft:enchant_randomly");
        return function;
    }

    // ------------------------------------------------------------------
    // Конструктор тестовых таблиц.
    // ------------------------------------------------------------------

    private static JsonObject table(JsonObject... pools) {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (JsonObject pool : pools) array.add(pool);
        root.add("pools", array);
        return root;
    }

    private static JsonObject pool(double rolls, JsonObject... entries) {
        return pool(new JsonPrimitive(rolls), entries);
    }

    private static JsonObject pool(JsonElement rolls, JsonObject... entries) {
        JsonObject pool = new JsonObject();
        pool.add("rolls", rolls);
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) array.add(entry);
        pool.add("entries", array);
        return pool;
    }

    private static JsonObject singleEntryPool(double rolls, JsonObject entry) {
        return pool(new JsonPrimitive(rolls), entry);
    }

    private static JsonObject singleEntryPool(JsonElement rolls, JsonObject entry) {
        return pool(rolls, entry);
    }

    private static JsonObject poolWithCondition(double rolls, JsonObject condition, JsonObject... entries) {
        return poolWithCondition(new JsonPrimitive(rolls), condition, entries);
    }

    private static JsonObject poolWithCondition(JsonElement rolls, JsonObject condition, JsonObject... entries) {
        JsonObject pool = pool(rolls, entries);
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        pool.add("conditions", conditions);
        return pool;
    }

    private static JsonObject entry(String name, double weight) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", name);
        entry.addProperty("weight", weight);
        return entry;
    }

    private static JsonObject chanceEntry(String name, double weight, double chance) {
        JsonObject entry = entry(name, weight);
        JsonArray conditions = new JsonArray();
        conditions.add(chanceCond(chance));
        entry.add("conditions", conditions);
        return entry;
    }

    private static JsonObject lootingEntry(String name, double weight, double chance) {
        JsonObject entry = entry(name, weight);
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:random_chance_with_looting");
        condition.addProperty("chance", chance);
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        entry.add("conditions", conditions);
        return entry;
    }

    private static JsonObject emptyEntry(double weight) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:empty");
        entry.addProperty("weight", weight);
        return entry;
    }

    private static JsonObject composite(String type, JsonObject... children) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", type);
        entry.addProperty("weight", 1);
        JsonArray array = new JsonArray();
        for (JsonObject child : children) array.add(child);
        entry.add("children", array);
        return entry;
    }

    private static JsonObject chanceCond(double chance) {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:random_chance");
        condition.addProperty("chance", chance);
        return condition;
    }

    private static JsonObject rangeObject(int min, int max) {
        JsonObject range = new JsonObject();
        range.addProperty("min", min);
        range.addProperty("max", max);
        return range;
    }

    private static JsonObject binomial(int count, double probability) {
        JsonObject range = new JsonObject();
        range.addProperty("n", count);
        range.addProperty("p", probability);
        return range;
    }

    // ------------------------------------------------------------------
    // Проверки.
    // ------------------------------------------------------------------

    private void checkChance(JsonObject root, String id, double expected, String message) {
        checkDouble(dropChance(LootTableSupports.parsePools(root), id), expected, message);
    }

    private static double dropChance(List<LootTableSupports.Drop> drops, String id) {
        for (LootTableSupports.Drop drop : drops) {
            if (drop.ids().contains(id)) return drop.chance();
        }
        throw new AssertionError("No drop for " + id);
    }

    private static List<String> dropConditions(List<LootTableSupports.Drop> drops, String id) {
        for (LootTableSupports.Drop drop : drops) {
            if (drop.ids().contains(id)) return drop.conditions();
        }
        throw new AssertionError("No drop for " + id);
    }

    private static void checkDouble(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > EPSILON) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
