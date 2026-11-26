package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.Sequence;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;

import static dev.sixik.unigui.tests.SceneTutorialDemo.icon;

/** Подставляет материалы и иконки текущей версии Minecraft для сценария о печке. */
public final class SceneFurnaceTutorial {
    private SceneFurnaceTutorial() {
    }

    /** @return новый сценарий, использующий ванильные модели глубинного сланца и печки. */
    public static Sequence create() {
        var cold = Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH);
        var lit = cold.setValue(FurnaceBlock.LIT, true);
        var furnace = BlockNode.of(cold);
        return dev.sixik.unigui.demo.ui.scene.SceneFurnaceTutorial.create(
                center -> BlockNode.of((center ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState()),
                furnace, burning -> furnace.setState(burning ? lit : cold),
                icon(Items.DEEPSLATE_TILES, "deepslate_tiles"), icon(Items.FURNACE, "furnace"), icon(Items.COAL, "coal"));
    }
}
