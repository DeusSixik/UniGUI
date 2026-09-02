package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.HudRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** HUD hook для Minecraft 1.21.1. */
@Mixin(Gui.class)
public abstract class MixinHudRender {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void unigui$renderHud(GuiGraphics graphics, DeltaTracker deltaTracker,
                                  CallbackInfo callback) {
        HudRender.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
