package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Supplies physics and lighting defaults for the isolated particles created with a null level.
 */
@Mixin(Particle.class)
abstract class SceneParticleMixin {
    /**
     * World context, absent on scene particles.
     */
    @Shadow
    @Final
    protected ClientLevel level;
    /**
     * Controls world collision queries during movement.
     */
    @Shadow
    protected boolean hasPhysics;

    /**
     * Skips collision queries for isolated particles, including constructors which leave physics enabled.
     *
     * @param ci location of the injected movement prologue
     */
    @Inject(method = "move", at = @At("HEAD"))
    private void unigui$skipWorldPhysics(CallbackInfo ci) {
        if (level == null) hasPhysics = false;
    }

    /**
     * Uses full-bright lighting when no world is associated with the particle.
     *
     * @param partialTick fraction of a tick from the renderer
     * @param cir         receives the packed light coordinates
     */
    @Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
    private void unigui$useSceneLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (level == null) cir.setReturnValue(LightCoordsUtil.FULL_BRIGHT);
    }
}
