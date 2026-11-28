package dev.sixik.unigui.cleanroom.mixin;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Access to the vanilla factories without inserting particles into the world's render queues.
 */
@Mixin(ParticleManager.class)
public interface SceneParticleFactories {
    /**
     * @return registered factories keyed by the numeric vanilla particle type
     */
    @Accessor("particleTypes")
    Map<Integer, IParticleFactory> unigui$factories();
}
