package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraft;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost.SampleBlock;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost.SampleEntity;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost.SampleItem;
import dev.sixik.unigui.demo.ui.scene.SceneShowcase;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Minecraft-backed widgets: item, block and entity previews, vanilla tooltips, the registry pickers and a 3D scene.
 *
 * <p>The widgets exist in three flavours (1.7.10, 1.12.2, 1.20+) with different Minecraft types, so this category
 * never names them. It asks the installed {@link DemoMinecraftHost} for version-neutral samples; the platform
 * module maps each sample to a real item, block or entity. Without a host (headless tests, other engines) it shows
 * a notice.</p>
 */
final class MinecraftCategory extends DemoCategory {
    /**
     * Side of the square preview area in UI pixels.
     */
    private static final float PREVIEW = 40.0f;

    private static final List<Control> CONTROLS = List.of(
            new Control("mc-items", "Item previews", "Draw item stacks through the vanilla renderer: cached icons, live icons and vanilla tooltips."),
            new Control("mc-blocks", "Block previews", "Show blocks through their inventory model."),
            new Control("mc-entities", "Entity previews", "Show living entities standing with their head turned toward a point."),
            new Control("mc-pickers", "Pickers", "Search the item registry and the available textures in a popup grid."),
            new Control("mc-scene", "3D scene", "Build a scene from blocks, structures, items and mobs with an interactive camera and UI on top."));

    @Override
    String title() {
        return "Minecraft";
    }

    @Override
    boolean expandedByDefault() {
        return false;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        DemoMinecraftHost host = DemoMinecraft.host();
        if (host == null) {
            root.addChild(samplePanel("Minecraft is not available",
                    "These widgets render through the game. Open the gallery from a Minecraft 1.7.10, 1.12.2 or 1.20+ client to see them.",
                    label("No Minecraft host is installed.", UniGuiDemo.GalleryTemplate.TEXT_DARK), ""));
            return;
        }
        switch (controlId) {
            case "mc-items" -> addItems(root, host);
            case "mc-blocks" -> addBlocks(root, host);
            case "mc-entities" -> addEntities(root, host);
            case "mc-pickers" -> addPickers(root, host);
            case "mc-scene" -> addScene(root, host);
            default ->
                    root.addChild(samplePanel("Preview", "Interactive sample", label(controlId, UniGuiDemo.GalleryTemplate.TEXT_DARK), ""));
        }
    }

    /**
     * Overlay that hosts tooltips and picker popups; {@code null} when the category is built outside the gallery.
     */
    private OverlayLayer overlay() {
        return gallery == null ? null : gallery.overlay;
    }

    /**
     * Приёмник оверлеев страницы: подсказки уходят в корень окна и живут, пока открыта эта страница.
     */
    private Consumer<Widget> pageOverlays() {
        return gallery == null ? null : gallery::addPageOverlay;
    }

    private void addItems(VBox root, DemoMinecraftHost host) {
        Consumer<Widget> overlay = pageOverlays();
        HBox cached = row();
        add(cached, host.item("diamond", SampleItem.DIAMOND, 1, true, 1.0f, PREVIEW, overlay));
        add(cached, host.item("64 iron", SampleItem.IRON_INGOT, 64, true, 1.0f, PREVIEW, overlay));
        add(cached, host.item("worn pick", SampleItem.WORN_PICKAXE, 1, true, 1.0f, PREVIEW, overlay));
        add(cached, host.item("healing", SampleItem.HEALING_POTION, 1, true, 1.0f, PREVIEW, overlay));
        add(cached, host.item("bow", SampleItem.BOW, 1, true, 1.0f, PREVIEW, overlay));
        add(cached, host.item("40% alpha", SampleItem.EMERALD, 1, true, 0.4f, PREVIEW, overlay));
        add(cached, host.item("no overlay", SampleItem.IRON_INGOT, 64, false, 1.0f, PREVIEW, overlay));
        root.addChild(samplePanel("Cached icons",
                "Stable items are baked once into a small texture and then drawn as a plain quad. Hover an item for the vanilla tooltip (running on " + host.platform() + ").",
                cached,
                "MinecraftItemPreviewWidget icon =\n        MinecraftWidgets.itemPreview(\"diamond\", stack);\nicon.previewSize(40.0f);\nicon.decorations(false); // hide count and durability\nicon.opacity(0.4f);\n// The tooltip belongs to the window's root layer:\nrootOverlay.addOverlay(icon.vanillaTooltip());"));

        HBox live = row();
        add(live, host.item("glint", SampleItem.ENCHANTED_SWORD, 1, true, 1.0f, PREVIEW, overlay));
        add(live, host.item("clock", SampleItem.CLOCK, 1, true, 1.0f, PREVIEW, overlay));
        add(live, host.item("compass", SampleItem.COMPASS, 1, true, 1.0f, PREVIEW, overlay));
        add(live, host.item("chest", SampleItem.CHEST, 1, true, 1.0f, PREVIEW, overlay));
        root.addChild(samplePanel("Live icons",
                "Animated or specially rendered items are never cached: the enchantment glint, the clock and the compass keep moving.",
                live,
                "// The widget picks the path itself: a stable stack uses the\n// baked icon, an animated one is drawn by the vanilla renderer.\nMinecraftWidgets.itemPreview(\"clock\", clockStack);"));
    }

    private void addBlocks(VBox root, DemoMinecraftHost host) {
        HBox blocks = row();
        add(blocks, host.block("grass", SampleBlock.GRASS, PREVIEW));
        add(blocks, host.block("red wool", SampleBlock.RED_WOOL, PREVIEW));
        add(blocks, host.block("glass", SampleBlock.GLASS, PREVIEW));
        add(blocks, host.block("log", SampleBlock.LOG, PREVIEW));
        root.addChild(samplePanel("Blocks",
                "A block is drawn through the item pipeline, so it looks like its inventory icon. A block without an item shows nothing.",
                blocks,
                "MinecraftBlockPreviewWidget grass =\n        new MinecraftBlockPreviewWidget(\"grass\", block);\ngrass.previewSize(40.0f);"));
    }

    private void addEntities(VBox root, DemoMinecraftHost host) {
        HBox entities = row();
        add(entities, host.entity("zombie", SampleEntity.ZOMBIE, PREVIEW));
        add(entities, host.entity("pig", SampleEntity.PIG, PREVIEW));
        add(entities, host.entity("creeper", SampleEntity.CREEPER, PREVIEW));
        add(entities, host.entity("chicken", SampleEntity.CHICKEN, PREVIEW));
        add(entities, host.entity("you", SampleEntity.PLAYER, PREVIEW));
        root.addChild(samplePanel("Entities",
                "The entity is created in the current world and turns its head toward a point. A player head appears if an entity cannot be created or drawn; the player needs a loaded world.",
                entities,
                "// entityType: EntityType (1.20+), EntityEntry (1.12.2)\n// or an EntityList name (1.7.10)\nMinecraftEntityPreviewWidget zombie =\n        new MinecraftEntityPreviewWidget(\"zombie\", entityType);\nzombie.previewSize(40.0f);\nzombie.look(22.0f, 12.0f);"));
    }

    private void addPickers(VBox root, DemoMinecraftHost host) {
        OverlayLayer overlay = overlay();
        HBox pickers = row();
        add(pickers, host.itemPicker(overlay));
        add(pickers, host.texturePicker(overlay));
        root.addChild(samplePanel("Registry pickers",
                "Open a picker, type to search and choose an entry. The item picker lists the item registry; the texture picker scans the loaded resources.",
                pickers,
                "OverlayLayer overlay = new OverlayLayer(content);\nMinecraftWidgets.itemPicker().overlayLayer(overlay);\nMinecraftWidgets.texturePicker().overlayLayer(overlay);"));
    }

    private void addScene(VBox root, DemoMinecraftHost host) {
        SceneShowcase.Blocks blocks = host.sceneBlocks();
        if (blocks == null || ScenePlatform.current() == null) {
            root.addChild(samplePanel("3D scene", "3D scenes are not available on " + host.platform() + ".",
                    label("No scene support.", UniGuiDemo.GalleryTemplate.TEXT_DARK), ""));
            return;
        }
        Scene3D scene = SceneShowcase.create(blocks);
        scene.layout(style -> style.widthPercent(100.0f).height(380.0f).flexGrow(0.0f).flexShrink(0.0f));
        root.addChild(samplePanel("Structure, blocks, items and mobs",
                "A house assembles layer by layer from a StructureNode. Drag to orbit, scroll to zoom, use the panel to"
                        + " replay the assembly or cut the layers. The same code runs on " + host.platform() + ".",
                scene,
                "Scene3D viewport = new Scene3D().highlightBlocks(true);\n"
                        + "StructureNode house = new StructureNode();\n"
                        + "house.setBlock(0, 0, 0, \"minecraft:stone_bricks\");\n"
                        + "house.setBlock(0, 1, 0, \"minecraft:oak_log\");\n"
                        + "BlockNode furnace = new BlockNode(\"minecraft:furnace[facing=south]\");\n"
                        + "furnace.setPosition(3, 0, 0);\n"
                        + "viewport.scene().addRoot(house).addRoot(furnace)\n"
                        + "        .addRoot(new ItemNode(\"minecraft:diamond_sword\"))\n"
                        + "        .addRoot(new EntityNode(\"minecraft:zombie\"));\n"
                        + "viewport.content(controlPanel); // UI drawn over the scene\n"
                        + "house.playAssembly(viewport, 4.0f, Integer.MAX_VALUE);"));
        addFurnaceTutorial(root, host);
    }

    /**
     * Adds the step-by-step furnace animation with its timeline under the structure scene.
     */
    private void addFurnaceTutorial(VBox root, DemoMinecraftHost host) {
        SceneTutorialPlayer.Sequence sequence = host.furnaceTutorial();
        if (sequence == null) return;
        SceneTutorialPlayer player = new SceneTutorialPlayer(sequence);
        player.layout(style -> style.widthPercent(100.0f).height(400.0f).flexGrow(0.0f).flexShrink(0.0f));
        root.addChild(samplePanel("Animated tutorial",
                "Every frame is computed from the timeline: the floor rises, the furnace drops and bounces, then it"
                        + " lights up. Drag the timeline or click a step to scrub, the scene follows both ways.",
                player,
                "Scene3D viewport = new Scene3D();\n"
                        + "BlockNode furnace = new BlockNode(\"minecraft:furnace[facing=south]\");\n"
                        + "viewport.scene().addRoot(furnace);\n"
                        + "// Called for any time, also when scrubbing backwards:\n"
                        + "FrameSampler frame = seconds -> {\n"
                        + "    furnace.setPosition(0, dropHeight(seconds), 0);\n"
                        + "    furnace.setState(seconds >= 4 ? \"minecraft:furnace[facing=south,lit=true]\"\n"
                        + "            : \"minecraft:furnace[facing=south]\");\n"
                        + "};\n"
                        + "new SceneTutorialPlayer(new Sequence(\"Placing a furnace\", viewport, 6,\n"
                        + "        checkpoints, frame));"));
    }

    /**
     * Row that wraps onto the next line instead of overflowing when the interface scale is large.
     */
    private static HBox row() {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).flexWrap(FlexWrap.WRAP));
        return row;
    }

    private static void add(HBox row, Widget widget) {
        if (widget != null) row.addChild(widget);
    }
}
