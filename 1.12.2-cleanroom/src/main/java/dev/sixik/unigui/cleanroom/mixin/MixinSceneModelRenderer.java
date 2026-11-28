package dev.sixik.unigui.cleanroom.mixin;

import dev.sixik.unigui.cleanroom.backend.CleanroomSceneCapture;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Streams boxes under their animated matrix instead of replaying an opaque GL display list.
 */
@Mixin(ModelRenderer.class)
public abstract class MixinSceneModelRenderer {
    /**
     * @param scale model units per block
     */
    @Shadow
    protected abstract void compileDisplayList(float scale);

    /**
     * Skips recording a display list while its underlying quads are captured.
     *
     * @param part  current model part
     * @param scale model units per block
     */
    @Redirect(method = {"render", "renderWithRotation", "postRender"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelRenderer;compileDisplayList(F)V"))
    private void unigui$compile(ModelRenderer part, float scale) {
        if (!CleanroomSceneCapture.active()) compileDisplayList(scale);
    }

    /**
     * @param list  native display-list name
     * @param scale original render method scale
     */
    @Redirect(method = {"render", "renderWithRotation"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;callList(I)V"))
    private void unigui$drawBoxes(int list, float scale) {
        if (!CleanroomSceneCapture.active()) {
            GlStateManager.callList(list);
            return;
        }
        var buffer = Tessellator.getInstance().getBuffer();
        for (ModelBox box : ((ModelRenderer) (Object) this).cubeList) box.render(buffer, scale);
    }
}
