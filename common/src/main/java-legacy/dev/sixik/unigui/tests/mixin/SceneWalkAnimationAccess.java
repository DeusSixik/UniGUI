package dev.sixik.unigui.tests.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives offscreen previews exact, reversible control of the native walk interpolator. */
@Mixin(WalkAnimationState.class)
public interface SceneWalkAnimationAccess {
    /** @return previous tick's stride strength */
    @Accessor("speedOld")
    float unigui$getPreviousSpeed();

    /** @param strength previous tick's stride strength */
    @Accessor("speedOld")
    void unigui$setPreviousSpeed(float strength);

    /** @param phase accumulated walk phase in native units */
    @Accessor("position")
    void unigui$setPosition(float phase);
}
