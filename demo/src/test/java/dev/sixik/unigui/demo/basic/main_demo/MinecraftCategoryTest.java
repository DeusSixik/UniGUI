package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraft;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftCategoryTest {
    @AfterEach
    void removeHost() {
        DemoMinecraft.host(null);
    }

    @Test
    void addDemo_withoutHost_showsNoticeForEveryControl() {
        MinecraftCategory category = new MinecraftCategory();

        for (DemoCategory.Control control : category.controls()) {
            VBox root = new VBox();
            category.addDemo(control.id(), root);
            root.applyQueuedMutations();
            assertEquals(1, root.children().size(), control.id());
        }
    }

    @Test
    void addDemo_withHost_requestsEverySampleAndSkipsUnsupportedOnes() {
        RecordingHost host = new RecordingHost();
        DemoMinecraft.host(host);
        MinecraftCategory category = new MinecraftCategory();

        for (DemoCategory.Control control : category.controls()) {
            category.addDemo(control.id(), new VBox());
        }

        assertEquals(List.of(
                DemoMinecraftHost.SampleItem.DIAMOND, DemoMinecraftHost.SampleItem.IRON_INGOT,
                DemoMinecraftHost.SampleItem.WORN_PICKAXE, DemoMinecraftHost.SampleItem.HEALING_POTION,
                DemoMinecraftHost.SampleItem.BOW, DemoMinecraftHost.SampleItem.EMERALD,
                DemoMinecraftHost.SampleItem.IRON_INGOT, DemoMinecraftHost.SampleItem.ENCHANTED_SWORD,
                DemoMinecraftHost.SampleItem.CLOCK, DemoMinecraftHost.SampleItem.COMPASS,
                DemoMinecraftHost.SampleItem.CHEST), host.items);
        assertEquals(List.of(
                DemoMinecraftHost.SampleBlock.GRASS, DemoMinecraftHost.SampleBlock.RED_WOOL,
                DemoMinecraftHost.SampleBlock.GLASS, DemoMinecraftHost.SampleBlock.LOG), host.blocks);
        assertEquals(List.of(
                DemoMinecraftHost.SampleEntity.ZOMBIE, DemoMinecraftHost.SampleEntity.PIG,
                DemoMinecraftHost.SampleEntity.CREEPER, DemoMinecraftHost.SampleEntity.CHICKEN,
                DemoMinecraftHost.SampleEntity.PLAYER), host.entities);
        assertTrue(host.itemPickers == 1 && host.texturePickers == 1);
    }

    @Test
    void addDemo_withHost_passesOpacityAndDecorationFlagsThrough() {
        RecordingHost host = new RecordingHost();
        DemoMinecraft.host(host);

        new MinecraftCategory().addDemo("mc-items", new VBox());

        assertTrue(host.lowOpacitySeen, "the alpha sample must be dimmed");
        assertTrue(host.undecoratedSeen, "the overlay-less sample must hide decorations");
    }

    @Test
    void gallery_itemTooltips_liveInRootOverlayAndAreRemovedWithThePage() {
        DemoMinecraft.host(new RecordingHost());
        UniScreen screen = UniGuiDemo.createScreen();
        OverlayLayer root = (OverlayLayer) screen.root();
        UniGuiDemo.GalleryTemplate gallery = (UniGuiDemo.GalleryTemplate) root.content();

        gallery.navigation.selectItem("mc-items");
        root.applyQueuedMutations();
        assertEquals(11, tooltipCount(root), "one tooltip per item preview, all in the root overlay layer");

        gallery.navigation.selectItem("mc-blocks");
        root.applyQueuedMutations();
        assertEquals(0, tooltipCount(root), "leaving the page removes its tooltips");

        gallery.navigation.selectItem("mc-items");
        root.applyQueuedMutations();
        assertEquals(11, tooltipCount(root), "coming back must not stack a second set");
    }

    private static long tooltipCount(OverlayLayer root) {
        return root.children().stream()
                .filter(child -> child instanceof Label label && label.text().startsWith("tooltip:"))
                .count();
    }

    @Test
    void addDemo_hostThatSupportsNothing_stillBuildsPanels() {
        DemoMinecraft.host(() -> "empty");
        MinecraftCategory category = new MinecraftCategory();

        for (DemoCategory.Control control : category.controls()) {
            VBox root = new VBox();
            category.addDemo(control.id(), root);
            root.applyQueuedMutations();
            assertFalse(root.children().isEmpty(), control.id());
        }
    }

    @Test
    void controls_haveUniqueIdsAcrossTheGallery() {
        List<String> ids = new ArrayList<>();
        for (DemoCategory.Control control : new MinecraftCategory().controls()) ids.add(control.id());
        assertEquals(ids.size(), ids.stream().distinct().count());
    }

    /** Host that records what was requested and answers with plain labels. */
    private static final class RecordingHost implements DemoMinecraftHost {
        final List<SampleItem> items = new ArrayList<>();
        final List<SampleBlock> blocks = new ArrayList<>();
        final List<SampleEntity> entities = new ArrayList<>();
        int itemPickers;
        int texturePickers;
        boolean lowOpacitySeen;
        boolean undecoratedSeen;

        @Override
        public String platform() {
            return "test";
        }

        @Override
        public Widget item(String label, SampleItem item, int count, boolean decorations, float opacity,
                           float size, Consumer<Widget> overlays) {
            items.add(item);
            if (overlays != null) overlays.accept(new Label("tooltip:" + label));
            lowOpacitySeen |= opacity < 1.0f;
            undecoratedSeen |= !decorations;
            return new Label(label);
        }

        @Override
        public Widget block(String label, SampleBlock block, float size) {
            blocks.add(block);
            return new Label(label);
        }

        @Override
        public Widget entity(String label, SampleEntity entity, float size) {
            entities.add(entity);
            // The player is unavailable before a world is loaded.
            return entity == SampleEntity.PLAYER ? null : new Label(label);
        }

        @Override
        public Widget itemPicker(OverlayLayer overlay) {
            itemPickers++;
            return new Label("items");
        }

        @Override
        public Widget texturePicker(OverlayLayer overlay) {
            texturePickers++;
            return new Label("textures");
        }
    }
}
