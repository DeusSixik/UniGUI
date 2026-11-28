package dev.sixik.unigui.gtnh.mixins.early;

import dev.sixik.unigui.gtnh.backend.GtnhSceneCapture;
import net.minecraft.client.model.ModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Submits posed boxes in place of a precompiled display list during capture. */
@Mixin(ModelRenderer.class)
abstract class MixinSceneModelRenderer {
    /**
     * @param scale model units to blocks
     * @param callback controls display-list compilation
     */
    @Inject(method = "compileDisplayList", at = @At("HEAD"), cancellable = true)
    private void unigui$skipCompile(float scale, CallbackInfo callback) {
        if (GtnhSceneCapture.active()) callback.cancel();
    }

    /**
     * @param displayList vanilla GL display list
     * @param scale model units to blocks
     */
    @Redirect(method = { "render", "renderWithRotation" }, at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glCallList(I)V", remap = false))
    private void unigui$renderBoxes(int displayList, float scale) {
        GtnhSceneCapture.renderModel((ModelRenderer) (Object) this, displayList, scale);
    }
}
