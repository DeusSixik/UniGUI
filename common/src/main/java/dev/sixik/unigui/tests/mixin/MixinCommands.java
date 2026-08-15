package dev.sixik.unigui.tests.mixin;

import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.tests.TestCommands;
import dev.sixik.unigui.tests.UniGuiDemo;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class MixinCommands {

    @Inject(method = "<init>", at = @At("RETURN"))
    public void register(Commands.CommandSelection commandSelection, CommandBuildContext commandBuildContext, CallbackInfo ci) {
        CommandDispatcher<CommandSourceStack> dispatcher = ((Commands) (Object) this).getDispatcher();
        TestCommands.register(dispatcher);
        UniGuiDemo.register(dispatcher);
    }
}
