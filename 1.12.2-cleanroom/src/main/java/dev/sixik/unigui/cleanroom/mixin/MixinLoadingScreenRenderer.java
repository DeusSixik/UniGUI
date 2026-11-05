package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomLoadingHooks;
import net.minecraft.client.LoadingScreenRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks the "Loading level" / "Saving level" screens and draws loading layers over them.
 *
 * <p>{@code setLoadingProgress} paints the whole screen and then swaps buffers with {@code updateDisplay}; the layers
 * go in right before that swap, so they appear on top of the vanilla background and progress bar.</p>
 */
@Mixin(LoadingScreenRenderer.class)
public abstract class MixinLoadingScreenRenderer {
    @Inject(method = "resetProgressAndMessage(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void unigui$loading(String title, CallbackInfo ci) {
        CleanroomLoadingHooks.levelLoading(title);
    }

    @Inject(method = "displaySavingString(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void unigui$saving(String title, CallbackInfo ci) {
        CleanroomLoadingHooks.levelSaving(title);
    }

    @Inject(method = "displayLoadingString(Ljava/lang/String;)V", at = @At("HEAD"), require = 1)
    private void unigui$message(String text, CallbackInfo ci) {
        CleanroomLoadingHooks.message(text);
    }

    @Inject(method = "setLoadingProgress(I)V", at = @At("HEAD"), require = 1)
    private void unigui$progress(int percent, CallbackInfo ci) {
        CleanroomLoadingHooks.progress(percent);
    }

    @Inject(method = "setLoadingProgress(I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateDisplay()V"), require = 1)
    private void unigui$renderLayers(int percent, CallbackInfo ci) {
        CleanroomLoadingHooks.renderLevelLoading();
    }

    @Inject(method = "setDoneWorking()V", at = @At("HEAD"), require = 1)
    private void unigui$done(CallbackInfo ci) {
        CleanroomLoadingHooks.done();
    }
}