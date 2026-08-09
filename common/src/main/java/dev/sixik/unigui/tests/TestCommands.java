package dev.sixik.unigui.tests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.debug.DebugOverlayAnchor;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRenderImpl;
import dev.sixik.unigui.backend.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
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

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.time.Duration;
import java.time.LocalDate;

public final class TestCommands {
    private static Runnable changeMode = () -> {};
    private static Supplier<String> renderMode = UiRenderPolicy.Mode.VSYNC::name;

    private TestCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui")
                .executes(ctx -> {
                    RenderSystem.recordRenderCall(TestCommands::openExamplesScreen);
                    return 0;
                })
                .then(Commands.literal("docking").executes(ctx -> {
                    RenderSystem.recordRenderCall(TestCommands::openDockingEditorScreen);
                    return 0;
                })));
    }

    private static void openExamplesScreen() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        Widget root = examples(context);
        MinecraftWidgetScreen screen = openScreen(Component.empty(), root, context);
        installRenderModeToggle(screen);
    }

    private static void openDockingEditorScreen() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        Widget root = dockingEditorExample(context);
        MinecraftWidgetScreen screen = openScreen(Component.literal("UniGUI Docking Editor"), root, context);
        installRenderModeToggle(screen);
    }

    private static MinecraftWidgetScreen openScreen(Component title, Widget root, DefaultUIContext context) {
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(title, root, context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        Minecraft.getInstance().setScreen(screen);
        return screen;
    }

    private static void installRenderModeToggle(MinecraftWidgetScreen screen) {
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
                dockingWindowsPage(),
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
                "Docking & Windows",
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

    private static OverlayLayer dockingEditorExample(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();
        viewport.addChild(backgroundFrame());

        DockPanel app = new DockPanel();
        app.margin(10.0f);
        viewport.addChild(app);

        OverlayLayer layer = new OverlayLayer(viewport);

        Label status = new Label("Docking editor: drag tabs to split, use the overflow menu, or open floating panels.");
        status.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(1.0f);

        DockingRoot docking = new DockingRoot();
        docking.addDocument("scene", "Scene", editorScenePane())
                .addDocument("graph", "Recipe Graph", editorGraphPane())
                .addDocument("preview", "Item Preview", editorPreviewPane())
                .addDocument("layout", "Layout XML", editorTextPane("Layout XML",
                        "<dock-root>\n  <slot id=\"scene\" />\n  <tool id=\"inspector\" side=\"right\" />\n</dock-root>"))
                .addDocument("style", "Theme", editorTextPane("Theme",
                        "primary: #5AA7FF\naccent: #F7B955\npanel-radius: 4px\nshadow: soft"))
                .addDocument("events", "Events", editorTextPane("Events",
                        "onClick -> open recipe\nonDrop -> reparent widget\nonSave -> snapshot layout"))
                .addDocument("bindings", "Bindings", editorTextPane("Bindings",
                        "recipe.output = preview.item\ninspector.target = selection.current"))
                .addDocument("animation", "Animation", editorTextPane("Animation",
                        "hover.fade = 90ms\nwindow.open = easeOutBack\ntab.switch = instant"))
                .addDocument("profiler", "Profiler", editorTextPane("Profiler",
                        "layout: 0.31 ms\nrender: 0.82 ms\nwidgets: 94\noverlays: 1"))
                .addToolPane("assets", "Assets", editorAssetsPane(), DockArea.LEFT)
                .addToolPane("inspector", "Inspector", editorInspectorPane(), DockArea.RIGHT)
                .addToolPane("console", "Console", editorConsolePane(), DockArea.BOTTOM)
                .selectPane("scene");
        DockPane style = docking.manager().findPane("style");
        if (style != null) style.dirty(true);
        DockPane inspector = docking.manager().findPane("inspector");
        if (inspector != null) inspector.pinned(true);
        docking.grow(1.0f);
        // ImGui-style split: panels share a border line, no separate gap block
        docking.splitHandleRenderer(DockSplitHandleRenderers.IMGUI_STYLE);

        docking.onDragStarted(event -> status.text("Docking editor: dragging " + event.paneId()));
        docking.onDropPreviewChanged(event -> {
            if (event.newIntent().valid()) {
                status.text("Docking editor: " + event.newIntent().area() + " -> " + event.newIntent().targetPaneId());
            } else {
                status.text("Docking editor: drop preview cleared");
            }
        });
        docking.onLayoutChanged(event -> status.text("Docking editor: layout " + event.operation()));

        app.addChild(dockingEditorToolbar(layer, docking, status), DockSide.TOP);
        app.addChild(dockingEditorStatusBar(status), DockSide.BOTTOM);
        app.addChild(docking);
        return layer;
    }

    private static Box dockingEditorToolbar(OverlayLayer layer, DockingRoot docking, Label status) {
        Box bar = panelBox(0.045f, 0.052f, 0.070f, 0.96f);
        bar.preferredSize(LayoutConstraints.AUTO, 34.0f).grow(0.0f);

        HBox row = new HBox();
        row.spacing(8.0f);
        row.margin(8.0f, 6.0f).grow(0.0f);

        Label title = new Label("UniGUI Docking Editor");
        title.preferredSize(168.0f, 20.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);

        Label hint = new Label("Small editor-style screen for docking, overflow tabs, floating windows and modal stack");
        hint.preferredSize(LayoutConstraints.AUTO, 20.0f).align(Alignment.START, Alignment.CENTER).grow(1.0f);

        Button addTab = new Button("New Tab");
        addTab.preferredSize(76.0f, 22.0f).grow(0.0f);
        Button floatPreview = new Button("Float Preview");
        floatPreview.preferredSize(104.0f, 22.0f).grow(0.0f);
        ToggleButton lockRedock = new ToggleButton("Lock Redock");
        lockRedock.preferredSize(96.0f, 22.0f).grow(0.0f);
        Button modal = new Button("Modal");
        modal.preferredSize(64.0f, 22.0f).grow(0.0f);
        Button render = new Button("Render: " + renderMode.get());
        render.preferredSize(106.0f, 22.0f).grow(0.0f);

        final int[] scratchCounter = {1};
        addTab.onClick(event -> {
            String id = "editor-scratch-" + scratchCounter[0];
            docking.addDocument(id, "Scratch " + scratchCounter[0], editorTextPane("Scratch " + scratchCounter[0],
                    "Temporary editor document.\nDrag this tab to test split targets."));
            docking.selectPane(id);
            status.text("Docking editor: added " + id);
            scratchCounter[0]++;
        });

        floatPreview.onClick(event -> {
            WindowWidget window = new WindowWidget("Floating Item Preview",
                    samplePane("Item Preview", "Floating tool window opened by /unigui docking."))
                    .position(318.0f, 86.0f)
                    .closeOnOutsideClick(false);
            window.preferredSize(238.0f, 122.0f).grow(0.0f);
            layer.addOverlay(window);
            window.open();
            status.text("Docking editor: floating preview opened");
        });

        lockRedock.onCheckedChanged(event -> {
            docking.floatingWindowsRedockLocked(event.newValue());
            status.text(event.newValue()
                    ? "Docking editor: floating dock windows will not redock"
                    : "Docking editor: floating dock windows can redock");
        });

        modal.onClick(event -> {
            WindowWidget dialog = new WindowWidget("Save Layout",
                    samplePane("Snapshot", "Fixed modal: overlay stack blocks input, drag and resize are disabled."))
                    .position(296.0f, 124.0f)
                    .modal(true)
                    .fixedModal(true)
                    .closeOnOutsideClick(false);
            dialog.preferredSize(250.0f, 124.0f).grow(0.0f);
            layer.addOverlay(dialog);
            dialog.openModal();
            status.text("Docking editor: modal opened");
        });

        render.onClick(event -> {
            changeMode.run();
            render.text("Render: " + renderMode.get());
            status.text("Docking editor: render mode " + renderMode.get());
        });

        row.addChild(title);
        row.addChild(hint);
        row.addChild(addTab);
        row.addChild(floatPreview);
        row.addChild(lockRedock);
        row.addChild(modal);
        row.addChild(render);
        bar.addChild(row);
        return bar;
    }

    private static Box dockingEditorStatusBar(Label status) {
        Box bar = panelBox(0.040f, 0.045f, 0.060f, 0.94f);
        bar.preferredSize(LayoutConstraints.AUTO, 26.0f).grow(0.0f);

        HBox row = new HBox();
        row.spacing(8.0f);
        row.margin(8.0f, 4.0f).grow(0.0f);

        Label command = new Label("/unigui docking");
        command.preferredSize(104.0f, 18.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);
        status.align(Alignment.START, Alignment.CENTER);

        row.addChild(command);
        row.addChild(status);
        bar.addChild(row);
        return bar;
    }

    private static Widget editorScenePane() {
        VBox pane = new VBox();
        pane.spacing(8.0f);
        pane.margin(8.0f).grow(0.0f);

        Label title = new Label("Canvas");
        title.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        pane.addChild(title);

        StackPanel canvas = new StackPanel();
        canvas.preferredSize(LayoutConstraints.AUTO, 126.0f).grow(0.0f);
        Box base = panelBox(0.028f, 0.034f, 0.050f, 0.94f);
        base.align(Alignment.STRETCH, Alignment.STRETCH);
        canvas.addChild(base);
        canvas.addChild(editorCanvasNode("DockingRoot", 34.0f, 20.0f, 144.0f, 32.0f, 0.18f, 0.38f, 0.62f));
        canvas.addChild(editorCanvasNode("Inspector", 214.0f, 20.0f, 104.0f, 32.0f, 0.24f, 0.46f, 0.34f));
        canvas.addChild(editorCanvasNode("Console", 90.0f, 76.0f, 174.0f, 32.0f, 0.54f, 0.34f, 0.20f));
        pane.addChild(canvas);
        pane.addChild(paragraph("Drag tabs to split the editor workspace. The center document group intentionally has many tabs for overflow testing."));
        return pane;
    }

    private static Box editorCanvasNode(String label, float left, float top, float width, float height,
                                        float r, float g, float b) {
        Box node = panelBox(r, g, b, 0.92f);
        node.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(left)
                .top(top)
                .width(width)
                .height(height));
        Label text = new Label(label);
        text.align(Alignment.CENTER, Alignment.CENTER).preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO).grow(1.0f);
        node.addChild(text);
        return node;
    }

    private static Widget editorGraphPane() {
        VBox pane = new VBox();
        pane.spacing(6.0f);
        pane.margin(8.0f).grow(0.0f);
        pane.addChild(paragraph("Recipe graph mockup: input slots feed a machine node and output preview."));

        HBox nodes = new HBox();
        nodes.spacing(8.0f);
        nodes.preferredSize(LayoutConstraints.AUTO, 58.0f).grow(0.0f);
        nodes.addChild(infoCard("Input", "Iron + Gear"));
        nodes.addChild(infoCard("Machine", "Assembler"));
        nodes.addChild(infoCard("Output", "Recipe Machine"));
        pane.addChild(nodes);
        return pane;
    }

    private static Widget editorPreviewPane() {
        WrapPanel previews = wrap();
        previews.margin(8.0f).grow(0.0f);
        previews.addChild(itemPreview("Recipe", Items.CRAFTING_TABLE));
        previews.addChild(itemPreview("Result", Items.REDSTONE));
        previews.addChild(blockPreview("Block", Blocks.IRON_BLOCK));
        previews.addChild(blockPreview("Machine", Blocks.SMITHING_TABLE));
        return previews;
    }

    private static Widget editorAssetsPane() {
        VBox pane = new VBox();
        pane.spacing(5.0f);
        pane.margin(7.0f).grow(0.0f);
        pane.addChild(paragraph("Assets"));
        pane.addChild(editorAssetRow("Containers / DockPanel"));
        pane.addChild(editorAssetRow("Controls / Button"));
        pane.addChild(editorAssetRow("Data / TreeView"));
        pane.addChild(editorAssetRow("Minecraft / ItemPreview"));
        pane.addChild(editorAssetRow("Overlays / WindowWidget"));
        return pane;
    }

    private static Widget editorAssetRow(String text) {
        Box row = panelBox(0.060f, 0.070f, 0.092f, 0.88f);
        row.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);
        Label label = new Label(text);
        label.margin(6.0f, 3.0f).align(Alignment.START, Alignment.CENTER).grow(1.0f);
        row.addChild(label);
        return row;
    }

    private static Widget editorInspectorPane() {
        VBox pane = new VBox();
        pane.spacing(5.0f);
        pane.margin(7.0f).grow(0.0f);
        pane.addChild(paragraph("Selection: DockingRoot"));
        pane.addChild(editorPropertyRow("x", "10"));
        pane.addChild(editorPropertyRow("y", "10"));
        pane.addChild(editorPropertyRow("grow", "1.0"));
        pane.addChild(editorPropertyRow("tabs", "overflow menu"));
        pane.addChild(editorPropertyRow("drop zones", "enabled"));
        return pane;
    }

    private static Widget editorPropertyRow(String key, String value) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.preferredSize(LayoutConstraints.AUTO, 22.0f).grow(0.0f);
        Label k = new Label(key);
        k.preferredSize(72.0f, 18.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);
        Label v = new Label(value);
        v.preferredSize(LayoutConstraints.AUTO, 18.0f).align(Alignment.START, Alignment.CENTER).grow(1.0f);
        row.addChild(k);
        row.addChild(v);
        return row;
    }

    private static Widget editorConsolePane() {
        return editorTextPane("Console",
                "[info] Docking editor opened\n" +
                        "[hint] Wheel over tabs to scroll\n" +
                        "[hint] Click ... to select hidden tabs\n" +
                        "[hint] Drag current tab to right/left/bottom split");
    }

    private static Widget editorTextPane(String title, String text) {
        VBox pane = new VBox();
        pane.spacing(4.0f);
        pane.margin(7.0f).grow(0.0f);
        Label label = new Label(title);
        label.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        TextBlock body = paragraph(text);
        body.preferredSize(LayoutConstraints.AUTO, 84.0f).grow(0.0f);
        pane.addChild(label);
        pane.addChild(body);
        return pane;
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
        cards.addChild(infoCard("Widgets", "Buttons, inputs, selection, pickers, loaders"));
        cards.addChild(infoCard("Layout", "Dock, stack, wrap, split panels, tabs and trees"));
        cards.addChild(infoCard("Data", "Virtualized list/table, charts, sparklines and graphs"));
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

        WrapPanel pickerRow = wrap();
        DatePicker date = new DatePicker().value(LocalDate.of(2026, 8, 9));
        date.preferredSize(168.0f, 24.0f).grow(0.0f);
        TimeSpanField span = new TimeSpanField().value(Duration.ofSeconds(5 * 60L + 30L));
        span.preferredSize(168.0f, 22.0f).grow(0.0f);
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.preferredSize(116.0f, 22.0f).grow(0.0f);
        Label pickerStatus = new Label("Pickers: waiting for change");
        pickerStatus.preferredSize(260.0f, 16.0f).grow(0.0f);
        date.onDateChanged(event -> pickerStatus.text("Date: " + event.newValue()));
        colorPicker.onColorChanged(event -> pickerStatus.text(String.format(Locale.ROOT, "Color: #%08X", event.newArgb())));
        VBox dateTime = new VBox();
        dateTime.spacing(4.0f);
        dateTime.grow(0.0f);
        dateTime.addChild(span);
        dateTime.addChild(date);
        dateTime.addChild(pickerStatus);
        pickerRow.addChild(dateTime);
        pickerRow.addChild(colorPicker);
        page.addChild(section("Pickers", pickerRow));
        return page;
    }

    private static VBox layoutPage() {
        VBox page = page("Layout & Containers", "Composition widgets used to build inspectors and tool screens.");

        Breadcrumb breadcrumb = new Breadcrumb()
                .items(List.of("UniGUI", "Examples", "Layout", "Breadcrumb"))
                .silentSelectedIndex(3);
        breadcrumb.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO).grow(0.0f);
        page.addChild(section("Breadcrumb", breadcrumb));

        page.addChild(layoutV3SmokeSection());

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

        DockingRoot docking = new DockingRoot();
        docking.addDocument("scene", "Scene", samplePane("Scene", "Center document tab retained inside DockingRoot."))
                .addDocument("recipe", "Recipe", samplePane("Recipe", "Second document shares the center tab group."))
                .addToolPane("assets", "Assets", samplePane("Assets", "Left tool window."), DockArea.LEFT)
                .addToolPane("inspector", "Inspector", samplePane("Inspector", "Right tool window."), DockArea.RIGHT)
                .addToolPane("log", "Log", samplePane("Log", "Bottom tool output."), DockArea.BOTTOM)
                .selectPane("scene");
        DockPane recipePane = docking.manager().findPane("recipe");
        if (recipePane != null) {
            recipePane.dirty(true);
        }
        DockPane assetsPane = docking.manager().findPane("assets");
        if (assetsPane != null) {
            assetsPane.pinned(false);
        }
        docking.preferredSize(LayoutConstraints.AUTO, 132.0f).grow(0.0f);
        // ImGui-style split: panels share a border line, no separate gap block
        docking.splitHandleRenderer(DockSplitHandleRenderers.IMGUI_STYLE);
        VBox dockingDemo = new VBox();
        dockingDemo.spacing(6.0f);
        dockingDemo.grow(0.0f);
        dockingDemo.addChild(docking);
        HBox dockingActions = new HBox();
        dockingActions.spacing(6.0f);
        dockingActions.grow(0.0f);
        Label dockingStatus = new Label("Snapshot: not captured");
        dockingStatus.preferredSize(260.0f, 18.0f).grow(0.0f);
        Button saveDockLayout = new Button("Snapshot");
        saveDockLayout.preferredSize(82.0f, 22.0f).grow(0.0f);
        Button restoreDockLayout = new Button("Restore");
        restoreDockLayout.preferredSize(76.0f, 22.0f).grow(0.0f);
        final String[] encodedDockSnapshot = {""};
        saveDockLayout.onClick(event -> {
            encodedDockSnapshot[0] = DockLayoutSnapshotCodec.encode(docking.manager().snapshot());
            dockingStatus.text("Snapshot: " + Math.min(encodedDockSnapshot[0].length(), 999) + " chars");
        });
        restoreDockLayout.onClick(event -> {
            if (encodedDockSnapshot[0].isEmpty()) return;
            java.util.Map<String, DockPane> registry = new java.util.HashMap<>();
            for (String paneId : List.of("scene", "recipe", "assets", "inspector", "log")) {
                DockPane pane = docking.manager().findPane(paneId);
                if (pane != null) registry.put(paneId, pane);
            }
            docking.restoreLayout(DockLayoutSnapshotCodec.decode(encodedDockSnapshot[0]), registry);
            dockingStatus.text("Snapshot: restored");
        });
        dockingActions.addChild(saveDockLayout);
        dockingActions.addChild(restoreDockLayout);
        dockingActions.addChild(dockingStatus);
        dockingDemo.addChild(dockingActions);
        page.addChild(section("DockingRoot documents/tools", dockingDemo));

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

        TreeList treeList = new TreeList()
                .addPath("Assets", "Textures", "Buttons")
                .addPath("Assets", "Shaders", "SDF")
                .addPath("Screens", "Inventory", "Crafting");
        treeList.preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO).grow(0.0f);
        page.addChild(section("TreeList", treeList));

        TreeListPicker<String> picker = new TreeListPicker<String>()
                .values(List.of("Blocks/Crafting Table", "Items/Diamond", "Entities/Zombie"))
                .labelProvider(value -> "Pick: " + value);
        picker.preferredSize(190.0f, LayoutConstraints.AUTO).grow(0.0f);
        page.addChild(section("TreeListPicker", picker));

        Carousel carousel = new Carousel()
                .addPage(samplePane("Page 1", "Carousel keeps one retained page visible."))
                .addPage(samplePane("Page 2", "Use arrows to switch pages."))
                .addPage(samplePane("Page 3", "PageView is an alias-style wrapper."));
        carousel.preferredSize(LayoutConstraints.AUTO, 116.0f).grow(0.0f);
        page.addChild(section("Carousel / PageView", carousel));

        View view = new View("View")
                .addContent(paragraph("A lightweight titled content surface for feature modules."));
        view.preferredSize(LayoutConstraints.AUTO, 70.0f).grow(0.0f);
        page.addChild(section("View", view));
        return page;
    }

    private static OverlayLayer dockingWindowsPage() {
        VBox page = page("Docking & Windows", "Dockable documents, tool panes, floating windows, modal stack and layout snapshots.");

        DockingRoot docking = new DockingRoot();
        docking.addDocument("scene", "Scene", samplePane("Scene", "Center document. Drag tabs to split, tab, or float."))
                .addDocument("recipe", "Recipe*", samplePane("Recipe", "Dirty document tab with active document semantics."))
                .addToolPane("assets", "Assets", samplePane("Assets", "Left tool pane. Future auto-hide metadata is exposed in state."), DockArea.LEFT)
                .addToolPane("inspector", "Inspector", samplePane("Inspector", "Right tool pane."), DockArea.RIGHT)
                .addToolPane("log", "Log", samplePane("Log", "Bottom tool pane for output."), DockArea.BOTTOM)
                .selectPane("scene");
        DockPane recipe = docking.manager().findPane("recipe");
        if (recipe != null) recipe.dirty(true);
        DockPane assets = docking.manager().findPane("assets");
        if (assets != null) assets.pinned(false).autoHide(true);
        docking.preferredSize(LayoutConstraints.AUTO, 176.0f).grow(0.0f);

        Label status = new Label("Docking: drag tabs, use Ctrl+Tab / Ctrl+W, or snapshot layout.");
        status.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        docking.onDragStarted(event -> status.text("Docking: dragging " + event.paneId()));
        docking.onDropPreviewChanged(event -> {
            if (event.newIntent().valid()) {
                status.text("Docking preview: " + event.newIntent().area() + " -> " + event.newIntent().targetPaneId());
            } else {
                status.text("Docking preview: cleared");
            }
        });
        docking.onLayoutChanged(event -> status.text("Docking layout: " + event.operation()));
        docking.onLayoutRestored(event -> status.text("Docking restored: "
                + event.restoredPaneCount() + " panes, missing " + event.missingPaneCount()));

        HBox actions = new HBox();
        actions.spacing(6.0f);
        actions.grow(0.0f);
        Button addDoc = new Button("Add doc");
        addDoc.preferredSize(72.0f, 22.0f).grow(0.0f);
        Button floatTool = new Button("Float inspector");
        floatTool.preferredSize(108.0f, 22.0f).grow(0.0f);
        Button modalButton = new Button("Modal");
        modalButton.preferredSize(68.0f, 22.0f).grow(0.0f);
        Button snapshot = new Button("Snapshot");
        snapshot.preferredSize(82.0f, 22.0f).grow(0.0f);
        Button restore = new Button("Restore");
        restore.preferredSize(74.0f, 22.0f).grow(0.0f);
        actions.addChild(addDoc);
        actions.addChild(floatTool);
        actions.addChild(modalButton);
        actions.addChild(snapshot);
        actions.addChild(restore);

        page.addChild(section("Dock workspace", docking));
        page.addChild(section("Dock actions", actions));
        page.addChild(status);

        OverlayLayer layer = new OverlayLayer(page);
        WindowWidget floatingInspector = new WindowWidget("Floating Inspector",
                samplePane("Inspector", "This window is produced by the docking demo floating action."))
                .position(330.0f, 92.0f)
                .closeOnOutsideClick(false);
        floatingInspector.preferredSize(230.0f, 118.0f).grow(0.0f);
        WindowWidget modal = new WindowWidget("Docking Modal",
                samplePane("Modal", "Modal scrim blocks input below the top dialog."))
                .position(300.0f, 122.0f)
                .modal(true)
                .closeOnOutsideClick(false);
        modal.preferredSize(230.0f, 118.0f).grow(0.0f);

        final int[] docCounter = {1};
        final String[] encodedSnapshot = {""};
        addDoc.onClick(event -> {
            String id = "scratch-" + docCounter[0]++;
            docking.addDocument(id, "Scratch " + docCounter[0], samplePane("Scratch", "New document " + id));
            docking.selectPane(id);
            status.text("Docking layout: added " + id);
        });
        floatTool.onClick(event -> {
            WindowWidget floated = docking.manager().floatPane("inspector");
            if (floated != null) {
                floated.position(330.0f, 92.0f).open();
                layer.addOverlay(floated);
                status.text("Docking layout: inspector floated");
            } else {
                floatingInspector.open();
                status.text("Docking layout: example floating window opened");
            }
        });
        modalButton.onClick(event -> modal.openModal());
        snapshot.onClick(event -> {
            encodedSnapshot[0] = DockLayoutSnapshotCodec.encode(docking.manager().snapshot());
            status.text("Docking snapshot: " + Math.min(encodedSnapshot[0].length(), 999) + " chars");
        });
        restore.onClick(event -> {
            if (encodedSnapshot[0].isEmpty()) return;
            java.util.Map<String, DockPane> registry = new java.util.HashMap<>();
            for (DockPane pane : docking.manager().panes()) {
                registry.put(pane.id(), pane);
            }
            docking.restoreLayout(DockLayoutSnapshotCodec.decode(encodedSnapshot[0]), registry);
        });

        layer.addOverlay(floatingInspector);
        layer.addOverlay(modal);
        return layer;
    }

    private static Box layoutV3SmokeSection() {
        VBox smoke = new VBox();
        smoke.spacing(8.0f);
        smoke.grow(0.0f);

        Label hint = new Label("Layout V3 is the default layout path for these widgets.");
        hint.preferredSize(LayoutConstraints.AUTO, 22.0f).align(Alignment.START, Alignment.CENTER).grow(1.0f);
        HBox controls = new HBox();
        controls.spacing(8.0f);
        controls.addChild(hint);
        smoke.addChild(controls);

        HBox row = new HBox();
        row.spacing(8.0f);
        row.layout(style -> style.padding(4.0f).alignItems(Align.CENTER).justifyContent(Justify.SPACE_BETWEEN));
        row.preferredSize(LayoutConstraints.AUTO, 44.0f).grow(0.0f);
        row.addChild(smokeTile("Fixed", 54.0f, 24.0f, 0.16f, 0.34f, 0.54f));
        row.addChild(smokeTile("Grow", 44.0f, 20.0f, 0.18f, 0.48f, 0.32f)
                .layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).flexBasis(SizeValue.px(44.0f))));
        row.addChild(smokeTile("End", 46.0f, 28.0f, 0.50f, 0.32f, 0.18f));
        smoke.addChild(section("V3 LinearBox row/column", row));

        WrapPanel cards = wrap();
        cards.preferredSize(LayoutConstraints.AUTO, 92.0f).grow(0.0f);
        cards.addChild(smokeTile("50%", 72.0f, 28.0f, 0.24f, 0.36f, 0.62f)
                .layout(style -> style.widthPercent(34.0f).minWidth(64.0f).maxWidth(112.0f)));
        cards.addChild(smokeTile("Clamp", 82.0f, 28.0f, 0.28f, 0.52f, 0.38f));
        cards.addChild(smokeTile("Wrap", 96.0f, 28.0f, 0.55f, 0.40f, 0.22f));
        cards.addChild(smokeTile("Next line", 110.0f, 28.0f, 0.44f, 0.26f, 0.56f));
        smoke.addChild(section("V3 WrapPanel cards", cards));

        StackPanel stack = new StackPanel();
        stack.preferredSize(LayoutConstraints.AUTO, 64.0f).grow(0.0f);
        Box base = smokeTile("Stack stretch", 80.0f, 24.0f, 0.12f, 0.22f, 0.34f);
        base.align(Alignment.STRETCH, Alignment.STRETCH);
        Box overlay = smokeTile("Absolute", 82.0f, 24.0f, 0.62f, 0.30f, 0.18f);
        overlay.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .right(8.0f)
                .top(8.0f)
                .width(82.0f)
                .height(24.0f));
        stack.addChild(base);
        stack.addChild(overlay);
        smoke.addChild(section("V3 StackPanel overlay", stack));

        SplitPanel split = new SplitPanel(
                samplePane("First pane", "Pane slots are V3 flex items."),
                samplePane("Second pane", "Splitter is an absolute handle over the gap."));
        split.splitRatio(0.42f)
                .splitterThickness(6.0f)
                .minFirstSize(90.0f)
                .minSecondSize(110.0f);
        split.preferredSize(LayoutConstraints.AUTO, 76.0f).grow(0.0f);
        smoke.addChild(section("V3 SplitPanel panes", split));

        VBox nestedPanels = new VBox();
        nestedPanels.spacing(5.0f);
        nestedPanels.preferredSize(LayoutConstraints.AUTO, 86.0f).grow(0.0f);
        HBox nestedRow = new HBox();
        nestedRow.spacing(6.0f);
        nestedRow.layout(style -> style.padding(4.0f).alignItems(Align.CENTER));
        nestedRow.preferredSize(LayoutConstraints.AUTO, 38.0f).grow(0.0f);
        nestedRow.addChild(smokeTile("35% min/max", 74.0f, 22.0f, 0.22f, 0.38f, 0.58f)
                .layout(style -> style.widthPercent(35.0f).minWidth(66.0f).maxWidth(118.0f).height(22.0f)));
        nestedRow.addChild(smokeTile("grow", 42.0f, 20.0f, 0.18f, 0.50f, 0.36f)
                .layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).flexBasis(SizeValue.px(42.0f))));
        VBox nestedColumn = new VBox();
        nestedColumn.spacing(3.0f);
        nestedColumn.layout(style -> style.padding(3.0f));
        nestedColumn.preferredSize(LayoutConstraints.AUTO, 38.0f).grow(0.0f);
        nestedColumn.addChild(smokeTile("nested 100%", 72.0f, 14.0f, 0.48f, 0.30f, 0.58f)
                .layout(style -> style.widthPercent(100.0f).minWidth(90.0f).height(14.0f)));
        nestedPanels.addChild(nestedRow);
        nestedPanels.addChild(nestedColumn);
        smoke.addChild(section("V3 nested panels percent/min/max", nestedPanels));

        StackPanel edgeCanvas = new StackPanel();
        edgeCanvas.preferredSize(LayoutConstraints.AUTO, 72.0f).grow(0.0f);
        Box edgeBack = panelBox(0.030f, 0.042f, 0.062f, 0.92f);
        edgeBack.align(Alignment.STRETCH, Alignment.STRETCH);
        Button edgePopupAnchor = new Button("Edge popup");
        edgePopupAnchor.preferredSize(96.0f, 22.0f)
                .align(Alignment.END, Alignment.END)
                .margin(0.0f, 0.0f, 6.0f, 6.0f)
                .grow(0.0f);
        edgeCanvas.addChild(edgeBack);
        edgeCanvas.addChild(edgePopupAnchor);
        OverlayLayer edgeLayer = new OverlayLayer(edgeCanvas);
        Popup edgePopup = new Popup(
                edgePopupAnchor,
                samplePane("Flipped popup", "Opened near the host edge; should stay visible."))
                .offset(0.0f, 4.0f);
        edgePopupAnchor.onClick(event -> edgePopup.toggle());
        edgeLayer.addOverlay(edgePopup);
        smoke.addChild(section("Overlay smoke: popup near edge", edgeLayer));

        StackPanel multiOverlayCanvas = new StackPanel();
        multiOverlayCanvas.preferredSize(LayoutConstraints.AUTO, 88.0f).grow(0.0f);
        Box multiOverlayBack = panelBox(0.026f, 0.034f, 0.052f, 0.92f);
        multiOverlayBack.align(Alignment.STRETCH, Alignment.STRETCH);
        Button menuAnchor = new Button("Menu");
        menuAnchor.preferredSize(62.0f, 22.0f)
                .align(Alignment.START, Alignment.START)
                .margin(8.0f, 8.0f, 0.0f, 0.0f)
                .grow(0.0f);
        Button tooltipAnchor = new Button("Tip");
        tooltipAnchor.preferredSize(54.0f, 22.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .grow(0.0f);
        Button inspectorAnchor = new Button("Inspect");
        inspectorAnchor.preferredSize(72.0f, 22.0f)
                .align(Alignment.END, Alignment.END)
                .margin(0.0f, 0.0f, 8.0f, 8.0f)
                .grow(0.0f);
        Button openAllOverlays = new Button("Open all");
        openAllOverlays.preferredSize(78.0f, 22.0f)
                .align(Alignment.END, Alignment.START)
                .margin(0.0f, 8.0f, 8.0f, 0.0f)
                .grow(0.0f);
        multiOverlayCanvas.addChild(multiOverlayBack);
        multiOverlayCanvas.addChild(menuAnchor);
        multiOverlayCanvas.addChild(tooltipAnchor);
        multiOverlayCanvas.addChild(inspectorAnchor);
        multiOverlayCanvas.addChild(openAllOverlays);

        OverlayLayer multiOverlayLayer = new OverlayLayer(multiOverlayCanvas);
        Popup menuPopup = new Popup(
                menuAnchor,
                samplePane("Menu popup", "First overlay keeps normal content below."))
                .offset(0.0f, 4.0f);
        Popup tooltipPopup = new Popup(
                tooltipAnchor,
                samplePane("Tooltip popup", "Second overlay stacks above the same host."))
                .offset(-34.0f, 4.0f);
        Popup inspectorPopup = new Popup(
                inspectorAnchor,
                samplePane("Inspector popup", "Third overlay checks draw and hit-test order."))
                .offset(-72.0f, 4.0f);
        menuAnchor.onClick(event -> menuPopup.toggle());
        tooltipAnchor.onClick(event -> tooltipPopup.toggle());
        inspectorAnchor.onClick(event -> inspectorPopup.toggle());
        openAllOverlays.onClick(event -> {
            menuPopup.open();
            tooltipPopup.open();
            inspectorPopup.open();
        });
        multiOverlayLayer.addOverlay(menuPopup);
        multiOverlayLayer.addOverlay(tooltipPopup);
        multiOverlayLayer.addOverlay(inspectorPopup);
        smoke.addChild(section("Overlay smoke: multiple floating widgets", multiOverlayLayer));

        VBox dropContent = new VBox();
        dropContent.spacing(6.0f);
        ComboBox clippedDropDown = new ComboBox()
                .items(List.of("Overlay portal", "Inside ScrollView", "Not clipped", "No relayout"))
                .silentSelectedIndex(0)
                .useOverlay();
        clippedDropDown.preferredSize(150.0f, LayoutConstraints.AUTO).grow(0.0f);
        dropContent.addChild(clippedDropDown);
        dropContent.addChild(paragraph("The dropdown opens through the root OverlayLayer, not inside the small ScrollView clip."));
        dropContent.addChild(smokeTile("Scroll filler", 120.0f, 24.0f, 0.18f, 0.32f, 0.50f));
        ScrollView clippedScroll = new ScrollView(dropContent);
        clippedScroll.scrollStep(10.0f);
        clippedScroll.preferredSize(LayoutConstraints.AUTO, 58.0f).grow(0.0f);
        smoke.addChild(section("Overlay smoke: dropdown in clipped scroll", clippedScroll));

        return section("Layout V3 Smoke", smoke);
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
                .cellRichTextProvider((row, column) -> {
                    String state = row % 3 == 0 ? "Queued" : row % 3 == 1 ? "Running" : "Done";
                    int score = (row * 17) % 100;
                    return switch (column) {
                        case 0 -> tableNameText(row);
                        case 1 -> tableStateText(state);
                        case 2 -> tableScoreText(score);
                        default -> RichText.plain("");
                    };
                })
                .sortKeyProvider((row, column) -> column == 2 ? (row * 17) % 100 : row);
        table.columns(List.of(
                new VirtualTableColumn(tableHeaderText("Name"), 120.0f),
                new VirtualTableColumn(tableHeaderText("State"), 90.0f)
                        .align(Alignment.CENTER, Alignment.CENTER),
                new VirtualTableColumn(tableHeaderText("Score"), 64.0f)
                        .align(Alignment.END, Alignment.CENTER)
                        .overflowMode(TextOverflowMode.SHRINK_TO_FIT)));
        table.preferredSize(LayoutConstraints.AUTO, 150.0f).grow(0.0f);
        page.addChild(section("VirtualTableView", table));

        WrapPanel visuals = wrap();
        List<Integer> series = List.of(8, 14, 10, 22, 18, 30, 24, 36, 28);
        Chart chart = new Chart().values(series).type(Chart.Type.BAR);
        chart.preferredSize(220.0f, 120.0f).grow(0.0f);
        Sparkline sparkline = new Sparkline().values(series);
        sparkline.preferredSize(160.0f, 40.0f).grow(0.0f);
        GraphView graph = new GraphView()
                .addNode("A", 0.15f, 0.30f)
                .addNode("B", 0.48f, 0.16f)
                .addNode("C", 0.78f, 0.34f)
                .addNode("D", 0.42f, 0.78f)
                .addEdge("A", "B")
                .addEdge("B", "C")
                .addEdge("B", "D")
                .addEdge("A", "D");
        graph.preferredSize(220.0f, 120.0f).grow(0.0f);
        Label visualStatus = new Label("Visual events: click a bar, spark point, or graph node");
        visualStatus.preferredSize(360.0f, 16.0f).grow(0.0f);
        chart.onBarClick(event -> visualStatus.text(String.format(Locale.ROOT,
                "Chart bar #%d = %.2f", event.index(), event.value())));
        sparkline.onPointClick(event -> visualStatus.text(String.format(Locale.ROOT,
                "Spark point #%d = %.2f", event.index(), event.value())));
        graph.onNodeClick(event -> visualStatus.text(String.format(Locale.ROOT,
                "Graph node %s @ %.2f, %.2f", event.id(), event.normalizedX(), event.normalizedY())));
        visuals.addChild(chart);
        visuals.addChild(sparkline);
        visuals.addChild(graph);
        visuals.addChild(visualStatus);
        page.addChild(section("Charts / Sparkline / GraphView", visuals));
        return page;
    }

    private static RichText tableHeaderText(String label) {
        return RichText.builder()
                .font(Fonts.defaultFace()).size(12.0f)
                .color(MutableColor.rgba(0.35f, 0.80f, 1.0f, 1.0f)).append("◆ ")
                .color(MutableColor.rgba(0.92f, 0.96f, 1.0f, 1.0f)).append(label)
                .build();
    }

    private static RichText tableNameText(int row) {
        return RichText.builder()
                .font(Fonts.defaultFace()).size(11.0f)
                .color(MutableColor.rgba(0.88f, 0.94f, 1.0f, 1.0f)).append("Item ")
                .font(MinecraftFonts.uniformFace()).size(10.0f)
                .color(MutableColor.rgba(0.50f, 0.86f, 1.0f, 1.0f)).append(String.valueOf(row))
                .build();
    }

    private static RichText tableStateText(String state) {
        MutableColor color = switch (state) {
            case "Running" -> MutableColor.rgba(0.35f, 1.0f, 0.45f, 1.0f);
            case "Done" -> MutableColor.rgba(0.75f, 0.95f, 1.0f, 1.0f);
            default -> MutableColor.rgba(1.0f, 0.78f, 0.28f, 1.0f);
        };
        return RichText.of(state, MinecraftFonts.defaultFace(), 11.0f, color);
    }

    private static RichText tableScoreText(int score) {
        MutableColor color = score >= 70
                ? MutableColor.rgba(0.35f, 1.0f, 0.45f, 1.0f)
                : score >= 35
                ? MutableColor.rgba(1.0f, 0.82f, 0.30f, 1.0f)
                : MutableColor.rgba(1.0f, 0.45f, 0.45f, 1.0f);
        return RichText.of(String.valueOf(score), Fonts.defaultFace(), 12.0f, color);
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
        Button modalButton = new Button("Modal");
        modalButton.preferredSize(76.0f, 22.0f).grow(0.0f);
        Button menuButton = new Button("Context");
        menuButton.preferredSize(82.0f, 22.0f).grow(0.0f);
        Button toastButton = new Button("Toast");
        toastButton.preferredSize(72.0f, 22.0f).grow(0.0f);
        ToggleButton freeDrag = new ToggleButton("Free drag");
        freeDrag.preferredSize(86.0f, 22.0f).grow(0.0f);
        row.addChild(tooltipAnchor);
        row.addChild(popupAnchor);
        row.addChild(windowButton);
        row.addChild(modalButton);
        row.addChild(menuButton);
        row.addChild(toastButton);
        row.addChild(freeDrag);
        page.addChild(section("Overlay controls", row));

        Label windowStatus = new Label("WindowManager: idle");
        windowStatus.preferredSize(360.0f, 16.0f).grow(0.0f);
        page.addChild(windowStatus);

        TextBlock body = paragraph("Tooltip does not capture input. Popup is anchored and closes on outside click. WindowWidget has a draggable title bar.");
        page.addChild(body);

        OverlayLayer layer = new OverlayLayer(page);
        Popup popup = new Popup(popupAnchor, samplePane("Popup content", "Anchored popup with retained content."));
        ContextMenu menu = new ContextMenu()
                .item("Inspect", () -> {})
                .item("Duplicate", () -> {})
                .separator()
                .item("Delete", () -> {});
        Toast toast = new Toast("NotificationView / Toast").duration(2.5f);
        WindowWidget window = new WindowWidget("Example Window", samplePane("Dialog body", "Drag the title bar, or close with x."))
                .position(260.0f, 74.0f)
                .closeOnOutsideClick(false);
        window.preferredSize(220.0f, 124.0f).grow(0.0f);
        WindowWidget modal = new WindowWidget("Modal Window", samplePane("Modal body", "Input below this dialog is blocked until it closes."))
                .position(230.0f, 104.0f)
                .modal(true)
                .closeOnOutsideClick(false);
        modal.preferredSize(240.0f, 132.0f).grow(0.0f);

        popupAnchor.onClick(event -> popup.toggle());
        windowButton.onClick(event -> window.toggle());
        modalButton.onClick(event -> modal.openModal());
        menuButton.onClick(event -> menu.toggle(menuButton.layoutBounds().x(), menuButton.layoutBounds().y() + menuButton.layoutBounds().height() + 4.0f));
        toastButton.onClick(event -> toast.toast("Saved recipe-machine layout snapshot."));
        freeDrag.onCheckedChanged(event -> window.constrainToHost(!event.newValue()));
        window.onOpened(event -> windowStatus.text("WindowManager: opened"));
        window.onClosed(event -> windowStatus.text("WindowManager: closed"));
        window.onActivated(event -> windowStatus.text("WindowManager: active"));
        window.onDeactivated(event -> windowStatus.text("WindowManager: inactive"));
        window.onMoved(event -> windowStatus.text(String.format(Locale.ROOT,
                "WindowManager: moved %.0f, %.0f", event.newX(), event.newY())));
        window.onResizeStarted(event -> windowStatus.text("WindowManager: resize " + event.handle()));
        window.onResized(event -> windowStatus.text(String.format(Locale.ROOT,
                "WindowManager: resized %.0fx%.0f", event.newWidth(), event.newHeight())));
        window.onResizeEnded(event -> windowStatus.text(String.format(Locale.ROOT,
                "WindowManager: resize done %.0fx%.0f", event.width(), event.height())));
        modal.onModalOpened(event -> windowStatus.text("WindowManager: modal opened depth " + event.stackDepth()));
        modal.onModalClosed(event -> windowStatus.text("WindowManager: modal closed depth " + event.stackDepth()));
        layer.addOverlay(new Tooltip(tooltipAnchor, "Tooltips are overlay-hosted and layout-independent."));
        layer.addOverlay(new Tooltip(popupAnchor, "Click to toggle an anchored Popup."));
        layer.addOverlay(new Tooltip(windowButton, "Click to open a draggable WindowWidget."));
        layer.addOverlay(popup);
        layer.addOverlay(menu);
        layer.addOverlay(toast);
        layer.addOverlay(window);
        layer.addOverlay(modal);
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

        RichTextView richTextView = new RichTextView(RichText.builder()
                .font(Fonts.defaultFace()).size(13.0f)
                .color(MutableColor.rgba(0.72f, 0.86f, 1.0f, 1.0f)).append("RichTextView wraps ")
                .color(MutableColor.rgba(0.30f, 1.0f, 0.55f, 1.0f)).append("colored retained text ")
                .color(MutableColor.rgba(1.0f, 0.78f, 0.30f, 1.0f)).append("as a reusable widget.")
                .build());
        richTextView.preferredSize(LayoutConstraints.AUTO, 42.0f).grow(0.0f);
        page.addChild(section("RichTextView", richTextView));
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

    private static Box smokeTile(String title, float width, float height, float r, float g, float b) {
        Box tile = panelBox(r, g, b, 0.90f);
        tile.preferredSize(width, height).grow(0.0f);
        Label label = new Label(title);
        label.margin(6.0f, 4.0f)
                .preferredSize(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.CENTER, Alignment.CENTER)
                .grow(0.0f);
        tile.addChild(label);
        return tile;
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
