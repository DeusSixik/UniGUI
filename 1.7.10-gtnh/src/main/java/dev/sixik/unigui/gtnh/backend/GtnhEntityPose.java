package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.scene.EntityNode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

/**
 * Saves and applies pose data for a single native 1.7.10 render.
 */
final class GtnhEntityPose {
    /**
     * Original age in ticks and pitch angles in degrees.
     */
    private final int age;
    private final float pitch, previousPitch;
    /**
     * Original fields read by the living entity renderer.
     */
    private final LivingPose living;
    /**
     * Sampled main-hand swing progress, with negative values leaving the entity's swing alone.
     */
    private float swing = -1;

    /**
     * @param entity source of the original native state
     */
    GtnhEntityPose(Entity entity) {
        age = entity.ticksExisted;
        pitch = entity.rotationPitch;
        previousPitch = entity.prevRotationPitch;
        living = entity instanceof EntityLivingBase value ? new LivingPose(value) : null;
    }

    /**
     * @param progress normalized progress through a swing clip
     */
    void swing(float progress) {
        swing = progress;
    }

    /**
     * Vanilla evaluates gait as {@code limbSwing - strength * (1 - partial)}. Add
     * {@code strength * (1 - partial)} to the stored phase so the sampled pose is independent of render timing.
     *
     * @param entity  saved native entity
     * @param node    node with the sampled scene time and actions
     * @param partial original fractional render tick
     * @return fractional tick to use with the overridden age
     */
    float apply(Entity entity, EntityNode node, float partial) {
        if (node.hasAnimation()) {
            double ticks = Math.min(node.animationSeconds() * 20.0, Integer.MAX_VALUE);
            entity.ticksExisted = (int) ticks;
            partial = (float) (ticks - entity.ticksExisted);
        }
        if (node.hasHead()) {
            entity.rotationPitch = (float) Math.toDegrees(node.headPitch());
            entity.prevRotationPitch = entity.rotationPitch;
        }
        if (entity instanceof EntityLivingBase value) {
            node.sampleNativeAnimation(this);
            if (node.hasHead()) {
                value.rotationYawHead = value.renderYawOffset + (float) Math.toDegrees(node.headYaw());
                value.prevRotationYawHead = value.rotationYawHead;
                value.prevRenderYawOffset = value.renderYawOffset;
            }
            if (node.hasAnimation()) {
                float strength = node.walkStrength();
                value.limbSwing = node.walkPhase() / (value.isChild() ? 3 : 1) + strength * (1 - partial);
                value.limbSwingAmount = value.prevLimbSwingAmount = strength;
            }
            if (swing >= 0) {
                value.swingProgress = value.prevSwingProgress = swing;
            }
        }
        return partial;
    }

    /**
     * @param entity target that receives the original native state
     */
    void restore(Entity entity) {
        entity.ticksExisted = age;
        entity.rotationPitch = pitch;
        entity.prevRotationPitch = previousPitch;
        if (living != null) living.restore((EntityLivingBase) entity);
    }

    /**
     * Original head, gait and swing fields, captured before the scene overrides them.
     */
    private record LivingPose(float head, float previousHead, float previousBody,
                              float gait, float strength, float previousStrength,
                              float swing, float previousSwing) {
        /**
         * @param entity renderer source
         */
        private LivingPose(EntityLivingBase entity) {
            this(entity.rotationYawHead, entity.prevRotationYawHead, entity.prevRenderYawOffset,
                entity.limbSwing, entity.limbSwingAmount, entity.prevLimbSwingAmount,
                entity.swingProgress, entity.prevSwingProgress);
        }

        /**
         * @param entity source to restore after rendering
         */
        private void restore(EntityLivingBase entity) {
            entity.rotationYawHead = head;
            entity.prevRotationYawHead = previousHead;
            entity.prevRenderYawOffset = previousBody;
            entity.limbSwing = gait;
            entity.limbSwingAmount = strength;
            entity.prevLimbSwingAmount = previousStrength;
            entity.swingProgress = swing;
            entity.prevSwingProgress = previousSwing;
        }
    }
}
