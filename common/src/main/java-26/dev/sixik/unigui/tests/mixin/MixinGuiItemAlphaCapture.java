package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.UiItemAlpha;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Запоминает прозрачность предмета UniGUI при добавлении в GUI (Minecraft 26.1), см. {@link UiItemAlpha}. */
@Mixin(GuiRenderState.class)
public abstract class MixinGuiItemAlphaCapture {
    @Inject(method = "addItem", at = @At("HEAD"))
    private void unigui$captureItemAlpha(GuiItemRenderState itemState, CallbackInfo callback) {
        UiItemAlpha.capture(itemState);
    }
}
