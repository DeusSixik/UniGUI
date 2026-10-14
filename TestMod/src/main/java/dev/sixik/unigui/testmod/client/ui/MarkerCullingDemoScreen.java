package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapMarker;
import dev.sixik.unigui.widgets.world.AnchorWidget;

import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.TEXT;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.fullWidth;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.label;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.slider;

/**
 * Демо фрустум-куллинга маркеров {@link MapCanvas}.
 *
 * <p>Карта 4096x4096 с мелкой сеткой и несколькими тысячами маркеров.
 * Статус-строка показывает, сколько маркеров реально видимо после cull:
 * внеэкранные пропускают measure/tick/render, а сетка рисует только
 * линии в видимом rect.</p>
 */
public final class MarkerCullingDemoScreen {
    private static final float MAP_SIZE = 4096.0f;
    private static final int DEFAULT_MARKERS = 3000;

    private MarkerCullingDemoScreen() {
    }

    public static void open() {
        DemoScaffold.open("Marker Culling Demo", "MARKER CULLING",
                "Pan: drag. Zoom: Ctrl+wheel. Culled markers skip measure/tick/render.",
                MarkerCullingDemoScreen::build);
    }

    private static void build(DemoScaffold.DemoShell shell) {
        MapCanvas map = new MapCanvas()
                .mapSize(MAP_SIZE, MAP_SIZE)
                .gridSize(32.0f)
                .viewport(40.0f, 30.0f, 0.5f)
                .zoomRange(0.15f, 2.5f);
        shell.content(map);

        Label stats = shell.stats();
        Runnable refresh = () -> stats.text(describe(map));
        map.onViewportChanged(event -> refresh.run());

        Slider density = slider(500.0f, 8000.0f, DEFAULT_MARKERS);
        Label densityLabel = fullWidth(label("Markers: " + DEFAULT_MARKERS, TEXT), 20.0f);
        density.onValueChanged(event -> {
            int count = Math.round(density.value());
            densityLabel.text("Markers: " + count);
            rebuildMarkers(map, count);
            refresh.run();
        });

        shell.add(densityLabel);
        shell.add(density);
        shell.onRecenter(() -> {
            map.viewport(40.0f, 30.0f, 0.5f);
            refresh.run();
        });
        rebuildMarkers(map, DEFAULT_MARKERS);
        refresh.run();
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
}
