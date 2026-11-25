package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.LoadingScreenRender;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Подключает UniGUI после загрузочного overlay (Minecraft 26.2).
 *
 * <p>В 26.2 сборка GUI кадра (HUD, overlay, экран) переехала из {@code GameRenderer.extractGui} в
 * {@code Gui.extractRenderState(DeltaTracker, boolean, boolean)}.</p>
 */
@Mixin(Gui.class)
public abstract class MixinGameRendererLoading {
    @Redirect(
            method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Overlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
    )
    private void unigui$renderLoadingScreen(Overlay overlay, GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY, float partialTick) {
        overlay.extractRenderState(graphics, mouseX, mouseY, partialTick);
        LoadingScreenRender.render(graphics, mouseX, mouseY, partialTick);
    }
}
