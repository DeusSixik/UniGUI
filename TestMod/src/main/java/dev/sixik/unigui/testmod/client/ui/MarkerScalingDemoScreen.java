package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.debug.DebugFlags;
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
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapMarker;
import dev.sixik.unigui.widgets.world.AnchorWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Демо zoom-scaling маркеров {@link MapCanvas}.
 *
 * <p>Маркеры трёх типов: фиксированные (как раньше), масштабируемые от zoom
 * без ограничений и масштабируемые с min/max клампом. Панель позволяет
 * переключать scaling, двигать пол/потолок и reference zoom, зум — Ctrl+колесо.</p>
 */
public final class MarkerScalingDemoScreen {
    private static final MutableColor BG = MutableColor.rgba(0.014f, 0.018f, 0.028f, 1.0f);
    private static final MutableColor PANEL = MutableColor.rgba(0.050f, 0.064f, 0.088f, 0.96f);
    private static final MutableColor BORDER = MutableColor.rgba(0.220f, 0.400f, 0.580f, 0.88f);
    private static final MutableColor TEXT = MutableColor.rgba(0.820f, 0.900f, 0.960f, 1.0f);
    private static final MutableColor MUTED = MutableColor.rgba(0.500f, 0.600f, 0.700f, 1.0f);
    private static final MutableColor FIXED_BG = MutableColor.rgba(0.30f, 0.32f, 0.38f, 0.92f);
    private static final MutableColor SCALED_BG = MutableColor.rgba(0.12f, 0.38f, 0.52f, 0.92f);
    private static final MutableColor CLAMPED_BG = MutableColor.rgba(0.52f, 0.36f, 0.12f, 0.92f);

    private static final float MAP_SIZE = 2048.0f;
    private static final float BASE_WIDTH = 96.0f;
    private static final float BASE_HEIGHT = 26.0f;

    private MarkerScalingDemoScreen() {
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
        context.debugFlags(DebugFlags.PROFILER_OVERLAY);

        Runnable[] close = new Runnable[1];
        Widget root = root(() -> close[0].run());
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("Marker Scaling Demo"), root, context) {
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
                .gridSize(128.0f)
                .viewport(60.0f, 40.0f, 1.0f)
                .zoomRange(0.1f, 4.0f);
        map.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        Label stats = label("", TEXT);
        stats.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Label hint = label("Pan: drag. Zoom: Ctrl+wheel. Grey = fixed, blue = scaled, amber = scaled + clamped.", MUTED);
        hint.layout(style -> style.widthPercent(100.0f).height(34.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        Runnable refresh = () -> stats.text(describe(map));
        map.onViewportChanged(event -> refresh.run());

        Checkbox scalingCheck = new Checkbox("Zoom scaling");
        scalingCheck.checked(true);
        scalingCheck.onCheckedChanged(event -> {
            applyScaling(map, scalingCheck.checked());
            refresh.run();
        });

        Label minLabel = label("Min size: 48x13", TEXT);
        minLabel.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Slider minSlider = slider(0.0f, 1.0f, 0.5f);
        minSlider.onValueChanged(event -> {
            float factor = minSlider.value();
            minLabel.text("Min size: " + Math.round(BASE_WIDTH * factor) + "x" + Math.round(BASE_HEIGHT * factor));
            applyClamp(map, BASE_WIDTH * factor, BASE_HEIGHT * factor, true);
            refresh.run();
        });

        Label maxLabel = label("Max size: 144x39", TEXT);
        maxLabel.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Slider maxSlider = slider(1.0f, 3.0f, 1.5f);
        maxSlider.onValueChanged(event -> {
            float factor = maxSlider.value();
            maxLabel.text("Max size: " + Math.round(BASE_WIDTH * factor) + "x" + Math.round(BASE_HEIGHT * factor));
            applyClamp(map, BASE_WIDTH * factor, BASE_HEIGHT * factor, false);
            refresh.run();
        });

        Label refLabel = label("Reference zoom: 1.00", TEXT);
        refLabel.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Slider refSlider = slider(0.25f, 2.0f, 1.0f);
        refSlider.onValueChanged(event -> {
            refLabel.text("Reference zoom: " + String.format("%.2f", refSlider.value()));
            applyReferenceZoom(map, refSlider.value());
            refresh.run();
        });

        Button recenter = button("Recenter", () -> {
            map.viewport(60.0f, 40.0f, 1.0f);
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
        Label title = label("MARKER SCALING", TEXT);
        title.layout(style -> style.widthPercent(100.0f).height(22.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        side.addChild(title);
        side.addChild(stats);
        side.addChild(hint);
        side.addChild(scalingCheck);
        side.addChild(minLabel);
        side.addChild(minSlider);
        side.addChild(maxLabel);
        side.addChild(maxSlider);
        side.addChild(refLabel);
        side.addChild(refSlider);
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

        rebuildMarkers(map);
        refresh.run();
        return new OverlayLayer(body);
    }

    private static void rebuildMarkers(MapCanvas map) {
        map.anchorLayer().clear();
        // Три колонки: фиксированные / масштабируемые / масштабируемые с клампом.
        float[] columns = {MAP_SIZE * 0.25f, MAP_SIZE * 0.5f, MAP_SIZE * 0.75f};
        int rows = 8;
        for (int column = 0; column < columns.length; column++) {
            for (int row = 0; row < rows; row++) {
                float x = columns[column];
                float y = MAP_SIZE * (row + 1) / (rows + 1);
                MapMarker marker = marker(column, row);
                String id = "m-" + column + "-" + row;
                if (column == 0) {
                    map.addMarker(id, x, y, marker)
                            .screenSize(BASE_WIDTH, BASE_HEIGHT)
                            .pivot(0.5f, 0.5f);
                } else if (column == 1) {
                    map.addMarker(id, x, y, marker)
                            .screenSize(BASE_WIDTH, BASE_HEIGHT)
                            .pivot(0.5f, 0.5f)
                            .zoomScaling(true, 1.0f);
                } else {
                    map.addMarker(id, x, y, marker)
                            .screenSize(BASE_WIDTH, BASE_HEIGHT)
                            .pivot(0.5f, 0.5f)
                            .minScreenSize(BASE_WIDTH * 0.5f, BASE_HEIGHT * 0.5f)
                            .maxScreenSize(BASE_WIDTH * 1.5f, BASE_HEIGHT * 1.5f)
                            .zoomScaling(true, 1.0f);
                }
            }
        }
    }

    private static MapMarker marker(int column, int row) {
        MapMarker marker = new MapMarker(column == 0 ? "fixed" : column == 1 ? "scaled" : "clamped");
        marker.themeEnabled(false);
        marker.backgroundVisible(true);
        marker.borderVisible(false);
        marker.radius(4.0f);
        marker.background().set(column == 0 ? FIXED_BG : column == 1 ? SCALED_BG : CLAMPED_BG);
        return marker;
    }

    private static void applyScaling(MapCanvas map, boolean enabled) {
        // Первая колонка всегда фиксированная — эталон для сравнения.
        for (AnchorWidget anchor : map.anchorLayer().anchors()) {
            if (anchor.id().startsWith("m-0-")) continue;
            anchor.zoomScaling(enabled, anchor.referenceZoom());
        }
    }

    private static void applyClamp(MapCanvas map, float width, float height, boolean min) {
        for (AnchorWidget anchor : map.anchorLayer().anchors()) {
            if (!anchor.id().startsWith("m-2-")) continue;
            if (min) {
                anchor.minScreenSize(width, height);
            } else {
                anchor.maxScreenSize(width, height);
            }
        }
    }

    private static void applyReferenceZoom(MapCanvas map, float referenceZoom) {
        for (AnchorWidget anchor : map.anchorLayer().anchors()) {
            if (anchor.id().startsWith("m-0-")) continue;
            anchor.zoomScaling(anchor.zoomScaling(), referenceZoom);
        }
    }

    private static String describe(MapCanvas map) {
        float zoom = map.viewport().zoom();
        String sample = "";
        AnchorWidget anchor = map.anchorLayer().anchor("m-2-0");
        if (anchor != null) {
            sample = ", clamped sample "
                    + Math.round(anchor.widget().layoutBounds().width()) + "x"
                    + Math.round(anchor.widget().layoutBounds().height());
        }
        return "Zoom " + String.format("%.2f", zoom) + sample;
    }

    private static Slider slider(float min, float max, float value) {
        Slider slider = new Slider();
        slider.range(min, max);
        slider.value(value);
        slider.layout(style -> style.widthPercent(100.0f).height(24.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        return slider;
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
