package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.ViewportChangedEvent;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.world.AnchorWidget;
import dev.sixik.unigui.widgets.world.WorldCanvas;

public final class WorldCanvasSelfTest {
    private static final float EPSILON = 0.0005f;

    public static void main(String[] args) {
        new WorldCanvasSelfTest().run();
        System.out.println("WorldCanvasSelfTest passed");
    }

    private void run() {
        projectionUsesCanvasOriginAndViewportOffset();
        anchorsArrangeAsFixedSizeScreenWidgets();
        anchorCullingSetsZeroBoundsOffscreen();
        zoomScalingClampsToMinMaxSize();
        steadyStateMeasureSkipsCleanAnchors();
        tickAndRenderSkipCulledAnchors();
        scrollZoomDispatchesViewportChangedEvent();
        worldLayerRendersWithProjectionHelpers();
    }

    private void projectionUsesCanvasOriginAndViewportOffset() {
        WorldCanvas canvas = new WorldCanvas().viewport(20.0f, 30.0f, 2.0f);
        canvas.measure(new LayoutContext(400.0f, 300.0f));
        canvas.arrange(new MutableRect(10.0f, 15.0f, 400.0f, 300.0f));

        assertClose(50.0f, canvas.worldToRootX(10.0f), "world->root x");
        assertClose(65.0f, canvas.worldToRootY(10.0f), "world->root y");
        assertClose(10.0f, canvas.rootToWorldX(50.0f), "root->world x");
        assertClose(10.0f, canvas.rootToWorldY(65.0f), "root->world y");
    }

    private void anchorsArrangeAsFixedSizeScreenWidgets() {
        WorldCanvas canvas = new WorldCanvas().viewport(5.0f, 6.0f, 2.0f);
        TestWidget marker = new TestWidget(12.0f, 8.0f);
        AnchorWidget anchor = canvas.anchorLayer().add("camp", 10.0f, 20.0f, marker)
                .screenSize(40.0f, 20.0f)
                .pivot(0.5f, 1.0f);

        canvas.measure(new LayoutContext(300.0f, 200.0f));
        canvas.arrange(new MutableRect(100.0f, 50.0f, 300.0f, 200.0f));

        assertTrue(anchor.arrangedVisible(), "anchor should be visible");
        assertClose(125.0f, anchor.projectedRootX(), "projected root x");
        assertClose(96.0f, anchor.projectedRootY(), "projected root y");
        assertClose(105.0f, marker.layoutBounds().x(), "marker x");
        assertClose(76.0f, marker.layoutBounds().y(), "marker y");
        assertClose(40.0f, marker.layoutBounds().width(), "marker width");
        assertClose(20.0f, marker.layoutBounds().height(), "marker height");
    }

    private void anchorCullingSetsZeroBoundsOffscreen() {
        WorldCanvas canvas = new WorldCanvas().viewport(0.0f, 0.0f, 1.0f);
        TestWidget marker = new TestWidget(10.0f, 10.0f);
        AnchorWidget anchor = canvas.anchorLayer().add("far", 1000.0f, 1000.0f, marker)
                .screenSize(10.0f, 10.0f)
                .cullOutsideViewport(true);

        canvas.measure(new LayoutContext(100.0f, 100.0f));
        canvas.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        assertTrue(!anchor.arrangedVisible(), "offscreen anchor should be culled");
        assertClose(0.0f, marker.layoutBounds().width(), "culled marker width");
        assertClose(0.0f, marker.layoutBounds().height(), "culled marker height");
    }

    private void zoomScalingClampsToMinMaxSize() {
        // base 40x20 при referenceZoom 1: zoom 2 -> 80x40, в клампе [20x10, 60x30] -> 60x30.
        WorldCanvas canvas = new WorldCanvas().viewport(0.0f, 0.0f, 2.0f);
        TestWidget marker = new TestWidget(12.0f, 8.0f);
        AnchorWidget anchor = canvas.anchorLayer().add("poi", 50.0f, 50.0f, marker)
                .screenSize(40.0f, 20.0f)
                .pivot(0.5f, 0.5f)
                .minScreenSize(20.0f, 10.0f)
                .maxScreenSize(60.0f, 30.0f)
                .zoomScaling(true, 1.0f);

        canvas.measure(new LayoutContext(400.0f, 300.0f));
        canvas.arrange(new MutableRect(0.0f, 0.0f, 400.0f, 300.0f));

        assertTrue(anchor.arrangedVisible(), "scaled anchor should be visible");
        assertClose(60.0f, marker.layoutBounds().width(), "scaled width clamped to max");
        assertClose(30.0f, marker.layoutBounds().height(), "scaled height clamped to max");
        // pivot применяется к зажатому размеру: центр в projected (100, 100).
        assertClose(100.0f - 30.0f, marker.layoutBounds().x(), "scaled marker x");
        assertClose(100.0f - 15.0f, marker.layoutBounds().y(), "scaled marker y");

        // zoom 0.25 -> 10x5, ниже пола -> 20x10.
        canvas.viewport(0.0f, 0.0f, 0.25f);
        canvas.measure(new LayoutContext(400.0f, 300.0f));
        canvas.arrange(new MutableRect(0.0f, 0.0f, 400.0f, 300.0f));

        assertClose(20.0f, marker.layoutBounds().width(), "scaled width clamped to min");
        assertClose(10.0f, marker.layoutBounds().height(), "scaled height clamped to min");

        // Без zoom-scaling min/max — жёсткие границы базового размера:
        // база 40x20 с полом 100x100 поднимается до 100x100.
        AnchorWidget bounded = canvas.anchorLayer().add("bounded", 50.0f, 50.0f, new TestWidget(12.0f, 8.0f))
                .screenSize(40.0f, 20.0f)
                .minScreenSize(100.0f, 100.0f);
        canvas.measure(new LayoutContext(400.0f, 300.0f));
        canvas.arrange(new MutableRect(0.0f, 0.0f, 400.0f, 300.0f));
        assertClose(100.0f, bounded.widget().layoutBounds().width(), "min bound applies without zoom scaling");
        assertClose(100.0f, bounded.widget().layoutBounds().height(), "min bound applies without zoom scaling");
    }

    private void steadyStateMeasureSkipsCleanAnchors() {
        WorldCanvas canvas = new WorldCanvas().viewport(0.0f, 0.0f, 1.0f);
        CountingWidget near = new CountingWidget(10.0f, 10.0f);
        CountingWidget far = new CountingWidget(10.0f, 10.0f);
        canvas.anchorLayer().add("near", 10.0f, 10.0f, near).screenSize(10.0f, 10.0f);
        canvas.anchorLayer().add("far", 1000.0f, 1000.0f, far).screenSize(10.0f, 10.0f);

        canvas.measure(new LayoutContext(100.0f, 100.0f));
        canvas.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));
        canvas.clearInvalidation(dev.sixik.unigui.api.core.InvalidationFlags.ALL);
        near.clearInvalidation(dev.sixik.unigui.api.core.InvalidationFlags.ALL);
        far.clearInvalidation(dev.sixik.unigui.api.core.InvalidationFlags.ALL);
        near.measures = 0;
        far.measures = 0;

        canvas.measure(new LayoutContext(100.0f, 100.0f));

        assertTrue(near.measures == 0 && far.measures == 0,
                "steady-state measure should skip anchors with known clean sizes, got near="
                        + near.measures + " far=" + far.measures);
    }

    private void tickAndRenderSkipCulledAnchors() {
        WorldCanvas canvas = new WorldCanvas().viewport(0.0f, 0.0f, 1.0f);
        CountingWidget near = new CountingWidget(10.0f, 10.0f);
        CountingWidget far = new CountingWidget(10.0f, 10.0f);
        canvas.anchorLayer().add("near", 10.0f, 10.0f, near).screenSize(10.0f, 10.0f);
        canvas.anchorLayer().add("far", 1000.0f, 1000.0f, far).screenSize(10.0f, 10.0f);

        canvas.measure(new LayoutContext(100.0f, 100.0f));
        canvas.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        near.ticks = 0;
        far.ticks = 0;
        near.renders = 0;
        far.renders = 0;
        canvas.tick(new dev.sixik.unigui.api.core.FrameContext(1L, 1.0f / 60.0f, 0.0f,
                dev.sixik.unigui.api.core.FramePhase.ANIMATION));
        canvas.render(new NoopRenderContext());

        assertTrue(near.ticks == 1 && far.ticks == 0,
                "tick should run only for on-screen anchors, got near=" + near.ticks + " far=" + far.ticks);
        assertTrue(near.renders == 1 && far.renders == 0,
                "render should draw only on-screen anchors, got near=" + near.renders + " far=" + far.renders);
    }

    private void scrollZoomDispatchesViewportChangedEvent() {
        WorldCanvas canvas = new WorldCanvas().viewport(0.0f, 0.0f, 1.0f);
        ChangeCapture capture = new ChangeCapture();
        canvas.onViewportChanged(capture::capture);
        canvas.measure(new LayoutContext(200.0f, 160.0f));
        canvas.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 160.0f));

        ScrollEvent scroll = new ScrollEvent(canvas, 100.0f, 80.0f, 100.0f, 80.0f,
                0.0f, 1.0f, KeyModifiers.CONTROL);
        canvas.handle(scroll);

        assertTrue(scroll.isCancelled(), "ctrl wheel should be consumed");
        assertTrue(capture.called, "viewport changed event should fire");
        assertTrue(canvas.viewport().zoom() > 1.0f, "zoom should increase");
        assertClose(1.0f, capture.oldZoom, "old zoom");
        assertClose(canvas.viewport().zoom(), capture.newZoom, "new zoom");
    }

    private void worldLayerRendersWithProjectionHelpers() {
        WorldCanvas canvas = new WorldCanvas().viewport(2.0f, 3.0f, 4.0f);
        final float[] projected = new float[2];
        canvas.addWorldLayer((WorldCanvas c, DrawScope draw) -> {
            projected[0] = c.worldToRootX(5.0f);
            projected[1] = c.worldToRootY(6.0f);
        });

        canvas.measure(new LayoutContext(100.0f, 100.0f));
        canvas.arrange(new MutableRect(10.0f, 20.0f, 100.0f, 100.0f));
        canvas.render(new NoopRenderContext());

        assertClose(32.0f, projected[0], "layer projected x");
        assertClose(47.0f, projected[1], "layer projected y");
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

    private static final class ChangeCapture {
        private boolean called;
        private float oldZoom;
        private float newZoom;

        private void capture(ViewportChangedEvent event) {
            called = true;
            oldZoom = event.oldZoom();
            newZoom = event.newZoom();
        }
    }

    private static final class TestWidget extends WidgetBase {
        private final float preferredWidth;
        private final float preferredHeight;
        private int ticks;

        private TestWidget(float preferredWidth, float preferredHeight) {
            this.preferredWidth = preferredWidth;
            this.preferredHeight = preferredHeight;
        }

        @Override
        public void measure(LayoutContext context) {
            if (visibility() == Visibility.COLLAPSED) {
                setDesiredSize(LayoutSize.ZERO);
                return;
            }
            setDesiredSize(preferredWidth, preferredHeight);
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            ticks++;
        }
    }

    private static final class CountingWidget extends WidgetBase {
        private final float preferredWidth;
        private final float preferredHeight;
        private int measures;
        private int ticks;
        private int renders;

        private CountingWidget(float preferredWidth, float preferredHeight) {
            this.preferredWidth = preferredWidth;
            this.preferredHeight = preferredHeight;
        }

        @Override
        public void measure(LayoutContext context) {
            measures++;
            if (visibility() == Visibility.COLLAPSED) {
                setDesiredSize(LayoutSize.ZERO);
                return;
            }
            setDesiredSize(preferredWidth, preferredHeight);
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            ticks++;
        }

        @Override
        public void render(dev.sixik.unigui.api.render.RenderContext context) {
            renders++;
        }
    }
}
