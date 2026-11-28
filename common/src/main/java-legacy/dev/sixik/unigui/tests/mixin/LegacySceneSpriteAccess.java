package dev.sixik.unigui.tests.mixin;

import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reads the current atlas rectangle from the native quad particle. */
@Mixin(SingleQuadParticle.class)
public interface LegacySceneSpriteAccess {
    /** @return left atlas U coordinate */
    @Invoker("getU0")
    float unigui$getU0();

    /** @return right atlas U coordinate */
    @Invoker("getU1")
    float unigui$getU1();

    /** @return top atlas V coordinate */
    @Invoker("getV0")
    float unigui$getV0();

    /** @return bottom atlas V coordinate */
    @Invoker("getV1")
    float unigui$getV1();
}
