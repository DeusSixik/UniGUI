package dev.sixik.unigui.cleanroom.commands;

import dev.sixik.unigui.cleanroom.demo.IntegrationLab;
import dev.sixik.unigui.cleanroom.demo.MinecraftLab;
import dev.sixik.unigui.cleanroom.demo.PostEffectLab;
import dev.sixik.unigui.cleanroom.demo.TextLab;
import dev.sixik.unigui.cleanroom.demo.TextureLab;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CommandUniGUI extends CommandBase implements ICommand {

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
    public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings) throws CommandException {
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

    }
}
