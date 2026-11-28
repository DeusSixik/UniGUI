package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads local particle state without invoking native world lighting or submitting a world draw. */
@Mixin(Particle.class)
public interface LegacySceneParticleAccess {
    /** @param enabled whether a particle may collide with a Minecraft level */
    @Accessor("hasPhysics")
    void unigui$setPhysics(boolean enabled);

    /** @return X position in blocks */
    @Accessor("x")
    double unigui$getX();

    /** @return Y position in blocks */
    @Accessor("y")
    double unigui$getY();

    /** @return Z position in blocks */
    @Accessor("z")
    double unigui$getZ();

    /** @return billboard roll in radians */
    @Accessor("roll")
    float unigui$getRoll();

    /** @return red tint in [0, 1] */
    @Accessor("rCol")
    float unigui$getRed();

    /** @return green tint in [0, 1] */
    @Accessor("gCol")
    float unigui$getGreen();

    /** @return blue tint in [0, 1] */
    @Accessor("bCol")
    float unigui$getBlue();

    /** @return sprite opacity in [0, 1] */
    @Accessor("alpha")
    float unigui$getAlpha();
}
