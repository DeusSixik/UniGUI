package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.StructureNode;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.rendering.RenderToTexture;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.Objects;

/**
 * Общая для всех версий 3D-сцена для галереи {@code UniGuiDemo}: дом из {@link StructureNode} собирается по слоям, рядом стоят
 * {@link BlockNode}, {@link ItemNode} и {@link EntityNode}, поверх сцены лежит панель управления.
 *
 * <p>Контент задаётся строками, поэтому код сцены одинаков на 1.7.10, 1.12.2 и 1.20+. Различаются только
 * идентификаторы блоков, их выбирает {@link Blocks}. На 1.7.10 и 1.12.2 предметы и мобы пока не рисуются.</p>
 */
public final class SceneShowcase {
    /**
     * Ширина дома по X и Z в блоках.
     */
    private static final int SIZE = 7;
    /**
     * Высота стен над полом.
     */
    private static final int WALL_HEIGHT = 3;
    /**
     * Длительность сборки дома в секундах.
     */
    private static final float ASSEMBLY_SECONDS = 4;

    private SceneShowcase() {
    }

    /**
     * Создаёт виджет сцены с панелью управления.
     *
     * @param blocks идентификаторы блоков текущей версии.
     * @return готовый виджет; сборка дома запускается сразу.
     */
    public static Scene3D create(Blocks blocks) {
        Objects.requireNonNull(blocks, "blocks");
        Scene3D viewport = new Scene3D().highlightBlocks(true).highlightObjects(true);
        viewport.updateMode(RenderToTexture.UpdateMode.EVERY_FRAME);
        viewport.clearColor().set(0.046f, 0.071f, 0.11f, 1);
        viewport.camera().setPerspective((float) Math.toRadians(45), 0.2f, 160);
        viewport.maxZoomDistance(60);
        viewport.orbit().setOrbit(new MutableVec3(5.5f, 2.5f, 3.5f), 20, 0.55, 0.45);

        StructureNode house = house(blocks);
        BlockNode furnace = new BlockNode(blocks.furnace());
        furnace.setPosition(10, 1, 1);
        ItemNode item = new ItemNode(blocks.item());
        item.setPosition(10.5f, 1.9f, 3.5f).setScale(1.4f, 1.4f, 1.4f)
                .setRotation(new MutableQuat().setYawPitch(0.6, 0));
        EntityNode mob = new EntityNode(blocks.entity());
        mob.setPosition(10.5f, 1, 5.8f).setRotation(new MutableQuat().setYawPitch(-0.9, 0))
                .setHead(0.6f, 0.3f);
        viewport.scene().addRoot(house).addRoot(furnace).addRoot(item).addRoot(mob);

        viewport.addAnchor(tag("BlockNode"), furnace, new MutableVec3(0.5f, 1.35f, 0.5f));
        viewport.addAnchor(tag("ItemNode"), item, new MutableVec3(0, 0.6f, 0));
        viewport.addAnchor(tag("EntityNode"), mob, new MutableVec3(0, 2.2f, 0));
        viewport.addAnchor(tag("StructureNode"), house,
                new MutableVec3(SIZE / 2f, WALL_HEIGHT + SIZE / 2 + 3.6f, SIZE / 2f));

        viewport.content(controls(viewport, house));
        house.playAssembly(viewport, ASSEMBLY_SECONDS, Integer.MAX_VALUE);
        return viewport;
    }

    /**
     * Собирает дом на газоне: каменный пол, брёвна по углам, стены с окнами и дверным проёмом,
     * ступенчатая крыша и фонарь на вершине. Газон выходит за дом, чтобы на нём стояли отдельные узлы.
     *
     * @param blocks идентификаторы блоков.
     * @return структура в локальных координатах: пол на Y = 0.
     */
    private static StructureNode house(Blocks blocks) {
        StructureNode house = new StructureNode();
        for (int x = -1; x <= 12; x++) {
            for (int z = -1; z <= SIZE; z++) {
                boolean inside = x >= 0 && x < SIZE && z >= 0 && z < SIZE;
                house.setBlock(x, 0, z, inside ? blocks.floor() : blocks.ground());
            }
        }
        int last = SIZE - 1, middle = SIZE / 2;
        for (int y = 1; y <= WALL_HEIGHT; y++) {
            for (int x = 0; x < SIZE; x++) {
                for (int z = 0; z < SIZE; z++) {
                    boolean edgeX = x == 0 || x == last, edgeZ = z == 0 || z == last;
                    if (!edgeX && !edgeZ) continue;
                    if (z == 0 && x == middle && y < WALL_HEIGHT) continue;
                    String block = edgeX && edgeZ ? blocks.pillar()
                            : y == 2 && (x == middle || z == middle) ? blocks.window() : blocks.wall();
                    house.setBlock(x, y, z, block);
                }
            }
        }
        for (int step = 0; step <= middle; step++) {
            int y = WALL_HEIGHT + 1 + step;
            for (int x = step; x < SIZE - step; x++) {
                for (int z = step; z < SIZE - step; z++) house.setBlock(x, y, z, blocks.roof());
            }
        }
        house.setBlock(middle, WALL_HEIGHT + middle + 2, middle, blocks.light());
        house.setBlock(middle, 1, middle + 1, blocks.light());
        return house;
    }

    /**
     * Создаёт панель управления в левом верхнем углу.
     *
     * @param viewport сцена.
     * @param house    собираемая структура.
     * @return панель для слоя UI поверх сцены.
     */
    private static Box controls(Scene3D viewport, StructureNode house) {
        int top = WALL_HEIGHT + SIZE / 2 + 2;
        VBox column = new VBox();
        column.spacing(6);
        column.layout(style -> style.margin(8).flexGrow(0).flexShrink(0));

        Label title = new Label("UniGUI 3D Scene");
        title.color(MutableColor.rgba(0.92f, 0.95f, 1, 1));
        title.layout(style -> style.size(LayoutConstraints.AUTO, 16).flexGrow(0).flexShrink(0));
        TextBlock hint = new TextBlock("Drag to orbit, scroll to zoom. The same scene runs on every version.");
        hint.color(MutableColor.rgba(0.68f, 0.73f, 0.82f, 1));
        hint.layout(style -> style.size(LayoutConstraints.AUTO, 30).flexGrow(0).flexShrink(0));

        Label sliceLabel = new Label("Layers: all");
        sliceLabel.layout(style -> style.size(LayoutConstraints.AUTO, 14).flexGrow(0).flexShrink(0));
        Slider slice = new Slider().range(0, top).step(1).value(top);
        slice.layout(style -> style.size(LayoutConstraints.AUTO, 14).flexGrow(0).flexShrink(0));
        slice.onValueChanged(event -> {
            viewport.stopParameterAnimation(house);
            int layer = Math.round(event.newValue());
            house.setTopVisibleY(layer >= top ? Integer.MAX_VALUE : layer);
            sliceLabel.text(layer >= top ? "Layers: all" : "Layers: up to Y = " + layer);
        });

        Button assemble = new Button("Assemble again");
        assemble.layout(style -> style.size(LayoutConstraints.AUTO, 22).flexGrow(0).flexShrink(0));
        assemble.onClick(event -> {
            slice.value(top);
            house.playAssembly(viewport, ASSEMBLY_SECONDS, Integer.MAX_VALUE);
        });

        Checkbox blocksHighlight = new Checkbox("Highlight blocks");
        blocksHighlight.silentChecked(true);
        blocksHighlight.textColor(1, 1, 1, 1);
        blocksHighlight.layout(style -> style.size(LayoutConstraints.AUTO, 18).flexGrow(0).flexShrink(0));
        blocksHighlight.onCheckedChanged(event -> viewport.highlightBlocks(event.newValue()));
        Checkbox objectsHighlight = new Checkbox("Highlight objects");
        objectsHighlight.silentChecked(true);
        objectsHighlight.textColor(1, 1, 1, 1);
        objectsHighlight.layout(style -> style.size(LayoutConstraints.AUTO, 18).flexGrow(0).flexShrink(0));
        objectsHighlight.onCheckedChanged(event -> viewport.highlightObjects(event.newValue()));

        column.addChild(title);
        column.addChild(hint);
        column.addChild(assemble);
        column.addChild(sliceLabel);
        column.addChild(slice);
        column.addChild(blocksHighlight);
        column.addChild(objectsHighlight);

        Box panel = new Box().themeEnabled(false).backgroundVisible(true).borderVisible(true).radius(4)
                .background(MutableColor.rgba(0.035f, 0.045f, 0.065f, 0.86f))
                .border(MutableColor.rgba(0.32f, 0.42f, 0.58f, 0.6f));
        panel.layout(style -> style.size(210, LayoutConstraints.AUTO).margin(10)
                .align(Alignment.START, Alignment.START).flexGrow(0).flexShrink(0));
        panel.addChild(column);
        return panel;
    }

    /**
     * Создаёт подпись над узлом.
     *
     * @param text текст подписи.
     * @return виджет подписи.
     */
    private static Label tag(String text) {
        Label label = new Label(text);
        label.color(MutableColor.rgba(0.95f, 0.97f, 1, 1));
        return label;
    }

    /**
     * Идентификаторы блоков, предмета и моба сцены.
     *
     * @param ground  газон вокруг дома.
     * @param floor   пол дома.
     * @param pillar  угловые столбы.
     * @param wall    стены.
     * @param window  окна.
     * @param roof    крыша.
     * @param light   светильник.
     * @param furnace отдельный {@link BlockNode}.
     * @param item    отдельный {@link ItemNode}.
     * @param entity  отдельный {@link EntityNode}.
     */
    public record Blocks(String ground, String floor, String pillar, String wall, String window, String roof,
                         String light, String furnace, String item, String entity) {
        /**
         * Современные идентификаторы. Они работают на всех версиях: на 1.7.10 и 1.12.2 платформа переводит
         * {@code oak_log}, {@code grass_block} и {@code lit=true} в старые id и метаданные.
         */
        public static final Blocks MODERN = new Blocks("minecraft:grass_block", "minecraft:stone_bricks",
                "minecraft:oak_log", "minecraft:oak_planks", "minecraft:glass", "minecraft:dark_oak_planks",
                "minecraft:glowstone", "minecraft:furnace[facing=south,lit=true]", "minecraft:diamond_sword",
                "minecraft:zombie");
    }
}
