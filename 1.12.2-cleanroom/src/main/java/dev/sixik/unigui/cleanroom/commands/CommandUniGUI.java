package dev.sixik.unigui.cleanroom.commands;

import dev.sixik.unigui.cleanroom.demo.PostEffectLab;
import dev.sixik.unigui.cleanroom.demo.TextureLab;
import dev.sixik.unigui.cleanroom.demo.unity.UnityInspectorScreen;
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
        return "/unigui [textures|posteffect]";
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
        if (strings.length > 0 && "posteffect".equalsIgnoreCase(strings[0])) {
            iCommandSender.sendMessage(new net.minecraft.util.text.TextComponentString(PostEffectLab.next()));
            return;
        }
        UnityInspectorScreen.open();
    }
}
