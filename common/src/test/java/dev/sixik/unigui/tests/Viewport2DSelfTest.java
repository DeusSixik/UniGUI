package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.api.viewport.ViewportChange;
import dev.sixik.unigui.api.viewport.ViewportPoint;

public final class Viewport2DSelfTest {
    private static final float EPSILON = 0.0005f;

    public static void main(String[] args) {
        new Viewport2DSelfTest().run();
        System.out.println("Viewport2DSelfTest passed");
    }

    private void run() {
        projectionRoundTrips();
        zoomRangeClamps();
        zoomAtKeepsPivotWorldPointStable();
        panByUsesScreenDeltas();
        worldBoundsClamp();
        invalidInputsDoNotPoisonState();
        setWithChangeReportsOldAndNewState();
    }

    private void projectionRoundTrips() {
        Viewport2D viewport = new Viewport2D().position(37.0f, -18.0f).zoom(2.5f);
        ViewportPoint screen = viewport.worldToScreen(12.0f, 8.0f);
        ViewportPoint world = viewport.screenToWorld(screen.x(), screen.y());
        assertClose(12.0f, world.x(), "round-trip world x");
        assertClose(8.0f, world.y(), "round-trip world y");
    }

    private void zoomRangeClamps() {
        Viewport2D viewport = new Viewport2D().zoomRange(0.5f, 2.0f);
        viewport.zoom(0.1f);
        assertClose(0.5f, viewport.zoom(), "min zoom clamp");
        viewport.zoom(9.0f);
        assertClose(2.0f, viewport.zoom(), "max zoom clamp");
    }

    private void zoomAtKeepsPivotWorldPointStable() {
        Viewport2D viewport = new Viewport2D().position(20.0f, 30.0f).zoom(1.25f).zoomRange(0.25f, 8.0f);
        float pivotScreenX = 140.0f;
        float pivotScreenY = 90.0f;
        ViewportPoint before = viewport.screenToWorld(pivotScreenX, pivotScreenY);
        viewport.zoomAt(pivotScreenX, pivotScreenY, 1.8f);
        ViewportPoint after = viewport.screenToWorld(pivotScreenX, pivotScreenY);
        assertClose(before.x(), after.x(), "zoomAt pivot world x");
        assertClose(before.y(), after.y(), "zoomAt pivot world y");
    }

    private void panByUsesScreenDeltas() {
        Viewport2D viewport = new Viewport2D().position(10.0f, 20.0f).zoom(2.0f);
        viewport.panBy(5.0f, -7.0f);
        assertClose(15.0f, viewport.x(), "pan x");
        assertClose(13.0f, viewport.y(), "pan y");
    }

    private void worldBoundsClamp() {
        Viewport2D viewport = new Viewport2D()
                .worldBounds(0.0f, 0.0f, 100.0f, 50.0f)
                .clampToWorldBounds(true)
                .position(1000.0f, -1000.0f)
                .zoom(2.0f);
        viewport.clamp(80.0f, 40.0f);
        assertClose(0.0f, viewport.x(), "clamped x max");
        assertClose(-60.0f, viewport.y(), "clamped y min");
    }

    private void invalidInputsDoNotPoisonState() {
        Viewport2D viewport = new Viewport2D().position(Float.NaN, Float.POSITIVE_INFINITY).zoom(Float.NaN);
        assertFinite(viewport.x(), "invalid x sanitized");
        assertFinite(viewport.y(), "invalid y sanitized");
        assertFinite(viewport.zoom(), "invalid zoom sanitized");
        viewport.zoomAt(Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        assertFinite(viewport.x(), "zoomAt invalid x sanitized");
        assertFinite(viewport.y(), "zoomAt invalid y sanitized");
        assertFinite(viewport.zoom(), "zoomAt invalid zoom sanitized");
    }

    private void setWithChangeReportsOldAndNewState() {
        Viewport2D viewport = new Viewport2D().position(1.0f, 2.0f).zoom(1.5f);
        ViewportChange change = viewport.setWithChange(3.0f, 4.0f, 2.0f);
        assertTrue(change.changed(), "change should report changed");
        assertClose(1.0f, change.oldX(), "old x");
        assertClose(2.0f, change.oldY(), "old y");
        assertClose(1.5f, change.oldZoom(), "old zoom");
        assertClose(3.0f, change.newX(), "new x");
        assertClose(4.0f, change.newY(), "new y");
        assertClose(2.0f, change.newZoom(), "new zoom");
    }

    private static void assertClose(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > EPSILON) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertFinite(float value, String message) {
        if (!Float.isFinite(value)) {
            throw new AssertionError(message + ": got " + value);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}