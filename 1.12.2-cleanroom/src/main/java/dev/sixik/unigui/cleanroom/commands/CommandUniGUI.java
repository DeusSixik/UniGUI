package dev.sixik.unigui.cleanroom.commands;

import dev.sixik.unigui.cleanroom.backend.CleanroomClientEvents;
import dev.sixik.unigui.cleanroom.demo.IntegrationLab;
import dev.sixik.unigui.cleanroom.demo.MinecraftLab;
import dev.sixik.unigui.cleanroom.demo.PostEffectLab;
import dev.sixik.unigui.cleanroom.demo.TextLab;
import dev.sixik.unigui.cleanroom.demo.TextureLab;
import dev.sixik.unigui.demo.basic.main_demo.UniGuiDemo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Client command for opening UniGUI demos from chat.
 */
public class CommandUniGUI extends CommandBase {

    @Override
    public String getName() {
        return "unigui";
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/unigui [textures|text|minecraft|posteffect|lab <" + IntegrationLab.USAGE + ">|demo <name>]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer p_184882_1_, ICommandSender p_184882_2_) {
        return true;
    }

    @Override
    public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings) {
        CleanroomClientEvents.runAfterChat(() -> openClient(iCommandSender, strings));
    }

    /**
     * Handles the command after chat has closed, so a screen opened here remains visible.
     *
     * @param iCommandSender client command sender for status messages.
     * @param strings        command arguments after {@code /unigui}.
     */
    private static void openClient(ICommandSender iCommandSender, String[] strings) {
        if (strings.length > 0 && "textures".equalsIgnoreCase(strings[0])) {
            TextureLab.open();
            return;
        }
        if (strings.length > 0 && "minecraft".equalsIgnoreCase(strings[0])) {
            MinecraftLab.open();
            return;
        }
        if (strings.length > 0 && "text".equalsIgnoreCase(strings[0])) {
            TextLab.open();
            return;
        }
        if (strings.length > 0 && "posteffect".equalsIgnoreCase(strings[0])) {
            iCommandSender.sendMessage(new net.minecraft.util.text.TextComponentString(PostEffectLab.next()));
            return;
        }
        if (strings.length > 0 && "demo".equalsIgnoreCase(strings[0])) {
            String demoName = strings.length > 1 ? strings[1] : null;
            if (!CleanroomDemoLauncher.open(demoName)) {
                iCommandSender.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "Unknown demo. Available: " + String.join(", ", CleanroomDemoLauncher.names())));
            }
            return;
        }
        if (strings.length > 1 && "lab".equalsIgnoreCase(strings[0])) {
            iCommandSender.sendMessage(new net.minecraft.util.text.TextComponentString(
                    IntegrationLab.run(strings[1].toLowerCase(java.util.Locale.ROOT))));
            return;
        }

        UniGuiDemo.openGui();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "demo", "textures", "text", "minecraft", "posteffect", "lab");
        }
        if (args.length == 2 && "demo".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, CleanroomDemoLauncher.names());
        }
        return List.of();
    }
}
