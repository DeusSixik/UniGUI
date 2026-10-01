package dev.sixik.isf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.sixik.isf.importer.IsfImportRequest;
import dev.sixik.isf.importer.IsfRecipeGenerator;
import dev.sixik.isf.importer.LootTableSupports;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Серверные команды ISF. */
public final class IsfCommands {
    private IsfCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("isf")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("generateRecipes")
                        .executes(context -> generate(context, ""))
                        .then(Commands.argument("selectors", StringArgumentType.greedyString())
                                .executes(context -> generate(context,
                                        StringArgumentType.getString(context, "selectors")))))
                .then(Commands.literal("loot")
                        .then(Commands.literal("list")
                                .executes(context -> listLoot(context, ""))
                                .then(Commands.argument("filter", StringArgumentType.greedyString())
                                        .executes(context -> listLoot(context,
                                                StringArgumentType.getString(context, "filter")))))
                        .then(Commands.literal("disable")
                                .then(Commands.argument("table", ResourceLocationArgument.id())
                                        .executes(context -> setLootDisabled(context,
                                                ResourceLocationArgument.getId(context, "table"), true))))
                        .then(Commands.literal("enable")
                                .then(Commands.argument("table", ResourceLocationArgument.id())
                                        .executes(context -> setLootDisabled(context,
                                                ResourceLocationArgument.getId(context, "table"), false))))));
    }

    private static int generate(CommandContext<CommandSourceStack> context, String selectors) {
        IsfImportRequest request;
        try {
            request = IsfImportRequest.parse(selectors);
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(Component.literal(exception.getMessage()));
            return 0;
        }
        IsfRecipeGenerator.Result result = new IsfRecipeGenerator().generate(context.getSource().getServer(), request);
        context.getSource().sendSuccess(() -> Component.literal(
                "ISF: generated " + result.generatedTypes() + " recipe types and "
                        + result.generated() + " recipes in " + result.outputDirectory()
                        + ". Run /reload to apply them."), true);
        if (!result.unsupportedCategories().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "ISF: skipped unsupported recipe categories: "
                            + String.join(", ", result.unsupportedCategories())), false);
        }
        return result.generated();
    }

    private static int listLoot(CommandContext<CommandSourceStack> context, String filter) {
        MinecraftServer server = context.getSource().getServer();
        Set<ResourceLocation> disabled;
        try {
            disabled = LootTableSupports.loadDisabledTables(server.getResourceManager());
        } catch (RuntimeException exception) {
            context.getSource().sendFailure(Component.literal("ISF: cannot read loot tables"));
            return 0;
        }
        String needle = filter == null ? "" : filter.trim().toLowerCase(java.util.Locale.ROOT);
        List<String> lines = new ArrayList<>();
        int total = 0;
        for (dev.sixik.isf.definition.IsfRecipeDefinition recipe
                : LootTableSupports.generate(server.getResourceManager())) {
            ResourceLocation tableId = LootTableSupports.lootTableId(recipe);
            if (tableId == null) continue;
            total++;
            if (!needle.isEmpty() && !tableId.toString().toLowerCase(java.util.Locale.ROOT)
                    .contains(needle)) {
                continue;
            }
            if (lines.size() >= 50) continue;
            lines.add("  " + tableId + (disabled.contains(tableId) ? " [disabled]" : ""));
        }
        if (lines.size() >= 50) {
            lines.add("  ... and more, refine the filter");
        }
        int shown = Math.min(lines.size(), 50);
        final String message = "ISF loot tables (" + total + ", " + disabled.size() + " disabled):\n"
                + String.join("\n", lines);
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return shown;
    }

    private static int setLootDisabled(CommandContext<CommandSourceStack> context,
                                       ResourceLocation tableId, boolean disabled) {
        MinecraftServer server = context.getSource().getServer();
        Set<ResourceLocation> known = new java.util.LinkedHashSet<>();
        for (dev.sixik.isf.definition.IsfRecipeDefinition recipe
                : LootTableSupports.generate(server.getResourceManager())) {
            ResourceLocation knownTable = LootTableSupports.lootTableId(recipe);
            if (knownTable != null) known.add(knownTable);
        }
        if (!known.contains(tableId)) {
            context.getSource().sendFailure(Component.literal(
                    "ISF: unknown loot table: " + tableId + ". See /isf loot list"));
            return 0;
        }
        java.nio.file.Path file = IsfRecipeGenerator.lootDisabledFile(server);
        Set<String> ids = new TreeSet<>();
        try {
            if (java.nio.file.Files.exists(file)) {
                for (ResourceLocation id : LootTableSupports.parseDisabledTables(
                        new com.google.gson.Gson().fromJson(
                                java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8),
                                com.google.gson.JsonObject.class))) {
                    ids.add(id.toString());
                }
            }
            boolean changed = disabled ? ids.add(tableId.toString()) : ids.remove(tableId.toString());
            if (!changed) {
                context.getSource().sendSuccess(() -> Component.literal(
                        "ISF: loot table " + tableId + (disabled ? " is already disabled"
                                : " is not disabled")), false);
                return 1;
            }
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            ids.forEach(array::add);
            root.add("disabled", array);
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file,
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException | RuntimeException exception) {
            context.getSource().sendFailure(Component.literal(
                    "ISF: cannot update " + file + ": " + exception.getMessage()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "ISF: loot table " + tableId + (disabled ? " disabled" : " enabled")
                        + ". Run /reload to apply."), true);
        return 1;
    }
}
