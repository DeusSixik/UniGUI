package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SceneTimeline;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftEntityActions;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftParticleEmitter;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftScene;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.Sequence;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;

import java.util.ArrayList;
import java.util.List;

import static dev.sixik.unigui.demo.ui.scene.SceneFurnaceTutorial.*;
import static dev.sixik.unigui.tests.SceneTutorialDemo.icon;

/**
 * Supplies Minecraft models and icons for the furnace tutorial.
 */
public final class SceneFurnaceTutorial {
    /**
     * Wide, low smoke with a 0.36-block depth transition and a translucent base alpha.
     */
    private static final MinecraftParticleEmitter.SoftParticles FLOOR_SMOKE =
            new MinecraftParticleEmitter.SoftParticles(0.36f, 2.1f, 0.72f, 0.28f);
    /**
     * Slow horizontal drift in blocks per tick, with variation spread across each tile.
     */
    private static final MinecraftParticleEmitter.Motion FLOOR_SMOKE_MOTION =
            new MinecraftParticleEmitter.Motion(0.003f, 0, 0.003f, 0.003f, 0.14f, 0.9f);

    /**
     * This class supplies demo factories.
     */
    private SceneFurnaceTutorial() {
    }

    /**
     * @return a new tutorial with vanilla blocks and the additional models supported by this renderer
     */
    public static Sequence create() {
        var cold = Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH);
        var lit = cold.setValue(FurnaceBlock.LIT, true);
        var furnace = BlockNode.of(cold);
        var smoke = new ArrayList<MinecraftParticleEmitter>();
        return createWithIgnition(
                center -> floorTile(center, smoke),
                furnace, burning -> furnace.setState(burning ? lit : cold),
                icon(Items.DEEPSLATE_TILES, "deepslate_tiles"), icon(Items.FURNACE, "furnace"), icon(Items.COAL, "coal"),
                ignition -> {
                    if (MinecraftClientCompat.NATIVE_SCENE_PARTICLES) addIgnitionParticles(ignition, furnace, smoke);
                    return MinecraftClientCompat.NATIVE_SCENE_MATERIALS ? effectModels(ignition) : List.of();
                }, MinecraftClientCompat.NATIVE_SCENE_MATERIALS, MinecraftClientCompat.NATIVE_SCENE_PARTICLES);
    }

    /**
     * Gives each exposed tile its own soft smoke source, 0.16 blocks above its top face.
     *
     * @param center whether this tile is under the furnace
     * @param smoke  collection of sources sampled by the ignition timeline
     * @return the tile with its smoke source where supported
     */
    private static BlockNode floorTile(boolean center, List<MinecraftParticleEmitter> smoke) {
        var tile = BlockNode.of((center ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState());
        if (MinecraftClientCompat.NATIVE_SCENE_PARTICLES && !center) {
            // Stagger births so neighboring patches build up at different times after ignition.
            double start = 0.06 + smoke.size() * 0.025;
            var emitter = new MinecraftParticleEmitter(ParticleTypes.CAMPFIRE_COSY_SMOKE, start, 2.75, 6)
                    .setMotion(FLOOR_SMOKE_MOTION).setSoftParticles(FLOOR_SMOKE);
            emitter.setPosition(0.5f, 1.16f, 0.5f);
            tile.addChild(emitter);
            smoke.add(emitter);
        }
        return tile;
    }

    /**
     * Attaches fire to the furnace opening and binds the particle sources to ignition time.
     *
     * @param ignition stage whose time zero marks ignition
     * @param furnace   parent in furnace block coordinates
     * @param smoke     sources supplied as the floor tiles are created
     */
    private static void addIgnitionParticles(SceneTimeline ignition, SceneNode furnace, List<MinecraftParticleEmitter> smoke) {
        var flame = new MinecraftParticleEmitter(ParticleTypes.FLAME, 0, 1.65, 12)
                .setMotion(new MinecraftParticleEmitter.Motion(0, 0.04f, 0.055f, 0.018f, 0.085f, 0.9f));
        flame.setPosition(0.5f, 0.42f, 1.12f);
        furnace.addChild(flame);
        ignition.onSample(flame::setTime);
        for (var emitter : smoke) ignition.onSample(emitter::setTime);
    }

    /**
     * Sets up the models framing the furnace and their post-ignition poses.
     *
     * @param motion timeline in seconds relative to ignition
     * @return native models, with additional layers and a cave spider when a client world is available
     */
    private static List<SceneNode> effectModels(SceneTimeline motion) {
        var zombie = new EntityNode("minecraft:zombie").setPosition(-0.55f, 1, 0.2f).setScale(0.8f, 0.8f, 0.8f);
        var creeper = new EntityNode("minecraft:creeper").setPosition(1.47f, 1, -0.57f).setScale(0.8f, 0.8f, 0.8f);
        motion.track(zombie)
                .walkTo(0.2f, 1.5f, -0.44f, 1, 0.83f, AnimationEasing.QUAD_IN_OUT, 15, 0.58f)
                .yaw(1.6f, 0.90f)
                .head(0, 0, 0, null).head(1.6f, 0.32f, 0.22f, AnimationEasing.QUAD_IN_OUT)
                .action(1.6f, MinecraftEntityActions.mainHandSwing(), 0.8f, 0);
        motion.track(creeper)
                .walk(0, 0, 0)
                .head(0, 0, 0, null).head(1.3f, -0.55f, 0.20f, AnimationEasing.QUAD_IN_OUT)
                .head(2.4f, -0.35f, 0.08f, AnimationEasing.QUAD_IN_OUT);
        List<SceneNode> models = new ArrayList<>(List.of(
                zombie, creeper,
                ItemNode.of(SceneDemoItems.stack(Items.DIAMOND, "diamond")).setPosition(-0.62f, 1.52f, 1.42f),
                ItemNode.of(SceneDemoItems.foil(Items.IRON_SWORD, "iron_sword")).setPosition(1.54f, 1.52f, 0.84f)));
        if (MinecraftScene.entity(zombie) instanceof LivingEntity preview) {
            preview.setItemSlot(EquipmentSlot.HEAD, SceneDemoItems.helmet(preview));
            preview.setItemSlot(EquipmentSlot.MAINHAND, SceneDemoItems.stack(Items.GOLDEN_SWORD, "golden_sword"));
            var spider = new EntityNode("minecraft:cave_spider").setPosition(0.78f, 1, -0.56f);
            models.add(spider);
            motion.track(spider)
                    .walkTo(0.75f, 2.8f, -0.37f, 1, -0.56f, AnimationEasing.QUAD_IN_OUT, 12, 0.6f);
        }
        return models;
    }
}
