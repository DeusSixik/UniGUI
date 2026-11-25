package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Передаёт ввод мыши и клавиатуры screen-overlay слоям Minecraft 26.1. */
@Mixin(ContainerEventHandler.class)
public interface MixinScreenOverlayInput {
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$mouseClicked(MouseButtonEvent event, boolean doubleClick,
                                     CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.mouseClicked(screen, event.x(), event.y(), event.button())) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$mouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.mouseReleased(screen, event.x(), event.y(), event.button())) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$mouseDragged(MouseButtonEvent event, double dragX, double dragY,
                                     CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.mouseDragged(screen, event.x(), event.y(), event.button(), dragX, dragY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                      CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.mouseScrolled(screen, mouseX, mouseY, scrollY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "keyReleased(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$keyReleased(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen
                && ScreenOverlayRender.keyReleased(screen, event.key(), event.scancode(), event.modifiers())) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$charTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Screen screen && Character.isBmpCodePoint(event.codepoint())
                && ScreenOverlayRender.charTyped(screen, (char) event.codepoint(), 0)) {
            callback.setReturnValue(true);
        }
    }
}
