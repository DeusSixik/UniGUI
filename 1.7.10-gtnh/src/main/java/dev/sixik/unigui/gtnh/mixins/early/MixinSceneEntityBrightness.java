package dev.sixik.unigui.gtnh.mixins.early;

import dev.sixik.unigui.gtnh.backend.GtnhSceneEntityLight;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the entity drawn in a scene full brightness. Renderers that tint by {@link Entity#getBrightness}, like the
 * block inside a minecart, otherwise read the light at the preview position, which lies outside loaded chunks and
 * comes back as 0.
 */
@Mixin(Entity.class)
abstract class MixinSceneEntityBrightness {
    /**
     * @param partialTick fraction of the tick @param callback vanilla brightness from 0 to 1
     */
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private void unigui$sceneBrightness(float partialTick, CallbackInfoReturnable<Float> callback) {
        if (GtnhSceneEntityLight.isDrawing((Entity) (Object) this)) callback.setReturnValue(1f);
    }
}
