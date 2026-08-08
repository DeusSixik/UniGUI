package dev.sixik.unigui.tests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.DebugOverlayAnchor;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class TestCommands {
    private static Runnable changeMode = () -> {};
    private static Supplier<String> renderMode = UiRenderPolicy.Mode.VSYNC::name;

    private TestCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui").executes(ctx -> {
            RenderSystem.recordRenderCall(() -> {
                DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
                Widget root = examples(context);
                MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), root, context);
                screen.renderPolicy(UiRenderPolicy.vsync());

                renderMode = () -> screen.renderPolicy().mode().name();
                changeMode = () -> {
                    UiRenderPolicy.Mode mode = screen.renderPolicy().mode();
                    switch (mode) {
                        case CONTINUOUS -> screen.renderPolicy(UiRenderPolicy.vsync());
                        case VSYNC -> screen.renderPolicy(UiRenderPolicy.onDirty());
                        case ON_DIRTY -> screen.renderPolicy(UiRenderPolicy.fixedFps(60));
                        case FIXED_FPS -> screen.renderPolicy(UiRenderPolicy.continuous());
                    }
                };

                Minecraft.getInstance().setScreen(screen);
            });
            return 0;
        }));
    }

    private static Widget examples(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        DockPanel app = new DockPanel();
        app.margin(10.0f);
        viewport.addChild(app);

        Breadcrumb path = new Breadcrumb()
                .items(List.of("UniGUI", "Examples", "Overview"))
                .silentSelectedIndex(2);
        path.preferredSize(LayoutConstraints.AUTO, 22.0f).grow(1.0f);

        StackPanel pageHost = new StackPanel();
        WidgetBase[] pages = new WidgetBase[]{
                overviewPage(),
                widgetsPage(),
                layoutPage(),
                dataPage(),
                overlaysPage(),
                textPage(),
                minecraftPage(),
                stressPage()
        };
        String[] names = new String[]{
                "Overview",
                "Widgets",
                "Layout",
                "Data",
                "Overlays",
                "Text",
                "Minecraft",
                "Stress"
        };
        for (WidgetBase page : pages) {
            pageHost.addChild(page);
        }

        app.addChild(topBar(path), DockSide.TOP);
        app.addChild(sidebar(context, names, pages, path), DockSide.LEFT);
        app.addChild(statusBar(), DockSide.BOTTOM);

        ScrollView contentScroll = new ScrollView(pageHost);
        contentScroll.scrollStep(18.0f);
        contentScroll.margin(8.0f, 0.0f, 0.0f, 0.0f).grow(1.0f);
        app.addChild(contentScroll);

        selectPage(0, null, pages, path, names);
        return new OverlayLayer(viewport);
    }

    private static Box backgroundFrame() {
        Box frame = panelBox(0.020f, 0.024f, 0.032f, 0.98f);
        frame.align(Alignment.STRETCH, Alignment.STRETCH)
                .margin(6.0f)
                .grow(1.0f);
        return frame;
    }

    private static Box topBar(Breadcrumb path) {
        Box bar = panelBox(0.045f, 0.052f, 0.070f, 0.96f);
        bar.preferredSize(LayoutConstraints.AUTO, 34.0f).grow(0.0f);

        HBox row = new HBox();
        row.spacing(10.0f);
        row.margin(8.0f, 6.0f).grow(0.0f);

        Label title = new Label("UniGUI Examples");
        title.preferredSize(128.0f, 20.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);

        Label hint = new Label("ImGui-style interactive gallery for retained Minecraft UI widgets");
        hint.preferredSize(LayoutConstraints.AUTO, 20.0f).align(Alignment.START, Alignment.CENTER).grow(1.0f);

        row.addChild(title);
        row.addChild(path);
        row.addChild(hint);
        bar.addChild(row);
        return bar;
    }

    private static Box sidebar(DefaultUIContext context, String[] names, WidgetBase[] pages, Breadcrumb path) {
        VBox nav = new VBox();
        nav.spacing(6.0f);
        nav.margin(6.0f).grow(0.0f);

        Label title = new Label("Examples");
        title.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        nav.addChild(title);

        ToggleButton[] buttons = new ToggleButton[names.length];
        for (int i = 0; i < names.length; i++) {
            int index = i;
            ToggleButton button = new ToggleButton(names[i]);
            button.preferredSize(LayoutConstraints.AUTO, 21.0f).grow(0.0f);
            button.onClick(event -> selectPage(index, buttons, pages, path, names));
            buttons[i] = button;
            nav.addChild(button);
        }

        Separator sep = new Separator();
        sep.preferredSize(LayoutConstraints.AUTO, 1.0f).margin(0.0f, 3.0f).grow(0.0f);
        nav.addChild(sep);

        nav.addChild(debugControls(context));

        Box box = panelBox(0.033f, 0.038f, 0.052f, 0.94f);
        box.preferredSize(176.0f, LayoutConstraints.AUTO).grow(0.0f);
        ScrollView scroll = new ScrollView(nav);
        scroll.scrollStep(12.0f);
        scroll.grow(1.0f);
        box.addChild(padded(scroll, 6.0f));
        return box;
    }

    private static VBox debugControls(DefaultUIContext context) {
        VBox controls = new VBox();
        controls.spacing(5.0f);
        controls.grow(0.0f);

        Label title = new Label("Runtime");
        title.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        controls.addChild(title);

        Checkbox debug = new Checkbox("Debug overlay");
        debug.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        debug.onCheckedChanged(event -> context.debugFlags(event.newValue() ? DebugFlags.ALL : DebugFlags.NONE));
        controls.addChild(debug);

        Button render = new Button("Render: VSYNC");
        render.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        render.onClick(event -> {
            changeMode.run();
            render.text("Render: " + renderMode.get());
        });
        controls.addChild(render);

        DebugOverlayAnchor[] anchors = DebugOverlayAnchor.values();
        int[] anchorIndex = {0};
        Button anchor = new Button("Overlay: top left");
        anchor.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        anchor.onClick(event -> {
            anchorIndex[0] = (anchorIndex[0] + 1) % anchors.length;
            DebugOverlayAnchor value = anchors[anchorIndex[0]];
            context.debugOverlaySettings().anchor(value);
            anchor.text("Overlay: " + value.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        });
        controls.addChild(anchor);

        float[] scales = {0.75f, 0.50f, 1.0f};
        int[] scaleIndex = {0};
        Button scale = new Button("Scale: 75%");
        scale.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        scale.onClick(event -> {
            scaleIndex[0] = (scaleIndex[0] + 1) % scales.length;
            float value = scales[scaleIndex[0]];
            context.debugOverlaySettings().scale(value);
            scale.text("Scale: " + Math.round(value * 100.0f) + "%");
        });
        controls.addChild(scale);

        int[] windows = {30, 60, 100, 300};
        int[] windowIndex = {2};
        Button window = new Button("Range: 100 frames");
        window.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        window.onClick(event -> {
            windowIndex[0] = (windowIndex[0] + 1) % windows.length;
            int value = windows[windowIndex[0]];
            context.debugOverlaySettings().sampleWindow(value);
            window.text("Range: " + value + " frames");
        });
        controls.addChild(window);
        return controls;
    }

    private static Box statusBar() {
        Box status = panelBox(0.040f, 0.045f, 0.060f, 0.94f);
        status.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);
        Label text = new Label("/unigui  |  click samples, drag splitters/table columns, use keyboard in lists/tables");
        text.margin(8.0f, 4.0f).preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        status.addChild(text);
        return status;
    }

    private static VBox overviewPage() {
        VBox page = page("Overview", "A compact dashboard showing the current UniGUI feature surface.");

        TextBlock intro = paragraph("This command is now a real examples browser: use the left menu like an ImGui demo window. "
                + "Each page demonstrates a stable API area and keeps controls interactive.");
        page.addChild(intro);

        WrapPanel cards = wrap();
        cards.addChild(infoCard("Widgets", "Buttons, inputs, selection, loaders, breadcrumbs"));
        cards.addChild(infoCard("Layout", "Dock, stack, wrap, split panels, tabs and trees"));
        cards.addChild(infoCard("Data", "Virtualized list/table with sorting and editing"));
        cards.addChild(infoCard("Minecraft", "Item, block and entity previews"));
        page.addChild(cards);
        return page;
    }

    private static VBox widgetsPage() {
        VBox page = page("Widgets", "Common controls and small interactive state examples.");

        Label status = new Label("Ready");
        status.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        page.addChild(status);

        WrapPanel actions = wrap();
        Button primary = new Button("Button");
        primary.preferredSize(76.0f, 22.0f).grow(0.0f);
        primary.onClick(event -> status.text("Button clicked"));

        ToggleButton toggle = new ToggleButton("Toggle");
        toggle.preferredSize(76.0f, 22.0f).grow(0.0f);
        toggle.onCheckedChanged(event -> status.text("Toggle: " + event.newValue()));

        Checkbox checkbox = new Checkbox("Checkbox");
        checkbox.preferredSize(96.0f, 22.0f).grow(0.0f);
        checkbox.onCheckedChanged(event -> status.text("Checkbox: " + event.newValue()));

        SearchField search = new SearchField("filter examples");
        search.preferredSize(150.0f, 22.0f).grow(0.0f);
        search.onSearchSubmitted(event -> status.text("Search: " + search.text()));

        actions.addChild(primary);
        actions.addChild(toggle);
        actions.addChild(checkbox);
        actions.addChild(search);
        page.addChild(section("Buttons & Input", actions));

        WrapPanel choiceRow = wrap();
        ComboBox combo = new ComboBox()
                .items(List.of("Dark theme", "Light theme", "High contrast", "Minecraft-ish"))
                .silentSelectedIndex(0);
        combo.preferredSize(150.0f, LayoutConstraints.AUTO).grow(0.0f);
        combo.onSelectionChanged(event -> status.text("Combo: " + combo.selectedItem()));

        RadioButton compact = new RadioButton("Compact", "Compact");
        RadioButton comfortable = new RadioButton("Comfortable", "Comfortable");
        RadioButton detailed = new RadioButton("Detailed", "Detailed");
        compact.preferredSize(78.0f, 20.0f).grow(0.0f);
        comfortable.preferredSize(104.0f, 20.0f).grow(0.0f);
        detailed.preferredSize(82.0f, 20.0f).grow(0.0f);
        new RadioGroup().add(compact).add(comfortable).add(detailed).silentSelectedValue("Comfortable");

        choiceRow.addChild(combo);
        choiceRow.addChild(compact);
        choiceRow.addChild(comfortable);
        choiceRow.addChild(detailed);
        page.addChild(section("Selection", choiceRow));

        WrapPanel feedback = wrap();
        Slider slider = new Slider().range(0.0f, 100.0f).step(5.0f).value(42.0f);
        slider.preferredSize(150.0f, 22.0f).grow(0.0f);
        ProgressBar progress = new ProgressBar().range(0.0f, 100.0f).value(42.0f);
        progress.preferredSize(112.0f, 12.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);
        NumberField number = new NumberField().range(0.0d, 100.0d).value(42.0d);
        number.preferredSize(76.0f, 22.0f).grow(0.0f);
        slider.onValueChanged(event -> {
            progress.value(event.newValue());
            number.value(event.newValue());
        });
        number.onValueChanged(event -> {
            slider.value((float) event.newValue());
            progress.value((float) event.newValue());
        });

        LoadingIndicator spinner = new Spinner().speed(1.3f).segments(10);
        spinner.preferredSize(24.0f, 24.0f).grow(0.0f);
        LoadingIndicator dots = new LoadingIndicator().mode(LoadingIndicator.Mode.DOTS).speed(1.1f);
        dots.preferredSize(72.0f, 24.0f).grow(0.0f);
        LoadingIndicator bar = new LoadingIndicator().mode(LoadingIndicator.Mode.BAR).speed(0.8f);
        bar.preferredSize(118.0f, 8.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);

        feedback.addChild(slider);
        feedback.addChild(progress);
        feedback.addChild(number);
        feedback.addChild(spinner);
        feedback.addChild(dots);
        feedback.addChild(bar);
        page.addChild(section("Feedback", feedback));
        return page;
    }

    private static VBox layoutPage() {
        VBox page = page("Layout & Containers", "Composition widgets used to build inspectors and tool screens.");

        Breadcrumb breadcrumb = new Breadcrumb()
                .items(List.of("UniGUI", "Examples", "Layout", "Breadcrumb"))
                .silentSelectedIndex(3);
        breadcrumb.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO).grow(0.0f);
        page.addChild(section("Breadcrumb", breadcrumb));

        SplitPanel split = new SplitPanel(
                samplePane("Left Pane", "Drag the divider. Minimum sizes prevent collapse."),
                samplePane("Right Pane", "SplitPanel supports horizontal and vertical orientation."));
        split.splitRatio(0.38f).minFirstSize(90.0f).minSecondSize(120.0f);
        split.preferredSize(LayoutConstraints.AUTO, 88.0f).grow(0.0f);
        page.addChild(section("SplitPanel", split));

        TabControl tabs = new TabControl();
        tabs.addTab("Tabs", samplePane("TabControl", "Only selected content is visible, pages remain retained."));
        tabs.addTab("Accordion", samplePane("Accordion", "Expandable sections are useful for inspectors."));
        tabs.addTab("Tree", samplePane("TreeView", "Hierarchical data with keyboard navigation."));
        tabs.preferredSize(LayoutConstraints.AUTO, 94.0f).grow(0.0f);
        page.addChild(section("Tabs", tabs));

        Accordion accordion = new Accordion();
        accordion.addPanel(expandable("Graphics", "Render policy, caches, preview widgets and scale."))
                .addPanel(expandable("Input", "Focus, capture, cursors, text edit and shortcuts."))
                .addPanel(expandable("Debug", "Profiler overlay and validation aids."));
        accordion.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO).grow(0.0f);
        page.addChild(section("Accordion", accordion));

        TreeView tree = new TreeView();
        TreeViewNode root = tree.addRoot("UniGUI");
        TreeViewNode widgets = root.addChild("Widgets");
        widgets.addChild("Button");
        widgets.addChild("ComboBox");
        widgets.addChild("TreeView");
        TreeViewNode backend = root.addChild("Minecraft Backend");
        backend.addChild("Items");
        backend.addChild("Blocks");
        backend.addChild("Entities");
        tree.silentSelect(widgets.child(2));
        tree.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO).grow(0.0f);
        page.addChild(section("TreeView", tree));
        return page;
    }

    private static VBox dataPage() {
        VBox page = page("Data", "Virtualized list and table examples for large datasets.");

        VirtualListView list = new VirtualListView()
                .itemCount(2_000)
                .itemHeight(18.0f)
                .selectionMode(SelectionMode.MULTIPLE)
                .itemFactory(index -> {
                    Label row = new Label("Virtual row #" + index);
                    row.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
                    return row;
                });
        list.preferredSize(LayoutConstraints.AUTO, 118.0f).grow(0.0f);
        page.addChild(section("VirtualListView", list));

        VirtualTableView table = new VirtualTableView()
                .rowCount(500)
                .selectionMode(SelectionMode.MULTIPLE)
                .editable(true)
                .cellTextProvider((row, column) -> switch (column) {
                    case 0 -> "Item " + row;
                    case 1 -> row % 3 == 0 ? "Queued" : row % 3 == 1 ? "Running" : "Done";
                    case 2 -> String.valueOf((row * 17) % 100);
                    default -> "";
                })
                .sortKeyProvider((row, column) -> column == 2 ? (row * 17) % 100 : row);
        table.addColumn("Name", 120.0f)
                .addColumn("State", 90.0f)
                .addColumn("Score", 64.0f);
        table.preferredSize(LayoutConstraints.AUTO, 150.0f).grow(0.0f);
        page.addChild(section("VirtualTableView", table));
        return page;
    }

    private static OverlayLayer overlaysPage() {
        VBox page = page("Overlays", "Popup, Tooltip and WindowWidget render over normal layout through OverlayLayer.");

        WrapPanel row = wrap();
        Button tooltipAnchor = new Button("Hover me");
        tooltipAnchor.preferredSize(82.0f, 22.0f).grow(0.0f);
        Button popupAnchor = new Button("Popup");
        popupAnchor.preferredSize(76.0f, 22.0f).grow(0.0f);
        Button windowButton = new Button("Window");
        windowButton.preferredSize(76.0f, 22.0f).grow(0.0f);
        ToggleButton freeDrag = new ToggleButton("Free drag");
        freeDrag.preferredSize(86.0f, 22.0f).grow(0.0f);
        row.addChild(tooltipAnchor);
        row.addChild(popupAnchor);
        row.addChild(windowButton);
        row.addChild(freeDrag);
        page.addChild(section("Overlay controls", row));

        TextBlock body = paragraph("Tooltip does not capture input. Popup is anchored and closes on outside click. WindowWidget has a draggable title bar.");
        page.addChild(body);

        OverlayLayer layer = new OverlayLayer(page);
        Popup popup = new Popup(popupAnchor, samplePane("Popup content", "Anchored popup with retained content."));
        WindowWidget window = new WindowWidget("Example Window", samplePane("Dialog body", "Drag the title bar, or close with x."))
                .position(260.0f, 74.0f)
                .closeOnOutsideClick(false);
        window.preferredSize(220.0f, 124.0f).grow(0.0f);

        popupAnchor.onClick(event -> popup.toggle());
        windowButton.onClick(event -> window.toggle());
        freeDrag.onCheckedChanged(event -> window.constrainToHost(!event.newValue()));
        layer.addOverlay(new Tooltip(tooltipAnchor, "Tooltips are overlay-hosted and layout-independent."));
        layer.addOverlay(new Tooltip(popupAnchor, "Click to toggle an anchored Popup."));
        layer.addOverlay(new Tooltip(windowButton, "Click to open a draggable WindowWidget."));
        layer.addOverlay(popup);
        layer.addOverlay(window);
        return layer;
    }

    private static VBox textPage() {
        VBox page = page("Text & Fonts", "Plain TextWidget, TextBlock overflow and RichText runs.");

        TextBlock clipped = new TextBlock("CLIP: this text is intentionally too long for its box and should clip cleanly.");
        clipped.overflowMode(TextOverflowMode.CLIP);
        clipped.preferredSize(260.0f, 22.0f).grow(0.0f);
        page.addChild(section("Overflow / Clip", clipped));

        TextBlock marquee = new TextBlock("MARQUEE_ON_HOVER: hover this line to scroll a long status message.");
        marquee.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        marquee.preferredSize(260.0f, 22.0f).grow(0.0f);
        page.addChild(section("Overflow / Marquee", marquee));

        TextBlock sdf = new TextBlock();
        sdf.richText(RichText.of("The quick brown fox 0123456789", Fonts.defaultFace(), 16.0f));
        sdf.preferredSize(LayoutConstraints.AUTO, 26.0f).grow(0.0f);
        page.addChild(section("SDF Font", sdf));

        TextBlock mixed = new TextBlock();
        mixed.richText(RichText.builder()
                .font(Fonts.defaultFace()).size(18.0f)
                .color(MutableColor.rgba(0.25f, 0.85f, 1.0f, 1.0f)).append("SDF ")
                .font(MinecraftFonts.defaultFace()).size(13.0f)
                .color(MutableColor.rgba(1.0f, 0.75f, 0.2f, 1.0f)).append("Minecraft default ")
                .font(MinecraftFonts.uniformFace()).size(14.0f)
                .color(MutableColor.rgba(0.35f, 1.0f, 0.45f, 1.0f)).append("Uniform ")
                .font(MinecraftFonts.altFace()).size(16.0f)
                .color(MutableColor.rgba(1.0f, 0.4f, 0.8f, 1.0f)).append("Alt")
                .build());
        mixed.preferredSize(LayoutConstraints.AUTO, 30.0f).grow(0.0f);
        page.addChild(section("RichText", mixed));
        return page;
    }

    private static VBox minecraftPage() {
        VBox page = page("Minecraft", "Backend-specific preview widgets for item, block and entity rendering.");

        WrapPanel previews = wrap();
        previews.addChild(itemPreview("Diamond", Items.DIAMOND));
        previews.addChild(itemPreview("Pickaxe", Items.ENCHANTED_GOLDEN_APPLE));
        previews.addChild(blockPreview("Crafting", Blocks.CRAFTING_TABLE));
        previews.addChild(blockPreview("Furnace", Blocks.FURNACE));
        previews.addChild(entityPreview("Player-ish", EntityType.ZOMBIE));
        previews.addChild(entityPreview("Creeper", EntityType.CREEPER));
        page.addChild(previews);

        WrapPanel actions = wrap();
        Button swapItem = new Button("Swap item");
        swapItem.preferredSize(82.0f, 22.0f).grow(0.0f);
        MinecraftItemPreviewWidget item = itemPreview("Mutable", Items.APPLE);
        swapItem.onClick(event -> item.stack(new ItemStack(item.stack().is(Items.APPLE) ? Items.EMERALD : Items.APPLE)));
        actions.addChild(swapItem);
        actions.addChild(item);
        page.addChild(section("Mutable preview", actions));
        return page;
    }

    private static VBox stressPage() {
        VBox page = page("Stress", "Dense render smoke tests for cache, scissor and Minecraft preview regressions.");

        WrapPanel entities = wrap();
        entities.spacing(1.0f);
        entities.lineSpacing(1.0f);
        List<EntityType<? extends LivingEntity>> types = List.of(
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.CREEPER,
                EntityType.VILLAGER,
                EntityType.PIG,
                EntityType.COW,
                EntityType.SHEEP,
                EntityType.CHICKEN
        );
        for (int i = 0; i < 160; i++) {
            MinecraftEntityPreviewWidget entity = new MinecraftEntityPreviewWidget("", types.get(i % types.size()));
            entity.labelVisible(false);
            entity.backgroundVisible(false);
            entity.borderVisible(false);
            entity.previewSize(16.0f);
            entity.look(7.0f, 4.0f);
            entity.preferredSize(26.0f, 26.0f).grow(0.0f);
            entities.addChild(entity);
        }
        page.addChild(section("Entity grid", entities));
        return page;
    }

    private static VBox page(String title, String subtitle) {
        VBox page = new VBox();
        page.spacing(8.0f);
        page.margin(8.0f);
        page.grow(1.0f);

        Label heading = new Label(title);
        heading.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        page.addChild(heading);

        TextBlock text = paragraph(subtitle);
        text.preferredSize(LayoutConstraints.AUTO, 34.0f).grow(0.0f);
        page.addChild(text);
        return page;
    }

    private static Box section(String title, Widget body) {
        Box box = panelBox(0.045f, 0.052f, 0.068f, 0.88f);
        box.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO).grow(0.0f);

        VBox content = new VBox();
        content.spacing(6.0f);
        content.margin(8.0f).grow(0.0f);

        Label label = new Label(title);
        label.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        content.addChild(label);
        content.addChild(body);
        box.addChild(content);
        return box;
    }

    private static VBox samplePane(String title, String text) {
        VBox pane = new VBox();
        pane.spacing(4.0f);
        pane.margin(6.0f).grow(0.0f);
        Label label = new Label(title);
        label.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        TextBlock body = paragraph(text);
        body.preferredSize(LayoutConstraints.AUTO, 42.0f).grow(0.0f);
        pane.addChild(label);
        pane.addChild(body);
        return pane;
    }

    private static ExpandablePanel expandable(String title, String text) {
        ExpandablePanel panel = new ExpandablePanel(title);
        panel.addContent(paragraph(text));
        panel.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO).grow(0.0f);
        return panel;
    }

    private static TextBlock paragraph(String text) {
        TextBlock block = new TextBlock(text);
        block.wrap(true);
        block.overflowMode(TextOverflowMode.CLIP);
        return block;
    }

    private static WrapPanel wrap() {
        WrapPanel wrap = new WrapPanel();
        wrap.spacing(8.0f);
        wrap.lineSpacing(8.0f);
        wrap.grow(0.0f);
        return wrap;
    }

    private static Box infoCard(String title, String body) {
        Box card = panelBox(0.055f, 0.064f, 0.085f, 0.92f);
        card.preferredSize(160.0f, 76.0f).grow(0.0f);
        card.addChild(samplePane(title, body));
        return card;
    }

    private static MinecraftItemPreviewWidget itemPreview(String label, net.minecraft.world.level.ItemLike item) {
        MinecraftItemPreviewWidget preview = new MinecraftItemPreviewWidget(label, item);
        preview.previewSize(34.0f);
        preview.preferredSize(86.0f, 64.0f).grow(0.0f);
        return preview;
    }

    private static MinecraftBlockPreviewWidget blockPreview(String label, net.minecraft.world.level.block.Block block) {
        MinecraftBlockPreviewWidget preview = new MinecraftBlockPreviewWidget(label, block);
        preview.previewSize(34.0f);
        preview.preferredSize(86.0f, 64.0f).grow(0.0f);
        return preview;
    }

    private static MinecraftEntityPreviewWidget entityPreview(String label, EntityType<? extends LivingEntity> entityType) {
        MinecraftEntityPreviewWidget preview = new MinecraftEntityPreviewWidget(label, entityType);
        preview.previewSize(38.0f);
        preview.preferredSize(90.0f, 70.0f).grow(0.0f);
        return preview;
    }

    private static Box padded(Widget child, float padding) {
        Box box = new Box();
        box.layout(style -> style.padding(dev.sixik.unigui.api.layout.EdgeInsets.all(padding)));
        box.addChild(child);
        return box;
    }

    private static Box panelBox(float r, float g, float b, float a) {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(4.0f);
        box.background().set(r, g, b, a);
        box.borderColor().set(0.20f, 0.28f, 0.36f, 0.75f);
        return box;
    }

    private static void selectPage(int selectedIndex, ToggleButton[] buttons, WidgetBase[] pages, Breadcrumb path, String[] names) {
        for (int i = 0; i < pages.length; i++) {
            boolean selected = i == selectedIndex;
            pages[i].visible(selected);
            if (buttons != null) {
                buttons[i].silentChecked(selected);
            }
        }
        path.items(List.of("UniGUI", "Examples", names[selectedIndex]));
        path.silentSelectedIndex(2);
    }
}
