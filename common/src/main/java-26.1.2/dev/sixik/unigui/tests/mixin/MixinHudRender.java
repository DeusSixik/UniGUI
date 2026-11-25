package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import dev.sixik.unigui.backend.minecraft_impl.HudRender;
import dev.sixik.unigui.backend.minecraft_impl.UiDebugCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD hook для Minecraft 26.1: в 26.1 HUD собирается в {@code Gui.extractRenderState}.
 *
 * <p>{@code RETURN}, а не {@code TAIL}: Forge 26.1 передаёт HUD в {@code ForgeLayeredDraw} и выходит из метода
 * раньше, поэтому до конца метода дело не доходит. {@code RETURN} срабатывает на любом выходе, ровно один раз
 * за вызов.</p>
 */
@Mixin(Gui.class)
public abstract class MixinHudRender {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("RETURN"))
    private void unigui$renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                  CallbackInfo callback) {
        HudRender.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (MinecraftClientCompat.screen() == null) UiDebugCapture.onScreenExtract(null);
    }
}
