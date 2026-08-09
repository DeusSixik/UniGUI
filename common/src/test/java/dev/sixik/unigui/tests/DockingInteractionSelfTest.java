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
import dev.sixik.unigui.widgets.DockArea;
import dev.sixik.unigui.widgets.DockDropIntent;
import dev.sixik.unigui.widgets.DockPane;
import dev.sixik.unigui.widgets.DockingRoot;
import dev.sixik.unigui.widgets.Label;
import dev.sixik.unigui.widgets.OverlayLayer;
import dev.sixik.unigui.widgets.StackPanel;
import dev.sixik.unigui.widgets.WindowWidget;
import dev.sixik.unigui.widgets.render.DockDropPreviewState;
import dev.sixik.unigui.widgets.render.DockPaneState;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;

public final class DockingInteractionSelfTest {
    public static void main(String[] args) {
        new DockingInteractionSelfTest().run();
    }

    private void run() {
        testDragThresholdPreviewAndEdgeDropMapping();
        testFloatingDropDetachesPaneAndClearsPreview();
        testFloatingDropOutsideHostIsIgnoredByDefault();
        testFloatingDropInsideOverlayCreatesNonCloseableRedockableWindow();
        testOverflowTabsScrollAndMenuSelection();
        testDockPaneContentIsClippedToLeafContentBounds();
        testTinySplitLayoutDoesNotProduceNegativeBounds();
        System.out.println("DockingInteractionSelfTest passed");
    }

    private void testDragThresholdPreviewAndEdgeDropMapping() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        Counter drag = new Counter();
        root.onDragStarted(event -> drag.started++);
        root.onDragMoved(event -> {
            drag.moved++;
            drag.lastArea = event.intent().area();
        });
        root.onDragEnded(event -> {
            drag.ended++;
            drag.dropped = event.dropped();
            drag.lastArea = event.intent().area();
        });
        root.onDropPreviewChanged(event -> drag.previewChanges++);

        root.addDocument("scene", "Scene", content("Scene body"))
                .addToolPane("inspector", "Inspector", content("Inspector body"), DockArea.RIGHT)
                .selectPane("scene");
        layout(root, 320.0f, 160.0f);

        context.routedEvents().dispatch(new PointerPressedEvent(
                root, 8.0f, 6.0f, 8.0f, 6.0f, 0, PointerButton.PRIMARY));
        expect(root.dragController().active()
                        && !root.dockDragging()
                        && context.capturedPointer(0) == root,
                "Dock tab press should arm dock dragging and capture the pointer");

        context.routedEvents().dispatch(new PointerMovedEvent(root, 10.0f, 7.0f, 10.0f, 7.0f, 0));
        expect(!root.dockDragging()
                        && !root.dockDropPreview().valid()
                        && drag.started == 0,
                "Dock drag should not start before crossing the threshold");

        DockPane inspector = root.manager().findPane("inspector");
        float targetX = inspector.content().layoutBounds().x() + 1.0f;
        float targetY = inspector.content().layoutBounds().y() + 8.0f;
        context.routedEvents().dispatch(new PointerMovedEvent(root, targetX, targetY, targetX, targetY, 0));

        DockDropIntent preview = root.dockDropPreview();
        expect(root.dockDragging()
                        && preview.valid()
                        && preview.targetPaneId().equals("inspector")
                        && preview.area() == DockArea.LEFT
                        && drag.started == 1
                        && drag.moved >= 1,
                "Cross-threshold drag over a target edge should publish a LEFT drop preview");

        context.routedEvents().dispatch(new PointerReleasedEvent(
                root, targetX, targetY, targetX, targetY, 0, PointerButton.PRIMARY));

        expect(!root.dragController().active()
                        && context.capturedPointer(0) == null
                        && drag.ended == 1
                        && drag.dropped
                        && drag.lastArea == DockArea.LEFT
                        && !root.dockDropPreview().valid()
                        && root.rootNode().isSplit()
                        && root.rootNode().first().findPane("scene") != null
                        && root.rootNode().second().findPane("inspector") != null,
                "Dropping on a target edge should apply the mapped split operation and clear drag state");
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

    private void testFloatingDropOutsideHostIsIgnoredByDefault() {
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

        expect(root.manager().paneCount() == 1
                        && root.manager().containsPane("safe")
                        && root.lastFloatingWindow() == null,
                "Dropping outside the floating host should keep the pane docked");
    }

    private void testFloatingDropInsideOverlayCreatesNonCloseableRedockableWindow() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        final DockDropPreviewState[] floatingPreview = new DockDropPreviewState[1];
        root.dropPreviewRenderer((draw, state) -> floatingPreview[0] = state);
        root.floatingWindowsRedockLocked(true);
        root.preferredSize(180.0f, 110.0f).grow(0.0f);
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
        expect(root.dockDropPreview().floating(),
                "Dragging outside the dock root but inside the overlay host should produce a FLOAT preview");
        context.routedEvents().dispatch(new PointerReleasedEvent(
                root, 240.0f, 54.0f, 240.0f, 54.0f, 1, PointerButton.PRIMARY));
        layer.applyQueuedMutations();

        WindowWidget floating = root.lastFloatingWindow();
        expect(root.manager().paneCount() == 1
                        && root.manager().containsPane("anchor")
                        && floating != null
                        && floating.parent() == layer
                        && floating.opened()
                        && !floating.closeButtonVisible()
                        && floating.constrainToHost()
                        && floating.dockRedockLocked(),
                "FLOAT drop inside an overlay should create a non-closeable constrained floating dock window and inherit redock lock");

        layout(layer, 360.0f, 220.0f);
        floating.position(10.0f, 10.0f);
        floatingPreview[0] = null;
        context.routedEvents().dispatch(new WindowMovedEvent(floating, 200.0f, 54.0f, 10.0f, 10.0f));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new DrawList()));
        expect(floatingPreview[0] == null,
                "dockRedockLocked(true) should suppress floating dock drop previews");

        context.routedEvents().dispatch(new WindowMoveEndedEvent(floating, 10.0f, 10.0f));
        layer.applyQueuedMutations();
        expect(root.manager().containsPane("anchor")
                        && !root.manager().containsPane("floatable")
                        && floating.opened(),
                "dockRedockLocked(true) should keep a floating dock window detached on release over a DockingRoot");

        floating.dockRedockLocked(false);
        floatingPreview[0] = null;
        context.routedEvents().dispatch(new WindowMovedEvent(floating, 200.0f, 54.0f, 10.0f, 10.0f));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new DrawList()));
        expect(floatingPreview[0] != null
                        && floatingPreview[0].visible()
                        && floatingPreview[0].sourcePaneId().equals("floatable"),
                "Moving a floating dock window over a DockingRoot should show a live dock drop preview");

        context.routedEvents().dispatch(new WindowMoveEndedEvent(floating, 10.0f, 10.0f));
        layer.applyQueuedMutations();
        floatingPreview[0] = null;
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new DrawList()));

        expect(root.manager().containsPane("floatable")
                        && !floating.opened()
                        && floatingPreview[0] == null,
                "Moving the floating dock window back over a DockingRoot should redock the original pane and clear preview");
    }

    private void testOverflowTabsScrollAndMenuSelection() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        final DockPaneState[] captured = new DockPaneState[1];
        root.paneRenderer((draw, state) -> captured[0] = state);
        for (int i = 0; i < 12; i++) {
            root.addDocument("doc-" + i, "Doc " + i, content("Doc " + i + " body"));
        }
        root.selectPane("doc-0");
        layout(root, 124.0f, 180.0f);
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));

        expect(captured[0] != null
                        && captured[0].overflow()
                        && captured[0].overflowButtonWidth() > 0.0f
                        && captured[0].lastVisibleTab() - captured[0].firstVisibleTab() + 1 < captured[0].tabs().size(),
                "Overflowing dock tabs should expose a dedicated overflow button and clipped visible range");
        for (int i = captured[0].firstVisibleTab(); i <= captured[0].lastVisibleTab(); i++) {
            expect(captured[0].tabs().get(i).x() + captured[0].tabs().get(i).width() <= captured[0].overflowButtonX() + 0.01f,
                    "Visible tabs should not draw underneath the overflow button");
        }

        for (int i = 0; i < 16; i++) {
            context.routedEvents().dispatch(new ScrollEvent(root, 8.0f, 6.0f, 8.0f, 6.0f, 0.0f, 1.0f));
        }
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));
        expect(captured[0].firstVisibleTab() == 0,
                "Mouse wheel over an overflowing tab strip should be able to scroll back to the first tab");

        int firstBeforeScroll = captured[0].firstVisibleTab();
        context.routedEvents().dispatch(new ScrollEvent(root, 8.0f, 6.0f, 8.0f, 6.0f, 0.0f, -1.0f));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));
        expect(captured[0].firstVisibleTab() > firstBeforeScroll,
                "Mouse wheel over an overflowing tab strip should scroll the visible tab range");

        float buttonX = captured[0].overflowButtonX() + captured[0].overflowButtonWidth() * 0.5f;
        float buttonY = captured[0].overflowButtonY() + captured[0].overflowButtonHeight() * 0.5f;
        context.routedEvents().dispatch(new PointerPressedEvent(root, buttonX, buttonY, buttonX, buttonY, 3, PointerButton.PRIMARY));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));
        expect(captured[0].overflowMenuOpen(),
                "Clicking the overflow button should open the tab selection menu");
        int tabScrollBeforeMenuSelection = captured[0].firstVisibleTab();

        float menuX = Math.min(captured[0].overflowButtonX(), root.layoutBounds().x() + Math.max(0.0f, root.layoutBounds().width() - 168.0f)) + 8.0f;
        float menuTop = Math.min(captured[0].overflowButtonY() + captured[0].overflowButtonHeight(),
                root.layoutBounds().y() + Math.max(0.0f, root.layoutBounds().height() - 20.0f * 8.0f));
        float menuDoc0Y = menuTop + 10.0f;
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
                        && !captured[0].overflowMenuOpen()
                        && captured[0].firstVisibleTab() == tabScrollBeforeMenuSelection,
                "Selecting a scrolled tab from the overflow menu should activate it, close the menu and preserve tab strip scroll");

        context.routedEvents().dispatch(new PointerPressedEvent(root, buttonX, buttonY, buttonX, buttonY, 8, PointerButton.PRIMARY));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));
        expect(captured[0].overflowMenuOpen(), "Overflow menu should reopen from the overflow button");
        context.routedEvents().dispatch(new PointerPressedEvent(root, 4.0f, root.layoutBounds().height() - 4.0f,
                4.0f, root.layoutBounds().height() - 4.0f, 9, PointerButton.PRIMARY));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));
        expect(!captured[0].overflowMenuOpen(),
                "Clicking anywhere outside the overflow menu should close it");

        int firstBeforeVisibleTabClick = captured[0].firstVisibleTab();
        DockPaneState stateBeforeVisibleTabClick = captured[0];
        int visibleIndex = stateBeforeVisibleTabClick.firstVisibleTab();
        float visibleTabX = stateBeforeVisibleTabClick.tabs().get(visibleIndex).x() + 4.0f;
        float visibleTabY = stateBeforeVisibleTabClick.tabs().get(visibleIndex).y() + 6.0f;
        context.routedEvents().dispatch(new PointerPressedEvent(root, visibleTabX, visibleTabY, visibleTabX, visibleTabY, 10, PointerButton.PRIMARY));
        context.routedEvents().dispatch(new PointerReleasedEvent(root, visibleTabX, visibleTabY, visibleTabX, visibleTabY, 10, PointerButton.PRIMARY));
        root.render(new dev.sixik.unigui.impl.render.DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList()));
        expect(captured[0].firstVisibleTab() == firstBeforeVisibleTabClick,
                "Selecting a visible overflow tab should not reset the tab strip scroll offset");
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
        label.preferredSize(90.0f, 18.0f).grow(1.0f);
        return label;
    }

    private static void layout(DockingRoot root, float width, float height) {
        root.measure(new LayoutContext(width, height));
        root.arrange(new MutableRect(0.0f, 0.0f, width, height));
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
