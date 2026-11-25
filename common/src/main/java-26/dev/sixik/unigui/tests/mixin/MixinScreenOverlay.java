package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import dev.sixik.unigui.backend.minecraft_impl.UiDebugCapture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Рисует UniGUI-слой поверх экрана, но под vanilla tooltip (они рисуются в extractDeferredElements). */
@Mixin(Screen.class)
public abstract class MixinScreenOverlay {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"))
    private void unigui$renderScreenOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick, CallbackInfo callback) {
        UiDebugCapture.onScreenExtract((Screen) (Object) this);
        ScreenOverlayRender.mouseMoved((Screen) (Object) this, mouseX, mouseY);
        ScreenOverlayRender.render((Screen) (Object) this, graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"))
    private void unigui$beginExtract(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                     CallbackInfo callback) {
        UiDebugCapture.beginExtract();
    }

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (ScreenOverlayRender.keyPressed((Screen) (Object) this, event.key(), event.scancode(), event.modifiers())) {
            callback.setReturnValue(true);
        }
    }
}
