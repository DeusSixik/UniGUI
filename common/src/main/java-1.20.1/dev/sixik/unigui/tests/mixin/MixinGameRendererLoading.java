package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.LoadingScreenRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Подключает UniGUI после рендера загрузочного overlay Minecraft 1.20.1. */
@Mixin(GameRenderer.class)
public abstract class MixinGameRendererLoading {
    @Redirect(
            method = "render(FJZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Overlay;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
    )
    private void unigui$renderLoadingScreen(Overlay overlay, GuiGraphics graphics,
                                            int mouseX, int mouseY, float partialTick) {
        overlay.render(graphics, mouseX, mouseY, partialTick);
        LoadingScreenRender.render(graphics, mouseX, mouseY, partialTick);
    }
}
