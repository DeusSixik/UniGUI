package dev.sixik.unigui.tests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class TestCommands {
    private static final int SAMPLE_OVERVIEW = 0;
    private static final int SAMPLE_CONTROLS_TEXT = 1;
    private static final int SAMPLE_VIRTUAL_DATA = 2;
    private static final int SAMPLE_EDITABLE_TABLE = 3;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui").executes(ctx -> {
            RenderSystem.recordRenderCall(() -> {
                final DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
                final Widget widget = demoInterface(context);
                final MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), widget, context);
                Minecraft.getInstance().setScreen(screen);
            });

            return 0;
        }));
    }

    private static Widget demoInterface(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();

        Box frame = panelBox(0.025f, 0.028f, 0.035f, 0.96f);
        frame.preferredSize(620.0f, 340.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .grow(0.0f);

        StackPanel frameContent = new StackPanel();
        DockPanel root = new DockPanel();
        root.margin(8.0f);
        frameContent.addChild(root);
        frame.addChild(frameContent);
        viewport.addChild(frame);

        root.addChild(header(), DockSide.TOP);

        StackPanel sampleHost = new StackPanel();
        sampleHost.margin(8.0f);

        WidgetBase[] samples = new WidgetBase[]{
                overviewSample(),
                controlsAndTextSample(),
                virtualDataSample(),
                editableTableSample()
        };
        for (WidgetBase sample : samples) {
            sampleHost.addChild(sample);
        }

        ToggleButton[] navButtons = new ToggleButton[]{
                navButton("Overview"),
                navButton("Controls + Text"),
                navButton("Virtual Data"),
                navButton("Editable Table")
        };

        Box navBox = navigation(context, navButtons, samples);
        root.addChild(navBox, DockSide.LEFT);
        root.addChild(footer(), DockSide.BOTTOM);
        root.addChild(sampleHost, DockSide.LEFT);
        selectSample(SAMPLE_OVERVIEW, navButtons, samples);

        return viewport;
    }

    private static Box header() {
        Box header = panelBox(0.06f, 0.07f, 0.10f, 0.96f);
        header.preferredSize(LayoutConstraints.AUTO, 30.0f).grow(0.0f);

        HBox headerRow = new HBox();
        headerRow.spacing(8.0f);
        headerRow.margin(8.0f, 4.0f).grow(0.0f);

        Label title = new Label("UniGUI samples");
        title.preferredSize(130.0f, 18.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);

        Label subtitle = new Label("Left tabs switch sample panels. Keep Debug tools enabled for profiler overlay.");
        subtitle.preferredSize(LayoutConstraints.AUTO, 18.0f).align(Alignment.START, Alignment.CENTER).grow(1.0f);

        headerRow.addChild(title);
        headerRow.addChild(subtitle);
        header.addChild(padded(headerRow, 8.0f, 4.0f));
        return header;
    }

    private static Box navigation(DefaultUIContext context, ToggleButton[] navButtons, WidgetBase[] samples) {
        VBox nav = new VBox();
        nav.spacing(6.0f);
        nav.preferredSize(146.0f, LayoutConstraints.AUTO).margin(6.0f).grow(0.0f);

        Label navTitle = new Label("Samples");
        navTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        nav.addChild(navTitle);

        for (int index = 0; index < navButtons.length; index++) {
            final int sampleIndex = index;
            ToggleButton button = navButtons[index];
            button.onClick(event -> selectSample(sampleIndex, navButtons, samples));
            nav.addChild(button);
        }

        Separator navSeparator = new Separator();
        navSeparator.preferredSize(LayoutConstraints.AUTO, 1.0f).margin(0.0f, 2.0f).grow(0.0f);
        nav.addChild(navSeparator);

        Checkbox debugTools = new Checkbox("Debug tools");
        debugTools.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        debugTools.onCheckedChanged(event -> {
            context.debugFlags(event.newValue() ? DebugFlags.ALL : DebugFlags.NONE);
        });
        nav.addChild(debugTools);

        TextBlock navHint = new TextBlock("Tip: add each new framework feature as a new sample panel here.");
        navHint.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        navHint.preferredSize(LayoutConstraints.AUTO, 42.0f).grow(0.0f);
        nav.addChild(navHint);

        Box navBox = panelBox(0.035f, 0.04f, 0.055f, 0.90f);
        navBox.preferredSize(160.0f, LayoutConstraints.AUTO).grow(0.0f);
        navBox.addChild(padded(nav, 6.0f));
        return navBox;
    }

    private static Box footer() {
        Box footer = panelBox(0.04f, 0.045f, 0.06f, 0.92f);
        footer.preferredSize(LayoutConstraints.AUTO, 22.0f).grow(0.0f);

        Label footerText = new Label("/unigui  |  arrows/page navigate lists/tables, Enter/F2 edits table cells");
        footerText.preferredSize(LayoutConstraints.AUTO, 14.0f)
                .margin(8.0f, 4.0f)
                .align(Alignment.START, Alignment.CENTER)
                .grow(0.0f);
        footer.addChild(padded(footerText, 8.0f, 4.0f));
        return footer;
    }

    private static VBox overviewSample() {
        VBox sample = samplePanel("Overview", "Small smoke-test dashboard for the currently implemented MVP pieces.");

        TextBlock text = new TextBlock("Use the sample buttons on the left to test independent feature groups. "
                + "The Debug tools checkbox stays outside samples so profiling can be enabled everywhere.");
        text.overflowMode(TextOverflowMode.CLIP);
        text.preferredSize(LayoutConstraints.AUTO, 40.0f).grow(0.0f);
        sample.addChild(text);

        HBox cards = new HBox();
        cards.spacing(8.0f);
        cards.grow(1.0f);
        cards.addChild(infoCard("Layout", "Dock / Stack / Wrap / VBox / HBox"));
        cards.addChild(infoCard("Input", "Focus / pointer capture / text editing"));
        cards.addChild(infoCard("Virtual", "List + table + sorted navigation"));
        sample.addChild(cards);

        return sample;
    }

    private static VBox controlsAndTextSample() {
        VBox sample = samplePanel("Controls + Text", "SearchField clipping, Slider drag, ProgressBar fill and TextBlock overflow modes.");

        WrapPanel toolbar = new WrapPanel().spacing(6.0f).lineSpacing(4.0f);
        toolbar.grow(0.0f);

        SearchField search = new SearchField("very long search query that should clip inside the widget");
        search.preferredSize(160.0f, 20.0f).grow(0.0f);

        Slider slider = new Slider().range(0.0f, 100.0f).step(5.0f).value(35.0f);
        slider.preferredSize(130.0f, 20.0f).grow(0.0f);

        ProgressBar progress = new ProgressBar().range(0.0f, 100.0f).value(42.0f);
        progress.preferredSize(100.0f, 12.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);

        NumberField number = new NumberField().range(0.0d, 100d).value(42.0d);
        number.preferredSize(70.0f, 20.0f).grow(0.0f);
        number.onValueChanged(event -> progress.value((float) event.newValue()));

        toolbar.addChild(search);
        toolbar.addChild(slider);
        toolbar.addChild(progress);
        toolbar.addChild(number);
        sample.addChild(toolbar);

        HBox textModes = new HBox();
        textModes.spacing(8.0f);
        textModes.grow(1.0f);
        textModes.addChild(textModeCard("CLIP", TextOverflowMode.CLIP));
        textModes.addChild(textModeCard("SHRINK", TextOverflowMode.SHRINK_TO_FIT));
        textModes.addChild(textModeCard("MARQUEE", TextOverflowMode.MARQUEE_ON_HOVER));
        sample.addChild(textModes);

        return sample;
    }

    private static VBox virtualDataSample() {
        VBox sample = samplePanel("Virtual Data", "VirtualListView and VirtualTableView with keyboard navigation and sorting.");

        HBox main = new HBox();
        main.spacing(8.0f);
        main.grow(1.0f);

        VirtualListView list = new VirtualListView()
                .itemCount(500)
                .itemHeight(18.0f)
                .overscan(2)
                .selectionMode(SelectionMode.MULTIPLE)
                .itemFactory(index -> new Label("Recipe #" + (index + 1)));
        list.preferredSize(150.0f, LayoutConstraints.AUTO).grow(0.0f);
        list.selectIndex(2);
        main.addChild(list);

        VirtualTableView table = demoTable(false);
        table.grow(1.0f);
        main.addChild(table);

        sample.addChild(main);
        return sample;
    }

    private static VBox editableTableSample() {
        VBox sample = samplePanel("Editable Table", "Select a cell and press Enter/F2. Enter commits, Escape cancels.");

        String[][] rows = {
                {"Copper Gear", "Parts", "12"},
                {"Iron Plate", "Parts", "8"},
                {"Steam Motor", "Machines", "64"},
                {"Glass Tube", "Parts", "6"},
                {"Basic Circuit", "Electronics", "24"},
                {"Recipe Machine", "Machines", "128"}
        };

        TextBlock status = new TextBlock("No edits yet.");
        status.overflowMode(TextOverflowMode.CLIP);
        status.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        sample.addChild(status);

        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 130.0f)
                .addColumn("Category", 90.0f)
                .addColumn("Price", 58.0f)
                .rowCount(rows.length)
                .rowHeight(18.0f)
                .headerHeight(20.0f)
                .overscan(2)
                .selectionMode(SelectionMode.MULTIPLE)
                .editable(true)
                .cellTextProvider((row, column) -> rows[row][column])
                .sortKeyProvider((row, column) -> column == 2 ? Integer.parseInt(rows[row][column]) : rows[row][column]);
        table.grow(1.0f);
        table.activeCell(0, 0);
        table.selectRow(0);
        table.onCellEditStarted(event -> status.text("Editing row " + event.row() + ", col " + event.column() + ": " + event.text()));
        table.onCellEditCommitted(event -> {
            rows[event.row()][event.column()] = event.newText();
            status.text("Committed row " + event.row() + ", col " + event.column() + ": " + event.oldText() + " -> " + event.newText());
        });
        table.onCellEditCancelled(event -> status.text("Cancelled row " + event.row() + ", col " + event.column()));
        sample.addChild(table);

        return sample;
    }

    private static VirtualTableView demoTable(boolean editable) {
        String[] names = {
                "Copper Gear", "Iron Plate", "Steam Motor", "Glass Tube", "Basic Circuit",
                "Recipe Machine", "Carbon Filter", "Pressure Valve", "Steel Frame", "Energy Cell",
                "Fluid Pump", "Mixer Rotor", "Heat Exchanger", "Logic Core", "Assembler Arm"
        };
        String[] categories = {
                "Parts", "Parts", "Machines", "Parts", "Electronics",
                "Machines", "Consumables", "Parts", "Structures", "Power",
                "Fluid", "Machines", "Thermal", "Electronics", "Machines"
        };
        int[] prices = {12, 8, 64, 6, 24, 128, 18, 15, 48, 96, 42, 35, 76, 88, 55};

        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 120.0f)
                .addColumn("Category", 90.0f)
                .addColumn("Price", 58.0f)
                .rowCount(names.length)
                .rowHeight(18.0f)
                .headerHeight(20.0f)
                .overscan(2)
                .selectionMode(SelectionMode.MULTIPLE)
                .editable(editable)
                .cellTextProvider((row, column) -> demoCellText(names, categories, prices, row, column))
                .sortKeyProvider((row, column) -> demoSortKey(names, categories, prices, row, column))
                .sortBy(2, SortDirection.DESCENDING);
        table.selectRow(5);
        return table;
    }

    private static VBox samplePanel(String title, String hint) {
        VBox sample = new VBox();
        sample.spacing(8.0f);
        sample.margin(0.0f);

        Label titleLabel = new Label(title);
        titleLabel.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        sample.addChild(titleLabel);

        TextBlock hintBlock = new TextBlock(hint);
        hintBlock.overflowMode(TextOverflowMode.CLIP);
        hintBlock.preferredSize(LayoutConstraints.AUTO, 24.0f).grow(0.0f);
        sample.addChild(hintBlock);
        return sample;
    }

    private static Box infoCard(String title, String body) {
        Box card = panelBox(0.045f, 0.050f, 0.070f, 0.92f);
        card.grow(1.0f);

        VBox content = new VBox();
        content.spacing(4.0f);
        content.margin(8.0f);

        Label titleLabel = new Label(title);
        titleLabel.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);

        TextBlock bodyText = new TextBlock(body);
        bodyText.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        bodyText.preferredSize(LayoutConstraints.AUTO, 42.0f).grow(0.0f);

        content.addChild(titleLabel);
        content.addChild(bodyText);
        card.addChild(padded(content, 8.0f));
        return card;
    }

    private static Box textModeCard(String title, TextOverflowMode mode) {
        Box card = panelBox(0.045f, 0.050f, 0.070f, 0.92f);
        card.grow(1.0f);

        VBox content = new VBox();
        content.spacing(4.0f);

        Label label = new Label(title);
        label.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);

        TextBlock text = new TextBlock("This is intentionally long text to test overflow behavior in a narrow card.");
        text.overflowMode(mode);
        text.preferredSize(LayoutConstraints.AUTO, 38.0f).grow(0.0f);

        content.addChild(label);
        content.addChild(text);
        card.addChild(padded(content, 8.0f));
        return card;
    }

    private static ToggleButton navButton(String text) {
        ToggleButton button = new ToggleButton(text);
        button.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        return button;
    }

    private static void selectSample(int selectedIndex, ToggleButton[] navButtons, WidgetBase[] samples) {
        for (int index = 0; index < samples.length; index++) {
            boolean selected = index == selectedIndex;
            samples[index].visible(selected);
            navButtons[index].silentChecked(selected);
        }
    }

    private static String demoCellText(String[] names, String[] categories, int[] prices, int row, int column) {
        return switch (column) {
            case 0 -> names[row];
            case 1 -> categories[row];
            case 2 -> Integer.toString(prices[row]);
            default -> "";
        };
    }

    private static Comparable<?> demoSortKey(String[] names, String[] categories, int[] prices, int row, int column) {
        return switch (column) {
            case 0 -> names[row];
            case 1 -> categories[row];
            case 2 -> prices[row];
            default -> row;
        };
    }

    private static Box panelBox(float r, float g, float b, float a) {
        Box box = new Box();
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(3.0f);
        box.background().set(r, g, b, a);
        box.borderColor().set(0.22f, 0.24f, 0.30f, 0.95f);
        return box;
    }

    private static StackPanel padded(WidgetBase child, float margin) {
        return padded(child, margin, margin);
    }

    private static StackPanel padded(WidgetBase child, float horizontal, float vertical) {
        StackPanel panel = new StackPanel();
        child.margin(horizontal, vertical);
        panel.addChild(child);
        return panel;
    }
}
