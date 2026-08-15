package dev.sixik.unigui.testmod.mixin.minecraft;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VideoSettingsScreen.class)
public abstract class MixinVideoSettingsScreen extends OptionsSubScreen {

    private MixinVideoSettingsScreen(Screen screen, Options options, Component component) {
        super(screen, options, component);
    }
}
