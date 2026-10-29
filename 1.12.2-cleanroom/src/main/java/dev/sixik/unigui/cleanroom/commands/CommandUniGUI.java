package dev.sixik.unigui.cleanroom.commands;

import dev.sixik.unigui.cleanroom.demo.ComplexUiDemo;
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
        return "/unigui";
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
        ComplexUiDemo.openDemo();
    }
}
