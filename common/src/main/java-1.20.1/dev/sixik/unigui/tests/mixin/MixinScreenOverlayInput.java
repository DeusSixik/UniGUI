package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Передаёт ввод мыши и клавиатуры screen-overlay слоям Minecraft 1.20.1. */
@Mixin(ContainerEventHandler.class)
public interface MixinScreenOverlayInput {
    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$mouseClicked(double mouseX, double mouseY, int button,
                                     CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.mouseClicked(screen, mouseX, mouseY, button)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$mouseReleased(double mouseX, double mouseY, int button,
                                      CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.mouseReleased(screen, mouseX, mouseY, button)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged(DDIDD)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$mouseDragged(double mouseX, double mouseY, int button,
                                     double dragX, double dragY,
                                     CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.mouseDragged(screen, mouseX, mouseY, button, dragX, dragY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$mouseScrolled(double mouseX, double mouseY, double delta,
                                      CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.mouseScrolled(screen, mouseX, mouseY, delta)) {
            callback.setReturnValue(true);
        }
    }

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