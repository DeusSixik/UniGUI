package dev.sixik.unigui.tests.mixin;

import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Гасит реакцию ванильных слотов под перекрывающими overlay-панелями.
 *
 * <p>Hover-подсветка слота, vanilla tooltip и клики по слотам вычисляются в render
 * через {@code isHovering}, поэтому отмены Forge-событий мыши недостаточно.
 * Перегрузка {@code isHovering(Slot, ...)} делегирует в {@code isHovering(IIIIDD)Z},
 * так что одной точки входа достаточно для всех сценариев.</p>
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinScreenOverlayHover {
    @Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true)
    private void unigui$blockHoverUnderOverlay(int x, int y, int width, int height,
                                               double mouseX, double mouseY,
                                               CallbackInfoReturnable<Boolean> callback) {
        if (ScreenOverlayRender.blocksPointer(
                (AbstractContainerScreen<?>) (Object) this, mouseX, mouseY)) {
            callback.setReturnValue(false);
        }
    }
}
