package dev.sixik.unigui.cleanroom.commands;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SceneTimeline;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.backend.shared.LegacyFurnaceDemo;
import dev.sixik.unigui.cleanroom.backend.CleanroomCampfireSmoke;
import dev.sixik.unigui.cleanroom.backend.CleanroomEntityActions;
import dev.sixik.unigui.cleanroom.backend.CleanroomParticleEmitter;
import dev.sixik.unigui.cleanroom.backend.CleanroomScene;
import dev.sixik.unigui.demo.ui.scene.SceneFurnaceTutorial;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies the shared Furnace sequence with Cleanroom's block, item, entity and particle models.
 */
final class CleanroomFurnaceDemo {
    private CleanroomFurnaceDemo() {
    }

    /**
     * Opens the common Furnace player using the capabilities of the current GL context.
     */
    static void open() {
        var cold = Blocks.FURNACE.getDefaultState().withProperty(BlockFurnace.FACING, EnumFacing.SOUTH);
        var lit = Blocks.LIT_FURNACE.getDefaultState().withProperty(BlockFurnace.FACING, EnumFacing.SOUTH);
        var furnace = BlockNode.of(cold);
        var floor = new ItemStack(Blocks.STONEBRICK);
        var furnaceIcon = new ItemStack(Blocks.FURNACE);
        var coal = new ItemStack(Items.COAL);
        boolean advanced = GL.getCapabilities().OpenGL32;
        if (!advanced) {
            LegacyFurnaceDemo.open(CleanroomFurnaceDemo::tile, furnace, burning -> furnace.setState(burning ? lit : cold),
                    floor, furnaceIcon, coal);
            return;
        }
        var smoke = new ArrayList<CleanroomCampfireSmoke>();
        var sequence = SceneFurnaceTutorial.createWithIgnition(center -> {
                    var tile = tile(center);
                    if (!center) {
                        var emitter = floorSmoke(smoke.size());
                        tile.addChild(emitter);
                        smoke.add(emitter);
                    }
                    return tile;
                }, furnace, burning -> furnace.setState(burning ? lit : cold),
                () -> LegacyFurnaceDemo.preview(floor), () -> LegacyFurnaceDemo.preview(furnaceIcon),
                () -> LegacyFurnaceDemo.preview(coal), ignition -> {
                    addFire(ignition, furnace);
                    smoke.forEach(emitter -> ignition.onSample(emitter::setTime));
                    return models(ignition);
                }, true, true);
        new UniScreen(sequence.title(), new SceneTutorialPlayer(sequence)).openGui();
    }

    /**
     * @param center whether the tile supports the furnace @return stone-brick floor tile
     */
    private static BlockNode tile(boolean center) {
        var state = Blocks.STONEBRICK.getDefaultState();
        return BlockNode.of(center ? state.withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CHISELED) : state);
    }

    /**
     * Places each emitter over its tile and staggers the first birth by
     * {@code 0.06 + index * 0.025} seconds to match the modern demo.
     *
     * @param index zero-based index of the exposed tile's emitter
     * @return smoke emitter in tile-local coordinates
     */
    private static CleanroomCampfireSmoke floorSmoke(int index) {
        var emitter = new CleanroomCampfireSmoke(0.06 + index * 0.025, 2.75, 6);
        emitter.setPosition(0.5f, 1.16f, 0.5f);
        return emitter;
    }

    /**
     * @param timeline ignition timeline in seconds
     * @param furnace  parent for the fire, in block coordinates
     */
    private static void addFire(SceneTimeline timeline, SceneNode furnace) {
        var flame = new CleanroomParticleEmitter(EnumParticleTypes.FLAME, 0, 1.65, 12)
                .setMotion(new CleanroomParticleEmitter.Motion(0, 0.035f, 0.045f, 0.018f, 0.085f, 0.9f));
        flame.setPosition(0.5f, 0.42f, 1.12f);
        furnace.addChild(flame);
        timeline.onSample(flame::setTime);
    }

    /**
     * @param timeline ignition timeline in seconds @return models to receive the selected material effect
     */
    private static List<SceneNode> models(SceneTimeline timeline) {
        var zombie = new EntityNode("minecraft:zombie").setPosition(-0.55f, 1, 0.2f).setScale(0.8f, 0.8f, 0.8f);
        var creeper = new EntityNode("minecraft:creeper").setPosition(1.47f, 1, -0.57f).setScale(0.8f, 0.8f, 0.8f);
        var spider = new EntityNode("minecraft:cave_spider").setPosition(0.78f, 1, -0.56f);
        timeline.track(zombie)
                .walkTo(0.2f, 1.5f, -0.44f, 1, 0.83f, AnimationEasing.QUAD_IN_OUT, 15, 0.58f)
                .yaw(1.6f, 0.9f)
                .head(0, 0, 0, null).head(1.6f, 0.32f, 0.22f, AnimationEasing.QUAD_IN_OUT)
                .action(1.6f, CleanroomEntityActions.mainHandSwing(), 0.8f, 0);
        timeline.track(creeper).walk(0, 0, 0)
                .head(0, 0, 0, null).head(1.3f, -0.55f, 0.20f, AnimationEasing.QUAD_IN_OUT)
                .head(2.4f, -0.35f, 0.08f, AnimationEasing.QUAD_IN_OUT);
        timeline.track(spider)
                .walkTo(0.75f, 2.8f, -0.37f, 1, -0.56f, AnimationEasing.QUAD_IN_OUT, 12, 0.6f);
        if (CleanroomScene.entity(zombie) instanceof EntityLivingBase preview) {
            preview.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            preview.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        }
        var sword = new ItemStack(Items.IRON_SWORD);
        sword.addEnchantment(Enchantments.UNBREAKING, 1);
        return List.of(zombie, creeper, spider,
                ItemNode.of(new ItemStack(Items.DIAMOND)).setPosition(-0.62f, 1.52f, 1.42f),
                ItemNode.of(sword).setPosition(1.54f, 1.52f, 0.84f));
    }
}
