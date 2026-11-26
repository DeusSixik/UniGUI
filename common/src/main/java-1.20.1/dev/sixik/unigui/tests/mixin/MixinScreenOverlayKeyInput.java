package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Передаёт отпускание клавиш и ввод символов screen-overlay слоям Minecraft 1.20.1. */
@Mixin(ContainerEventHandler.class)
public interface MixinScreenOverlayKeyInput {
    @Inject(method = "keyReleased(III)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$keyReleased(int keyCode, int scanCode, int modifiers,
                                    CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.keyReleased(screen, keyCode, scanCode, modifiers)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped(CI)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$charTyped(char codePoint, int modifiers,
                                   CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.charTyped(screen, codePoint, modifiers)) {
            callback.setReturnValue(true);
        }
    }
}
