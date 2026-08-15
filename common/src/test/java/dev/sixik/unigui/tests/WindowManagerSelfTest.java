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
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.widgets.display.Text;
import dev.sixik.unigui.widgets.feedback.WindowManager;
import dev.sixik.unigui.widgets.interaction.TextInput;

public final class WindowManagerSelfTest {
    public static void main(String[] args) {
        new WindowManagerSelfTest().run();
    }

    private void run() {
        testActivationZOrderMoveClampAndResizeClamp();
        testWindowBodyDragStartsOnTextButNotInputWidgets();
        testAlreadyOpenModalRegistrationPublishesModalOpenedAndBlocksBackgroundInput();
        testFixedModalBlocksDraggingAndResizing();
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
        first.layout(style -> style.size(120.0f, 70.0f).flexGrow(0).flexShrink(0.0f));
        WindowWidget second = new WindowWidget("Second", new Label("Second body"))
                .position(20.0f, 14.0f)
                .open();
        second.layout(style -> style.size(100.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
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

        first.position(10.0f, 8.0f).layout(style -> style.size(90.0f, 52.0f));
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
        background.layout(style -> style.size(100.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        Counter backgroundClicks = new Counter();
        background.onClick(event -> backgroundClicks.count++);

        OverlayLayer layer = new OverlayLayer(background);
        layer.setUiContextInternal(context);

        WindowWidget modal = new WindowWidget("Late modal", new Label("Modal body"))
                .position(20.0f, 12.0f)
                .modal(true)
                .open();
        modal.layout(style -> style.size(110.0f, 64.0f).flexGrow(0).flexShrink(0.0f));

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

    private void testWindowBodyDragStartsOnTextButNotInputWidgets() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new PanelWidget());
        layer.setUiContextInternal(context);

        Label bodyText = new Label("Plain text body");
        WindowWidget textWindow = new WindowWidget("Text drag", bodyText)
                .position(10.0f, 8.0f)
                .open();
        textWindow.layout(style -> style.size(150.0f, 82.0f).flexGrow(0).flexShrink(0.0f));
        Counter textMove = new Counter();
        textWindow.onMoveStarted(event -> textMove.started++);
        textWindow.onMoveEnded(event -> textMove.ended++);
        layer.addOverlay(textWindow);
        layout(layer, 260.0f, 160.0f);

        float textPressX = bodyText.layoutBounds().x() + 12.0f;
        float textPressY = bodyText.layoutBounds().y() + 8.0f;
        context.routedEvents().dispatch(new PointerPressedEvent(
                bodyText, textPressX, textPressY, 12.0f, 8.0f, 21, PointerButton.PRIMARY));
        expect(textWindow.dragging()
                        && context.capturedPointer(21) == textWindow
                        && textMove.started == 1,
                "Pressing plain text in a window body should start window dragging");
        context.routedEvents().dispatch(new PointerReleasedEvent(
                textWindow, textPressX + 20.0f, textPressY + 4.0f, 20.0f, 4.0f, 21, PointerButton.PRIMARY));
        expect(!textWindow.dragging()
                        && context.capturedPointer(21) == null
                        && textMove.ended == 1,
                "Releasing a body-text drag should stop window dragging and release capture");

        Button button = new Button("Input button");
        WindowWidget buttonWindow = new WindowWidget("Button block", button)
                .position(18.0f, 14.0f)
                .open();
        buttonWindow.layout(style -> style.size(150.0f, 82.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(buttonWindow);
        layout(layer, 260.0f, 160.0f);

        float buttonPressX = button.layoutBounds().x() + 12.0f;
        float buttonPressY = button.layoutBounds().y() + 8.0f;
        context.routedEvents().dispatch(new PointerPressedEvent(
                button, buttonPressX, buttonPressY, 12.0f, 8.0f, 22, PointerButton.PRIMARY));
        expect(!buttonWindow.dragging()
                        && context.capturedPointer(22) == null,
                "Pressing an input button in a window body should not start window dragging");

        TextInput input = new TextInput().text("Editable");
        WindowWidget inputWindow = new WindowWidget("TextInput block", input)
                .position(26.0f, 20.0f)
                .open();
        inputWindow.layout(style -> style.size(160.0f, 86.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(inputWindow);
        layout(layer, 280.0f, 170.0f);

        float inputPressX = input.layoutBounds().x() + 12.0f;
        float inputPressY = input.layoutBounds().y() + 8.0f;
        context.routedEvents().dispatch(new PointerPressedEvent(
                input, inputPressX, inputPressY, 12.0f, 8.0f, 23, PointerButton.PRIMARY));
        expect(!inputWindow.dragging()
                        && context.capturedPointer(23) != inputWindow,
                "Pressing a TextInput in a window body should not start window dragging");
    }

    private void testFixedModalBlocksDraggingAndResizing() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new PanelWidget());
        layer.setUiContextInternal(context);

        WindowWidget movableModal = new WindowWidget("Movable modal", new Label("Body"))
                .position(12.0f, 10.0f)
                .modal(true)
                .open();
        movableModal.layout(style -> style.size(120.0f, 72.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(movableModal);
        layout(layer, 240.0f, 150.0f);

        context.routedEvents().dispatch(new PointerPressedEvent(
                movableModal,
                movableModal.layoutBounds().x() + 16.0f,
                movableModal.layoutBounds().y() + 8.0f,
                16.0f,
                8.0f,
                31,
                PointerButton.PRIMARY));
        expect(movableModal.dragging()
                        && context.capturedPointer(31) == movableModal,
                "Plain modal windows should remain draggable unless fixedModal is enabled");
        context.routedEvents().dispatch(new PointerReleasedEvent(
                movableModal,
                movableModal.layoutBounds().x() + 16.0f,
                movableModal.layoutBounds().y() + 8.0f,
                16.0f,
                8.0f,
                31,
                PointerButton.PRIMARY));

        WindowWidget fixedModal = new WindowWidget("Fixed modal", new Label("Body"))
                .position(24.0f, 18.0f)
                .modal(true)
                .fixedModal(true)
                .open();
        fixedModal.layout(style -> style.size(120.0f, 72.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(fixedModal);
        layout(layer, 240.0f, 150.0f);

        context.routedEvents().dispatch(new PointerPressedEvent(
                fixedModal,
                fixedModal.layoutBounds().x() + 16.0f,
                fixedModal.layoutBounds().y() + 8.0f,
                16.0f,
                8.0f,
                32,
                PointerButton.PRIMARY));
        expect(!fixedModal.dragging()
                        && context.capturedPointer(32) == null,
                "fixedModal(true) should prevent header dragging");

        context.routedEvents().dispatch(new PointerPressedEvent(
                fixedModal,
                fixedModal.layoutBounds().x() + fixedModal.layoutBounds().width() - 1.0f,
                fixedModal.layoutBounds().y() + fixedModal.layoutBounds().height() - 1.0f,
                fixedModal.layoutBounds().width() - 1.0f,
                fixedModal.layoutBounds().height() - 1.0f,
                33,
                PointerButton.PRIMARY));
        expect(!fixedModal.resizing()
                        && fixedModal.resizeHandle().equals("none")
                        && context.capturedPointer(33) == null,
                "fixedModal(true) should prevent resize handles from starting a resize");
    }

    private void testCloseWhileResizingReleasesCaptureAndEndsResizeLifecycle() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new PanelWidget());
        layer.setUiContextInternal(context);

        WindowWidget window = new WindowWidget("Resizable", new Label("Body"))
                .position(12.0f, 10.0f)
                .minWindowSize(80.0f, 48.0f)
                .open();
        window.layout(style -> style.size(120.0f, 72.0f).flexGrow(0).flexShrink(0.0f));
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