package dev.sixik.unigui.testmod.mixin.minecraft;

import dev.sixik.unigui.testmod.client.ui.minecraft.MinecraftVideoSettingsMenu;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public abstract class MixinOptionsScreen extends Screen {

    protected MixinOptionsScreen(Component component) {
        super(component);
    }

    @Shadow
    protected abstract Button openScreenButton(Component arg2, Supplier<Screen> supplier);

    @Shadow
    @Final
    private Options options;

    @Shadow
    @Final
    private static Component VIDEO;

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/OptionsScreen;openScreenButton(Lnet/minecraft/network/chat/Component;Ljava/util/function/Supplier;)Lnet/minecraft/client/gui/components/Button;",
            ordinal = 2))
    public Button init(OptionsScreen instance, Component arg, Supplier<Screen> supplier) {
        return this.openScreenButton(VIDEO, () -> MinecraftVideoSettingsMenu.openGui(this, this.options));
    }
}
