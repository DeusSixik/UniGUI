package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.FloatInterpolator;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.Checkpoint;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.Sequence;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialStage;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.minecraft.MinecraftDemoHost;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.function.Supplier;

/**
 * Открывает туториалы с ванильными моделями современных версий Minecraft.
 */
public final class SceneTutorialDemo {
    /**
     * Начало появления воронки в секундах.
     */
    private static final float HOPPER_IN = 1.5f;
    /**
     * Момент установки воронки в секундах.
     */
    private static final float HOPPER_SEATED = 2.75f;
    /**
     * Момент появления предмета над воронкой в секундах.
     */
    private static final float ITEM_IN = 3.75f;
    /**
     * Момент начала падения предмета в секундах.
     */
    private static final float ITEM_FALL = 4.5f;
    /**
     * Момент передачи предмета в сундук в секундах.
     */
    private static final float ITEM_STORED = 6.25f;

    private SceneTutorialDemo() {
    }

    /**
     * Открывает туториал на клиентском потоке.
     */
    public static void open() {
        Minecraft.getInstance().execute(() -> MinecraftClientCompat.setScreen(
                new MinecraftWidgetScreen(Component.literal("Hopper and chest"), new SceneTutorialPlayer(hopperSequence()))));
    }

    /**
     * Открывает сцену с печкой на клиентском потоке.
     */
    public static void openFurnace() {
        Minecraft.getInstance().execute(() -> {
            if (MinecraftClientCompat.NATIVE_SCENE_MATERIALS) MinecraftDemoHost.install();
            var sequence = SceneFurnaceTutorial.create();
            MinecraftClientCompat.setScreen(new MinecraftWidgetScreen(Component.literal(sequence.title()),
                    new OverlayLayer(new SceneTutorialPlayer(sequence))));
        });
    }

    /**
     * Создаёт исходный сценарий о передаче предмета в сундук.
     *
     * @return сцена и функция, которая восстанавливает любую позу по времени.
     */
    static Sequence hopperSequence() {
        Scene3D viewport = new Scene3D();
        viewport.camera().setOrthographic(5.4f, 0.1f, 32);
        viewport.orbit().setOrbit(new MutableVec3(0.5f, 1.76f, 0.5f), 8, 0.65, 0.47);
        BlockNode hopper = BlockNode.of(Blocks.HOPPER.defaultBlockState());
        ItemNode item = ItemNode.of(SceneDemoItems.ironIngot());
        item.setScale(0.58f, 0.58f, 0.58f).setRotation(new MutableQuat().setYawPitch(0.3, -0.15));
        BlockNode chest = new BlockNode("minecraft:chest[facing=south]");
        chest.setPosition(0, 1, 0);
        viewport.scene().addRoot(platform()).addRoot(chest).addRoot(hopper).addRoot(item);
        var pointer = SceneTutorialStage.focus(viewport);

        return new Sequence("Hopper and chest", viewport, 9, List.of(
                new Checkpoint(0, "Chest", "Place a chest",
                        "It will receive the items. Leave room above it for the hopper.", icon(Items.CHEST, "chest")),
                new Checkpoint(HOPPER_IN, "Hopper", "Aim the hopper",
                        "Sneak and place the hopper on the chest. Its spout must point down.", icon(Items.HOPPER, "hopper")),
                new Checkpoint(ITEM_IN, "Load", "Load the hopper",
                        "Drop an ingot on top. The hopper picks it up at its opening.", icon(Items.IRON_INGOT, "iron_ingot")),
                new Checkpoint(ITEM_STORED, "Done", "The ingot is in the chest",
                        "Check the chest. A redstone signal stops the transfer.", icon(Items.CHEST, "chest"))
        ), seconds -> {
            // Для обеих высот: y = from + (to - from) * easing((seconds - start) / (end - start)).
            float placement = AnimationEasing.CUBIC_OUT.apply((seconds - HOPPER_IN) / (HOPPER_SEATED - HOPPER_IN));
            float hopperY = FloatInterpolator.LINEAR.interpolate(3.05f, 2, placement);
            hopper.setVisible(seconds >= HOPPER_IN).setPosition(0, hopperY, 0);

            float fall = AnimationEasing.QUAD_IN.apply((seconds - ITEM_FALL) / (ITEM_STORED - ITEM_FALL));
            float itemY = FloatInterpolator.LINEAR.interpolate(3.75f, 2.8f, fall);
            item.setVisible(seconds >= ITEM_IN && seconds < ITEM_STORED).setPosition(0.5f, itemY, 0.5f);

            if (seconds >= ITEM_IN && seconds < ITEM_STORED) pointer.position(1.04f, itemY + 0.06f, 0.52f);
            else if (seconds >= HOPPER_IN && seconds < ITEM_IN) pointer.position(0.57f, hopperY + 0.78f, 1.05f);
            else pointer.position(0.5f, 1.52f, 1.015f);
        });
    }

    /**
     * Строит основание под туториалом.
     *
     * @return структура с платформой размером три на три блока.
     */
    private static StructureNode platform() {
        StructureNode base = new StructureNode();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                var block = Blocks.DEEPSLATE_TILES;
                if (x == 0 && z == 0) block = Blocks.GILDED_BLACKSTONE;
                else if (x != 0 && z != 0) block = Blocks.POLISHED_DEEPSLATE;
                base.setBlock(x, 0, z, block.defaultBlockState());
            }
        }
        return base;
    }

    /**
     * Создаёт иконку этапа с моделью для текущей версии Minecraft.
     *
     * @param item  предмет для предпросмотра.
     * @param model путь модели в 26.x.
     * @return фабрика виджета для карточки этапа.
     */
    static Supplier<MinecraftItemPreviewWidget> icon(Item item, String model) {
        return () -> {
            var preview = new MinecraftItemPreviewWidget("", SceneDemoItems.stack(item, model));
            preview.previewSize(16).labelVisible(false);
            return preview.decorations(false);
        };
    }
}
