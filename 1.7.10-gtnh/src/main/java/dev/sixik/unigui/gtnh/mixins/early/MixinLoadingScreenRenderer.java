package dev.sixik.unigui.gtnh.mixins.early;

import dev.sixik.unigui.gtnh.backend.GtnhLoadingHooks;
import net.minecraft.client.LoadingScreenRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks the "Loading level" / "Saving level" screens and draws loading layers over them.
 *
 * <p>{@code setLoadingProgress} paints the whole screen and then swaps buffers with {@code Minecraft.func_147120_f()}
 * (update display); the layers go in right before that swap, so they appear on top of the vanilla background and
 * progress bar.</p>
 */
@Mixin(LoadingScreenRenderer.class)
public abstract class MixinLoadingScreenRenderer {
    @Inject(method = "resetProgressAndMessage(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void unigui$reset(String title, CallbackInfo ci) {
        GtnhLoadingHooks.levelTitle(title);
    }

    @Inject(method = "displayProgressMessage(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void unigui$display(String title, CallbackInfo ci) {
        GtnhLoadingHooks.levelTitle(title);
    }

    @Inject(method = "resetProgresAndWorkingMessage(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void unigui$message(String text, CallbackInfo ci) {
        GtnhLoadingHooks.message(text);
    }

    @Inject(method = "setLoadingProgress(I)V", at = @At("HEAD"), require = 1)
    private void unigui$progress(int percent, CallbackInfo ci) {
        GtnhLoadingHooks.progress(percent);
    }

    @Inject(method = "setLoadingProgress(I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;func_147120_f()V"), require = 1)
    private void unigui$renderLayers(int percent, CallbackInfo ci) {
        GtnhLoadingHooks.renderLevelLoading();
    }
}