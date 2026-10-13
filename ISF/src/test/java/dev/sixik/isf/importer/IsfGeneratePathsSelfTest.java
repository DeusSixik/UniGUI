package dev.sixik.isf.importer;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

/**
 * Проверка раскладки сгенерированных рецептов по папкам типов (без запуска Minecraft).
 *
 * <p>Запуск: {@code :1.20.1:ISF:isfGeneratePathsSelfTest}.</p>
 */
public final class IsfGeneratePathsSelfTest {
    public static void main(String[] args) {
        Path root = Path.of("generated");

        // Крафт меча: generated/isf/crafting/minecraft/iron_sword.json.
        check(IsfRecipeGenerator.generatedRecipePath(root,
                        id("isf:crafting"), id("minecraft:iron_sword")).toString().replace('\\', '/')
                        .endsWith("generated/isf/crafting/minecraft/iron_sword.json"),
                "crafting recipe nested by type");

        // Чужой неймспейс типа не коллизирует: generated/mymod/alloy/...
        check(IsfRecipeGenerator.generatedRecipePath(root,
                        id("mymod:alloy"), id("minecraft:iron_ingot")).toString().replace('\\', '/')
                        .endsWith("generated/mymod/alloy/minecraft/iron_ingot.json"),
                "namespaced type folder");

        // Выход за корень через ".." запрещён (точек хватает, чтобы вылезти).
        boolean rejected = false;
        try {
            IsfRecipeGenerator.generatedRecipePath(root,
                    id("isf:crafting"), id("minecraft:../../../../outside"));
        } catch (IllegalStateException | IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "path traversal rejected");

        // Null-защита.
        boolean nulled = false;
        try {
            IsfRecipeGenerator.generatedRecipePath(null, id("isf:crafting"), id("minecraft:stick"));
        } catch (NullPointerException expected) {
            nulled = true;
        }
        check(nulled, "null root rejected");

        System.out.println("IsfGeneratePathsSelfTest passed");
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
