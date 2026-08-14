package dev.sixik.unigui.testmod.mixin;

import dev.sixik.unigui.testmod.client.TestModClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Добавляет быстрый вход в UniGUI demo на главный экран Minecraft.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void uniguiTestMod$addDemoButton(CallbackInfo ci) {
        addRenderableWidget(Button.builder(Component.literal("UniGUI Demo"), button -> TestModClient.openDemo())
                .bounds(width - 124, 8, 116, 20)
                .build());
    }
}