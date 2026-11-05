package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomClientBootstrap;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Container-specific guard. Mouse and keyboard events reach overlays through {@link MixinGuiScreenUniGuiOverlay}:
 * in 1.12.2 {@code GuiContainer} does not override {@code handleMouseInput}, its slot clicks run from
 * {@code GuiScreen.handleMouseInput} through {@code mouseClicked} and friends.
 */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainerUniGuiOverlay {
    /**
     * Slot hover highlight, the slot tooltip and slot clicks are all derived from this test, so one hook silences
     * the vanilla slot under an overlay panel that registered a pointer blocker.
     */
    @Inject(method = "isPointInRegion(IIIIII)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void unigui$blockHoverUnderOverlay(int x, int y, int width, int height, int pointX, int pointY,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (CleanroomClientBootstrap.backend().blocksPointer((GuiContainer) (Object) this, pointX, pointY)) {
            cir.setReturnValue(false);
        }
    }
}