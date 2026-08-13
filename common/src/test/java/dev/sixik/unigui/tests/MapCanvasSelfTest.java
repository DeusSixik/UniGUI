package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.viewport.ViewportPoint;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapMarker;
import dev.sixik.unigui.widgets.map.MapProjection;
import dev.sixik.unigui.widgets.map.MarkerStyle;

public final class MapCanvasSelfTest {
    private static final float EPSILON = 0.0005f;

    public static void main(String[] args) {
        new MapCanvasSelfTest().run();
        System.out.println("MapCanvasSelfTest passed");
    }

    private void run() {
        projectionBuilderMapsWorldToMap();
        mapMarkerArrangesThroughAnchorLayer();
        projectedMarkerUsesMapProjection();
        mapLayerProducesDrawCommands();
    }

    private void projectionBuilderMapsWorldToMap() {
        MapProjection projection = MapProjection.affine()
                .worldPoint(-1000.0f, -500.0f).mapPoint(0.0f, 0.0f)
                .worldPoint(1000.0f, 500.0f).mapPoint(4000.0f, 2000.0f)
                .build();

        ViewportPoint center = projection.project(0.0f, 0.0f);
        assertClose(2000.0f, center.x(), "projected center x");
        assertClose(1000.0f, center.y(), "projected center y");

        ViewportPoint world = projection.unproject(3000.0f, 1500.0f);
        assertClose(500.0f, world.x(), "unprojected world x");
        assertClose(250.0f, world.y(), "unprojected world y");
    }

    private void mapMarkerArrangesThroughAnchorLayer() {
        MapCanvas map = new MapCanvas()
                .mapSize(1000.0f, 500.0f)
                .gridVisible(false)
                .backgroundVisible(false)
                .viewport(10.0f, 20.0f, 2.0f);
        MapMarker marker = map.addMarker("camp", 100.0f, 50.0f, "Camp", MarkerStyle.CAMP);

        map.measure(new LayoutContext(320.0f, 220.0f));
        map.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 220.0f));

        assertTrue(marker.anchor().arrangedVisible(), "marker should be visible");
        assertClose(210.0f, marker.anchor().projectedRootX(), "marker projected x");
        assertClose(120.0f, marker.anchor().projectedRootY(), "marker projected y");
        assertClose(210.0f - MarkerStyle.CAMP.width() * 0.5f, marker.layoutBounds().x(), "marker bounds x");
        assertClose(120.0f - MarkerStyle.CAMP.height() * 0.5f, marker.layoutBounds().y(), "marker bounds y");
        assertClose(MarkerStyle.CAMP.width(), marker.layoutBounds().width(), "marker width");
        assertClose(MarkerStyle.CAMP.height(), marker.layoutBounds().height(), "marker height");
    }

    private void projectedMarkerUsesMapProjection() {
        MapProjection projection = MapProjection.axisAligned(
                0.0f, 0.0f,
                0.0f, 0.0f,
                100.0f, 100.0f,
                1000.0f, 500.0f);
        MapCanvas map = new MapCanvas()
                .mapSize(1000.0f, 500.0f)
                .projection(projection)
                .viewport(0.0f, 0.0f, 1.0f);
        MapMarker marker = map.addProjectedMarker("vault", 25.0f, 50.0f, "Vault", MarkerStyle.VAULT);

        map.measure(new LayoutContext(1200.0f, 700.0f));
        map.arrange(new MutableRect(0.0f, 0.0f, 1200.0f, 700.0f));

        assertClose(250.0f, marker.anchor().projectedRootX(), "projected marker x");
        assertClose(250.0f, marker.anchor().projectedRootY(), "projected marker y");
    }

    private void mapLayerProducesDrawCommands() {
        MapCanvas map = new MapCanvas()
                .mapSize(512.0f, 256.0f)
                .viewport(0.0f, 0.0f, 1.0f);
        NoopRenderContext context = new NoopRenderContext();

        map.measure(new LayoutContext(512.0f, 256.0f));
        map.arrange(new MutableRect(0.0f, 0.0f, 512.0f, 256.0f));
        map.render(context);

        assertTrue(context.drawList().size() > 0, "map layer should emit draw commands");
    }

    private static void assertClose(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > EPSILON) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
