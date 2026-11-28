package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomSceneCapture;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes completed vanilla buffers to the scoped scene capture.
 */
@Mixin(WorldVertexBufferUploader.class)
public abstract class MixinSceneVertexUploader {
    /**
     * @param buffer completed vanilla buffer @param ci draw cancellation
     */
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void unigui$capture(BufferBuilder buffer, CallbackInfo ci) {
        if (CleanroomSceneCapture.intercept(buffer)) ci.cancel();
    }
}
