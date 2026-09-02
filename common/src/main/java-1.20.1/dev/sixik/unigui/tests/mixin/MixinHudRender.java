package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.HudRender;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD hook для Minecraft 1.20.1. */
@Mixin(Gui.class)
public abstract class MixinHudRender {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At("TAIL"))
    private void unigui$renderHud(GuiGraphics graphics, float partialTick, CallbackInfo callback) {
        HudRender.render(graphics, partialTick);
    }
}
