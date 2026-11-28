package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.SceneTimeline;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Samples actual vanilla model poses with no world or graphics device. */
final class SceneEntityAnimationTest {
    /** Single-precision tolerance for model pose and timeline input comparisons. */
    private static final float EPSILON = 1e-6f;

    /** Freezes Minecraft registries before testing type-based previews. */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** The native spider model changes its leg pose and can be sampled back at the same phase. */
    @Test
    void walk_spiderModelPosesLegsAndRewinds() {
        var node = new EntityNode("minecraft:spider");
        var model = new SpiderModel(SpiderModel.createSpiderBodyLayer().bakeRoot());
        var state = new LivingEntityRenderState();
        model.setupAnim(state);
        var idle = SceneNativeMeshKey.PartPose.capture(model.allParts());

        node.setAnimation(1.25f, 2.5f, 0.7f);
        SceneEntity.sampleAnimation(node, state);
        assertEquals(25, state.ageInTicks, EPSILON);
        assertEquals(2.5f, state.walkAnimationPos, EPSILON);
        assertEquals(0.7f, state.walkAnimationSpeed, EPSILON);
        model.setupAnim(state);
        assertFalse(idle.matches(model.allParts()));
        var walking = SceneNativeMeshKey.PartPose.capture(model.allParts());

        SceneEntity.sampleAnimation(node.setAnimation(0, 0, 0), state);
        model.setupAnim(state);
        assertTrue(idle.matches(model.allParts()));
        SceneEntity.sampleAnimation(node.setAnimation(1.25f, 2.5f, 0.7f), state);
        model.setupAnim(state);
        assertTrue(walking.matches(model.allParts()));
    }

    /** A typed callback can select a real vanilla golem attack at an absolute animation time. */
    @Test
    void nativeAnimation_typedCallbackDrivesVanillaGolemAttack() {
        var node = new EntityNode("minecraft:iron_golem").setAnimation(1.25f, 0, 0);
        var model = new IronGolemModel(IronGolemModel.createBodyLayer().bakeRoot());
        var state = new IronGolemRenderState();
        model.setupAnim(state);
        var idle = SceneNativeMeshKey.PartPose.capture(model.allParts());
        node.setNativeAnimation(IronGolemRenderState.class, golem -> golem.attackTicksRemaining = 30 - golem.ageInTicks);

        SceneEntity.sampleAnimation(node, state);
        assertEquals(5, state.attackTicksRemaining, EPSILON);
        model.setupAnim(state);
        assertFalse(idle.matches(model.allParts()));
        // Other state types are still eligible for the generic clock.
        var other = new LivingEntityRenderState();
        SceneEntity.sampleAnimation(node, other);
        assertEquals(25, other.ageInTicks, EPSILON);
        node.clearNativeAnimation();
        var next = new IronGolemRenderState();
        SceneEntity.sampleAnimation(node, next);
        model.setupAnim(next);
        assertTrue(idle.matches(model.allParts()));
    }

    /** The built-in action turns the armed vanilla model's sword arm and seeks back to the same pose. */
    @Test
    void action_zombieMainHandSwing_changesSwordArmAndReturnsToIdle() {
        var node = new EntityNode("minecraft:zombie");
        var mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
        var model = new ZombieModel<ZombieRenderState>(LayerDefinition.create(mesh, 64, 64).bakeRoot());
        var timeline = new SceneTimeline();
        timeline.track(node).action(1, MinecraftEntityActions.mainHandSwing());
        var state = armedZombieState();
        model.setupAnim(state);
        float idle = model.rightArm.xRot;

        timeline.sample(1.15f);
        SceneEntity.sampleAnimation(node, state);
        assertEquals(InteractionHand.MAIN_HAND, state.currentSwing.hand());
        assertEquals(0.5f, state.swingAnimation, EPSILON);
        model.setupAnim(state);
        float swung = model.rightArm.xRot;
        assertTrue(Math.abs(swung - idle) > 0.1f);

        timeline.sample(1.3f);
        var next = armedZombieState();
        SceneEntity.sampleAnimation(node, next);
        model.setupAnim(next);
        assertEquals(idle, model.rightArm.xRot, EPSILON);

        timeline.sample(1.15f);
        SceneEntity.sampleAnimation(node, next);
        model.setupAnim(next);
        assertEquals(swung, model.rightArm.xRot, EPSILON);
    }

    /** @return a fresh render state holding a sword in the right hand */
    private static ZombieRenderState armedZombieState() {
        var state = new ZombieRenderState();
        state.rightHandItemStack = new ItemStack(Holder.direct(Items.GOLDEN_SWORD, DataComponentMap.EMPTY));
        state.rightArmPose = HumanoidModel.ArmPose.ITEM;
        return state;
    }

    /** Menu previews get dedicated state instances and clearing animation restores the default model. */
    @Test
    void preview_animatedStateDoesNotChangeDefaultOrOtherFrames() {
        var preview = SceneEntity.preview(MinecraftClientCompat.ZOMBIE);
        var node = new EntityNode("minecraft:zombie").setAnimation(0.5f, 3, 1);
        node.setNativeAnimation(ZombieRenderState.class, zombie -> zombie.isAggressive = true);
        var first = (ZombieRenderState) preview.fixedState(true);
        SceneEntity.sampleAnimation(node, first);
        assertEquals(10, first.ageInTicks, EPSILON);
        assertTrue(first.isAggressive);
        assertNotSame(first, preview.fixedState(true));
        var defaults = (ZombieRenderState) preview.fixedState(false);
        assertEquals(0, defaults.ageInTicks, EPSILON);
        assertFalse(defaults.isAggressive);
    }
}
