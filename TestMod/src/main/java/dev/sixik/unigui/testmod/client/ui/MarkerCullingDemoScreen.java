package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapMarker;
import dev.sixik.unigui.widgets.world.AnchorWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Демо фрустум-куллинга маркеров {@link MapCanvas}.
 *
 * <p>Карта 4096x4096 с мелкой сеткой и несколькими тысячами маркеров.
 * Статус-строка показывает, сколько маркеров реально видимо после cull:
 * внеэкранные пропускают measure/tick/render, а сетка рисует только
 * линии в видимом rect.</p>
 */
public final class MarkerCullingDemoScreen {
    private static final MutableColor BG = MutableColor.rgba(0.014f, 0.018f, 0.028f, 1.0f);
    private static final MutableColor PANEL = MutableColor.rgba(0.050f, 0.064f, 0.088f, 0.96f);
    private static final MutableColor BORDER = MutableColor.rgba(0.220f, 0.400f, 0.580f, 0.88f);
    private static final MutableColor TEXT = MutableColor.rgba(0.820f, 0.900f, 0.960f, 1.0f);
    private static final MutableColor MUTED = MutableColor.rgba(0.500f, 0.600f, 0.700f, 1.0f);

    private static final float MAP_SIZE = 4096.0f;
    private static final int DEFAULT_MARKERS = 3000;

    private MarkerCullingDemoScreen() {
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;

        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(2.0f));

        Runnable[] close = new Runnable[1];
        Widget root = root(() -> close[0].run());
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("Marker Culling Demo"), root, context) {
            @Override
            public void onClose() {
                Minecraft.getInstance().setScreen(previous);
            }
        };
        close[0] = screen::onClose;
        screen.useContextScale().scaleWithMinecraftGui(false);
        screen.useSdfDefaultFont();
        screen.renderPolicy(UiRenderPolicy.vsync());
        minecraft.setScreen(screen);
    }

    private static Widget root(Runnable closeAction) {
        StackPanel viewport = new StackPanel();
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f).overflow(Overflow.HIDDEN));

        Box background = new Box();
        background.themeEnabled(false);
        background.backgroundVisible(true);
        background.borderVisible(false);
        background.background().set(BG);
        background.layout(style -> style.sizePercent(100.0f, 100.0f)
                .align(Alignment.STRETCH, Alignment.STRETCH));
        viewport.addChild(background);

        MapCanvas map = new MapCanvas()
                .mapSize(MAP_SIZE, MAP_SIZE)
                .gridSize(32.0f)
                .viewport(40.0f, 30.0f, 0.5f)
                .zoomRange(0.15f, 2.5f);
        map.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        Label stats = label("", TEXT);
        stats.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Label hint = label("Pan: drag. Zoom: Ctrl+wheel. Culled markers skip measure/tick/render.", MUTED);
        hint.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        Runnable refresh = () -> stats.text(describe(map));
        map.onViewportChanged(event -> refresh.run());

        Slider density = new Slider();
        density.range(500.0f, 8000.0f);
        density.value(DEFAULT_MARKERS);
        density.layout(style -> style.widthPercent(100.0f).height(24.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Label densityLabel = label("Markers: " + DEFAULT_MARKERS, TEXT);
        densityLabel.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        density.onValueChanged(event -> {
            int count = Math.round(density.value());
            densityLabel.text("Markers: " + count);
            rebuildMarkers(map, count);
            refresh.run();
        });

        Button recenter = button("Recenter", () -> {
            map.viewport(40.0f, 30.0f, 0.5f);
            refresh.run();
        });
        recenter.layout(style -> style.widthPercent(100.0f).height(28.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Button close = button("Close", closeAction);
        close.layout(style -> style.widthPercent(100.0f).height(28.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        VBox side = new VBox();
        side.spacing(8.0f);
        side.layout(style -> style.width(240.0f).heightPercent(100.0f).padding(10.0f)
                .gap(8.0f).flexGrow(0.0f).flexShrink(0.0f));
        Label title = label("MARKER CULLING", TEXT);
        title.layout(style -> style.widthPercent(100.0f).height(22.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        side.addChild(title);
        side.addChild(stats);
        side.addChild(hint);
        side.addChild(densityLabel);
        side.addChild(density);
        side.addChild(recenter);
        side.addChild(close);

        Box sidePanel = new Box();
        sidePanel.themeEnabled(false);
        sidePanel.backgroundVisible(true);
        sidePanel.borderVisible(true);
        sidePanel.background().set(PANEL);
        sidePanel.borderColor().set(BORDER);
        sidePanel.layout(style -> style.width(260.0f).heightPercent(100.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        sidePanel.addChild(side);

        HBox body = new HBox();
        body.spacing(10.0f);
        body.layout(style -> style.sizePercent(96.0f, 90.0f)
                .align(Alignment.CENTER, Alignment.CENTER).gap(10.0f));
        body.addChild(map);
        body.addChild(sidePanel);

        rebuildMarkers(map, DEFAULT_MARKERS);
        refresh.run();
        return new OverlayLayer(body);
    }

    private static void rebuildMarkers(MapCanvas map, int count) {
        map.anchorLayer().clear();
        int safe = Math.max(0, count);
        int grid = (int) Math.ceil(Math.sqrt(safe));
        float step = MAP_SIZE / Math.max(1, grid + 1);
        int placed = 0;
        for (int row = 0; row < grid && placed < safe; row++) {
            for (int col = 0; col < grid && placed < safe; col++) {
                float x = (col + 1) * step;
                float y = (row + 1) * step;
                MapMarker marker = new MapMarker("#" + placed);
                map.addMarker("poi-" + placed, x, y, marker)
                        .screenSize(52.0f, 20.0f)
                        .pivot(0.5f, 0.5f);
                placed++;
            }
        }
    }

    private static String describe(MapCanvas map) {
        int total = map.anchorLayer().size();
        int visible = 0;
        for (AnchorWidget anchor : map.anchorLayer().anchors()) {
            if (anchor.arrangedVisible()) visible++;
        }
        return "Markers: " + visible + " visible / " + total + " total, zoom "
                + String.format("%.2f", map.viewport().zoom());
    }

    private static Label label(String text, MutableColor color) {
        Label label = new Label(text);
        label.color(color);
        return label;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.onClick(event -> action.run());
        return button;
    }
}
