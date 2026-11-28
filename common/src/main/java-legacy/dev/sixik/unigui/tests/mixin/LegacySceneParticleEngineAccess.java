package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Creates isolated particles without adding them to the world particle engine. */
@Mixin(ParticleEngine.class)
public interface LegacySceneParticleEngineAccess {
    /**
     * @param options native particle type and provider options
     * @param x local X spawn position in blocks
     * @param y local Y spawn position in blocks
     * @param z local Z spawn position in blocks
     * @param dx initial X velocity in blocks per tick
     * @param dy initial Y velocity in blocks per tick
     * @param dz initial Z velocity in blocks per tick
     * @return particle supplied by the active provider, or null
     */
    @Invoker("makeParticle")
    Particle unigui$makeParticle(ParticleOptions options, double x, double y, double z, double dx, double dy, double dz);
}
