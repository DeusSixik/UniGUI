package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.dnd.DragController;
import dev.sixik.unigui.widgets.dnd.DragOutcome;
import dev.sixik.unigui.widgets.dnd.DragSession;
import dev.sixik.unigui.widgets.dnd.DropController;
import dev.sixik.unigui.widgets.dnd.ReorderController;
import dev.sixik.unigui.widgets.editor.DragPayload;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Проверяет композиционный drag-and-drop: жест с порогом, drop-зоны, сортировку с индикатором,
 * overlay-ghost и автоскролл. Используются настоящий layout, hit tester и routed events.
 */
public final class DragReorderSelfTest {
    private static final float EPS = 1.0e-3f;
    private static final String TYPE = "test.item";

    private DragReorderSelfTest() {
    }

    public static void main(String[] args) {
        payloadCarriesData();
        clickBelowThresholdIsNotADrag();
        reorderMovesItemAndShowsIndicator();
        reorderToTheSamePlaceIsNoop();
        dropOutsideZoneReportsNoTarget();
        cancelRestoresSourceAndRemovesOverlay();
        dropControllerFiltersTypesAndReportsLifecycle();
        autoScrollMovesViewportNearEdge();
        System.out.println("DragReorderSelfTest passed");
    }

    private static void payloadCarriesData() {
        Object model = new Object();
        DragPayload payload = DragPayload.of("id", "type", "preview", model);
        expect(payload.data() == model && payload.hasData(), "payload keeps arbitrary data");
        expect(payload.data(Object.class) == model && payload.data(String.class) == null,
                "typed data access is type safe");
        DragPayload legacy = new DragPayload("a", "b", "c");
        expect(legacy.data() == null && !legacy.hasData(), "legacy 3-arg payload has no data");
    }

    private static void clickBelowThresholdIsNotADrag() {
        Scene scene = new Scene(4);
        DragController controller = scene.dragItem(0);

        scene.press(10, 10);
        expect(scene.context.capturedPointer(0) == scene.items.get(0), "press captures the pointer");
        expect(!scene.move(10, 12), "movement below threshold is not consumed");
        expect(!controller.dragging(), "no drag below threshold");
        boolean consumed = scene.release(10, 12);
        expect(!consumed, "click release is not swallowed");
        expect(scene.context.capturedPointer(0) == null, "click releases the capture");
        expect(scene.reorders.isEmpty(), "click does not reorder");
    }

    private static void reorderMovesItemAndShowsIndicator() {
        Scene scene = new Scene(4);
        DragController controller = scene.dragItem(0);
        Widget moved = scene.items.get(0);

        scene.press(10, 10);
        scene.move(10, 30);
        expect(controller.dragging(), "drag starts after the threshold");
        scene.root.applyQueuedMutations();
        expect(scene.root.children().size() == 2, "drag adds the ghost overlay to the OverlayLayer");

        scene.move(10, 65);
        DragSession session = controller.session();
        expect(session != null && session.dropTarget() == scene.list, "list is the active drop target");
        expect(Math.abs(session.deltaY() - 55.0f) < EPS, "session reports pointer delta");

        scene.move(10, 65);
        expect(scene.indicatorVisible(), "indicator is shown while a move is pending");
        expectNear(59.0f, scene.indicatorY(), "indicator sits in the gap between items 2 and 3");
        expectNear(2.0f, scene.indicatorHeight(), "indicator uses the configured thickness");

        scene.release(10, 65);
        expect(scene.outcomes.size() == 1 && scene.outcomes.get(0) == DragOutcome.DROPPED, "drop reports DROPPED");
        expect(scene.reorders.size() == 1 && scene.reorders.get(0)[0] == 0 && scene.reorders.get(0)[1] == 2,
                "reorder reports from=0 to=2");
        expect(scene.list.children().get(2) == moved, "item is moved to the new index");
        scene.root.applyQueuedMutations();
        expect(scene.root.children().size() == 1, "overlay is removed after drop");
        expect(scene.context.capturedPointer(0) == null, "drop releases the capture");

        // Перенос вверх: item 3 -> перед item 1.
        scene.relayout();
        Widget last = scene.list.children().get(3);
        scene.reorders.clear();
        scene.dragItem(3);
        scene.press(10, 70);
        scene.move(10, 50);
        scene.move(10, 22);
        scene.release(10, 22);
        expect(scene.reorders.size() == 1 && scene.reorders.get(0)[0] == 3 && scene.reorders.get(0)[1] == 1,
                "upward move reports from=3 to=1");
        expect(scene.list.children().get(1) == last, "item moved upwards");
    }

    private static void reorderToTheSamePlaceIsNoop() {
        Scene scene = new Scene(4);
        scene.dragItem(1);

        scene.press(10, 30);
        scene.move(10, 36);
        scene.move(10, 25);
        expect(!scene.indicatorVisible(), "no indicator when the item would stay in place");
        scene.release(10, 25);
        expect(scene.reorders.isEmpty(), "dropping in place does not reorder");
        expect(scene.outcomes.get(0) == DragOutcome.DROPPED, "container still accepts the drop");
    }

    private static void dropOutsideZoneReportsNoTarget() {
        Scene scene = new Scene(4);
        scene.dragItem(0);

        scene.press(10, 10);
        scene.move(10, 30);
        scene.move(250, 250);
        expect(scene.controllerSession().dropTarget() == null, "no target outside the list");
        scene.release(250, 250);
        expect(scene.outcomes.get(0) == DragOutcome.NO_TARGET, "drop outside reports NO_TARGET");
        expect(scene.reorders.isEmpty(), "no reorder outside the list");
        expect(scene.list.children().get(0) == scene.items.get(0), "order is unchanged");
    }

    private static void cancelRestoresSourceAndRemovesOverlay() {
        Scene scene = new Scene(4);
        DragController controller = scene.dragItem(0);
        WidgetBase source = (WidgetBase) scene.items.get(0);
        controller.sourceOpacity(0.3f);
        controller.ghost(new Box());

        scene.press(10, 10);
        scene.move(10, 30);
        expect(Math.abs(source.opacity() - 0.3f) < EPS, "source is dimmed while dragging");
        controller.cancel();
        scene.root.applyQueuedMutations();
        expect(Math.abs(source.opacity() - 1.0f) < EPS, "source opacity is restored");
        expect(scene.root.children().size() == 1, "cancel removes the overlay");
        expect(scene.outcomes.size() == 1 && scene.outcomes.get(0) == DragOutcome.CANCELLED, "cancel reports CANCELLED");
        expect(scene.context.capturedPointer(0) == null, "cancel releases the capture");
    }

    private static void dropControllerFiltersTypesAndReportsLifecycle() {
        Scene scene = new Scene(2);
        Box zone = new Box();
        zone.layout(style -> style.size(60.0f, 40.0f));
        scene.rightColumn.addChild(zone);
        scene.relayout();

        AtomicInteger enters = new AtomicInteger();
        AtomicInteger leaves = new AtomicInteger();
        AtomicInteger drops = new AtomicInteger();
        DropController controller = DropController.attach(zone)
                .accepts("other.type")
                .onDragEnter(event -> enters.incrementAndGet())
                .onDragLeave(event -> leaves.incrementAndGet())
                .onDrop(event -> drops.incrementAndGet());
        float zoneX = zone.layoutBounds().x() + 5.0f;
        float zoneY = zone.layoutBounds().y() + 5.0f;

        scene.dragItem(0);
        scene.press(10, 10);
        scene.move(10, 30);
        scene.move(zoneX, zoneY);
        expect(enters.get() == 0 && !controller.hovering(), "zone rejects payloads of another type");
        scene.release(zoneX, zoneY);
        expect(drops.get() == 0, "rejected zone receives no drop");

        controller.accepts(payload -> true);
        controller.accepts("*");
        scene.dragItem(0);
        scene.press(10, 10);
        scene.move(10, 30);
        scene.move(zoneX, zoneY);
        expect(enters.get() == 1 && controller.hovering(), "accepting zone sees one enter");
        scene.move(zoneX + 1.0f, zoneY + 1.0f);
        expect(enters.get() == 1, "moving inside the zone does not re-enter");
        scene.move(10, 30);
        expect(leaves.get() == 1 && !controller.hovering(), "leaving the zone reports leave");
        scene.move(zoneX, zoneY);
        scene.release(zoneX, zoneY);
        expect(drops.get() == 1, "accepting zone receives the drop");
        expect(leaves.get() == 2, "drop clears the hover state through leave");
    }

    private static void autoScrollMovesViewportNearEdge() {
        DefaultUIContext context = new DefaultUIContext();
        VBox list = new VBox();
        List<Widget> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Box item = new Box();
            item.layout(style -> style.height(40.0f));
            list.addChild(item);
            items.add(item);
        }
        ScrollView scroll = new ScrollView(list);
        scroll.layout(style -> style.size(100.0f, 100.0f));
        OverlayLayer root = new OverlayLayer(scroll);
        root.setUiContextInternal(context);
        root.applyQueuedMutations();
        root.measure(new LayoutContext(300.0f, 300.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 300.0f, 300.0f));
        expect(scroll.maxScrollY() > 100.0f, "test setup: scroll view has scrollable content, max=" + scroll.maxScrollY());

        DragController.attach(items.get(0)).payload(DragPayload.of("a", TYPE, "a"));
        Scene.dispatchPress(context, root, 10, 10);
        Scene.dispatchMove(context, root, 10, 30);
        root.applyQueuedMutations();
        expect(scroll.scrollY() == 0.0f, "no scrolling away from the edge");

        Scene.dispatchMove(context, root, 10, 98);
        float before = scroll.scrollY();
        FrameContext frame = new FrameContext(1, 0.1f, 0.0f, FramePhase.ANIMATION);
        for (int i = 0; i < 5; i++) root.tick(frame);
        expect(scroll.scrollY() > before + 100.0f, "pointer at the bottom edge scrolls down, y=" + scroll.scrollY());
        expect(scroll.scrollY() <= scroll.maxScrollY() + EPS, "scroll stays inside the limits");

        Scene.dispatchMove(context, root, 10, 1);
        float scrolledDown = scroll.scrollY();
        for (int i = 0; i < 3; i++) root.tick(frame);
        expect(scroll.scrollY() < scrolledDown, "pointer at the top edge scrolls back up");
        Scene.dispatchRelease(context, root, 10, 1);
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectNear(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > EPS) {
            throw new AssertionError(message + ": expected " + expected + " but was " + actual);
        }
    }

    /** Тестовая сцена: список из N элементов высотой 20 в OverlayLayer и пустая колонка справа. */
    private static final class Scene {
        private final DefaultUIContext context = new DefaultUIContext();
        private final VBox list = new VBox();
        private final VBox rightColumn = new VBox();
        private final List<Widget> items = new ArrayList<>();
        private final List<int[]> reorders = new ArrayList<>();
        private final List<DragOutcome> outcomes = new ArrayList<>();
        private final Box row = new Box();
        private final OverlayLayer root;
        private DragController controller;

        Scene(int count) {
            for (int i = 0; i < count; i++) {
                Box item = new Box();
                item.layout(style -> style.height(20.0f));
                item.id("item" + i);
                list.addChild(item);
                items.add(item);
            }
            list.layout(style -> style.size(100.0f, count * 20.0f));
            rightColumn.layout(style -> style.size(100.0f, 200.0f));
            row.layout(style -> style.size(300.0f, 300.0f));
            row.addChild(list);
            row.addChild(rightColumn);
            root = new OverlayLayer(row);
            root.setUiContextInternal(context);
            root.applyQueuedMutations();
            relayout();
            // Колонка справа: фиксируем позицию вручную, чтобы тест не зависел от flex-правил Box.
            rightColumn.arrange(new MutableRect(150.0f, 0.0f, 100.0f, 200.0f));
            list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, count * 20.0f));
            for (int i = 0; i < count; i++) {
                items.get(i).arrange(new MutableRect(0.0f, i * 20.0f, 100.0f, 20.0f));
            }

            ReorderController.attach(list)
                    .accepts(TYPE)
                    .orientation(Orientation.VERTICAL)
                    .indicator(null, 2.0f, 0.0f)
                    .onReorder((from, to) -> reorders.add(new int[]{from, to}));
        }

        void relayout() {
            root.applyQueuedMutations();
            list.applyQueuedMutations();
            rightColumn.applyQueuedMutations();
            root.measure(new LayoutContext(300.0f, 300.0f));
            root.arrange(new MutableRect(0.0f, 0.0f, 300.0f, 300.0f));
            rightColumn.arrange(new MutableRect(150.0f, 0.0f, 100.0f, 200.0f));
            list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, list.children().size() * 20.0f));
            for (int i = 0; i < list.children().size(); i++) {
                list.children().get(i).arrange(new MutableRect(0.0f, i * 20.0f, 100.0f, 20.0f));
            }
            for (Widget child : rightColumn.children()) {
                child.arrange(new MutableRect(150.0f, 0.0f, 60.0f, 40.0f));
            }
        }

        DragController dragItem(int index) {
            Widget item = list.children().get(index);
            controller = DragController.attach(item)
                    .payload(DragPayload.of(item.id(), TYPE, item.id(), item))
                    .threshold(4.0f)
                    .onDragEnd(end -> outcomes.add(end.outcome()));
            return controller;
        }

        DragSession controllerSession() {
            return controller.session();
        }

        boolean indicatorVisible() {
            DragSession session = controller.session();
            return session != null && session.indicatorVisible();
        }

        float indicatorY() {
            return controller.session().indicatorY();
        }

        float indicatorHeight() {
            return controller.session().indicatorHeight();
        }

        void press(float x, float y) {
            dispatchPress(context, root, x, y);
        }

        boolean move(float x, float y) {
            return dispatchMove(context, root, x, y);
        }

        boolean release(float x, float y) {
            return dispatchRelease(context, root, x, y);
        }

        static void dispatchPress(DefaultUIContext context, Widget root, float x, float y) {
            HitTestResult hit = context.hitTester().hitTest(root, x, y).orElseThrow();
            context.routedEvents().dispatch(new PointerPressedEvent(
                    hit.widget(), x, y, hit.localX(), hit.localY(), 0, PointerButton.PRIMARY));
        }

        static boolean dispatchMove(DefaultUIContext context, Widget root, float x, float y) {
            Widget captured = context.capturedPointer(0);
            if (captured != null) {
                return context.routedEvents().dispatch(new PointerMovedEvent(captured, x, y, 0.0f, 0.0f, 0));
            }
            HitTestResult hit = context.hitTester().hitTest(root, x, y).orElse(null);
            return hit != null && context.routedEvents().dispatch(
                    new PointerMovedEvent(hit.widget(), x, y, hit.localX(), hit.localY(), 0));
        }

        static boolean dispatchRelease(DefaultUIContext context, Widget root, float x, float y) {
            Widget captured = context.capturedPointer(0);
            boolean consumed;
            if (captured != null) {
                consumed = context.routedEvents().dispatch(new PointerReleasedEvent(
                        captured, x, y, 0.0f, 0.0f, 0, PointerButton.PRIMARY));
                context.clearPointerCapture(0);
                return consumed;
            }
            HitTestResult hit = context.hitTester().hitTest(root, x, y).orElse(null);
            return hit != null && context.routedEvents().dispatch(new PointerReleasedEvent(
                    hit.widget(), x, y, hit.localX(), hit.localY(), 0, PointerButton.PRIMARY));
        }
    }
}
