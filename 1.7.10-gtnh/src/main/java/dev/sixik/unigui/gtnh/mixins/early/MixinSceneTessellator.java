package dev.sixik.unigui.gtnh.mixins.early;

import dev.sixik.unigui.gtnh.backend.GtnhSceneCapture;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes tessellator quads into the active UniGUI model capture.
 */
@Mixin(Tessellator.class)
abstract class MixinSceneTessellator {
    /**
     * Packed vertices and their counts in ints and vertices respectively.
     */
    @Shadow
    private int[] rawBuffer;
    @Shadow
    private int rawBufferIndex;
    @Shadow
    private int vertexCount;
    /**
     * OpenGL primitive mode of this batch.
     */
    @Shadow
    private int drawMode;
    /**
     * Optional components present in this batch.
     */
    @Shadow
    private boolean hasTexture;
    @Shadow
    private boolean hasColor;
    @Shadow
    private boolean hasNormals;
    /**
     * Whether startDrawing opened the current batch.
     */
    @Shadow
    private boolean isDrawing;

    /**
     * Clears the vanilla counts and position in the staging buffer.
     */
    @Shadow
    protected abstract void reset();

    /**
     * @param callback vanilla return value in bytes
     */
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void unigui$capture(CallbackInfoReturnable<Integer> callback) {
        if (!isDrawing || !GtnhSceneCapture.active()) return;
        isDrawing = false;
        int bytes = rawBufferIndex * Integer.BYTES;
        try {
            GtnhSceneCapture.submit(rawBuffer, vertexCount, drawMode, hasTexture, hasColor, hasNormals);
            callback.setReturnValue(bytes);
        } finally {
            reset();
        }
    }
}
