package dev.sixik.unigui.gtnh.mixins.early;

import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the native simulation's current sprite and lifetime.
 */
@Mixin(EntityFX.class)
public interface SceneParticleAccess {
    /**
     * @return native lifetime in ticks
     */
    @Accessor("particleMaxAge")
    int unigui$lifetime();

    /**
     * @param ticks deterministic native lifetime in ticks
     */
    @Accessor("particleMaxAge")
    void unigui$lifetime(int ticks);

    /**
     * @return native billboard scale
     */
    @Accessor("particleScale")
    float unigui$scale();

    /**
     * @param scale deterministic native sprite scale
     */
    @Accessor("particleScale")
    void unigui$scale(float scale);

    /**
     * @return column in the 16-by-16 atlas grid
     */
    @Accessor("particleTextureIndexX")
    int unigui$column();

    /**
     * @return row in the 16-by-16 atlas grid
     */
    @Accessor("particleTextureIndexY")
    int unigui$row();

    /**
     * @return native particle opacity in [0, 1]
     */
    @Accessor("particleAlpha")
    float unigui$alpha();

    /**
     * @param red red particle component in [0, 1]
     */
    @Accessor("particleRed")
    void unigui$red(float red);

    /**
     * @param green green particle component in [0, 1]
     */
    @Accessor("particleGreen")
    void unigui$green(float green);

    /**
     * @param blue blue particle component in [0, 1]
     */
    @Accessor("particleBlue")
    void unigui$blue(float blue);
}
