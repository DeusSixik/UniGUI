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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui").executes(ctx -> {

            RenderSystem.recordRenderCall(() -> {

                final DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
                final var widget = demoInterface(context);
                final MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.empty(), widget, context);
                Minecraft.getInstance().setScreen(screen);
            });

            return 0;
        }));

    }

    private static Widget demoInterface(DefaultUIContext context) {
        StackPanel viewport = new StackPanel();

        Box frame = panelBox(0.025f, 0.028f, 0.035f, 0.96f);
        frame.preferredSize(560.0f, 300.0f)
                .align(Alignment.CENTER, Alignment.CENTER)
                .grow(0.0f);

        StackPanel frameContent = new StackPanel();
        DockPanel root = new DockPanel();
        root.margin(8.0f);
        frameContent.addChild(root);
        frame.addChild(frameContent);
        viewport.addChild(frame);

        Box header = panelBox(0.06f, 0.07f, 0.10f, 0.96f);
        header.preferredSize(LayoutConstraints.AUTO, 30.0f).grow(0.0f);
        HBox headerRow = new HBox();
        headerRow.spacing(8.0f);
        headerRow.margin(8.0f, 4.0f).grow(0.0f);
        Label title = new Label("UniGUI retained-mode demo");
        title.preferredSize(155.0f, 18.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);
        Label subtitle = new Label("Dock/Stack/Wrap + virtual list/table + selection/sort");
        subtitle.preferredSize(LayoutConstraints.AUTO, 18.0f).align(Alignment.START, Alignment.CENTER).grow(1.0f);
        headerRow.addChild(title);
        headerRow.addChild(subtitle);
        header.addChild(padded(headerRow, 8.0f, 4.0f));
        root.addChild(header, DockSide.TOP);

        VBox nav = new VBox();
        nav.spacing(6.0f);
        nav.preferredSize(138.0f, LayoutConstraints.AUTO).margin(6.0f).grow(0.0f);
        Label navTitle = new Label("Navigation");
        navTitle.preferredSize(LayoutConstraints.AUTO, 16.0f).grow(0.0f);
        Button dashboard = new Button("Dashboard");
        dashboard.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        ToggleButton inventory = new ToggleButton("Inventory");
        inventory.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        Checkbox debugTools = new Checkbox("Debug tools");
        debugTools.onCheckedChanged((e) -> {
            context.debugFlags(e.newValue() ? DebugFlags.ALL : DebugFlags.NONE);
        });

        debugTools.preferredSize(LayoutConstraints.AUTO, 20.0f).grow(0.0f);
        Separator navSeparator = new Separator();
        navSeparator.preferredSize(LayoutConstraints.AUTO, 1.0f).margin(0.0f, 2.0f).grow(0.0f);
        TextBlock navHint = new TextBlock("Wheel list/table. Click headers to sort.");
        navHint.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
        navHint.preferredSize(LayoutConstraints.AUTO, 40.0f).grow(0.0f);
        nav.addChild(navTitle);
        nav.addChild(dashboard);
        nav.addChild(inventory);
        nav.addChild(debugTools);
        nav.addChild(navSeparator);
        nav.addChild(navHint);
        Box navBox = panelBox(0.035f, 0.04f, 0.055f, 0.90f);
        navBox.preferredSize(150.0f, LayoutConstraints.AUTO).grow(0.0f);
        navBox.addChild(padded(nav, 6.0f));
        root.addChild(navBox, DockSide.LEFT);

        Box footer = panelBox(0.04f, 0.045f, 0.06f, 0.92f);
        footer.preferredSize(LayoutConstraints.AUTO, 22.0f).grow(0.0f);
        Label footerText = new Label("/unigui  |  wheel list/table, click table headers");
        footerText.preferredSize(LayoutConstraints.AUTO, 14.0f).margin(8.0f, 4.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);
        footer.addChild(padded(footerText, 8.0f, 4.0f));
        root.addChild(footer, DockSide.BOTTOM);

        VBox content = new VBox();
        content.spacing(8.0f);
        content.margin(8.0f);

        WrapPanel toolbar = new WrapPanel().spacing(6.0f).lineSpacing(4.0f);
        toolbar.grow(0.0f);
        SearchField search = new SearchField("filter text");
        search.preferredSize(120.0f, 20.0f).grow(0.0f);
        Slider slider = new Slider().range(0.0f, 100.0f).step(5.0f).value(35.0f);
        slider.preferredSize(130.0f, 20.0f).grow(0.0f);
        ProgressBar progress = new ProgressBar().range(0.0f, 100.0f).value(42.0f);
        progress.preferredSize(100.0f, 12.0f).align(Alignment.START, Alignment.CENTER).grow(0.0f);
        NumberField number = new NumberField().range(0.0d, 100d).value(42.0d);
        number.onValueChanged((s) -> {
            progress.value((float) s.newValue());
        });
        number.preferredSize(70.0f, 20.0f).grow(0.0f);
        toolbar.addChild(search);
        toolbar.addChild(slider);
        toolbar.addChild(progress);
        toolbar.addChild(number);
        content.addChild(toolbar);

        HBox main = new HBox();
        main.spacing(8.0f);
        main.grow(1.0f);

        VirtualListView list = new VirtualListView()
                .itemCount(500)
                .itemHeight(18.0f)
                .overscan(2)
                .selectionMode(SelectionMode.MULTIPLE)
                .itemFactory(index -> new Label("Recipe #" + (index + 1)));
        list.preferredSize(145.0f, LayoutConstraints.AUTO).grow(0.0f);
        list.selectIndex(2);
        main.addChild(list);

        VirtualTableView table = demoTable();
        table.grow(1.0f);
        main.addChild(table);
        content.addChild(main);

        root.addChild(content, DockSide.LEFT);
        return viewport;
    }

    private static VirtualTableView demoTable() {
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
                .cellTextProvider((row, column) -> demoCellText(names, categories, prices, row, column))
                .sortKeyProvider((row, column) -> demoSortKey(names, categories, prices, row, column))
                .sortBy(2, SortDirection.DESCENDING);
        table.selectRow(5);
        return table;
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
