package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.WindowMovedEvent;
import dev.sixik.unigui.api.event.WindowMoveEndedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockDropIntent;
import dev.sixik.unigui.widgets.docking.DockNode;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;

public final class DockingInteractionSelfTest {
    public static void main(String[] args) {
        new DockingInteractionSelfTest().run();
    }

    private void run() {
        testDragThresholdAndClickWithoutDrag();
        testTabStripDropKeepsTabbedWithoutTearOff();
        testLiveTearOffHandsOffToWindowOnStripExit();
        testReorderTabsByDraggingWithinStrip();
        testWindowDropOnWestDiamondSplitsLeaf();
        testCrossTriggersAndGapsViaWindow();
        testReleasedWindowOutsideTriggersStaysFloating();
        testFloatingWindowRemembersSizeAcrossRedock();
        testDropZonesRenderDuringDrag();
        testTearOffFloatsPaneDroppedOutsideZones();
        testFloatingDropDetachesPaneAndClearsPreview();
        testDropOutsideHostTearsOffWithoutPreview();
        testFloatingDropInsideOverlayCreatesNonCloseableRedockableWindow();
        testDockableWindowDragDocksIntoRoot();
        testWindowDockingAreaHostsAndMerges();
        testOverflowTabsScrollAndMenuSelection();
        testDockPaneContentIsClippedToLeafContentBounds();
        testTinySplitLayoutDoesNotProduceNegativeBounds();
        System.out.println("DockingInteractionSelfTest passed");
    }

    private void testDragThresholdAndClickWithoutDrag() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        Counter drag = new Counter();
        root.onDragStarted(event -> drag.started++);
        root.onDragEnded(event -> drag.ended++);
        root.onDropPreviewChanged(event -> drag.previewChanges++);

        root.addDocument("scene", "Scene", content("Scene body"));
        root.tabPane("scene", new DockPane("inspector", "Inspector", content("Inspector body")));
        root.selectPane("scene");
        layout(root, 320.0f, 160.0f);

        press(context, root, 8.0f, 6.0f, 0);
        expect(root.dragController().active()
                        && !root.dockDragging()
                        && context.capturedPointer(0) == root,
                "Dock tab press should arm dock dragging and capture the pointer");

        move(context, root, 10.0f, 7.0f, 0);
        expect(!root.dockDragging()
                        && !root.dockDropPreview().valid()
                        && drag.started == 0,
                "Dock drag should not start before crossing the threshold");

        release(context, root, 10.0f, 7.0f, 0);
        expect(!root.dragController().active()
                        && drag.ended == 0
                        && root.manager().containsPane("scene")
                        && root.manager().containsPane("inspector")
                        && root.lastFloatingWindow() == null
                        && context.capturedPointer(0) == null,
                "Releasing before the threshold should be a click without drag events or tear-off");
    }

    private void testTabStripDropKeepsTabbedWithoutTearOff() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        Counter drag = new Counter();
        root.onDragStarted(event -> drag.started++);
        root.onDragEnded(event -> {
            drag.ended++;
            drag.dropped = event.dropped();
        });
        root.addDocument("scene", "Scene", content("Scene body"));
        root.tabPane("scene", new DockPane("inspector", "Inspector", content("Inspector body")));
        root.selectPane("scene");
        layout(root, 320.0f, 160.0f);

        press(context, root, 8.0f, 6.0f, 0);
        // Пустая часть стрипа (правее табов): не таб, не сосед — reorder
        // молчит, tear-off не стреляет, intent — таб в домашний лист.
        move(context, root, 300.0f, 11.0f, 0);
        DockDropIntent stripPreview = root.dockDropPreview();
        expect(root.dockDragging()
                        && stripPreview.valid()
                        && stripPreview.area() == DockArea.CENTER
                        && drag.started == 1
                        && root.manager().containsPane("scene")
                        && root.lastFloatingWindow() == null,
                "Hovering the empty tab strip should preview a tab drop without detaching the pane");

        release(context, root, 300.0f, 11.0f, 0);
        expect(!root.dragController().active()
                        && drag.ended == 1
                        && !drag.dropped
                        && tabbedTogether(root, "inspector", "scene")
                        && root.lastFloatingWindow() == null,
                "Dropping on the tab strip should keep the tabbed panes instead of floating a window");
    }

    private void testLiveTearOffHandsOffToWindowOnStripExit() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        Counter drag = new Counter();
        root.onDragStarted(event -> drag.started++);
        root.onDragMoved(event -> drag.moved++);
        root.onDragEnded(event -> {
            drag.ended++;
            drag.dropped = event.dropped();
        });
        root.addDocument("scene", "Scene", content("Scene body"));
        root.tabPane("scene", new DockPane("inspector", "Inspector", content("Inspector body")));
        root.selectPane("scene");
        OverlayLayer layer = overlayFixture(context, root, 320.0f, 160.0f, 640.0f, 400.0f);

        press(context, root, 8.0f, 6.0f, 0);
        // Выход из стрипа вниз (центр листа): таб отрывается сразу, не на release.
        move(context, root, 160.0f, 80.0f, 0);
        expect(drag.started == 1 && drag.moved >= 1,
                "Tab drag should start once the threshold is crossed");
        expect(!root.manager().containsPane("scene")
                        && root.manager().containsPane("inspector"),
                "Strip exit should detach the dragged pane immediately");

        WindowWidget window = root.lastFloatingWindow();
        expect(window != null
                        && window.opened()
                        && window.dragging()
                        && window.content() != null
                        && context.capturedPointer(0) == window,
                "Tear-off should open a dragging window under the cursor and hand the capture over");
        expect(!root.dragController().active()
                        && !root.dockDropPreview().valid()
                        && drag.ended == 1
                        && drag.dropped,
                "Handoff should close the tab-drag phase as a successful tear-off");
        layer.applyQueuedMutations();
        expect(window.parent() == layer,
                "Tear-off window should live in the overlay layer");

        // Жест продолжается окном: движение едет в window, не в root.
        // (183,80) — зазор между центральной и восточной ячейками: не триггер.
        move(context, window, 183.0f, 80.0f, 0);
        expect(window.lastDragRootX() == 183.0f && window.lastDragRootY() == 80.0f,
                "Pointer moves after handoff should drive the floating window");
        release(context, window, 183.0f, 80.0f, 0);
        expect(!window.dragging()
                        && window.opened()
                        && !root.manager().containsPane("scene"),
                "Releasing the torn-off window outside triggers should keep it floating");
    }

    private void testReorderTabsByDraggingWithinStrip() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("doc0", "Doc 0", content("Doc 0 body"));
        root.addDocument("doc1", "Doc 1", content("Doc 1 body"));
        root.addDocument("doc2", "Doc 2", content("Doc 2 body"));
        root.selectPane("doc0");
        layout(root, 320.0f, 160.0f);
        expect(tabOrder(root).equals(java.util.List.of("doc0", "doc1", "doc2")),
                "Fixture should tab three documents in insertion order");

        press(context, root, 8.0f, 6.0f, 0);
        // Таб doc2 (x ~213..320): перетаскиваемый doc0 встаёт на его место.
        move(context, root, 250.0f, 11.0f, 0);
        expect(tabOrder(root).equals(java.util.List.of("doc1", "doc2", "doc0"))
                        && root.manager().selectedPane().id().equals("doc0")
                        && root.dockDragging()
                        && root.lastFloatingWindow() == null,
                "Hovering another tab of the same leaf should reorder without tearing off");

        // Таб doc1 (x ~0..106 в новом порядке): возврат обратно.
        move(context, root, 50.0f, 11.0f, 0);
        expect(tabOrder(root).equals(java.util.List.of("doc0", "doc1", "doc2")),
                "Hovering the first tab should move the dragged tab back to the front");

        release(context, root, 50.0f, 11.0f, 0);
        expect(!root.dragController().active()
                        && root.manager().paneCount() == 3
                        && root.lastFloatingWindow() == null,
                "Releasing after reorder should keep all tabs docked");
    }

    private void testWindowDropOnWestDiamondSplitsLeaf() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        Counter drag = new Counter();
        root.onDragStarted(event -> drag.started++);
        root.onDragEnded(event -> {
            drag.ended++;
            drag.dropped = event.dropped();
        });
        root.addDocument("scene", "Scene", content("Scene body"));
        root.tabPane("scene", new DockPane("inspector", "Inspector", content("Inspector body")));
        root.selectPane("scene");
        OverlayLayer layer = overlayFixture(context, root, 320.0f, 160.0f, 640.0f, 400.0f);

        press(context, root, 8.0f, 6.0f, 0);
        float[] west = diamondCenter(root, DockArea.LEFT);
        move(context, root, west[0], west[1], 0);
        expect(drag.started == 1 && drag.ended == 1 && drag.dropped
                        && !root.manager().containsPane("scene"),
                "Moving onto the west diamond should tear the tab off first (live tear-off)");

        // Дальше жест идёт окном: наведение на тот же диамант даёт LEFT-оффер.
        WindowWidget window = root.lastFloatingWindow();
        move(context, window, west[0], west[1], 0);
        DrawList previewDrawList = renderRoot(root);
        expect(hasFloatingPreview(previewDrawList)
                        && nearRect(dropFillRect(previewDrawList), 0.0f, 0.0f, 320.0f * 0.33f, 160.0f),
                "Hovering the torn-off window over the west diamond should preview a left third");

        release(context, window, west[0], west[1], 0);
        layer.applyQueuedMutations();
        expect(!root.dragController().active()
                        && root.rootNode().isSplit()
                        && root.rootNode().first().findPane("scene") != null
                        && root.rootNode().second().findPane("inspector") != null
                        && !window.opened()
                        && window.content() == null
                        && root.lastFloatingWindow() == null,
                "Dropping the window on the west diamond should split the leaf and consume the window");
    }

    private void testCrossTriggersAndGapsViaWindow() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("scene", "Scene", content("Scene body"));
        root.tabPane("scene", new DockPane("inspector", "Inspector", content("Inspector body")));
        root.selectPane("scene");
        overlayFixture(context, root, 320.0f, 160.0f, 640.0f, 400.0f);

        press(context, root, 8.0f, 6.0f, 0);
        move(context, root, 500.0f, 300.0f, 0);
        WindowWidget window = root.lastFloatingWindow();
        expect(window != null && !root.manager().containsPane("scene"),
                "Tear-off fixture should detach the pane into a floating window");

        float[] center = diamondCenter(root, DockArea.CENTER);
        move(context, window, center[0], center[1], 0);
        expect(nearRect(dropFillRect(renderRoot(root)), 3.0f, 3.0f, 314.0f, 154.0f),
                "Hovering the center diamond should preview a tab drop over the whole leaf");

        float[] east = diamondCenter(root, DockArea.RIGHT);
        move(context, window, east[0], east[1], 0);
        expect(nearRect(dropFillRect(renderRoot(root)), 320.0f - 320.0f * 0.33f, 0.0f,
                        320.0f * 0.33f, 160.0f),
                "Hovering the east diamond should preview a right third");

        // Зазор между диамантами — не триггер: оффера нет.
        float gapX = (center[0] + east[0]) * 0.5f;
        move(context, window, gapX, center[1], 0);
        expect(dropFillRect(renderRoot(root)) == null,
                "Gaps between trigger diamonds should offer no drop zone");

        release(context, window, east[0], east[1], 0);
        expect(root.rootNode().isSplit()
                        && root.rootNode().first().findPane("inspector") != null
                        && root.rootNode().second().findPane("scene") != null
                        && !window.opened(),
                "Dropping the window on the east diamond should split the leaf with the pane last");
    }

    private void testReleasedWindowOutsideTriggersStaysFloating() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("scene", "Scene", content("Scene body"));
        root.tabPane("scene", new DockPane("inspector", "Inspector", content("Inspector body")));
        root.selectPane("scene");
        overlayFixture(context, root, 320.0f, 160.0f, 640.0f, 400.0f);

        press(context, root, 8.0f, 6.0f, 0);
        move(context, root, 500.0f, 300.0f, 0);
        WindowWidget window = root.lastFloatingWindow();
        expect(window != null && window.opened(), "Tear-off fixture should float the pane");

        // Релиз мимо креста/стрипа (зазор диамантов): окно остаётся плавать,
        // панель НЕ прибивается табом в чужой лист.
        float[] center = diamondCenter(root, DockArea.CENTER);
        float[] east = diamondCenter(root, DockArea.RIGHT);
        float gapX = (center[0] + east[0]) * 0.5f;
        move(context, window, gapX, center[1], 0);
        expect(dropFillRect(renderRoot(root)) == null,
                "Gap hover should show no drop preview");
        release(context, window, gapX, center[1], 0);
        expect(!root.manager().containsPane("scene")
                        && root.manager().containsPane("inspector")
                        && root.rootNode().isLeaf()
                        && window.opened()
                        && window.content() != null
                        && root.lastFloatingWindow() == window,
                "Releasing a floating window outside triggers should keep it floating instead of tabbing");
    }

    private void testFloatingWindowRemembersSizeAcrossRedock() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("scene", "Scene", content("Scene body"));
        root.tabPane("scene", new DockPane("inspector", "Inspector", content("Inspector body")));
        root.selectPane("scene");
        OverlayLayer layer = overlayFixture(context, root, 320.0f, 160.0f, 640.0f, 400.0f);

        press(context, root, 8.0f, 6.0f, 0);
        move(context, root, 500.0f, 300.0f, 0);
        WindowWidget first = root.lastFloatingWindow();
        expect(first != null
                        && almostEqual(first.layoutBounds().width(), 320.0f)
                        && almostEqual(first.layoutBounds().height(), 160.0f),
                "First tear-off without a remembered size should use the source leaf size");

        // Пользователь меняет размер плавающего окна, затем докирует его.
        first.layout(style -> style.size(260.0f, 140.0f));
        layout(layer, 640.0f, 400.0f);
        expect(almostEqual(first.layoutBounds().width(), 260.0f)
                        && almostEqual(first.layoutBounds().height(), 140.0f),
                "Resizing the floating window should stick");
        float[] center = diamondCenter(root, DockArea.CENTER);
        move(context, first, center[0], center[1], 0);
        release(context, first, center[0], center[1], 0);
        layer.applyQueuedMutations();
        expect(root.manager().containsPane("scene")
                        && !first.opened(),
                "Docking the resized window should consume it back into the tree");

        // Повторный отрыв возвращает прошлый размер окна, а не размер листа.
        press(context, root, 140.0f, 6.0f, 0);
        move(context, root, 500.0f, 300.0f, 0);
        WindowWidget second = root.lastFloatingWindow();
        expect(second != null
                        && second != first
                        && almostEqual(second.layoutBounds().width(), 260.0f)
                        && almostEqual(second.layoutBounds().height(), 140.0f),
                "Second tear-off should restore the window size from before docking");
        release(context, second, 500.0f, 300.0f, 0);
        expect(second.opened() && !root.manager().containsPane("scene"),
                "Releasing the restored window outside triggers should keep it floating");
    }

    private static boolean tabbedTogether(DockingRoot root, String firstId, String secondId) {
        return leafWith(root.rootNode(), firstId, secondId);
    }

    private static boolean leafWith(dev.sixik.unigui.widgets.docking.DockNode node, String firstId, String secondId) {
        if (node == null) return false;
        if (node.isLeaf()) {
            boolean first = false;
            boolean second = false;
            for (DockPane pane : node.panes()) {
                if (pane.id().equals(firstId)) first = true;
                if (pane.id().equals(secondId)) second = true;
            }
            return first && second;
        }
        return leafWith(node.first(), firstId, secondId) || leafWith(node.second(), firstId, secondId);
    }

    private void testDropZonesRenderDuringDrag() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("scene", "Scene", content("Scene body"))
                .addToolPane("inspector", "Inspector", content("Inspector body"), DockArea.RIGHT)
                .selectPane("scene");
        layout(root, 320.0f, 160.0f);

        DrawList calm = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(calm));
        long calmZones = countCommands(calm, DrawCommandType.ROUNDED_RECT);

        // Пустая часть стрипа правого листа: драг жив, tear-off молчит,
        // крест якорится на лист под курсором.
        press(context, root, 8.0f, 6.0f, 0);
        move(context, root, 300.0f, 11.0f, 0);
        expect(root.dockDragging() && root.dockDropPreview().valid(),
                "Strip hover should keep the tab drag alive with a valid preview");
        DrawList dragging = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(dragging));
        // Крест (5 ячеек × fill+stroke) + плашка таба + превью:
        // зон заметно больше, чем в покое. Краевых полос больше нет.
        expect(countCommands(dragging, DrawCommandType.ROUNDED_RECT) >= calmZones + 10,
                "Dragging should render the drop-zone cross anchored on the hovered leaf");

        release(context, root, 300.0f, 11.0f, 0);
        DrawList settled = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(settled));
        expect(!root.dockDragging()
                        && !root.dockDropPreview().valid()
                        && countCommands(settled, DrawCommandType.ROUNDED_RECT)
                                < countCommands(dragging, DrawCommandType.ROUNDED_RECT),
                "Drop zones should disappear after the drop settles");
    }

    private void testTearOffFloatsPaneDroppedOutsideZones() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("scene", "Scene", content("Scene body"))
                .addToolPane("inspector", "Inspector", content("Inspector body"), DockArea.RIGHT)
                .selectPane("scene");
        layout(root, 320.0f, 160.0f);

        // Вынос за пределы root мимо всех зон: панель становится окном, а не возвращается.
        // Live tear-off срабатывает уже на move (выход из стрипа), release лишь завершает жест окна.
        context.routedEvents().dispatch(new PointerPressedEvent(
                root, 8.0f, 6.0f, 8.0f, 6.0f, 0, PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerMovedEvent(root, 400.0f, 100.0f, 400.0f, 100.0f, 0));
        expect(!root.dockDropPreview().valid(), "Drag outside the host should expose no dock preview");
        expect(!root.manager().containsPane("scene")
                        && root.lastFloatingWindow() != null
                        && root.lastFloatingWindow().dragging()
                        && context.capturedPointer(0) == root.lastFloatingWindow(),
                "Strip exit should hand the gesture off to a dragging floating window immediately");
        context.routedEvents().dispatch(new PointerReleasedEvent(
                root, 400.0f, 100.0f, 400.0f, 100.0f, 0, PointerButton.PRIMARY));
        expect(!root.manager().containsPane("scene")
                        && root.manager().containsPane("inspector")
                        && root.lastFloatingWindow() != null,
                "Dropping outside all zones should tear the pane off into a floating window");

        // Opt-out: тот же жест — snap-back, панель на месте.
        DockingRoot strict = new DockingRoot();
        strict.setUiContextInternal(context);
        strict.tearOffOnDragOut(false);
        strict.addDocument("scene", "Scene", content("Scene body"))
                .addToolPane("inspector", "Inspector", content("Inspector body"), DockArea.RIGHT)
                .selectPane("scene");
        layout(strict, 320.0f, 160.0f);
        context.routedEvents().dispatch(new PointerPressedEvent(
                strict, 8.0f, 6.0f, 8.0f, 6.0f, 0, PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerMovedEvent(strict, 400.0f, 100.0f, 400.0f, 100.0f, 0));
        context.routedEvents().dispatch(new PointerReleasedEvent(
                strict, 400.0f, 100.0f, 400.0f, 100.0f, 0, PointerButton.PRIMARY));
        expect(strict.manager().containsPane("scene") && strict.lastFloatingWindow() == null,
                "Disabled tear-off should snap the pane back into the tree");

        // Единственная панель, отпущенная над собой: нечего отрывать.
        DockingRoot solo = new DockingRoot();
        solo.setUiContextInternal(context);
        solo.addDocument("only", "Only", content("Only body"));
        solo.selectPane("only");
        layout(solo, 320.0f, 160.0f);
        context.routedEvents().dispatch(new PointerPressedEvent(
                solo, 8.0f, 6.0f, 8.0f, 6.0f, 0, PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerMovedEvent(solo, 160.0f, 80.0f, 160.0f, 80.0f, 0));
        context.routedEvents().dispatch(new PointerReleasedEvent(
                solo, 160.0f, 80.0f, 160.0f, 80.0f, 0, PointerButton.PRIMARY));
        expect(solo.manager().containsPane("only") && solo.lastFloatingWindow() == null,
                "Dropping the only pane over its own leaf should keep it docked");
    }

    private void testFloatingDropDetachesPaneAndClearsPreview() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.allowFloatingOutsideHost(true);
        root.addDocument("floating", "Floating", content("Floating body"));
        layout(root, 220.0f, 120.0f);

        context.routedEvents().dispatch(new PointerPressedEvent(
                root, 8.0f, 6.0f, 8.0f, 6.0f, 0, PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerMovedEvent(root, 260.0f, 48.0f, 260.0f, 48.0f, 0));
        expect(root.dockDropPreview().floating(), "Dragging outside the dock root should produce a FLOAT intent");

        context.routedEvents().dispatch(new PointerReleasedEvent(
                root, 260.0f, 48.0f, 260.0f, 48.0f, 0, PointerButton.PRIMARY));

        expect(root.manager().paneCount() == 0
                        && root.lastFloatingWindow() != null
                        && root.lastFloatingWindow().opened()
                        && root.lastFloatingWindow().content() != null
                        && !root.dockDropPreview().valid()
                        && context.capturedPointer(0) == null,
                "FLOAT drop should detach the pane, open a floating bridge window and clear preview/capture state");
    }

    private void testDropOutsideHostTearsOffWithoutPreview() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("safe", "Safe", content("Safe body"));
        layout(root, 220.0f, 120.0f);

        context.routedEvents().dispatch(new PointerPressedEvent(
                root, 8.0f, 6.0f, 8.0f, 6.0f, 0, PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerMovedEvent(root, 260.0f, 48.0f, 260.0f, 48.0f, 0));
        expect(!root.dockDropPreview().valid(),
                "Dragging outside the floating host should not expose a FLOAT drop preview by default");
        context.routedEvents().dispatch(new PointerReleasedEvent(
                root, 260.0f, 48.0f, 260.0f, 48.0f, 0, PointerButton.PRIMARY));

        expect(root.manager().paneCount() == 0
                        && !root.manager().containsPane("safe")
                        && root.lastFloatingWindow() != null
                        && root.lastFloatingWindow().opened(),
                "Dropping outside the host should tear the pane off into a floating window");
    }

    private void testFloatingDropInsideOverlayCreatesNonCloseableRedockableWindow() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.floatingWindowsRedockLocked(true);
        root.layout(style -> style.size(180.0f, 110.0f).flexGrow(0).flexShrink(0.0f));
        root.addDocument("floatable", "Floatable", content("Floatable body"))
                .addDocument("anchor", "Anchor", content("Anchor body"))
                .selectPane("floatable");
        StackPanel host = new StackPanel();
        host.addChild(root);
        OverlayLayer layer = new OverlayLayer(host);
        layer.setUiContextInternal(context);
        layout(layer, 360.0f, 220.0f);

        context.routedEvents().dispatch(new PointerPressedEvent(
                root, 8.0f, 6.0f, 8.0f, 6.0f, 1, PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerMovedEvent(root, 240.0f, 54.0f, 240.0f, 54.0f, 1));
        layer.applyQueuedMutations();

        WindowWidget floating = root.lastFloatingWindow();
        expect(root.manager().paneCount() == 1
                        && root.manager().containsPane("anchor")
                        && !root.manager().containsPane("floatable")
                        && floating != null
                        && floating.parent() == layer
                        && floating.opened()
                        && floating.dragging()
                        && context.capturedPointer(1) == floating
                        && !floating.closeButtonVisible()
                        && floating.constrainToHost()
                        && floating.dockRedockLocked(),
                "Strip exit should live tear-off into a dragging overlay window and inherit redock lock");

        layout(layer, 360.0f, 220.0f);
        floating.position(10.0f, 10.0f);
        context.routedEvents().dispatch(new WindowMovedEvent(floating, 200.0f, 54.0f, 10.0f, 10.0f));
        DrawList suppressedDrawList = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(suppressedDrawList));
        expect(!hasFloatingPreview(suppressedDrawList),
                "dockRedockLocked(true) should suppress floating dock drop previews");

        context.routedEvents().dispatch(new WindowMoveEndedEvent(floating, 10.0f, 10.0f));
        layer.applyQueuedMutations();
        expect(root.manager().containsPane("anchor")
                        && !root.manager().containsPane("floatable")
                        && floating.opened(),
                "dockRedockLocked(true) should keep a floating dock window detached on release over a DockingRoot");

        root.floatingWindowsRedockLocked(false);
        expect(!floating.dockRedockLocked(),
                "floatingWindowsRedockLocked(false) should update already-created floating dock windows");
        // Дальше жест идёт окном: наведение на центральный диамант даёт таб-оффер.
        float[] center = diamondCenter(root, DockArea.CENTER);
        context.routedEvents().dispatch(new PointerMovedEvent(floating, center[0], center[1], center[0], center[1], 1));
        DrawList previewDrawList = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(previewDrawList));
        expect(hasFloatingPreview(previewDrawList),
                "Moving a floating dock window over a DockingRoot should show a live dock drop preview");

        context.routedEvents().dispatch(new PointerReleasedEvent(
                floating, center[0], center[1], center[0], center[1], 1, PointerButton.PRIMARY));
        layer.applyQueuedMutations();
        DrawList redockedDrawList = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(redockedDrawList));

        expect(root.manager().containsPane("floatable")
                        && !floating.opened()
                        && root.lastFloatingWindow() == null
                        && !hasFloatingPreview(redockedDrawList),
                "Moving the floating dock window back over a DockingRoot should redock the original pane and clear preview");
    }

    private void testDockableWindowDragDocksIntoRoot() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.layout(style -> style.size(180.0f, 110.0f).flexGrow(0).flexShrink(0.0f));
        root.addDocument("anchor", "Anchor", content("Anchor body"));
        StackPanel host = new StackPanel();
        host.addChild(root);
        OverlayLayer layer = new OverlayLayer(host);
        layer.setUiContextInternal(context);
        layout(layer, 360.0f, 220.0f);

        // Флаг выключен по умолчанию: окно остаётся свободным.
        WindowWidget plain = new WindowWidget("Plain", content("Plain body"))
                .position(10.0f, 10.0f)
                .open();
        plain.layout(style -> style.size(120.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(plain);
        layout(layer, 360.0f, 220.0f);
        expect(!plain.dockable() && plain.dockPaneId().isEmpty(),
                "WindowWidget should be non-dockable by default");
        DockingRoot.previewForeignWindowDrop(plain, 60.0f, 60.0f);
        DrawList noPreview = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(noPreview));
        expect(!hasFloatingPreview(noPreview),
                "Non-dockable window drag should not publish a dock drop preview");
        expect(!DockingRoot.finishForeignWindowDrop(plain, 60.0f, 60.0f)
                        && root.manager().paneCount() == 1
                        && plain.opened()
                        && plain.content() != null,
                "Non-dockable window release should keep the window floating");
        layer.removeOverlay(plain);

        // Включаем флаг: preview над root и докинг на release.
        WindowWidget dockable = new WindowWidget("Palette", content("Palette body"))
                .position(200.0f, 120.0f)
                .dockable(true)
                .dockPaneId("palette")
                .open();
        dockable.layout(style -> style.size(120.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(dockable);
        layout(layer, 360.0f, 220.0f);
        expect(dockable.dockable()
                        && dockable.dockPaneId().equals("palette")
                        && dockable.dockKind() == dev.sixik.unigui.widgets.docking.DockPaneKind.TOOL,
                "dockable(true) should expose pane id and default tool kind");

        float dropX = root.layoutBounds().x() + root.layoutBounds().width() * 0.5f;
        float dropY = root.layoutBounds().y() + root.layoutBounds().height() * 0.5f;
        DockingRoot.previewForeignWindowDrop(dockable, dropX, dropY);
        DrawList previewDrawList = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(previewDrawList));
        expect(hasFloatingPreview(previewDrawList),
                "Dragging a dockable window over a DockingRoot should show a dock drop preview");
        expect(DockingRoot.finishForeignWindowDrop(dockable, dropX, dropY),
                "Releasing a dockable window over a DockingRoot should dock it");
        layer.applyQueuedMutations();
        layout(layer, 360.0f, 220.0f);

        expect(root.manager().containsPane("palette")
                        && !dockable.opened()
                        && dockable.content() == null,
                "Docked window content should become a pane and the window should close");

        // Залоченное окно не докируется даже с включённым флагом.
        WindowWidget locked = new WindowWidget("Locked", content("Locked body"))
                .position(200.0f, 120.0f)
                .dockable(true)
                .dockPaneId("locked-pane")
                .dockRedockLocked(true)
                .open();
        locked.layout(style -> style.size(120.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(locked);
        layout(layer, 360.0f, 220.0f);
        DockingRoot.previewForeignWindowDrop(locked, dropX, dropY);
        DrawList lockedDrawList = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(lockedDrawList));
        expect(!hasFloatingPreview(lockedDrawList),
                "dockRedockLocked(true) should suppress dockable window drop previews");
        expect(!DockingRoot.finishForeignWindowDrop(locked, dropX, dropY)
                        && !root.manager().containsPane("locked-pane")
                        && locked.opened()
                        && locked.content() != null,
                "dockRedockLocked(true) should keep a dockable window floating on release");

        // Полный цикл через routed drag: press в header, move над root, release.
        WindowWidget dragged = new WindowWidget("Dragged", content("Dragged body"))
                .position(220.0f, 130.0f)
                .dockable(true)
                .dockPaneId("dragged-pane")
                .open();
        dragged.layout(style -> style.size(120.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(dragged);
        layout(layer, 360.0f, 220.0f);
        context.routedEvents().dispatch(new PointerPressedEvent(
                dragged,
                dragged.layoutBounds().x() + 12.0f,
                dragged.layoutBounds().y() + 6.0f,
                12.0f, 6.0f, 31, PointerButton.PRIMARY));
        expect(dragged.dragging(), "Header press should start window dragging");
        context.routedEvents().dispatch(new PointerMovedEvent(dragged, dropX, dropY, dropX, dropY, 31));
        DrawList dragPreview = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(dragPreview));
        expect(hasFloatingPreview(dragPreview),
                "Routed drag of a dockable window over a DockingRoot should show a preview");
        context.routedEvents().dispatch(new PointerReleasedEvent(
                dragged, dropX, dropY, dropX, dropY, 31, PointerButton.PRIMARY));
        layer.applyQueuedMutations();
        layout(layer, 360.0f, 220.0f);
        expect(root.manager().containsPane("dragged-pane")
                        && !dragged.opened(),
                "Routed drag release over a DockingRoot should dock the window");
    }

    private void testWindowDockingAreaHostsAndMerges() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new StackPanel());
        layer.setUiContextInternal(context);

        // Флаг выключен по умолчанию.
        WindowWidget plain = new WindowWidget("Plain", content("Plain body")).open();
        plain.layout(style -> style.size(200.0f, 140.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(plain);
        layout(layer, 640.0f, 400.0f);
        expect(!plain.dockingEnabled() && plain.dockingRoot() == null,
                "WindowWidget should not host a docking area by default");

        // Включение оборачивает контент в панель внутреннего root.
        WindowWidget host = new WindowWidget("Host", content("Host body"))
                .position(20.0f, 20.0f)
                .dockable(true)
                .dockingEnabled(true)
                .open();
        host.layout(style -> style.size(260.0f, 180.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(host);
        layout(layer, 640.0f, 400.0f);
        expect(host.dockingEnabled()
                        && host.dockingRoot() != null
                        && host.content() == host.dockingRoot()
                        && host.dockingRoot().manager().paneCount() == 1
                        && host.dockingRoot().acceptsDockDrops(),
                "dockingEnabled(true) should wrap window content into an internal docking pane");

        host.dockTool("extra", "Extra", content("Extra body"));
        layout(layer, 640.0f, 400.0f);
        expect(host.dockingRoot().manager().paneCount() == 2
                        && host.dockingRoot().manager().containsPane("extra"),
                "dockTool should add a pane to the window-hosted docking area");

        // Дроп обычного dockable-окна на окно-хост добавляет панель внутрь.
        WindowWidget guest = new WindowWidget("Guest", content("Guest body"))
                .position(420.0f, 60.0f)
                .dockable(true)
                .dockPaneId("guest-pane")
                .open();
        guest.layout(style -> style.size(200.0f, 120.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(guest);
        layout(layer, 640.0f, 400.0f);
        float hostCenterX = host.layoutBounds().x() + host.layoutBounds().width() * 0.5f;
        float hostCenterY = host.layoutBounds().y() + host.layoutBounds().height() * 0.5f;
        DockingRoot.previewForeignWindowDrop(guest, hostCenterX, hostCenterY);
        DrawList hostPreview = new DrawList();
        host.dockingRoot().render(new dev.sixik.unigui.impl.render.DefaultRenderContext(hostPreview));
        expect(hasFloatingPreview(hostPreview),
                "Dragging a dockable window over a window-host should preview inside the host");
        expect(DockingRoot.finishForeignWindowDrop(guest, hostCenterX, hostCenterY)
                        && host.dockingRoot().manager().containsPane("guest-pane")
                        && !guest.opened(),
                "Dropping a window on a window-host should dock it as an inner pane");

        // Дроп окна-хоста на другое окно-хост мёржит все панели.
        WindowWidget secondHost = new WindowWidget("Second", content("Second body"))
                .position(330.0f, 220.0f)
                .dockable(true)
                .dockingEnabled(true)
                .open();
        secondHost.layout(style -> style.size(240.0f, 160.0f).flexGrow(0).flexShrink(0.0f));
        secondHost.dockTool("second-extra", "Second Extra", content("Second extra"));
        layer.addOverlay(secondHost);
        layout(layer, 640.0f, 400.0f);
        int hostPanesBefore = host.dockingRoot().manager().paneCount();
        int secondPanes = secondHost.dockingRoot().manager().paneCount();
        expect(secondPanes == 2, "second host should own two panes before merge");
        secondHost.layout(style -> style.size(240.0f, 160.0f).flexGrow(0).flexShrink(0.0f));
        layout(layer, 640.0f, 400.0f);
        float secondCenterX = secondHost.layoutBounds().x() + secondHost.layoutBounds().width() * 0.5f;
        float secondCenterY = secondHost.layoutBounds().y() + secondHost.layoutBounds().height() * 0.5f;
        // Тащим host на secondHost: ищем цель от лица host как источника.
        DockingRoot.previewForeignWindowDrop(host, secondCenterX, secondCenterY);
        expect(DockingRoot.finishForeignWindowDrop(host, secondCenterX, secondCenterY)
                        && !host.opened()
                        && secondHost.dockingRoot().manager().paneCount() == hostPanesBefore + secondPanes,
                "Dropping a window-host on another host should merge all panes and close the source");

        // Выключение флага с одной панелью разворачивает контент обратно.
        WindowWidget single = new WindowWidget("Single", content("Single body"))
                .dockingEnabled(true)
                .open();
        single.layout(style -> style.size(200.0f, 120.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(single);
        layout(layer, 640.0f, 400.0f);
        Widget wrapped = single.dockingRoot().manager().panes().get(0).content();
        single.dockingEnabled(false);
        layout(layer, 640.0f, 400.0f);
        expect(!single.dockingEnabled()
                        && single.content() == wrapped
                        && !single.dockingRoot().acceptsDockDrops(),
                "Disabling docking with a single pane should unwrap content and block drops");

        layer.removeOverlay(plain);
        layer.removeOverlay(host);
        layer.removeOverlay(secondHost);
        layer.removeOverlay(single);
    }

    private void testOverflowTabsScrollAndMenuSelection() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        for (int i = 0; i < 12; i++) {
            root.addDocument("doc-" + i, "Doc " + i, content("Doc " + i + " body"));
        }
        root.selectPane("doc-0");
        layout(root, 124.0f, 180.0f);
        float tabHeight = root.tabHeight();
        float buttonX = root.layoutBounds().x() + root.layoutBounds().width() - 24.0f + 12.0f;
        float buttonY = root.layoutBounds().y() + Math.min(tabHeight, root.layoutBounds().height()) * 0.5f;
        float menuX = Math.min(buttonX - 12.0f, root.layoutBounds().x()
                + Math.max(0.0f, root.layoutBounds().width() - 168.0f)) + 8.0f;
        float menuTop = Math.min(buttonY - 11.0f + 22.0f,
                root.layoutBounds().y() + Math.max(0.0f, root.layoutBounds().height() - 20.0f * 8.0f));
        float menuDoc0Y = menuTop + 10.0f;

        DrawList initialDrawList = renderRoot(root);
        expect(hasOverflowDots(initialDrawList)
                        && docTitles(initialDrawList).contains("Doc 0")
                        && !docTitles(initialDrawList).contains("Doc 11"),
                "Overflowing dock tabs should render an overflow button and a clipped visible range");

        for (int i = 0; i < 16; i++) {
            context.routedEvents().dispatch(new ScrollEvent(root, 8.0f, 6.0f, 8.0f, 6.0f, 0.0f, 1.0f));
        }
        expect(docTitles(renderRoot(root)).contains("Doc 0"),
                "Mouse wheel over an overflowing tab strip should be able to scroll back to the first tab");

        context.routedEvents().dispatch(new ScrollEvent(root, 8.0f, 6.0f, 8.0f, 6.0f, 0.0f, -1.0f));
        java.util.Set<String> scrolledTitles = docTitles(renderRoot(root));
        expect(!scrolledTitles.contains("Doc 0"),
                "Mouse wheel over an overflowing tab strip should scroll the visible tab range");

        java.util.Set<String> titlesBeforeMenuSelection = docTitles(renderRoot(root));
        context.routedEvents().dispatch(new PointerPressedEvent(root, buttonX, buttonY, buttonX, buttonY, 3, PointerButton.PRIMARY));
        expect(hasOverflowMenu(renderRoot(root)),
                "Clicking the overflow button should open the tab selection menu");

        DockPane selectedBeforeMenuInteraction = root.manager().selectedPane();
        context.routedEvents().dispatch(new PointerMovedEvent(
                selectedBeforeMenuInteraction.content(), menuX, menuDoc0Y, menuX, menuDoc0Y, 7));
        DrawList hoverDrawList = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(hoverDrawList));
        expect(countCommands(hoverDrawList, DrawCommandType.RECT) >= 3,
                "Hovering an overflow menu row over dock content should render a visible row highlight");

        context.routedEvents().dispatch(new ScrollEvent(selectedBeforeMenuInteraction.content(), menuX, menuDoc0Y, menuX, menuDoc0Y, 0.0f, -1.0f));
        context.routedEvents().dispatch(new ScrollEvent(selectedBeforeMenuInteraction.content(), menuX, menuDoc0Y, menuX, menuDoc0Y, 0.0f, -1.0f));
        context.routedEvents().dispatch(new PointerPressedEvent(selectedBeforeMenuInteraction.content(), menuX, menuDoc0Y, menuX, menuDoc0Y, 4, PointerButton.PRIMARY));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));
        expect(root.manager().selectedPane().id().equals("doc-2")
                        && !hasOverflowMenu(renderRoot(root))
                        && docTitles(renderRoot(root)).equals(titlesBeforeMenuSelection),
                "Selecting a scrolled tab from the overflow menu should activate it, close the menu and preserve tab strip scroll");

        context.routedEvents().dispatch(new PointerPressedEvent(root, buttonX, buttonY, buttonX, buttonY, 8, PointerButton.PRIMARY));
        expect(hasOverflowMenu(renderRoot(root)), "Overflow menu should reopen from the overflow button");
        context.routedEvents().dispatch(new PointerPressedEvent(root, 4.0f, root.layoutBounds().height() - 4.0f,
                4.0f, root.layoutBounds().height() - 4.0f, 9, PointerButton.PRIMARY));
        expect(!hasOverflowMenu(renderRoot(root)),
                "Clicking anywhere outside the overflow menu should close it");

        java.util.Set<String> titlesBeforeClick = docTitles(renderRoot(root));
        String firstTitle = titlesBeforeClick.iterator().next();
        float[] titleCenter = textCenter(renderRoot(root), firstTitle);
        context.routedEvents().dispatch(new PointerPressedEvent(root, titleCenter[0], titleCenter[1], titleCenter[0], titleCenter[1], 10, PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerReleasedEvent(root, titleCenter[0], titleCenter[1], titleCenter[0], titleCenter[1], 10, PointerButton.PRIMARY));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));
        expect(root.manager().selectedPane().id().equals("doc-" + firstTitle.substring(4))
                        && docTitles(renderRoot(root)).equals(titlesBeforeClick),
                "Selecting a visible overflow tab should activate it without resetting the tab strip scroll offset");
    }

    private static DrawList renderRoot(DockingRoot root) {
        DrawList drawList = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(drawList));
        return drawList;
    }

    private static boolean hasOverflowDots(DrawList drawList) {
        return countFillColor(drawList, 0.28f, 0.78f, 1.0f, 0.85f) >= 3;
    }

    private static boolean hasOverflowMenu(DrawList drawList) {
        return countFillColor(drawList, 0.045f, 0.052f, 0.070f, 0.98f) >= 1;
    }

    private static boolean hasFloatingPreview(DrawList drawList) {
        return hasDropFill(drawList);
    }

    private static boolean hasDropFill(DrawList drawList) {
        for (DrawCommand command : drawList.commands()) {
            if ((command.type() == DrawCommandType.ROUNDED_RECT || command.type() == DrawCommandType.RECT)
                    && command.paint() != null
                    && !command.paint().isStroke()
                    && almostEqual(command.paint().color().r(), 0.18f)
                    && almostEqual(command.paint().color().g(), 0.56f)
                    && almostEqual(command.paint().color().b(), 1.0f)
                    && almostEqual(command.paint().color().a(), 0.22f)) {
                return true;
            }
        }
        return false;
    }

    private static int countFillColor(DrawList drawList, float r, float g, float b, float a) {
        int count = 0;
        for (DrawCommand command : drawList.commands()) {
            if (command.paint() != null
                    && !command.paint().isStroke()
                    && almostEqual(command.paint().color().r(), r)
                    && almostEqual(command.paint().color().g(), g)
                    && almostEqual(command.paint().color().b(), b)
                    && almostEqual(command.paint().color().a(), a)) {
                count++;
            }
        }
        return count;
    }

    private static java.util.Set<String> docTitles(DrawList drawList) {
        java.util.Set<String> titles = new java.util.LinkedHashSet<>();
        for (DrawCommand command : drawList.commands()) {
            if (command.type() != DrawCommandType.TEXT) continue;
            String text = command.richText() != null ? command.richText().plainText() : command.text();
            if (text != null && text.matches("Doc \\d+")) {
                titles.add(text);
            }
        }
        return titles;
    }

    private static boolean textPresent(DrawList drawList, String text) {
        return docTitles(drawList).contains(text);
    }

    private static float[] textCenter(DrawList drawList, String text) {
        for (DrawCommand command : drawList.commands()) {
            if (command.type() != DrawCommandType.TEXT) continue;
            String rendered = command.richText() != null ? command.richText().plainText() : command.text();
            if (text.equals(rendered)) {
                return new float[]{
                        command.bounds().x() + command.bounds().width() * 0.5f,
                        command.bounds().y() + command.bounds().height() * 0.5f};
            }
        }
        throw new AssertionError("Text '" + text + "' not found in draw list");
    }

    private void testTinySplitLayoutDoesNotProduceNegativeBounds() {
        DockingRoot root = new DockingRoot();
        DockPane left = new DockPane("left", "Left", content("Left body"));
        DockPane right = new DockPane("right", "Right", content("Right body"));
        root.addPane(left)
                .splitPane(left.id(), DockArea.RIGHT, right)
                .splitHandleThickness(64.0f);

        layout(root, 20.0f, 10.0f);

        expect(left.content().layoutBounds().width() >= 0.0f
                        && left.content().layoutBounds().height() >= 0.0f
                        && right.content().layoutBounds().width() >= 0.0f
                        && right.content().layoutBounds().height() >= 0.0f,
                "DockingRoot split layout should keep content bounds non-negative in tiny hosts");
    }

    private void testDockPaneContentIsClippedToLeafContentBounds() {
        DockingRoot root = new DockingRoot();
        root.addDocument("canvas", "Canvas", content("Content marker"))
                .addToolPane("inspector", "Inspector", content("Inspector body"), DockArea.RIGHT)
                .selectPane("canvas");
        layout(root, 320.0f, 160.0f);

        DrawList drawList = new DrawList();
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(drawList));

        DockPane canvas = root.manager().findPane("canvas");
        expect(canvas != null && canvas.content() != null,
                "DockingRoot test setup should keep the canvas pane registered");
        expect(countCommands(drawList, DrawCommandType.PUSH_CLIP) == countCommands(drawList, DrawCommandType.POP_CLIP),
                "DockingRoot should balance clip push/pop commands while rendering");
        expect(textDrawnInsideClip(drawList, "Content marker", canvas.content().layoutBounds()),
                "DockingRoot should clip selected pane content to its leaf content bounds");
    }

    private static Label content(String text) {
        Label label = new Label(text);
        label.layout(style -> style.size(90.0f, 18.0f).flexGrow(1).flexShrink(1.0f));
        return label;
    }

    private static void press(DefaultUIContext context, Widget target, float x, float y, int pointerId) {
        context.routedEvents().dispatch(new PointerPressedEvent(
                target, x, y, x, y, pointerId, PointerButton.PRIMARY));
    }

    private static void move(DefaultUIContext context, Widget target, float x, float y, int pointerId) {
        context.routedEvents().dispatch(new PointerMovedEvent(target, x, y, x, y, pointerId));
    }

    private static void release(DefaultUIContext context, Widget target, float x, float y, int pointerId) {
        context.routedEvents().dispatch(new PointerReleasedEvent(
                target, x, y, x, y, pointerId, PointerButton.PRIMARY));
    }

    private static java.util.List<String> tabOrder(DockingRoot root) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (DockPane pane : root.manager().panes()) {
            ids.add(pane.id());
        }
        return ids;
    }

    /**
     * Bounds первого залитого DROP_FILL-прямоугольника (превью дропа)
     * или {@code null}, если превью нет.
     */
    private static MutableRect dropFillRect(DrawList drawList) {
        for (DrawCommand command : drawList.commands()) {
            if ((command.type() == DrawCommandType.ROUNDED_RECT || command.type() == DrawCommandType.RECT)
                    && command.paint() != null
                    && !command.paint().isStroke()
                    && almostEqual(command.paint().color().r(), 0.18f)
                    && almostEqual(command.paint().color().g(), 0.56f)
                    && almostEqual(command.paint().color().b(), 1.0f)
                    && almostEqual(command.paint().color().a(), 0.22f)) {
                dev.sixik.unigui.api.math.RectView bounds = command.bounds();
                return new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            }
        }
        return null;
    }

    private static boolean nearRect(dev.sixik.unigui.api.math.RectView actual,
            float x, float y, float width, float height) {
        return actual != null
                && Math.abs(actual.x() - x) <= 1.5f
                && Math.abs(actual.y() - y) <= 1.5f
                && Math.abs(actual.width() - width) <= 1.5f
                && Math.abs(actual.height() - height) <= 1.5f;
    }

    private static void layout(DockingRoot root, float width, float height) {
        root.measure(new LayoutContext(width, height));
        root.arrange(new MutableRect(0.0f, 0.0f, width, height));
    }

    /**
     * Возвращает центр диаманта креста для single-leaf root.
     *
     * <p>Фикстура обязана держать все панели в одном листе (= bounds root):
     * тогда геометрия публичного {@code crossCellRect} точна без гаданий.</p>
     */
    private static float[] diamondCenter(DockingRoot root, DockArea area) {
        dev.sixik.unigui.api.math.RectView bounds = root.layoutBounds();
        dev.sixik.unigui.api.math.MutableRect cell = DockingRoot.crossCellRect(bounds, area);
        return new float[]{cell.x() + cell.width() * 0.5f, cell.y() + cell.height() * 0.5f};
    }

    private static void layout(OverlayLayer layer, float width, float height) {
        layer.measure(new LayoutContext(width, height));
        layer.arrange(new MutableRect(0.0f, 0.0f, width, height));
    }

    /**
     * Фикстура «root в оверлее»: root фиксированного размера в левом верхнем
     * углу слоя. Нужна жестам окно↔док: tear-off складывает окно в слой,
     * а foreign-превью ищет цель от окна вверх до слоя.
     *
     * @return overlay-слой (уже измеренный и разложенный)
     */
    private static OverlayLayer overlayFixture(DefaultUIContext context, DockingRoot root,
            float rootWidth, float rootHeight, float layerWidth, float layerHeight) {
        root.layout(style -> style.size(rootWidth, rootHeight).flexGrow(0).flexShrink(0.0f));
        StackPanel host = new StackPanel();
        host.addChild(root);
        OverlayLayer layer = new OverlayLayer(host);
        layer.setUiContextInternal(context);
        layout(layer, layerWidth, layerHeight);
        return layer;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static long countCommands(DrawList drawList, DrawCommandType type) {
        return drawList.commands().stream().filter(command -> command.type() == type).count();
    }

    private static boolean textDrawnInsideClip(DrawList drawList, String text, dev.sixik.unigui.api.math.RectView clip) {
        java.util.ArrayList<Boolean> stack = new java.util.ArrayList<>();
        for (DrawCommand command : drawList.commands()) {
            if (command.type() == DrawCommandType.PUSH_CLIP) {
                stack.add(boundsEqual(command.bounds(), clip));
            } else if (command.type() == DrawCommandType.POP_CLIP) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            } else if (command.type() == DrawCommandType.TEXT && text.equals(command.text())) {
                return stack.contains(Boolean.TRUE);
            }
        }
        return false;
    }

    private static boolean boundsEqual(dev.sixik.unigui.api.math.RectView a, dev.sixik.unigui.api.math.RectView b) {
        return almostEqual(a.x(), b.x())
                && almostEqual(a.y(), b.y())
                && almostEqual(a.width(), b.width())
                && almostEqual(a.height(), b.height());
    }

    private static boolean almostEqual(float a, float b) {
        return Math.abs(a - b) <= 0.01f;
    }

    private static final class Counter {
        private int started;
        private int moved;
        private int ended;
        private int previewChanges;
        private boolean dropped;
        private DockArea lastArea = DockArea.CENTER;
    }
}
