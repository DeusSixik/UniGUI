package dev.sixik.unigui.cleanroom.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the native values used by the legacy vanilla particle billboard.
 */
@Mixin(Particle.class)
public interface SceneParticleAccess {
    /**
     * @param enabled whether to query world collisions
     */
    @Accessor("canCollide")
    void unigui$setCollision(boolean enabled);

    /**
     * @return local X in blocks
     */
    @Accessor("posX")
    double unigui$x();

    /**
     * @return local Y in blocks
     */
    @Accessor("posY")
    double unigui$y();

    /**
     * @return local Z in blocks
     */
    @Accessor("posZ")
    double unigui$z();

    /**
     * @return native size before rendering
     */
    @Accessor("particleScale")
    float unigui$scale();

    /**
     * @return native lifetime in ticks
     */
    @Accessor("particleMaxAge")
    int unigui$lifetime();

    /**
     * @return atlas column in the 16-column grid
     */
    @Accessor("particleTextureIndexX")
    int unigui$column();

    /**
     * @return atlas row in the 16-row grid
     */
    @Accessor("particleTextureIndexY")
    int unigui$row();

    /**
     * @return native billboard angle in radians
     */
    @Accessor("particleAngle")
    float unigui$angle();

    /**
     * @return native opacity in [0,1]
     */
    @Accessor("particleAlpha")
    float unigui$alpha();
}
