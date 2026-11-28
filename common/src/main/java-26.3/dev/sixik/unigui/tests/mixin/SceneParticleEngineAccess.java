package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the loaded particle providers without creating entries in the world particle engine.
 */
@Mixin(ParticleEngine.class)
public interface SceneParticleEngineAccess {
    /**
     * @return providers with sprite bindings from the last resource reload
     */
    @Accessor("resourceManager")
    ParticleResources unigui$particleResources();
}
