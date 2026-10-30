package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomOverlayInput;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets screen-overlay layers see mouse and keyboard events first, for every screen including containers.
 *
 * <p>Drawing is not here: it is done from {@code GuiScreenEvent.DrawScreenEvent.Post}
 * (see {@code CleanroomClientEvents}). All injections are {@code require = 1}: Mixin ignores a missing target
 * silently by default, which is how an earlier hook on {@code GuiContainer.handleMouseInput} (a method that does
 * not exist in 1.12.2) went unnoticed.</p>
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenUniGuiOverlay {
    @Inject(method = "handleMouseInput()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void unigui$handleMouseInput(CallbackInfo ci) {
        if (CleanroomOverlayInput.handleMouse((GuiScreen) (Object) this)) ci.cancel();
    }

    /**
     * Runs for key presses, repeats and releases of every screen. {@code keyTyped} is not a usable hook: container
     * screens override it without calling {@code super}, and that is where E and the hotbar digits are handled.
     */
    @Inject(method = "handleKeyboardInput()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void unigui$handleKeyboardInput(CallbackInfo ci) {
        if (CleanroomOverlayInput.handleKeyboard((GuiScreen) (Object) this)) ci.cancel();
    }
}