package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.EntityActionClip;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.tests.mixin.SceneWalkAnimationAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Samples scene animation for renderers that read the living entity's pose directly.
 */
final class LegacySceneAnimation {
    /**
     * Native ticks per second used by the scene clock and default swing clip.
     */
    private static final int TICKS_PER_SECOND = 20;
    /**
     * Reusable six-tick vanilla swing preset for the legacy animation sample.
     */
    static final EntityActionClip<LegacySceneAnimation> SWING = new EntityActionClip<>(
            LivingEntity.SWING_DURATION / (float) TICKS_PER_SECOND,
            LegacySceneAnimation.class, (animation, progress) -> animation.swing = progress);
    /**
     * Sampled swing progress, or -1 when no swing action applies.
     */
    private float swing;
    /**
     * Original vanilla age in ticks.
     */
    private int age;
    /**
     * Original pitch and previous-tick pitch in degrees.
     */
    private float pitch, previousPitch;
    /**
     * Original absolute head yaw and previous head and body yaw in degrees.
     */
    private float head, previousHead, previousBody;
    /**
     * Original gait phase and stride strengths.
     */
    private float phase, speed, previousSpeed;
    /**
     * Original current and previous swing progress.
     */
    private float attack, previousAttack;
    /**
     * Hand selected by the entity's original swing.
     */
    private InteractionHand hand;

    /**
     * Samples the scene clock, gait, look direction and active action. With native gait phase p,
     * vanilla displays {@code p - strength * (1 - partial)}. Supplying
     * {@code p = desiredPhase + strength * (1 - partial)} preserves the sampled phase at any partial tick.
     *
     * @param entity  native entity used for one model render
     * @param node    scene pose and action overrides
     * @param partial original render partial tick in [0, 1]
     * @return partial tick used with the sampled age
     */
    float apply(Entity entity, EntityNode node, float partial) {
        age = entity.tickCount;
        pitch = entity.getXRot();
        previousPitch = entity.xRotO;
        swing = -1;
        node.sampleNativeAnimation(this);
        if (node.hasAnimation()) {
            double ticks = Math.min(TICKS_PER_SECOND * (double) node.animationSeconds(), Integer.MAX_VALUE);
            entity.tickCount = (int) ticks;
            partial = (float) (ticks - entity.tickCount);
        }
        if (node.hasHead()) {
            float degrees = (float) Math.toDegrees(node.headPitch());
            entity.setXRot(degrees);
            entity.xRotO = degrees;
        }
        if (entity instanceof LivingEntity living) {
            head = living.yHeadRot;
            previousHead = living.yHeadRotO;
            previousBody = living.yBodyRotO;
            var walk = living.walkAnimation;
            var access = (SceneWalkAnimationAccess) walk;
            phase = walk.position();
            speed = walk.speed();
            previousSpeed = access.unigui$getPreviousSpeed();
            hand = living.swingingArm;
            attack = living.attackAnim;
            previousAttack = living.oAttackAnim;
            if (node.hasHead()) {
                living.yHeadRot = living.yBodyRot + (float) Math.toDegrees(node.headYaw());
                living.yHeadRotO = living.yHeadRot;
                living.yBodyRotO = living.yBodyRot;
            }
            if (node.hasAnimation()) {
                float strength = node.walkStrength();
                walk.setSpeed(strength);
                access.unigui$setPreviousSpeed(strength);
                access.unigui$setPosition(node.walkPhase() + strength * (1 - partial));
            }
            if (swing >= 0) {
                living.swingingArm = InteractionHand.MAIN_HAND;
                living.attackAnim = swing;
                living.oAttackAnim = swing;
            }
        }
        return partial;
    }

    /**
     * Restores the pose of the entity after offscreen rendering.
     *
     * @param entity entity used by the preceding apply call
     */
    void restore(Entity entity) {
        entity.tickCount = age;
        entity.setXRot(pitch);
        entity.xRotO = previousPitch;
        if (entity instanceof LivingEntity living) {
            living.yHeadRot = head;
            living.yHeadRotO = previousHead;
            living.yBodyRotO = previousBody;
            living.swingingArm = hand;
            living.attackAnim = attack;
            living.oAttackAnim = previousAttack;
            var walk = living.walkAnimation;
            var access = (SceneWalkAnimationAccess) walk;
            walk.setSpeed(speed);
            access.unigui$setPreviousSpeed(previousSpeed);
            access.unigui$setPosition(phase);
        }
    }
}
