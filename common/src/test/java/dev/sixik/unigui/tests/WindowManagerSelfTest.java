package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.ModalOpenedEvent;
import dev.sixik.unigui.api.event.WindowOpenedEvent;
import dev.sixik.unigui.api.event.WindowResizedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.input.TransformHitTester;
import dev.sixik.unigui.widgets.Button;
import dev.sixik.unigui.widgets.Label;
import dev.sixik.unigui.widgets.OverlayLayer;
import dev.sixik.unigui.widgets.PanelWidget;
import dev.sixik.unigui.widgets.WindowWidget;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;

public final class WindowManagerSelfTest {
    public static void main(String[] args) {
        new WindowManagerSelfTest().run();
    }

    private void run() {
        testActivationZOrderMoveClampAndResizeClamp();
        testAlreadyOpenModalRegistrationPublishesModalOpenedAndBlocksBackgroundInput();
        testCloseWhileResizingReleasesCaptureAndEndsResizeLifecycle();
        testWindowEventsCopyCancellationAcrossRoutedSnapshots();
        System.out.println("WindowManagerSelfTest passed");
    }

    private void testActivationZOrderMoveClampAndResizeClamp() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new PanelWidget());
        layer.setUiContextInternal(context);
        TransformHitTester hitTester = new TransformHitTester();

        WindowWidget first = new WindowWidget("First", new Label("First body"))
                .position(12.0f, 8.0f)
                .open();
        first.preferredSize(120.0f, 70.0f).grow(0.0f);
        WindowWidget second = new WindowWidget("Second", new Label("Second body"))
                .position(20.0f, 14.0f)
                .open();
        second.preferredSize(100.0f, 60.0f).grow(0.0f);
        layer.addOverlay(first);
        layer.addOverlay(second);
        layout(layer, 180.0f, 100.0f);

        expect(layer.windows().activeWindow() == second
                        && second.active()
                        && !first.active()
                        && hitTester.hitTest(layer, 26.0f, 20.0f).orElseThrow().widget() == second,
                "Most recently registered open window should be active and topmost in hit-test order");

        layer.windows().activate(first);
        expect(layer.windows().activeWindow() == first
                        && first.active()
                        && !second.active()
                        && hitTester.hitTest(layer, 26.0f, 20.0f).orElseThrow().widget() == first,
                "WindowManager.activate should switch active state and bring the window to front");

        Counter moveEvents = new Counter();
        first.onMoveStarted(event -> moveEvents.started++);
        first.onMoved(event -> moveEvents.moved++);
        first.onMoveEnded(event -> moveEvents.ended++);

        context.routedEvents().dispatch(new PointerPressedEvent(
                first,
                first.layoutBounds().x() + 20.0f,
                first.layoutBounds().y() + 10.0f,
                20.0f,
                10.0f,
                11,
                PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerMovedEvent(first, 500.0f, 500.0f, 500.0f, 500.0f, 11));
        context.routedEvents().dispatch(new PointerReleasedEvent(first, 500.0f, 500.0f, 500.0f, 500.0f, 11, PointerButton.PRIMARY));
        layout(layer, 180.0f, 100.0f);

        expect(moveEvents.started == 1
                        && moveEvents.moved >= 1
                        && moveEvents.ended == 1
                        && context.capturedPointer(11) == null
                        && first.layoutBounds().x() + first.layoutBounds().width() <= 180.01f
                        && first.layoutBounds().y() + first.layoutBounds().height() <= 100.01f,
                "Window drag should publish move lifecycle events and clamp inside host bounds");

        first.position(10.0f, 8.0f).preferredSize(90.0f, 52.0f);
        layout(layer, 180.0f, 100.0f);
        Counter resizeEvents = new Counter();
        first.onResizeStarted(event -> resizeEvents.started++);
        first.onResized(event -> resizeEvents.resized++);
        first.onResizeEnded(event -> resizeEvents.ended++);

        context.routedEvents().dispatch(new PointerPressedEvent(
                first,
                first.layoutBounds().x() + first.layoutBounds().width() - 1.0f,
                first.layoutBounds().y() + first.layoutBounds().height() - 1.0f,
                first.layoutBounds().width() - 1.0f,
                first.layoutBounds().height() - 1.0f,
                12,
                PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerMovedEvent(first, 500.0f, 500.0f, 500.0f, 500.0f, 12));
        context.routedEvents().dispatch(new PointerReleasedEvent(first, 500.0f, 500.0f, 500.0f, 500.0f, 12, PointerButton.PRIMARY));
        layout(layer, 180.0f, 100.0f);

        expect(resizeEvents.started == 1
                        && resizeEvents.resized >= 1
                        && resizeEvents.ended == 1
                        && context.capturedPointer(12) == null
                        && first.layoutBounds().x() + first.layoutBounds().width() <= 180.01f
                        && first.layoutBounds().y() + first.layoutBounds().height() <= 100.01f,
                "Window resize should publish resize lifecycle events and clamp inside host bounds");
    }

    private void testAlreadyOpenModalRegistrationPublishesModalOpenedAndBlocksBackgroundInput() {
        DefaultUIContext context = new DefaultUIContext();
        Button background = new Button("Background");
        background.preferredSize(100.0f, 20.0f).grow(0.0f);
        Counter backgroundClicks = new Counter();
        background.onClick(event -> backgroundClicks.count++);

        OverlayLayer layer = new OverlayLayer(background);
        layer.setUiContextInternal(context);

        WindowWidget modal = new WindowWidget("Late modal", new Label("Modal body"))
                .position(20.0f, 12.0f)
                .modal(true)
                .open();
        modal.preferredSize(110.0f, 64.0f).grow(0.0f);

        Counter modalEvents = new Counter();
        modal.onModalOpened(event -> {
            modalEvents.count++;
            modalEvents.lastStackDepth = event.stackDepth();
        });

        layer.addOverlay(modal);
        layout(layer, 180.0f, 100.0f);

        expect(layer.windows().registered(modal)
                        && layer.windows().topModalWindow() == modal
                        && layer.windows().activeWindow() == modal
                        && modal.active()
                        && modalEvents.count == 1
                        && modalEvents.lastStackDepth == 1,
                "Registering an already-open modal should publish modal-opened and activate it");

        context.routedEvents().dispatch(new PointerPressedEvent(
                background, 4.0f, 4.0f, 4.0f, 4.0f, 0, PointerButton.PRIMARY));
        expect(backgroundClicks.count == 0,
                "Top modal should block pointer input to background content even when registered after open");
    }

    private void testCloseWhileResizingReleasesCaptureAndEndsResizeLifecycle() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new PanelWidget());
        layer.setUiContextInternal(context);

        WindowWidget window = new WindowWidget("Resizable", new Label("Body"))
                .position(12.0f, 10.0f)
                .minWindowSize(80.0f, 48.0f)
                .open();
        window.preferredSize(120.0f, 72.0f).grow(0.0f);
        Counter resizeEvents = new Counter();
        window.onResizeStarted(event -> resizeEvents.started++);
        window.onResizeEnded(event -> {
            resizeEvents.ended++;
            resizeEvents.lastHandle = event.handle();
        });

        layer.addOverlay(window);
        layout(layer, 220.0f, 140.0f);

        float pressX = window.layoutBounds().x() + window.layoutBounds().width() - 1.0f;
        float pressY = window.layoutBounds().y() + window.layoutBounds().height() - 1.0f;
        context.routedEvents().dispatch(new PointerPressedEvent(
                window,
                pressX,
                pressY,
                window.layoutBounds().width() - 1.0f,
                window.layoutBounds().height() - 1.0f,
                7,
                PointerButton.PRIMARY));

        expect(window.resizing()
                        && context.capturedPointer(7) == window
                        && resizeEvents.started == 1,
                "Resize press should enter resizing state and capture its pointer");

        window.close();

        expect(!window.opened(), "Closing during resize should close the window");
        expect(!window.resizing(), "Closing during resize should clear resizing state");
        expect(context.capturedPointer(7) == null, "Closing during resize should release pointer capture");
        expect(resizeEvents.ended == 1,
                "Closing during resize should publish one resize-ended event, got " + resizeEvents.ended);
        expect(resizeEvents.lastHandle.equals("bottom_right"),
                "Closing during resize should preserve completed resize handle, got " + resizeEvents.lastHandle);
    }

    private void testWindowEventsCopyCancellationAcrossRoutedSnapshots() {
        WindowWidget window = new WindowWidget("Events", new Label("Body"));
        PanelWidget currentTarget = new PanelWidget();

        WindowOpenedEvent opened = new WindowOpenedEvent(window);
        opened.cancel();
        expect(opened.routeTo(currentTarget, EventPhase.CAPTURE).isCancelled(),
                "WindowOpenedEvent.routeTo should copy cancellation state");

        WindowResizedEvent resized = new WindowResizedEvent(
                window,
                0.0f, 0.0f, 80.0f, 40.0f,
                2.0f, 3.0f, 100.0f, 60.0f,
                "right");
        resized.cancel();
        expect(resized.routeTo(currentTarget, EventPhase.BUBBLE).isCancelled(),
                "WindowResizedEvent.routeTo should copy cancellation state");

        ModalOpenedEvent modalOpened = new ModalOpenedEvent(window, 2);
        modalOpened.cancel();
        expect(modalOpened.routeTo(currentTarget, EventPhase.TARGET).isCancelled(),
                "ModalOpenedEvent.routeTo should copy cancellation state");
    }

    private static void layout(OverlayLayer layer, float width, float height) {
        layer.measure(new LayoutContext(width, height));
        layer.arrange(new MutableRect(0.0f, 0.0f, width, height));
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Counter {
        private int count;
        private int started;
        private int ended;
        private int moved;
        private int resized;
        private int lastStackDepth;
        private String lastHandle = "";
    }
}
