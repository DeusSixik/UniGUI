package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.UiItemAlpha;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Подставляет прозрачность предмета UniGUI в цвет blit'а из атласа предметов (Minecraft 26.2).
 * Ванильные предметы не затрагиваются: без записанной прозрачности цвет остаётся прежним.
 */
@Mixin(GuiRenderer.class)
public abstract class MixinGuiItemAlpha {
    @Unique
    private GuiItemRenderState unigui$blitItem;

    @Inject(method = "submitBlitFromItemAtlas", at = @At("HEAD"))
    private void unigui$rememberItem(GuiItemRenderState itemState,
                                     @org.spongepowered.asm.mixin.injection.Coerce Object slotView,
                                     CallbackInfo callback) {
        unigui$blitItem = itemState;
    }

    @ModifyArg(method = "submitBlitFromItemAtlas",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"),
            index = 11)
    private int unigui$itemAlpha(int color) {
        GuiItemRenderState item = unigui$blitItem;
        unigui$blitItem = null;
        return UiItemAlpha.color(item, color);
    }
}
