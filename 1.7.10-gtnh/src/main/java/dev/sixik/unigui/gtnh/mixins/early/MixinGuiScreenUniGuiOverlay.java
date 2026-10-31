package dev.sixik.unigui.gtnh.mixins.early;

import dev.sixik.unigui.gtnh.backend.GtnhOverlayInput;
import dev.sixik.unigui.gtnh.backend.GtnhTooltips;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Lets screen-overlay layers see mouse and keyboard events first, for every screen including containers, and keeps
 * vanilla tooltips above the overlay.
 *
 * <p>Drawing is not here: it is done from {@code GuiScreenEvent.DrawScreenEvent.Post}
 * (see {@code GtnhClientEvents}). All injections are {@code require = 1}: Mixin ignores a missing target silently by
 * default, which would hide a wrong method name.</p>
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenUniGuiOverlay {
    // Added by Forge, so it has no obfuscated name: remap = false.
    @Shadow(remap = false)
    protected abstract void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font);

    @Inject(method = "handleMouseInput()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void unigui$handleMouseInput(CallbackInfo ci) {
        if (GtnhOverlayInput.handleMouse((GuiScreen) (Object) this)) ci.cancel();
    }

    /**
     * Runs for key presses, repeats and releases of every screen. {@code keyTyped} is not a usable hook: container
     * screens override it without calling {@code super}, and that is where E and the hotbar digits are handled.
     */
    @Inject(method = "handleKeyboardInput()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void unigui$handleKeyboardInput(CallbackInfo ci) {
        if (GtnhOverlayInput.handleKeyboard((GuiScreen) (Object) this)) ci.cancel();
    }

    /**
     * Every tooltip of a vanilla screen ends in this method ({@code renderToolTip} and the creative tab tooltips
     * delegate to it). While a screen with overlay layers is being drawn the call is queued and replayed after the
     * overlay, see {@link GtnhTooltips}.
     */
    @Inject(method = "drawHoveringText(Ljava/util/List;IILnet/minecraft/client/gui/FontRenderer;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void unigui$deferTooltip(List<String> textLines, int x, int y, FontRenderer font, CallbackInfo ci) {
        if (GtnhTooltips.defer(() -> drawHoveringText(textLines, x, y, font))) ci.cancel();
    }
}