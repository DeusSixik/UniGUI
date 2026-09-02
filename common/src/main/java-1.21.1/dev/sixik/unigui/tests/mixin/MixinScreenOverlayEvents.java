package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Передаёт движение мыши screen-overlay слоям Minecraft. */
@Mixin(GuiEventListener.class)
public interface MixinScreenOverlayEvents {
    @Inject(method = "mouseMoved(DD)V", at = @At("HEAD"))
    private void unigui$mouseMoved(double mouseX, double mouseY, CallbackInfo callback) {
        if ((Object) this instanceof Screen screen) {
            ScreenOverlayRender.mouseMoved(screen, mouseX, mouseY);
        }
    }
}