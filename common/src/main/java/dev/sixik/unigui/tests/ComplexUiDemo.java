package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.MutableUIScaleProvider;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.Sparkline;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextInput;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Насыщенный экран для ручной проверки layout и производительности UniGUI.
 *
 * <p>Экран специально объединяет несколько разных сценариев: вложенные
 * flex-контейнеры, scrolling, текстовые поля, большое количество строк,
 * графики, progress-бары и собственный canvas. Его удобно открывать рядом с
 * Spark или другим профайлером, чтобы сравнивать стоимость idle-кадра,
 * прокрутки и интерактивных обновлений.</p>
 */
public final class ComplexUiDemo {
    private static final MutableUIScaleProvider SCALE = new MutableUIScaleProvider(2.0f);

    private static final MutableColor BACKGROUND = color(0.012f, 0.018f, 0.030f, 1.0f);
    private static final MutableColor PANEL = color(0.030f, 0.046f, 0.072f, 0.98f);
    private static final MutableColor PANEL_ALT = color(0.040f, 0.060f, 0.090f, 0.98f);
    private static final MutableColor BORDER = color(0.12f, 0.28f, 0.43f, 0.92f);
    private static final MutableColor TEXT = color(0.82f, 0.91f, 0.98f, 1.0f);
    private static final MutableColor MUTED = color(0.42f, 0.56f, 0.68f, 1.0f);
    private static final MutableColor CYAN = color(0.16f, 0.78f, 1.0f, 1.0f);
    private static final MutableColor GREEN = color(0.25f, 0.88f, 0.53f, 1.0f);
    private static final MutableColor ORANGE = color(1.0f, 0.62f, 0.22f, 1.0f);

    private ComplexUiDemo() {
    }

    /** Ставит demo-экран в очередь рендера Minecraft. */
    public static void openDemo() {
        RenderSystem.recordRenderCall(ComplexUiDemo::openDemoClient);
    }

    /** Открывает demo-экран; метод вызывается на render thread. */
    public static void openDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(SCALE);
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("UniGUI Complex Demo"), create(), context);
        screen.renderPolicy(UiRenderPolicy.continuous());
        screen.scaleWithMinecraftGui(false);
        Minecraft.getInstance().setScreen(screen);
    }

    /**
     * Создаёт корневой виджет без привязки к Minecraft screen.
     *
     * @return готовый demo-root для встраивания в другой экран
     */
    public static Widget create() {
        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));

        root.addChild(background());

        VBox content = new VBox();
        content.spacing(8.0f);
        content.layout(style -> style
                .margin(12.0f)
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.STRETCH)
                .flexGrow(1.0f)
                .flexShrink(1.0f));

        Label title = label("UNIGUI / RENDER LAB", TEXT, 16.0f);
        title.layout(style -> style.size(LayoutConstraints.AUTO, 24.0f)
                .flexGrow(1.0f).flexShrink(1.0f));

        Label status = label("READY  |  0 dirty widgets  |  frame budget 16.67 ms", MUTED, 11.0f);
        status.layout(style -> style.size(330.0f, 24.0f)
                .align(Alignment.END, Alignment.CENTER).flexGrow(0.0f).flexShrink(0.0f));

        Button refresh = button("REBUILD");
        refresh.layout(style -> style.size(82.0f, 24.0f).flexGrow(0.0f).flexShrink(0.0f));
        refresh.onClick(event -> status.text("REBUILT  |  layout invalidated  |  frame budget 16.67 ms"));

        Button pulse = button("PULSE");
        pulse.layout(style -> style.size(70.0f, 24.0f).flexGrow(0.0f).flexShrink(0.0f));
        pulse.onClick(event -> status.text("PULSE  |  animated properties active  |  inspect allocations"));

        HBox toolbar = new HBox();
        toolbar.spacing(8.0f);
        toolbar.layout(style -> style.size(LayoutConstraints.AUTO, 28.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        toolbar.addChild(title);
        toolbar.addChild(status);
        toolbar.addChild(refresh);
        toolbar.addChild(pulse);
        content.addChild(toolbar);

        content.addChild(mainWorkspace(status));
        content.addChild(footer());
        root.addChild(content);
        return root;
    }

    private static Widget mainWorkspace(Label status) {
        HBox workspace = new HBox();
        workspace.spacing(8.0f);
        workspace.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.STRETCH)
                .flexGrow(1.0f).flexShrink(1.0f));

        workspace.addChild(navigationPanel(status));
        workspace.addChild(centerPanel());
        workspace.addChild(inspectorPanel());
        return workspace;
    }

    private static Widget navigationPanel(Label status) {
        PanelColumn panel = panelColumn(210.0f);
        panel.addChild(label("WIDGET TREE", TEXT, 12.0f));

        TextInput search = new TextInput("Search widgets");
        search.layout(style -> style.size(LayoutConstraints.AUTO, 26.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        search.backgroundVisible(true);
        search.borderVisible(true);
        search.background().set(PANEL_ALT);
        search.borderColor().set(BORDER);
        panel.addChild(search);

        VBox tree = new VBox();
        tree.spacing(3.0f);
        tree.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .flexGrow(1.0f).flexShrink(1.0f));
        String[] nodes = {"Root", "  Header", "  MainWorkspace", "    Navigation", "    Metrics", "      CPU Sparkline", "      GPU Sparkline", "      Frame Canvas", "    Inspector", "  Footer"};
        for (int i = 0; i < nodes.length; i++) {
            Label node = label((i == 0 ? "> " : "") + nodes[i], i == 0 ? CYAN : MUTED, 10.0f);
            node.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f)
                    .flexGrow(0.0f).flexShrink(0.0f));
            tree.addChild(node);
        }

        ScrollView treeScroll = new ScrollView(tree);
        treeScroll.scrollStep(18.0f);
        treeScroll.layout(style -> style.overflowY(Overflow.AUTO)
                .overflowX(Overflow.HIDDEN).flexGrow(1.0f).flexShrink(1.0f));
        panel.addChild(treeScroll);

        Button invalidate = button("INVALIDATE TREE");
        invalidate.layout(style -> style.size(LayoutConstraints.AUTO, 24.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        invalidate.onClick(event -> status.text("TREE INVALIDATED  |  waiting for next layout pass"));
        panel.addChild(invalidate);
        return panel.widget();
    }

    private static Widget centerPanel() {
        PanelColumn panel = panelColumn(LayoutConstraints.AUTO);
        panel.shell.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        panel.addChild(label("FRAME TELEMETRY", TEXT, 12.0f));

        HBox metrics = new HBox();
        metrics.spacing(6.0f);
        metrics.layout(style -> style.size(LayoutConstraints.AUTO, 72.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        metrics.addChild(metric("CPU", "5.82 ms", CYAN, 0.48f));
        metrics.addChild(metric("GPU", "3.14 ms", GREEN, 0.26f));
        metrics.addChild(metric("ALLOC", "1.8 MB/s", ORANGE, 0.71f));
        panel.addChild(metrics);

        HBox charts = new HBox();
        charts.spacing(6.0f);
        charts.layout(style -> style.size(LayoutConstraints.AUTO, 112.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        charts.addChild(chart("CPU / frame", CYAN, 11));
        charts.addChild(chart("GPU / frame", GREEN, 17));
        charts.addChild(chart("Allocations", ORANGE, 23));
        panel.addChild(charts);

        LoadGrid grid = new LoadGrid();
        grid.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .flexGrow(1.0f).flexShrink(1.0f));
        panel.addChild(grid);

        VBox rows = new VBox();
        rows.spacing(3.0f);
        rows.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .flexGrow(0.0f).flexShrink(0.0f));
        for (int i = 0; i < 24; i++) {
            rows.addChild(heavyRow(i));
        }
        ScrollView rowsScroll = new ScrollView(rows);
        rowsScroll.scrollStep(22.0f);
        rowsScroll.layout(style -> style.size(LayoutConstraints.AUTO, 158.0f)
                .overflowY(Overflow.AUTO).overflowX(Overflow.HIDDEN)
                .flexGrow(0.0f).flexShrink(0.0f));
        panel.addChild(rowsScroll);
        return panel.widget();
    }

    private static Widget inspectorPanel() {
        PanelColumn panel = panelColumn(230.0f);
        panel.addChild(label("INSPECTOR", TEXT, 12.0f));
        panel.addChild(label("Selected: FrameTelemetry", MUTED, 10.0f));

        VBox inspectorRows = new VBox();
        inspectorRows.spacing(7.0f);
        inspectorRows.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .flexGrow(1.0f).flexShrink(1.0f));
        String[] properties = {"layout.measure", "layout.arrange", "render.commands", "render.batches", "text.glyphs", "texture.binds", "input.dispatch", "animation.tick", "cache.hit-rate", "memory.retained"};
        for (int i = 0; i < properties.length; i++) {
            VBox row = new VBox();
            row.spacing(2.0f);
            row.layout(style -> style.size(LayoutConstraints.AUTO, 28.0f)
                    .flexGrow(0.0f).flexShrink(0.0f));
            Label name = label(properties[i], MUTED, 9.0f);
            name.layout(style -> style.size(LayoutConstraints.AUTO, 12.0f)
                    .flexGrow(0.0f).flexShrink(0.0f));
            ProgressBar bar = new ProgressBar();
            bar.range(0.0f, 1.0f).value((i * 0.137f + 0.18f) % 0.92f);
            bar.layout(style -> style.size(LayoutConstraints.AUTO, 11.0f)
                    .flexGrow(1.0f).flexShrink(1.0f));
            row.addChild(name);
            row.addChild(bar);
            inspectorRows.addChild(row);
        }

        ScrollView inspectorScroll = new ScrollView(inspectorRows);
        inspectorScroll.scrollStep(20.0f);
        inspectorScroll.layout(style -> style.overflowY(Overflow.AUTO)
                .overflowX(Overflow.HIDDEN).flexGrow(1.0f).flexShrink(1.0f));
        panel.addChild(inspectorScroll);

        Button toggle = button("TOGGLE OVERLAY");
        toggle.layout(style -> style.size(LayoutConstraints.AUTO, 24.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        panel.addChild(toggle);
        return panel.widget();
    }

    private static Widget footer() {
        HBox footer = new HBox();
        footer.spacing(14.0f);
        footer.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        footer.addChild(label("FPS  144", GREEN, 10.0f));
        footer.addChild(label("DRAW  318", CYAN, 10.0f));
        footer.addChild(label("BATCH  27", TEXT, 10.0f));
        footer.addChild(label("HEAP  412 MB", ORANGE, 10.0f));
        footer.addChild(label("SCROLLABLE / INTERACTIVE / CONTINUOUS", MUTED, 10.0f));
        return footer;
    }

    private static Widget metric(String name, String value, MutableColor accent, float progress) {
        Box card = panelBox(PANEL_ALT);
        card.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        VBox body = new VBox();
        body.spacing(3.0f);
        body.layout(style -> style.margin(8.0f).flexGrow(1.0f).flexShrink(1.0f));
        body.addChild(label(name, accent, 10.0f));
        body.addChild(label(value, TEXT, 16.0f));
        ProgressBar bar = new ProgressBar();
        bar.range(0.0f, 1.0f).value(progress);
        bar.layout(style -> style.size(LayoutConstraints.AUTO, 7.0f)
                .flexGrow(1.0f).flexShrink(1.0f));
        body.addChild(bar);
        card.addChild(body);
        return card;
    }

    private static Widget chart(String title, MutableColor accent, int seed) {
        Box box = panelBox(PANEL_ALT);
        box.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        VBox content = new VBox();
        content.spacing(2.0f);
        content.layout(style -> style.margin(6.0f).flexGrow(1.0f).flexShrink(1.0f));
        content.addChild(label(title, MUTED, 9.0f));
        Sparkline sparkline = new Sparkline();
        sparkline.values(values(seed));
        sparkline.lineColor().set(accent);
        sparkline.fillColor().set(accent.r(), accent.g(), accent.b(), 0.16f);
        sparkline.pointColor().set(accent);
        sparkline.pointMode(Sparkline.PointMode.NONE);
        sparkline.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .flexGrow(1.0f).flexShrink(1.0f));
        content.addChild(sparkline);
        box.addChild(content);
        return box;
    }

    private static Widget heavyRow(int index) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.size(LayoutConstraints.AUTO, 21.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Label name = label(String.format("%02d  Widget.%s", index + 1, index % 3 == 0 ? "render" : index % 3 == 1 ? "layout" : "input"), MUTED, 9.0f);
        name.layout(style -> style.size(126.0f, 21.0f)
                .align(Alignment.START, Alignment.CENTER).flexGrow(0.0f).flexShrink(0.0f));
        ProgressBar bar = new ProgressBar();
        bar.range(0.0f, 1.0f).value((index * 0.071f + 0.16f) % 0.88f);
        bar.layout(style -> style.size(LayoutConstraints.AUTO, 9.0f)
                .align(Alignment.CENTER, Alignment.CENTER).flexGrow(1.0f).flexShrink(1.0f));
        Label value = label(String.format("%4.1f%%", ((index * 7) % 83) + 8.0f), TEXT, 9.0f);
        value.layout(style -> style.size(42.0f, 21.0f)
                .align(Alignment.END, Alignment.CENTER).flexGrow(0.0f).flexShrink(0.0f));
        row.addChild(name);
        row.addChild(bar);
        row.addChild(value);
        return row;
    }

    private static Box background() {
        Box box = panelBox(BACKGROUND);
        box.layout(style -> style.sizePercent(100.0f, 100.0f)
                .align(Alignment.STRETCH, Alignment.STRETCH));
        box.borderVisible(false);
        return box;
    }

    private static PanelColumn panelColumn(float width) {
        return new PanelColumn(width);
    }

    private static final class PanelColumn {
        private final Box shell;
        private final VBox column;

        private PanelColumn(float width) {
            shell = panelBox(PANEL);
            shell.layout(style -> style.size(width, LayoutConstraints.AUTO)
                    .flexGrow(0.0f).flexShrink(1.0f));
            column = new VBox();
            column.spacing(7.0f);
            column.layout(style -> style.margin(9.0f).flexGrow(1.0f).flexShrink(1.0f));
            shell.addChild(column);
        }

        private void addChild(Widget child) {
            column.addChild(child);
        }

        private Widget widget() {
            return shell;
        }
    }

    private static Box panelBox(MutableColor background) {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.borderVisible(true);
        box.radius(4.0f);
        box.background().set(background);
        box.borderColor().set(BORDER);
        box.borderWidth(1.0f);
        return box;
    }

    private static Label label(String text, MutableColor color, float size) {
        Label label = new Label(text);
        label.color(color);
        return label;
    }

    private static Button button(String text) {
        Button button = new Button(text);
        button.themeEnabled(false);
        button.backgroundVisible(true);
        button.borderVisible(true);
        button.radius(3.0f);
        button.background().set(PANEL_ALT);
        button.borderColor().set(BORDER);
        button.textColor().set(TEXT);
        return button;
    }

    private static List<Float> values(int seed) {
        List<Float> values = new ArrayList<>(32);
        for (int i = 0; i < 32; i++) {
            double wave = Math.sin((i + seed) * 0.48) * 0.22;
            double noise = ((i * 17 + seed * 13) % 11) * 0.018;
            values.add((float) Math.max(0.04, Math.min(0.96, 0.48 + wave + noise)));
        }
        return values;
    }

    private static MutableColor color(float r, float g, float b, float a) {
        return MutableColor.rgba(r, g, b, a);
    }

    /** Canvas с сеткой и множеством примитивов для проверки shape batching. */
    private static final class LoadGrid extends WidgetBase {
        private float phase;

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, 320.0f, 120.0f));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            float dt = frame == null ? 1.0f / 60.0f : Math.max(0.0f, frame.deltaSeconds());
            phase += dt;
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null) return;
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float width = layoutBounds().width();
            float height = layoutBounds().height();
            if (width <= 0.0f || height <= 0.0f) return;

            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            draw.roundedRect(x, y, width, height, 3.0f, Paint.fill(color(0.018f, 0.032f, 0.052f, 1.0f)));
            float cellWidth = width / 32.0f;
            float cellHeight = height / 8.0f;
            for (int row = 0; row < 8; row++) {
                for (int column = 0; column < 32; column++) {
                    float pulse = (float) ((Math.sin(phase * 1.8f + column * 0.31f + row * 0.7f) + 1.0) * 0.5);
                    MutableColor cell = row == 0
                            ? color(0.10f, 0.42f + pulse * 0.24f, 0.62f + pulse * 0.22f, 0.82f)
                            : color(0.06f, 0.16f + pulse * 0.12f, 0.24f + pulse * 0.18f, 0.92f);
                    draw.addRectFilled(x + column * cellWidth + 1.0f,
                            y + row * cellHeight + 1.0f,
                            Math.max(1.0f, cellWidth - 2.0f),
                            Math.max(1.0f, cellHeight - 2.0f), 1.0f, cell);
                }
            }
            draw.addRect(x, y, width, height, 3.0f, BORDER, 1.0f);
        }
    }

}
