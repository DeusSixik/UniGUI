package dev.sixik.isf.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.sixik.isf.importer.IsfImportRequest;
import dev.sixik.isf.importer.IsfRecipeGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
                                        StringArgumentType.getString(context, "selectors"))))));
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
                "ISF: generated " + result.generated() + " recipes in " + result.outputDirectory()
                        + ". Run /reload to apply them."), true);
        if (!result.unsupportedCategories().isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "Not implemented yet: " + String.join(", ", result.unsupportedCategories())));
        }
        return result.generated();
    }
}
