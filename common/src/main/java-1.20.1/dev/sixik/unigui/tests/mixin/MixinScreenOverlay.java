package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.DeferredMinecraftTooltips;
import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Рисует UniGUI-слой поверх экрана, но под vanilla tooltip. */
@Mixin(Screen.class)
public abstract class MixinScreenOverlay {
    @Inject(method = "renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    private void unigui$beginVanillaTooltipDeferral(GuiGraphics graphics, int mouseX, int mouseY,
                                                     float partialTick, CallbackInfo callback) {
        if (ScreenOverlayRender.hasVisibleLayer((Screen) (Object) this)
                && graphics instanceof DeferredMinecraftTooltips deferred) {
            deferred.unigui$beginTooltipDeferral();
        }
    }

    @Inject(method = "renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void unigui$renderScreenOverlay(GuiGraphics graphics, int mouseX, int mouseY,
                                            float partialTick, CallbackInfo callback) {
        DeferredMinecraftTooltips deferred = graphics instanceof DeferredMinecraftTooltips tooltips
                ? tooltips : null;
        if (deferred != null) deferred.unigui$endTooltipDeferral();
        try {
            ScreenOverlayRender.mouseMoved((Screen) (Object) this, mouseX, mouseY);
            ScreenOverlayRender.render((Screen) (Object) this, graphics, mouseX, mouseY, partialTick);
        } finally {
            if (deferred != null) deferred.unigui$flushDeferredTooltips();
        }
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$keyPressed(int keyCode, int scanCode, int modifiers,
                                   CallbackInfoReturnable<Boolean> callback) {
        if (ScreenOverlayRender.keyPressed((Screen) (Object) this, keyCode, scanCode, modifiers)) {
            callback.setReturnValue(true);
        }
    }

}
