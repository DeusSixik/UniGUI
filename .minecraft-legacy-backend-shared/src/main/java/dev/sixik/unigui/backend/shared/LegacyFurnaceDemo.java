package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.demo.ui.scene.SceneFurnaceTutorial;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Opens the shared furnace tutorial with the legacy clients' item previews.
 */
public final class LegacyFurnaceDemo {
    private LegacyFurnaceDemo() {
    }

    /**
     * Opens the animated furnace tutorial on the client thread.
     *
     * @param floor       factory for a floor tile, with {@code true} for the center tile.
     * @param furnace     node for the furnace block model.
     * @param setLit      switches the furnace between cold and lit models.
     * @param floorIcon   item displayed during floor construction.
     * @param furnaceIcon item displayed during the fall and bounce.
     * @param fireIcon    item displayed during ignition.
     */
    public static void open(Function<Boolean, ? extends SceneNode> floor, SceneNode furnace, Consumer<Boolean> setLit,
                            ItemStack floorIcon, ItemStack furnaceIcon, ItemStack fireIcon) {
        var sequence = create(floor, furnace, setLit, floorIcon, furnaceIcon, fireIcon);
        new UniScreen(sequence.title(), new SceneTutorialPlayer(sequence)).openGui();
    }

    /**
     * Builds the furnace tutorial with item preview icons, for a screen or for an embedded player.
     *
     * @param floor       factory for a floor tile, with {@code true} for the center tile.
     * @param furnace     node for the furnace block model.
     * @param setLit      switches the furnace between cold and lit models.
     * @param floorIcon   item displayed during floor construction.
     * @param furnaceIcon item displayed during the fall and bounce.
     * @param fireIcon    item displayed during ignition.
     * @return a new sequence with its own scene.
     */
    public static SceneTutorialPlayer.Sequence create(Function<Boolean, ? extends SceneNode> floor, SceneNode furnace,
                                                      Consumer<Boolean> setLit, ItemStack floorIcon,
                                                      ItemStack furnaceIcon, ItemStack fireIcon) {
        return SceneFurnaceTutorial.create(floor, furnace, setLit,
                () -> preview(floorIcon), () -> preview(furnaceIcon), () -> preview(fireIcon));
    }

    /**
     * Builds a compact item icon for a checkpoint card.
     *
     * @param stack item used as the icon.
     * @return preview with a 16-unit icon and hidden label and decorations.
     */
    public static MinecraftItemPreviewWidget preview(ItemStack stack) {
        var preview = new MinecraftItemPreviewWidget("", stack);
        preview.previewSize(16).labelVisible(false);
        return preview.decorations(false);
    }
}
