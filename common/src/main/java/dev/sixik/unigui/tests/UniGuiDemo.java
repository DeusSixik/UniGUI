package dev.sixik.unigui.tests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.api.core.MutableUIScaleProvider;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.selection.SelectionMode;
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
import dev.sixik.unigui.widgets.*;
import dev.sixik.unigui.widgets.render.DockSplitHandleRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class UniGuiDemo {
    private static final MutableUIScaleProvider SCALE = new MutableUIScaleProvider(2.0f);

    private UniGuiDemo() {
    }

    public static void openDemo() {
        RenderSystem.recordRenderCall(UniGuiDemo::openDemoClient);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui")
                .executes(ctx -> {
                    openDemo();
                    return 0;
                }));
    }

    private static MinecraftWidgetScreen openScreen(Component title, Widget root, DefaultUIContext context) {
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(title, root, context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(false);
        Minecraft.getInstance().setScreen(screen);
        return screen;
    }

    private static void openDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(SCALE);
        Widget root = demoScreenWidget(context);
        openScreen(Component.literal("UniGUI Demo"), root, context);
    }

    private static Widget demoScreenWidget(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        DockPanel app = new DockPanel();
        app.layout(style -> style.margin(10.0f));
        viewport.addChild(app);

        VBox shell = new VBox();
        shell.spacing(8.0f);
        shell.layout(style -> style.flexGrow(1).flexShrink(1.0f));

        HBox header = new HBox();
        header.spacing(8.0f);
        header.layout(style -> style.size(LayoutConstraints.AUTO, 34.0f).flexGrow(0).flexShrink(0.0f));

        Label title = new Label("UniGUI Widget Demo");
        title.layout(style -> style.size(210.0f, 24.0f).align(Alignment.START, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        Label hint = new Label("Every tab is interactive: click, type, drag, scroll, resize, open overlays.");
        hint.layout(style -> style.size(LayoutConstraints.AUTO, 24.0f).align(Alignment.START, Alignment.CENTER).flexGrow(1).flexShrink(1.0f));
        Button scale = new Button("Scale 200%");
        scale.layout(style -> style.size(92.0f, 22.0f).align(Alignment.END, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        float[] scales = {0.50f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f, 2.75f, 3.0f, 3.25f, 3.5f};
        int[] scaleIndex = {6};
        scale.onClick(event -> {
            scaleIndex[0] = (scaleIndex[0] + 1) % scales.length;
            SCALE.scale(scales[scaleIndex[0]]);
            scale.text("Scale " + Math.round(SCALE.scale() * 100.0f) + "%");
        });

        ToggleButton debug = new ToggleButton("Debug");
        debug.layout(style -> style.size(72.0f, 22.0f).align(Alignment.END, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        debug.onCheckedChanged(event -> {
            if (event.newValue()) {
                context.enableDebugFlags(DebugFlags.PROFILER_OVERLAY | DebugFlags.DRAW_COMMANDS | DebugFlags.BATCHES);
            } else {
                context.disableDebugFlags(DebugFlags.PROFILER_OVERLAY | DebugFlags.DRAW_COMMANDS | DebugFlags.BATCHES);
            }
        });

        header.addChild(title);
        header.addChild(hint);
        header.addChild(scale);
        header.addChild(debug);

        TabControl tabs = new TabControl();
        tabs.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        tabs.addTab("Overview", scroll(overviewPage()));
        tabs.addTab("Controls", scroll(controlsPage()));
        tabs.addTab("Text", scroll(textPage()));
        tabs.addTab("Containers", scroll(containersPage()));
        tabs.addTab("Data", scroll(dataPage()));
        tabs.addTab("Custom Renders", scroll(customRendersPage()));
        tabs.addTab("Node Graph", nodeGraphPage());
        tabs.addTab("Overlays", overlaysPage());
        tabs.addTab("Minecraft", scroll(minecraftPage()));
        tabs.addTab("Stress", scroll(stressPage()));

        shell.addChild(header);
        shell.addChild(tabs);

        Box status = panelBox(0.040f, 0.045f, 0.060f, 0.94f);
        status.layout(style -> style.size(LayoutConstraints.AUTO, 24.0f).flexGrow(0).flexShrink(0.0f));
        Label statusText = new Label("/unigui demo  |  independent UI scale  |  tabs cover controls, layout, data, overlays, Minecraft previews");
        statusText.layout(style -> style.margin(8.0f, 4.0f).size(LayoutConstraints.AUTO, 16.0f).flexGrow(0).flexShrink(0.0f));
        status.addChild(statusText);
        app.addChild(status, DockSide.BOTTOM);
        app.addChild(shell);

        return new OverlayLayer(viewport);
    }

    private static ScrollView scroll(Widget content) {
        ScrollView scroll = new ScrollView(content);
        scroll.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        return scroll;
    }

    private static VBox overviewPage() {
        VBox page = page("Overview", "A compact tour of the AAA UI Framework surface for Minecraft screens.");
        page.addChild(paragraph("This demo is intentionally dense: each card or section exercises retained widgets, events, layout, renderers, overlays, virtualization, and Minecraft-specific preview widgets."));
        WrapPanel cards = wrap();
        cards.addChild(infoCard("Core Controls", "Button, ToggleButton, Checkbox, RadioGroup, ComboBox, text inputs, sliders, progress and loaders."));
        cards.addChild(infoCard("Containers", "StackPanel, DockPanel, WrapPanel, GridBox, SplitPanel, TabControl, Accordion, TreeView, TreeList and Carousel."));
        cards.addChild(infoCard("Data", "VirtualListView, VirtualTableView, Chart, Sparkline and GraphView for large or visual datasets."));
        cards.addChild(infoCard("Advanced", "DockingRoot, WindowWidget, Popup, Tooltip, ContextMenu, Toast, NodeGraph and Minecraft item/block/entity previews."));
        page.addChild(cards);
        page.addChild(section("Quick factories", factoryGallery()));
        return page;
    }

    private static Widget factoryGallery() {
        WrapPanel row = wrap();
        row.addChild(new Border().layout(style -> style.size(64.0f, 28.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new Separator().layout(style -> style.size(120.0f, 10.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new TextureWidget(new SimpleTextureHandle("minecraft:textures/block/stone.png", 16, 16))
                .layout(style -> style.size(38.0f, 38.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new ImageView(new SimpleTextureHandle("minecraft:textures/item/diamond.png", 16, 16))
                .layout(style -> style.size(42.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new CanvasWidget().layout(style -> style.size(76.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new Path().layout(style -> style.size(76.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        row.addChild(new Shape().layout(style -> style.size(76.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        return row;
    }

    private static VBox controlsPage() {
        VBox page = page("Controls", "Interactive controls with events wired to status labels.");

        Label status = new Label("Ready");
        status.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(status);

        WrapPanel buttons = wrap();
        Button button = new Button("Button");
        ToggleButton toggle = new ToggleButton("Toggle");
        Checkbox checkbox = new Checkbox("Checkbox");
        button.layout(style -> style.size(76.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        toggle.layout(style -> style.size(76.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        checkbox.layout(style -> style.size(96.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        button.onClick(event -> status.text("Button clicked"));
        toggle.onCheckedChanged(event -> status.text("Toggle: " + event.newValue()));
        checkbox.onCheckedChanged(event -> status.text("Checkbox: " + event.newValue()));
        buttons.addChild(button);
        buttons.addChild(toggle);
        buttons.addChild(checkbox);
        page.addChild(section("Buttons", buttons));

        WrapPanel inputs = wrap();
        TextInput input = new TextInput("raw input");
        TextField field = new TextField("TextField");
        PasswordField password = new PasswordField("secret");
        SearchField search = new SearchField("filter recipes");
        NumberField number = new NumberField().range(0.0d, 100.0d).value(42.0d);
        input.layout(style -> style.size(120.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        field.layout(style -> style.size(120.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        password.layout(style -> style.size(120.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        search.layout(style -> style.size(140.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        number.layout(style -> style.size(84.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        field.onTextChanged(event -> status.text("TextField: " + event.newText()));
        search.onSearchSubmitted(event -> status.text("Search submitted: " + search.text()));
        number.onValueChanged(event -> status.text(String.format(Locale.ROOT, "Number: %.1f", event.newValue())));
        inputs.addChild(input);
        inputs.addChild(field);
        inputs.addChild(password);
        inputs.addChild(search);
        inputs.addChild(number);
        page.addChild(section("Text input", inputs));

        WrapPanel choice = wrap();
        ComboBox combo = new ComboBox()
                .items(List.of("Dark", "Light", "High Contrast", "Minecraft"))
                .silentSelectedIndex(0);
        combo.dropDownSameWidth();
        combo.layout(style -> style.size(142.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        combo.onSelectionChanged(event -> status.text("Combo: " + combo.selectedItem()));
        DropDownBox drop = new DropDownBox();
        drop.items(List.of("DropDownBox", "Alias of ComboBox", "Overlay-backed"));
        drop.silentSelectedIndex(0);
        drop.dropDownSameWidth();
        drop.layout(style -> style.size(130.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        RadioButton compact = new RadioButton("Compact", "compact");
        RadioButton normal = new RadioButton("Normal", "normal");
        RadioButton detailed = new RadioButton("Detailed", "detailed");
        compact.layout(style -> style.size(78.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        normal.layout(style -> style.size(70.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        detailed.layout(style -> style.size(78.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        new RadioGroup().add(compact).add(normal).add(detailed).silentSelectedValue("normal");
        choice.addChild(combo);
        choice.addChild(drop);
        choice.addChild(compact);
        choice.addChild(normal);
        choice.addChild(detailed);
        page.addChild(section("Selection", choice));

        WrapPanel feedback = wrap();
        Slider slider = new Slider().range(0.0f, 100.0f).step(5.0f).value(42.0f);
        ProgressBar progress = new ProgressBar().range(0.0f, 100.0f).value(42.0f);
        LoadingIndicator ring = new Spinner().speed(1.2f).segments(12);
        LoadingIndicator dots = new LoadingIndicator().mode(LoadingIndicator.Mode.DOTS);
        LoadingIndicator bar = new LoadingIndicator().mode(LoadingIndicator.Mode.BAR);
        slider.layout(style -> style.size(160.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        progress.layout(style -> style.size(130.0f, 12.0f).align(Alignment.START, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        ring.layout(style -> style.size(24.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        dots.layout(style -> style.size(72.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        bar.layout(style -> style.size(118.0f, 8.0f).align(Alignment.START, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        slider.onValueChanged(event -> progress.value(event.newValue()));
        feedback.addChild(slider);
        feedback.addChild(progress);
        feedback.addChild(ring);
        feedback.addChild(dots);
        feedback.addChild(bar);
        page.addChild(section("Slider / Progress / Loading", feedback));

        WrapPanel pickers = wrap();
        DatePicker date = new DatePicker().value(LocalDate.of(2026, 8, 10));
        TimeSpanField span = new TimeSpanField().value(Duration.ofMinutes(7).plusSeconds(30));
        ColorPicker color = new ColorPicker();
        date.layout(style -> style.size(168.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        span.layout(style -> style.size(168.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        color.layout(style -> style.size(116.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        date.onDateChanged(event -> status.text("Date: " + event.newValue()));
        color.onColorChanged(event -> status.text(String.format(Locale.ROOT, "Color: #%08X", event.newArgb())));
        pickers.addChild(date);
        pickers.addChild(span);
        pickers.addChild(color);
        page.addChild(section("Pickers", pickers));
        return page;
    }

    private static VBox textPage() {
        VBox page = page("Text & Fonts", "TextWidget, Label, TextBlock overflow modes and RichText runs.");
        WrapPanel simple = wrap();
        simple.addChild(new Text("Text").layout(style -> style.size(90.0f, 20.0f).flexGrow(0).flexShrink(0.0f)));
        simple.addChild(new Label("Label").layout(style -> style.size(90.0f, 20.0f).flexGrow(0).flexShrink(0.0f)));
        simple.addChild(new TextBlock("TextBlock wraps long retained text across multiple lines.").wrap(true)
                .layout(style -> style.size(240.0f, 42.0f).flexGrow(0).flexShrink(0.0f)));
        page.addChild(section("Plain widgets", simple));

        TextBlock clip = new TextBlock("CLIP: this text is intentionally too long for its box and should clip cleanly.");
        clip.overflowMode(TextOverflowMode.CLIP);
        clip.layout(style -> style.size(260.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        TextBlock shrink = new TextBlock("SHRINK_TO_FIT: compact text into the available width.");
        shrink.overflowMode(TextOverflowMode.SHRINK_TO_FIT);
        shrink.layout(style -> style.size(260.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        TextBlock marquee = new TextBlock("MARQUEE_ON_HOVER: hover this line to scroll a long status message.");
        marquee.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        marquee.layout(style -> style.size(260.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        VBox overflow = new VBox();
        overflow.spacing(5.0f);
        overflow.addChild(clip);
        overflow.addChild(shrink);
        overflow.addChild(marquee);
        page.addChild(section("Overflow modes", overflow));

        RichText rich = RichText.builder()
                .font(Fonts.defaultFace()).size(18.0f).color(MutableColor.rgba(0.25f, 0.85f, 1.0f, 1.0f)).append("SDF ")
                .font(MinecraftFonts.defaultFace()).size(13.0f).color(MutableColor.rgba(1.0f, 0.75f, 0.2f, 1.0f)).append("Minecraft ")
                .font(MinecraftFonts.uniformFace()).size(14.0f).color(MutableColor.rgba(0.35f, 1.0f, 0.45f, 1.0f)).append("Uniform ")
                .font(MinecraftFonts.altFace()).size(16.0f).color(MutableColor.rgba(1.0f, 0.4f, 0.8f, 1.0f)).append("Alt")
                .build();
        TextBlock richBlock = new TextBlock();
        richBlock.richText(rich);
        richBlock.layout(style -> style.size(LayoutConstraints.AUTO, 30.0f).flexGrow(0).flexShrink(0.0f));
        RichTextView richView = new RichTextView(rich);
        richView.layout(style -> style.size(LayoutConstraints.AUTO, 42.0f).flexGrow(0).flexShrink(0.0f));
        VBox richBox = new VBox();
        richBox.spacing(6.0f);
        richBox.addChild(richBlock);
        richBox.addChild(richView);
        page.addChild(section("RichText / Fonts", richBox));
        return page;
    }

    private static VBox containersPage() {
        VBox page = page("Containers", "Layout and composition widgets used by real screens.");

        WrapPanel panels = wrap();
        panels.addChild(smokeTile("Box", 82.0f, 42.0f, 0.10f, 0.16f, 0.22f));
        panels.addChild(smokeTile("PanelWidget", 112.0f, 42.0f, 0.12f, 0.18f, 0.12f));
        panels.addChild(smokeTile("HBox", 82.0f, 42.0f, 0.18f, 0.12f, 0.16f));
        panels.addChild(smokeTile("VBox", 82.0f, 42.0f, 0.15f, 0.14f, 0.24f));
        panels.addChild(smokeTile("GridBox", 100.0f, 42.0f, 0.20f, 0.15f, 0.10f));
        panels.addChild(smokeTile("WrapPanel", 116.0f, 42.0f, 0.10f, 0.20f, 0.20f));
        page.addChild(section("Basic containers", panels));

        Breadcrumb breadcrumb = new Breadcrumb().items(List.of("UniGUI", "Demo", "Containers")).silentSelectedIndex(2);
        breadcrumb.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("Breadcrumb", breadcrumb));

        SplitPanel split = new SplitPanel(samplePane("Left Pane", "Drag the divider."), samplePane("Right Pane", "Min sizes prevent collapse."));
        split.splitRatio(0.38f).minFirstSize(90.0f).minSecondSize(120.0f);
        split.layout(style -> style.size(LayoutConstraints.AUTO, 92.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("SplitPanel", split));

        TabControl tabs = new TabControl();
        tabs.addTab("Tab A", samplePane("TabControl", "Selected content stays retained."));
        tabs.addTab("Tab B", samplePane("Second page", "Switch without rebuilding the whole UI."));
        tabs.layout(style -> style.size(LayoutConstraints.AUTO, 96.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("TabControl", tabs));

        Accordion accordion = new Accordion();
        accordion.addPanel(expandable("Graphics", "Render policies, caches and preview widgets."))
                .addPanel(expandable("Input", "Focus, capture, hover and keyboard routing."))
                .addPanel(expandable("Debug", "Profiler overlay and draw command counters."));
        page.addChild(section("Accordion / ExpandablePanel", accordion));

        TreeView tree = new TreeView();
        TreeViewNode root = tree.addRoot("UniGUI");
        TreeViewNode widgets = root.addChild("Widgets");
        widgets.addChild("Button");
        widgets.addChild("ComboBox");
        widgets.addChild("TreeView");
        widgets.addChild("Very long TreeView row name that clips and scrolls on hover");
        root.addChild("Minecraft Backend").addChild("Preview Widgets");
        tree.silentSelect(widgets.child(2));
        tree.rowTextHoverScrollSpeed(18.0f);
        tree.layout(style -> style.size(190.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        TreeList treeList = new TreeList()
                .addPath("Assets", "Textures", "Buttons")
                .addPath("Assets", "Shaders", "SDF")
                .addPath("Screens", "Inventory", "Crafting")
                .addPath("Screens", "Recipe Machine", "Very long nested recipe category label");
        treeList.rowTextHoverScrollSpeed(36.0f);
        treeList.layout(style -> style.size(190.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        TreeListPicker<String> picker = new TreeListPicker<String>()
                .values(List.of("Blocks/Crafting Table", "Items/Diamond", "Entities/Zombie", "Very/Long/Category/That/Matches/Widget/Width"))
                .labelProvider(value -> "Pick: " + value);
        picker.dropDownSameWidth();
        picker.layout(style -> style.size(210.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        WrapPanel trees = wrap();
        trees.addChild(tree);
        trees.addChild(treeList);
        trees.addChild(picker);
        page.addChild(section("TreeView / TreeList / TreeListPicker", trees));

        Carousel carousel = new Carousel()
                .addPage(samplePane("Page 1", "Carousel keeps one page visible."))
                .addPage(samplePane("Page 2", "Use arrows to switch pages."))
                .addPage(samplePane("Page 3", "PageView follows the same retained-content idea."));
        carousel.layout(style -> style.size(LayoutConstraints.AUTO, 116.0f).flexGrow(0).flexShrink(0.0f));
        View view = new View("View").addContent(paragraph("Titled content surface for feature modules."));
        view.layout(style -> style.size(LayoutConstraints.AUTO, 72.0f).flexGrow(0).flexShrink(0.0f));
        VBox pageWidgets = new VBox();
        pageWidgets.spacing(8.0f);
        pageWidgets.addChild(carousel);
        pageWidgets.addChild(view);
        page.addChild(section("Carousel / PageView / View", pageWidgets));

        DockingRoot docking = compactDockingRoot();
        docking.layout(style -> style.size(LayoutConstraints.AUTO, 150.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("DockingRoot", docking));
        return page;
    }

    private static VBox dataPage() {
        VBox page = page("Data", "Virtualized rows, tables and lightweight visualizations.");

        VirtualListView list = new VirtualListView()
                .itemCount(1000)
                .itemHeight(22.0f)
                .selectionMode(SelectionMode.MULTIPLE)
                .itemFactory(index -> {
                    Label row = new Label("Virtual row #" + index);
                    row.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(1).flexShrink(1.0f));
                    return row;
                });
        list.layout(style -> style.size(LayoutConstraints.AUTO, 132.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("VirtualListView", list));

        VirtualTableView table = new VirtualTableView()
                .rowCount(400)
                .rowHeight(22.0f)
                .selectionMode(SelectionMode.MULTIPLE)
                .cellRichTextProvider((row, column) -> {
                    String state = switch (row % 3) {
                        case 0 -> "Queued";
                        case 1 -> "Running";
                        default -> "Done";
                    };
                    int score = (row * 17) % 100;
                    return switch (column) {
                        case 0 -> RichText.of("Recipe " + row, MinecraftFonts.defaultFace(), 11.0f);
                        case 1 -> RichText.of(state, MinecraftFonts.defaultFace(), 11.0f);
                        case 2 -> RichText.of(String.valueOf(score), MinecraftFonts.defaultFace(), 11.0f);
                        default -> RichText.plain("");
                    };
                })
                .sortKeyProvider((row, column) -> column == 2 ? (row * 17) % 100 : row);
        table.columns(List.of(
                new VirtualTableColumn(RichText.plain("Name"), 130.0f),
                new VirtualTableColumn(RichText.plain("State"), 90.0f).align(Alignment.CENTER, Alignment.CENTER),
                new VirtualTableColumn(RichText.plain("Score"), 72.0f).align(Alignment.END, Alignment.CENTER)
        ));
        table.layout(style -> style.size(LayoutConstraints.AUTO, 150.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(section("VirtualTableView", table));

        WrapPanel visuals = wrap();
        List<Integer> series = List.of(8, 14, 10, 22, 18, 30, 24, 36, 28);
        Chart chart = new Chart().values(series).type(Chart.Type.BAR);
        Sparkline spark = new Sparkline().values(series);
        GraphView graph = new GraphView()
                .addNode("A", 0.15f, 0.30f)
                .addNode("B", 0.48f, 0.16f)
                .addNode("C", 0.78f, 0.34f)
                .addNode("D", 0.42f, 0.78f)
                .addEdge("A", "B")
                .addEdge("B", "C")
                .addEdge("B", "D")
                .addEdge("A", "D");
        Label visualStatus = new Label("Click a bar, spark point, or graph node");
        chart.layout(style -> style.size(220.0f, 120.0f).flexGrow(0).flexShrink(0.0f));
        spark.layout(style -> style.size(160.0f, 40.0f).flexGrow(0).flexShrink(0.0f));
        graph.layout(style -> style.size(220.0f, 120.0f).flexGrow(0).flexShrink(0.0f));
        visualStatus.layout(style -> style.size(360.0f, 16.0f).flexGrow(0).flexShrink(0.0f));
        chart.onBarClick(event -> visualStatus.text(String.format(Locale.ROOT, "Chart bar #%d = %.2f", event.index(), event.value())));
        spark.onPointClick(event -> visualStatus.text(String.format(Locale.ROOT, "Spark point #%d = %.2f", event.index(), event.value())));
        graph.onNodeClick(event -> visualStatus.text(String.format(Locale.ROOT, "Graph node %s @ %.2f, %.2f", event.id(), event.normalizedX(), event.normalizedY())));
        visuals.addChild(chart);
        visuals.addChild(spark);
        visuals.addChild(graph);
        visuals.addChild(visualStatus);
        page.addChild(section("Chart / Sparkline / GraphView", visuals));
        return page;
    }

    private static VBox customRendersPage() {
        VBox page = page("Custom Renders", "ImGui-inspired custom spinner styles drawn through UniGUI DrawScope primitives.");
        page.addChild(paragraph("These examples use path strokes, variable line thickness, filled circles, alpha fades and discrete motion without texture assets."));

        WrapPanel circular = wrap();
        circular.addChild(spinnerTile("Arc Sweep", Spinner.Style.ARC_SWEEP, 1.05f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Ring Arc", Spinner.Style.RING_ARC, 0.82f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Dotted Trail", Spinner.Style.DOTTED_TRAIL, 0.95f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Dotted Pulse", Spinner.Style.DOTTED_PULSE, 0.72f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Discrete Fade", Spinner.Style.DISCRETE_FADE, 0.65f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Gradient Arc", Spinner.Style.GRADIENT_ARC, 0.90f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Multi Arc", Spinner.Style.MULTI_ARC, 0.76f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Growing Arcs", Spinner.Style.GROWING_ARCS, 0.78f, 48.0f, 48.0f));
        circular.addChild(spinnerTile("Section Fade", Spinner.Style.SECTION_FADE, 0.42f, 48.0f, 48.0f));
        page.addChild(section("Circular spinners", circular));

        WrapPanel dots = wrap();
        dots.addChild(spinnerTile("Dots Y", Spinner.Style.DOTS_Y, 1.10f, 86.0f, 32.0f));
        dots.addChild(spinnerTile("Dots Fade", Spinner.Style.DOTS_FADE, 1.00f, 86.0f, 32.0f));
        dots.addChild(spinnerTile("Dots Radius", Spinner.Style.DOTS_RADIUS, 0.92f, 86.0f, 32.0f));
        dots.addChild(spinnerTile("Dots Moving", Spinner.Style.DOTS_MOVING, 0.72f, 86.0f, 32.0f));
        page.addChild(section("Linear dot spinners", dots));
        return page;
    }

    private static Widget nodeGraphPage() {
        VBox page = page("Node Graph", "Drag nodes, pan with wheel, Ctrl+wheel zooms, drag ports to connect.");

        Label status = new Label("NodeGraph: ready");
        status.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        NodeGraph graph = new NodeGraph()
                .selectionMode(NodeGraphSelectionMode.MULTIPLE)
                .gridSize(24.0f);
        graph.layout(style -> style.flexGrow(1).flexShrink(1.0f));

        NodeGraphItem input = graph.addItem("input", nodePane("Input", "2 ingredients"), 36.0f, 42.0f).size(180.0f, 108.0f);
        input.addPort("item-out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.42f).type("item");
        input.addPort("fluid-out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.76f).type("fluid");
        NodeGraphItem machine = graph.addItem("machine", nodePane("Machine", "Consumes item + fluid"), 300.0f, 80.0f).size(210.0f, 124.0f);
        machine.addPort("item-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.32f).type("item");
        machine.addPort("fluid-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.66f).type("fluid");
        machine.addPort("result-out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.50f).type("item");
        NodeGraphItem output = graph.addItem("output", nodePane("Output", "Result slot"), 600.0f, 56.0f).size(170.0f, 106.0f);
        output.addPort("result-in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.50f).type("item");
        graph.addConnection("item-route", new NodeGraphPortRef("input", "item-out"), new NodeGraphPortRef("machine", "item-in")).type("item");
        graph.addConnection("fluid-route", new NodeGraphPortRef("input", "fluid-out"), new NodeGraphPortRef("machine", "fluid-in")).type("fluid");
        graph.addConnection("result-route", new NodeGraphPortRef("machine", "result-out"), new NodeGraphPortRef("output", "result-in")).type("item");

        graph.onItemMoveEnded(event -> status.text("NodeGraph: moved " + event.itemId()));
        graph.onSelectionChanged(event -> status.text(event.newSelection().isEmpty()
                ? "NodeGraph: selection cleared"
                : "NodeGraph: selected " + String.join(", ", event.newSelection())));
        graph.onConnectionCreated(event -> status.text("NodeGraph: connected " + event.from().itemId() + " -> " + event.to().itemId()));
        graph.onConnectionRemoved(event -> status.text("NodeGraph: removed " + event.connectionId()));
        graph.onViewportChanged(event -> status.text(String.format(Locale.ROOT, "NodeGraph: zoom %.2fx", event.newZoom())));

        page.addChild(status);
        page.addChild(graph);
        return page;
    }

    private static OverlayLayer overlaysPage() {
        VBox page = page("Overlays", "Popup, Tooltip, ContextMenu, Toast and WindowWidget above normal layout.");

        WrapPanel row = wrap();
        Button tooltipAnchor = new Button("Hover me");
        Button popupAnchor = new Button("Popup");
        Button menuButton = new Button("Context");
        Button toastButton = new Button("Toast");
        Button windowButton = new Button("Window");
        Button modalButton = new Button("Modal");
        ToggleButton freeDrag = new ToggleButton("Free drag");
        for (Button b : List.of(tooltipAnchor, popupAnchor, menuButton, toastButton, windowButton, modalButton, freeDrag)) {
            b.layout(style -> style.size(86.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
            row.addChild(b);
        }
        page.addChild(section("Overlay controls", row));

        Label status = new Label("WindowManager: idle");
        status.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(status);
        page.addChild(paragraph("Tooltips do not capture input. Popups are anchored. Windows drag/resize. Modal windows block input below them."));

        OverlayLayer layer = new OverlayLayer(page);
        Popup popup = new Popup(popupAnchor, samplePane("Popup content", "Anchored retained popup."));
        ContextMenu menu = new ContextMenu()
                .item("Inspect", () -> status.text("Context: Inspect"))
                .item("Duplicate", () -> status.text("Context: Duplicate"))
                .separator()
                .item("Delete", () -> status.text("Context: Delete"));
        Toast toast = new Toast("Toast / NotificationView").duration(2.5f);
        NotificationView notification = new NotificationView("NotificationView: persistent info card").duration(0.0f);
        notification.layout(style -> style.size(260.0f, 46.0f).flexGrow(0).flexShrink(0.0f));

        WindowWidget window = new WindowWidget("Example Window", samplePane("Dialog body", "Drag title, resize corners, close with x."))
                .position(260.0f, 74.0f)
                .closeOnOutsideClick(false);
        window.layout(style -> style.size(230.0f, 132.0f).flexGrow(0).flexShrink(0.0f));
        WindowWidget modal = new WindowWidget("Modal Window", samplePane("Modal body", "Input below this dialog is blocked until closed."))
                .position(230.0f, 112.0f)
                .modal(true)
                .closeOnOutsideClick(false);
        modal.layout(style -> style.size(250.0f, 136.0f).flexGrow(0).flexShrink(0.0f));

        popupAnchor.onClick(event -> popup.toggle());
        menuButton.onClick(event -> menu.toggle(menuButton.layoutBounds().x(), menuButton.layoutBounds().y() + menuButton.layoutBounds().height() + 4.0f));
        toastButton.onClick(event -> toast.toast("Saved UniGUI demo state."));
        windowButton.onClick(event -> window.toggle());
        modalButton.onClick(event -> modal.openModal());
        freeDrag.onCheckedChanged(event -> window.constrainToHost(!event.newValue()));
        window.onOpened(event -> status.text("WindowManager: opened"));
        window.onClosed(event -> status.text("WindowManager: closed"));
        window.onMoved(event -> status.text(String.format(Locale.ROOT, "Window moved %.0f, %.0f", event.newX(), event.newY())));
        window.onResized(event -> status.text(String.format(Locale.ROOT, "Window resized %.0fx%.0f", event.newWidth(), event.newHeight())));
        modal.onModalOpened(event -> status.text("Modal opened depth " + event.stackDepth()));
        modal.onModalClosed(event -> status.text("Modal closed depth " + event.stackDepth()));

        layer.addOverlay(new Tooltip(tooltipAnchor, "Tooltip through OverlayLayer"));
        layer.addOverlay(new Tooltip(popupAnchor, "Click to toggle Popup"));
        layer.addOverlay(popup);
        layer.addOverlay(menu);
        layer.addOverlay(toast);
        layer.addOverlay(notification);
        layer.addOverlay(window);
        layer.addOverlay(modal);
        return layer;
    }

    private static VBox minecraftPage() {
        VBox page = page("Minecraft", "Backend-specific item, block and entity previews.");

        WrapPanel previews = wrap();
        previews.addChild(itemPreview("Diamond", Items.DIAMOND));
        previews.addChild(itemPreview("Apple", Items.APPLE));
        previews.addChild(itemPreview("Pickaxe", Items.DIAMOND_PICKAXE));
        previews.addChild(blockPreview("Crafting", Blocks.CRAFTING_TABLE));
        previews.addChild(blockPreview("Furnace", Blocks.FURNACE));
        previews.addChild(blockPreview("Chest", Blocks.CHEST));
        previews.addChild(entityPreview("Zombie", EntityType.ZOMBIE));
        previews.addChild(entityPreview("Creeper", EntityType.CREEPER));
        previews.addChild(entityPreview("Villager", EntityType.VILLAGER));
        page.addChild(section("Preview widgets", previews));

        WrapPanel mutable = wrap();
        Button swap = new Button("Swap item");
        swap.layout(style -> style.size(82.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        MinecraftItemPreviewWidget item = itemPreview("Mutable", Items.APPLE);
        swap.onClick(event -> item.stack(new ItemStack(item.stack().is(Items.APPLE) ? Items.EMERALD : Items.APPLE)));
        mutable.addChild(swap);
        mutable.addChild(item);
        page.addChild(section("Mutable ItemStack", mutable));
        return page;
    }

    private static VBox stressPage() {
        VBox page = page("Stress", "Dense retained-widget and Minecraft-preview smoke test.");
        WrapPanel entities = wrap();
        entities.spacing(1.0f);
        entities.lineSpacing(1.0f);
        List<EntityType<? extends LivingEntity>> types = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.VILLAGER, EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN);
        for (int i = 0; i < 96; i++) {
            MinecraftEntityPreviewWidget entity = new MinecraftEntityPreviewWidget("", types.get(i % types.size()));
            entity.labelVisible(false);
            entity.backgroundVisible(false);
            entity.borderVisible(false);
            entity.previewSize(16.0f);
            entity.look(7.0f, 4.0f);
            entity.layout(style -> style.size(26.0f, 26.0f).flexGrow(0).flexShrink(0.0f));
            entities.addChild(entity);
        }
        page.addChild(section("Entity grid", entities));
        return page;
    }

    private static DockingRoot compactDockingRoot() {
        DockingRoot docking = new DockingRoot();
        docking.addDocument("scene", "Scene", samplePane("Scene", "Center document tab."))
                .addDocument("recipe", "Recipe", samplePane("Recipe", "Dirty document tab."))
                .addToolPane("assets", "Assets", samplePane("Assets", "Left tool pane."), DockArea.LEFT)
                .addToolPane("inspector", "Inspector", samplePane("Inspector", "Right tool pane."), DockArea.RIGHT)
                .addToolPane("log", "Log", samplePane("Log", "Bottom output."), DockArea.BOTTOM)
                .selectPane("scene");
        DockPane recipe = docking.manager().findPane("recipe");
        if (recipe != null) recipe.dirty(true);
        DockPane assets = docking.manager().findPane("assets");
        if (assets != null) assets.pinned(false);
        docking.splitHandleRenderer(DockSplitHandleRenderers.IMGUI_STYLE);
        return docking;
    }

    private static VBox page(String title, String subtitle) {
        VBox page = new VBox();
        page.spacing(8.0f);
        page.layout(style -> style.margin(8.0f).flexGrow(1).flexShrink(1.0f));

        Label heading = new Label(title);
        heading.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f));
        TextBlock sub = paragraph(subtitle);
        sub.layout(style -> style.size(LayoutConstraints.AUTO, 34.0f).flexGrow(0).flexShrink(0.0f));
        page.addChild(heading);
        page.addChild(sub);
        return page;
    }

    private static Box section(String title, Widget body) {
        Box box = panelBox(0.045f, 0.052f, 0.068f, 0.88f);
        box.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        VBox content = new VBox();
        content.spacing(6.0f);
        content.layout(style -> style.margin(8.0f).flexGrow(0).flexShrink(0.0f));
        Label label = new Label(title);
        label.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        content.addChild(label);
        content.addChild(body);
        box.addChild(content);
        return box;
    }

    private static VBox samplePane(String title, String text) {
        VBox pane = new VBox();
        pane.spacing(4.0f);
        pane.layout(style -> style.margin(6.0f).flexGrow(0).flexShrink(0.0f));
        Label label = new Label(title);
        label.layout(style -> style.size(LayoutConstraints.AUTO, 16.0f).flexGrow(0).flexShrink(0.0f));
        TextBlock body = paragraph(text);
        body.layout(style -> style.size(LayoutConstraints.AUTO, 42.0f).flexGrow(0).flexShrink(0.0f));
        pane.addChild(label);
        pane.addChild(body);
        return pane;
    }

    private static VBox nodePane(String title, String text) {
        VBox pane = samplePane(title, text);
        Button button = new Button("Action");
        button.layout(style -> style.size(72.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        pane.addChild(button);
        return pane;
    }

    private static ExpandablePanel expandable(String title, String text) {
        ExpandablePanel panel = new ExpandablePanel(title);
        panel.addContent(paragraph(text));
        panel.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
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
        wrap.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        return wrap;
    }

    private static Box infoCard(String title, String body) {
        Box card = panelBox(0.055f, 0.064f, 0.085f, 0.92f);
        card.layout(style -> style.size(174.0f, LayoutConstraints.AUTO).minHeight(86.0f).flexGrow(0).flexShrink(0.0f));

        VBox content = new VBox();
        content.spacing(4.0f);
        content.layout(style -> style.margin(6.0f).flexGrow(0).flexShrink(0.0f));

        Label label = new Label(title);
        label.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        TextBlock text = paragraph(body);
        text.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        content.addChild(label);
        content.addChild(text);
        card.addChild(content);
        return card;
    }

    private static Box spinnerTile(String title, Spinner.Style style, float speed, float width, float height) {
        Box tile = panelBox(0.045f, 0.052f, 0.068f, 0.90f);
        tile.layout(layout -> layout.size(132.0f, 86.0f).flexGrow(0).flexShrink(0.0f));

        VBox content = new VBox();
        content.spacing(5.0f);
        content.layout(layout -> layout
                .margin(6.0f)
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.CENTER)
                .flexGrow(0)
                .flexShrink(0.0f));

        Spinner spinner = new Spinner(style)
                .speed(speed)
                .thickness(style.name().startsWith("DOTS") ? 3.8f : 3.0f)
                .dots(style == Spinner.Style.DOTS_MOVING ? 5 : 9)
                .activeDots(4)
                .arcs(style == Spinner.Style.SECTION_FADE ? 5 : 3)
                .segments(32);
        spinner.accentColor().set(0.25f, 0.78f, 1.0f, 1.0f);
        spinner.secondaryColor().set(1.0f, 1.0f, 1.0f, 0.95f);
        spinner.trackColor().set(0.14f, 0.17f, 0.22f, 0.42f);
        spinner.layout(layout -> layout
                .size(width, height)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0)
                .flexShrink(0.0f));

        Label label = new Label(title);
        label.noWrap().clipOverflow();
        label.layout(layout -> layout
                .size(120.0f, 16.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0)
                .flexShrink(0.0f));

        content.addChild(spinner);
        content.addChild(label);
        tile.addChild(content);
        return tile;
    }

    private static Box smokeTile(String title, float width, float height, float r, float g, float b) {
        Box tile = panelBox(r, g, b, 0.90f);
        tile.layout(style -> style.size(width, height).flexGrow(0).flexShrink(0.0f));
        Label label = new Label(title);
        label.layout(style -> style.margin(6.0f, 4.0f).size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).align(Alignment.CENTER, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
        tile.addChild(label);
        return tile;
    }

    private static MinecraftItemPreviewWidget itemPreview(String label, net.minecraft.world.level.ItemLike item) {
        MinecraftItemPreviewWidget preview = new MinecraftItemPreviewWidget(label, item);
        preview.previewSize(34.0f);
        preview.layout(style -> style.size(86.0f, 64.0f).flexGrow(0).flexShrink(0.0f));
        return preview;
    }

    private static MinecraftBlockPreviewWidget blockPreview(String label, net.minecraft.world.level.block.Block block) {
        MinecraftBlockPreviewWidget preview = new MinecraftBlockPreviewWidget(label, block);
        preview.previewSize(34.0f);
        preview.layout(style -> style.size(86.0f, 64.0f).flexGrow(0).flexShrink(0.0f));
        return preview;
    }

    private static MinecraftEntityPreviewWidget entityPreview(String label, EntityType<? extends LivingEntity> type) {
        MinecraftEntityPreviewWidget preview = new MinecraftEntityPreviewWidget(label, type);
        preview.previewSize(38.0f);
        preview.layout(style -> style.size(90.0f, 70.0f).flexGrow(0).flexShrink(0.0f));
        return preview;
    }

    private static Box backgroundFrame() {
        Box frame = panelBox(0.020f, 0.024f, 0.032f, 0.98f);
        frame.layout(style -> style.align(Alignment.STRETCH, Alignment.STRETCH).margin(6.0f).flexGrow(1).flexShrink(1.0f));
        return frame;
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
}
