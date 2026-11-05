package dev.sixik.unigui.gtnh.mixins.early;

import dev.sixik.unigui.gtnh.backend.GtnhClientBootstrap;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Container-specific guard. Mouse and keyboard events reach overlays through {@link MixinGuiScreenUniGuiOverlay}.
 */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainerUniGuiOverlay {
    /**
     * Slot hover highlight, the slot tooltip and slot clicks are all derived from this test ({@code isMouseOverSlot}
     * and {@code getSlotAtPosition} call it), so one hook silences the vanilla slot under an overlay panel that
     * registered a pointer blocker. The method has no MCP name in 1.7.10.
     */
    @Inject(method = "func_146978_c(IIIIII)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void unigui$blockHoverUnderOverlay(int x, int y, int width, int height, int pointX, int pointY,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (GtnhClientBootstrap.backend().blocksPointer((GuiContainer) (Object) this, pointX, pointY)) {
            cir.setReturnValue(false);
        }
    }
}