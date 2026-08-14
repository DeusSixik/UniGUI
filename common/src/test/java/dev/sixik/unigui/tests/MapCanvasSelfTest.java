package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.viewport.ViewportPoint;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapMarker;
import dev.sixik.unigui.widgets.map.MapMarkerHandle;
import dev.sixik.unigui.widgets.map.MapProjection;

public final class MapCanvasSelfTest {
    private static final float EPSILON = 0.0005f;

    public static void main(String[] args) {
        new MapCanvasSelfTest().run();
        System.out.println("MapCanvasSelfTest passed");
    }

    private void run() {
        projectionBuilderMapsWorldToMap();
        markerWidgetArrangesThroughAnchorLayer();
        projectedMarkerUsesMapProjection();
        markerHandleMovesAnchor();
        markerLookupSkipsDetachedAnchors();
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

    private void markerWidgetArrangesThroughAnchorLayer() {
        MapCanvas map = new MapCanvas()
                .mapSize(1000.0f, 500.0f)
                .gridVisible(false)
                .backgroundVisible(false)
                .viewport(10.0f, 20.0f, 2.0f);
        MapMarker marker = new MapMarker("Camp");
        MapMarkerHandle<MapMarker> handle = map.addMarkerWidget("camp", 100.0f, 50.0f, marker)
                .screenSize(72.0f, 22.0f)
                .pivot(0.5f, 0.5f);

        map.measure(new LayoutContext(320.0f, 220.0f));
        map.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 220.0f));

        assertTrue(handle.anchor().arrangedVisible(), "marker should be visible");
        assertTrue(marker.anchor() == handle.anchor(), "MapMarker should receive its anchor");
        assertClose(210.0f, handle.anchor().projectedRootX(), "marker projected x");
        assertClose(120.0f, handle.anchor().projectedRootY(), "marker projected y");
        assertClose(210.0f - 72.0f * 0.5f, marker.layoutBounds().x(), "marker bounds x");
        assertClose(120.0f - 22.0f * 0.5f, marker.layoutBounds().y(), "marker bounds y");
        assertClose(72.0f, marker.layoutBounds().width(), "marker width");
        assertClose(22.0f, marker.layoutBounds().height(), "marker height");
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
        MapMarker marker = new MapMarker("Vault");
        MapMarkerHandle<MapMarker> handle = map.addProjectedMarkerWidget("vault", 25.0f, 50.0f, marker)
                .screenSize(74.0f, 22.0f);

        map.measure(new LayoutContext(1200.0f, 700.0f));
        map.arrange(new MutableRect(0.0f, 0.0f, 1200.0f, 700.0f));

        assertClose(250.0f, handle.mapX(), "projected marker map x");
        assertClose(250.0f, handle.mapY(), "projected marker map y");
        assertClose(250.0f, marker.anchor().projectedRootX(), "projected marker root x");
        assertClose(250.0f, marker.anchor().projectedRootY(), "projected marker root y");
    }

    private void markerHandleMovesAnchor() {
        MapCanvas map = new MapCanvas()
                .mapSize(1000.0f, 500.0f)
                .viewport(0.0f, 0.0f, 1.0f);
        MapMarkerHandle<MapMarker> handle = map.addMarker("movable", 10.0f, 20.0f, new MapMarker("Move"))
                .screenSize(40.0f, 20.0f);

        handle.position(30.0f, 45.0f);

        assertClose(30.0f, handle.mapX(), "moved marker x");
        assertClose(45.0f, handle.mapY(), "moved marker y");
        assertTrue(map.marker("movable") == handle, "marker lookup should return handle");
        handle.visible(false);
        map.measure(new LayoutContext(120.0f, 80.0f));
        map.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 80.0f));
        assertTrue(!handle.anchor().arrangedVisible(), "hidden marker anchor should not be arranged visible");
        handle.visible(true);
        assertTrue(map.removeMarker("movable"), "marker should be removable");
        assertTrue(map.marker("movable") == null, "removed marker should leave registry");
    }

    private void markerLookupSkipsDetachedAnchors() {
        MapCanvas map = new MapCanvas();
        MapMarkerHandle<MapMarker> first = map.addMarker("same", 10.0f, 20.0f, new MapMarker("First"));

        assertTrue(map.anchorLayer().remove("same"), "direct anchor removal should work");
        assertTrue(map.marker("same") == null, "detached marker handle should be ignored");

        MapMarkerHandle<MapMarker> second = map.addMarker("same", 30.0f, 40.0f, new MapMarker("Second"));
        assertTrue(first != second, "replacement marker should get a new handle");
        assertTrue(map.marker("same") == second, "lookup should return replacement handle");
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
