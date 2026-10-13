package dev.sixik.isf.client;

import dev.sixik.isf.definition.IsfRecipeDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Проверка детерминированного порядка вкладок/рецептов без запуска Minecraft.
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfClientOrderSelfTest}.</p>
 */
public final class IsfClientOrderSelfTest {
    public static void main(String[] args) {
        // Порядок рецептов сохраняется как вставили (не прыгает от хэшей карты).
        List<IsfRecipeDefinition> documents = List.of(
                recipe("isf:r3"), recipe("isf:r1"), recipe("isf:r2"));
        IsfClientState.replace(java.util.Set.of(), java.util.Set.of(), Map.of(), documents,
                Map.of(), Map.of(), List.of());
        check(new ArrayList<>(IsfClientState.recipes().keySet()).equals(
                        List.of(id("isf:r3"), id("isf:r1"), id("isf:r2"))),
                "recipe insertion order preserved");

        // Сортировка типов по порядку реестра; неизвестные — в конец по id.
        IsfClientState.replace(java.util.Set.of(), java.util.Set.of(), Map.of(), List.of(),
                Map.of(), Map.of(), List.of(id("isf:b"), id("isf:a")));
        check(IsfClientState.sortByTypeOrder(
                        List.of(id("isf:a"), id("isf:b"), id("isf:c"), id("isf:0")))
                        .equals(List.of(id("isf:b"), id("isf:a"), id("isf:0"), id("isf:c"))),
                "registry order first, unknown last by id");

        // Без порядка — строго по id.
        IsfClientState.replace(java.util.Set.of(), java.util.Set.of(), Map.of(), List.of(),
                Map.of(), Map.of(), List.of());
        check(IsfClientState.sortByTypeOrder(List.of(id("isf:b"), id("isf:a")))
                        .equals(List.of(id("isf:a"), id("isf:b"))),
                "empty order falls back to id");
        check(IsfClientState.sortByTypeOrder(null).isEmpty(), "null input safe");
        check(IsfClientState.typeOrder().isEmpty(), "empty order stored");

        // Верстак всегда первый, даже если в реестре он не первый.
        IsfClientState.replace(java.util.Set.of(), java.util.Set.of(), Map.of(), List.of(),
                Map.of(), Map.of(), List.of(id("isf:blasting"), id("isf:crafting"), id("isf:cooking")));
        check(IsfClientState.sortByTypeOrder(
                        List.of(id("isf:cooking"), id("isf:blasting"), id("isf:crafting")))
                        .equals(List.of(id("isf:crafting"), id("isf:blasting"), id("isf:cooking"))),
                "crafting table always first");

        IsfClientState.clear();
        System.out.println("IsfClientOrderSelfTest passed");
    }

    private static IsfRecipeDefinition recipe(String value) {
        ResourceLocation id = id(value);
        return new IsfRecipeDefinition(id, id("isf:crafting"), null,
                Map.of(), List.of(), null, null);
    }

    private static ResourceLocation id(String value) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) throw new AssertionError("Bad test id: " + value);
        return parsed;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
