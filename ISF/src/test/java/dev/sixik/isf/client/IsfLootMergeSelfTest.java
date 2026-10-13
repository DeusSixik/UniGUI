package dev.sixik.isf.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import dev.sixik.isf.definition.IsfSourceReference;
import dev.sixik.isf.definition.IsfTriggerBinding;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Проверка мержа одинаковых лут-таблиц в один экран (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfLootMergeSelfTest}.</p>
 */
public final class IsfLootMergeSelfTest {
    private static final ResourceLocation LOOT_TYPE = ResourceLocation.tryParse("isf:loot_table");

    public static void main(String[] args) {
        JsonArray drops = drops("[{\"ids\":[\"minecraft:torch\"],\"chance\":0.5,"
                + "\"count_min\":1.0,\"count_max\":3.0}]");

        // Четыре сундука с одинаковым дропом — один merged-рецепт с четырьмя источниками.
        IsfRecipeDefinition first = loot("one", "mymod:chest_a", drops);
        List<IsfRecipeDefinition> merged = IsfBrowserOverlay.mergeLootRecipes(List.of(
                first,
                loot("two", "mymod:chest_b", drops),
                loot("three", "mymod:chest_c", drops),
                loot("four", "mymod:chest_d", drops)));
        check(merged.size() == 1, "identical drops merged into one, got " + merged.size());
        IsfRecipeDefinition single = merged.get(0);
        check(single.visual() != null, "merged recipe has synthetic visual");
        JsonElement items = single.parameters().get("source_items");
        check(items != null && items.isJsonArray() && items.getAsJsonArray().size() == 4,
                "all four sources listed");
        check(single.parameters().get("source_item").getAsString().equals("mymod:chest_a"),
                "first source kept for compatibility");
        check(single.parameters().get("drops").equals(drops), "drops preserved");
        check(!single.id().equals(first.id()), "merged id differs from member id");

        // Разный дроп — отдельные страницы в исходном порядке.
        IsfRecipeDefinition other = loot("other", "mymod:chest_e",
                drops("[{\"ids\":[\"minecraft:stick\"],\"chance\":1.0,"
                        + "\"count_min\":1.0,\"count_max\":1.0}]"));
        List<IsfRecipeDefinition> split =
                IsfBrowserOverlay.mergeLootRecipes(List.of(first, other));
        check(split.size() == 2 && split.get(0) == first && split.get(1) == other,
                "different drops stay separate in order");

        // Таблицы сущностей (source_item — воздух) не мержатся даже с тем же дропом.
        IsfRecipeDefinition mob = loot("mob", "minecraft:air", drops);
        List<IsfRecipeDefinition> withMob =
                IsfBrowserOverlay.mergeLootRecipes(List.of(first, loot("two", "mymod:chest_b", drops), mob));
        check(withMob.size() == 2, "entity table stays single, got " + withMob.size());
        check(withMob.get(1) == mob, "entity table passes through");

        // Рецепт без drops — одиночка, null/короткие списки безопасны.
        IsfRecipeDefinition noDrops = new IsfRecipeDefinition(
                ResourceLocation.tryParse("isf:loot/mymod/broken"), LOOT_TYPE, null,
                Map.of("source_item", new JsonPrimitive("mymod:chest_x")),
                List.of(), null, null);
        List<IsfRecipeDefinition> withBroken =
                IsfBrowserOverlay.mergeLootRecipes(List.of(first, noDrops));
        check(withBroken.size() == 2 && withBroken.get(1) == noDrops, "drops-less recipe single");
        check(IsfBrowserOverlay.mergeLootRecipes(null).isEmpty(), "null safe");
        check(IsfBrowserOverlay.mergeLootRecipes(List.of(first)).size() == 1, "singleton safe");

        System.out.println("IsfLootMergeSelfTest passed");
    }

    private static IsfRecipeDefinition loot(String name, String source, JsonArray drops) {
        Map<String, JsonElement> parameters = new LinkedHashMap<>();
        parameters.put("source_item", new JsonPrimitive(source));
        parameters.put("table_id", new JsonPrimitive("mymod:chests/" + name));
        parameters.put("drops", drops);
        return new IsfRecipeDefinition(
                ResourceLocation.tryParse("isf:loot/mymod/" + name), LOOT_TYPE, null,
                Map.copyOf(parameters),
                List.of(new IsfTriggerBinding(
                        ResourceLocation.tryParse("isf:craft"),
                        ResourceLocation.tryParse("minecraft:torch"), null, Map.of())),
                null,
                new IsfSourceReference("loot_table",
                        ResourceLocation.tryParse("mymod:chests/" + name)));
    }

    private static JsonArray drops(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
