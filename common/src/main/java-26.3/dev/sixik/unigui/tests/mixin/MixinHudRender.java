package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import dev.sixik.unigui.backend.minecraft_impl.HudRender;
import dev.sixik.unigui.backend.minecraft_impl.UiDebugCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HUD hook для Minecraft 26.2: в 26.2 HUD собирается в {@code Hud.extractRenderState} (в 26.1 этот класс
 * назывался {@code Gui}, а {@code Gui} 26.2 управляет экранами и оверлеем).
 *
 * <p>{@code RETURN}, а не {@code TAIL}: Forge 26.x передаёт HUD в {@code ForgeLayeredDraw} и выходит из метода
 * раньше, поэтому до конца метода дело не доходит. {@code RETURN} срабатывает на любом выходе, ровно один раз
 * за вызов.</p>
 */
@Mixin(Hud.class)
public abstract class MixinHudRender {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("RETURN"))
    private void unigui$renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                  CallbackInfo callback) {
        HudRender.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (MinecraftClientCompat.screen() == null) UiDebugCapture.onScreenExtract(null);
    }
}
