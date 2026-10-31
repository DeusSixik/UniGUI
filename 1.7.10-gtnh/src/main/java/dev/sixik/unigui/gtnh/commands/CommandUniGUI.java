package dev.sixik.unigui.gtnh.commands;

import dev.sixik.unigui.gtnh.demo.IntegrationLab;
import dev.sixik.unigui.gtnh.demo.MinecraftLab;
import dev.sixik.unigui.gtnh.demo.PostEffectLab;
import dev.sixik.unigui.gtnh.demo.TextureLab;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.List;

/**
 * Client command {@code /unigui demo <name>} that opens the version-neutral demo screens.
 */
public class CommandUniGUI extends CommandBase {

    @Override
    public String getCommandName() {
        return "unigui";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/unigui [textures|minecraft|posteffect|lab <" + IntegrationLab.USAGE + ">|demo <" + String.join("|", GtnhDemoLauncher.names()) + ">]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && "textures".equalsIgnoreCase(args[0])) {
            TextureLab.open();
            return;
        }
        if (args.length > 0 && "minecraft".equalsIgnoreCase(args[0])) {
            MinecraftLab.open();
            return;
        }
        if (args.length > 0 && "posteffect".equalsIgnoreCase(args[0])) {
            sender.addChatMessage(new ChatComponentText(PostEffectLab.next()));
            return;
        }
        if (args.length > 1 && "lab".equalsIgnoreCase(args[0])) {
            sender.addChatMessage(new ChatComponentText(IntegrationLab.run(args[1].toLowerCase(java.util.Locale.ROOT))));
            return;
        }
        // `/unigui <name>` is accepted as a shorthand for `/unigui demo <name>`.
        if (args.length == 1 && GtnhDemoLauncher.supported(args[0])) {
            GtnhDemoLauncher.open(args[0]);
            return;
        }
        if (args.length > 0 && "demo".equalsIgnoreCase(args[0])) {
            String demoName = args.length > 1 ? args[1] : "widgets";
            if (!GtnhDemoLauncher.open(demoName)) {
                sender.addChatMessage(new ChatComponentText(
                        "Unknown demo. Available: " + String.join(", ", GtnhDemoLauncher.names())));
            }
            return;
        }
        sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(GtnhDemoLauncher.names());
            options.add("demo");
            options.add("textures");
            options.add("minecraft");
            options.add("posteffect");
            options.add("lab");
            return getListOfStringsMatchingLastWord(args, options.toArray(new String[0]));
        }
        if (args.length == 2 && "lab".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, IntegrationLab.USAGE.split("\\|"));
        }
        if (args.length == 2 && "demo".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, GtnhDemoLauncher.names().toArray(new String[0]));
        }
        return null;
    }
}