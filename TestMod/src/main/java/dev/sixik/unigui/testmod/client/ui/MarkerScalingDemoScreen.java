package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapMarker;
import dev.sixik.unigui.widgets.world.AnchorWidget;

import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.TEXT;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.checkbox;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.fullWidth;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.label;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.slider;

/**
 * Демо zoom-scaling маркеров {@link MapCanvas}.
 *
 * <p>Маркеры трёх типов: фиксированные (как раньше), масштабируемые от zoom
 * без ограничений и масштабируемые с min/max клампом. Панель позволяет
 * переключать scaling, двигать пол/потолок и reference zoom, зум — Ctrl+колесо.</p>
 */
public final class MarkerScalingDemoScreen {
    private static final MutableColor FIXED_BG = MutableColor.rgba(0.30f, 0.32f, 0.38f, 0.92f);
    private static final MutableColor SCALED_BG = MutableColor.rgba(0.12f, 0.38f, 0.52f, 0.92f);
    private static final MutableColor CLAMPED_BG = MutableColor.rgba(0.52f, 0.36f, 0.12f, 0.92f);

    private static final float MAP_SIZE = 2048.0f;
    private static final float BASE_WIDTH = 96.0f;
    private static final float BASE_HEIGHT = 26.0f;

    private MarkerScalingDemoScreen() {
    }

    public static void open() {
        DemoScaffold.open("Marker Scaling Demo", "MARKER SCALING",
                "Pan: drag. Zoom: Ctrl+wheel. Grey = fixed, blue = scaled, amber = scaled + clamped.",
                MarkerScalingDemoScreen::build);
    }

    private static void build(DemoScaffold.DemoShell shell) {
        MapCanvas map = new MapCanvas()
                .mapSize(MAP_SIZE, MAP_SIZE)
                .gridSize(128.0f)
                .viewport(60.0f, 40.0f, 1.0f)
                .zoomRange(0.1f, 4.0f);
        shell.content(map);

        Label stats = shell.stats();
        Runnable refresh = () -> stats.text(describe(map));
        map.onViewportChanged(event -> refresh.run());

        Checkbox scalingCheck = checkbox("Zoom scaling", true);
        scalingCheck.onCheckedChanged(event -> {
            applyScaling(map, scalingCheck.checked());
            refresh.run();
        });

        Label minLabel = fullWidth(label("Min size: 48x13", TEXT), 20.0f);
        Slider minSlider = slider(0.0f, 1.0f, 0.5f);
        minSlider.onValueChanged(event -> {
            float factor = minSlider.value();
            minLabel.text("Min size: " + Math.round(BASE_WIDTH * factor) + "x" + Math.round(BASE_HEIGHT * factor));
            applyClamp(map, BASE_WIDTH * factor, BASE_HEIGHT * factor, true);
            refresh.run();
        });

        Label maxLabel = fullWidth(label("Max size: 144x39", TEXT), 20.0f);
        Slider maxSlider = slider(1.0f, 3.0f, 1.5f);
        maxSlider.onValueChanged(event -> {
            float factor = maxSlider.value();
            maxLabel.text("Max size: " + Math.round(BASE_WIDTH * factor) + "x" + Math.round(BASE_HEIGHT * factor));
            applyClamp(map, BASE_WIDTH * factor, BASE_HEIGHT * factor, false);
            refresh.run();
        });

        Label refLabel = fullWidth(label("Reference zoom: 1.00", TEXT), 20.0f);
        Slider refSlider = slider(0.25f, 2.0f, 1.0f);
        refSlider.onValueChanged(event -> {
            refLabel.text("Reference zoom: " + String.format("%.2f", refSlider.value()));
            applyReferenceZoom(map, refSlider.value());
            refresh.run();
        });

        shell.add(scalingCheck);
        shell.add(minLabel);
        shell.add(minSlider);
        shell.add(maxLabel);
        shell.add(maxSlider);
        shell.add(refLabel);
        shell.add(refSlider);
        shell.onRecenter(() -> {
            map.viewport(60.0f, 40.0f, 1.0f);
            refresh.run();
        });
        rebuildMarkers(map);
        refresh.run();
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
}
