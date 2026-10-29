package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.commands.CommandUniGUI;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommandManager.class)
public abstract class MixinServerCommandManager$add_custom_commands extends CommandHandler {

    @Inject(method = "<init>", at = @At("RETURN"))
    public void sdm$init(MinecraftServer server, CallbackInfo ci) {
        registerCommand(new CommandUniGUI());
    }
}
